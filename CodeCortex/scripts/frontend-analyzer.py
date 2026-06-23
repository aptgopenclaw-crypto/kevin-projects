#!/usr/bin/env python3
"""
Phase 2: Frontend Analyzer (Vue 3 + TypeScript)
=================================================
Parses the Vue 3 / TypeScript frontend project and adds frontend knowledge
into the existing SQLite database (knowledge.db).

New tables added:
  frontend_api_functions  — API call functions extracted from src/api/**/*.ts
  frontend_stores         — Pinia stores from src/stores/**/*.ts
  frontend_views          — Vue views from src/views/**/*.vue
  frontend_components     — Vue components from src/components/**/*.vue
  frontend_routes         — Routes from src/router/**/*.ts
  fe_be_bindings          — Frontend API function → backend endpoint linkage

Usage: python scripts/frontend-analyzer.py
"""

import re
import os
import sys
import json
import sqlite3
from pathlib import Path
from collections import defaultdict
from typing import Optional

import tree_sitter_typescript as tsTs
from tree_sitter import Language, Parser

# ── Paths ──────────────────────────────────────────────────────────────────
PROJECT_ROOT = Path(__file__).resolve().parent.parent
FRONTEND_SRC = PROJECT_ROOT / "frontend" / "src"
DB_PATH = PROJECT_ROOT / "knowledge.db"

# ── Parsers ────────────────────────────────────────────────────────────────
LANG_TS = Language(tsTs.language_typescript())
LANG_TSX = Language(tsTs.language_tsx())

parser_ts = Parser(LANG_TS)
parser_tsx = Parser(LANG_TSX)

# ── Module mapping: API directory → backend module ─────────────────────────
API_DIR_TO_MODULE = {
    "announcement": "announcement",
    "assetTransfer": "assettransfer",
    "audit": "audit",
    "auth": "auth",
    "authConfig": "auth",
    "dept": "dept",
    "impersonation": "platform",
    "notification": "notification",
    "passwordPolicy": "auth",
    "platformAnnouncement": "platform",
    "rbac": "rbac",
    "setting": "setting",
    "tenant": "tenant",
    "user": "user",
    "workflow": "workflow",
    "axios": "_infra",
}

# views/xxx directory → backend module
VIEW_DIR_TO_MODULE = {
    "announcement": "announcement",
    "assetTransfer": "assettransfer",
    "audit": "audit",
    "login": "auth",
    "tenant": "tenant",
    "user": "user",
    "workflow": "workflow",
    "workflowDelegate": "workflow",
    "admin": "platform",
    "platform": "platform",
    "notification": "notification",
}

# HTTP method patterns in axiosIns calls.
# Use [^(]* to skip over nested generic type params like <unknown, BaseResponse<Page<X>>>
HTTP_METHODS_RE = re.compile(
    r'axiosIns\.(get|post|put|delete|patch|head)[^(]*\(\s*(?:`([^`]*)`|"([^"]*)"|\'([^\']*)\')',
    re.DOTALL,
)

# ── node_text helper ───────────────────────────────────────────────────────

def node_text(node, source: bytes) -> str:
    if node is None:
        return ""
    return source[node.start_byte:node.end_byte].decode("utf-8", errors="replace")


def find_children_of_type(node, type_name: str) -> list:
    return [c for c in node.children if c.type == type_name]


def find_all_descendants(node, type_name: str) -> list:
    results = []
    if node is None:
        return results
    if node.type == type_name:
        results.append(node)
    for child in node.children:
        results.extend(find_all_descendants(child, type_name))
    return results


# ── Vue SFC: extract <script setup> block ─────────────────────────────────

SCRIPT_SETUP_RE = re.compile(
    r'<script\s[^>]*lang=["\']ts["\'][^>]*>(.*?)</script>',
    re.DOTALL,
)
SCRIPT_PLAIN_RE = re.compile(
    r'<script[^>]*>(.*?)</script>',
    re.DOTALL,
)


def extract_script_from_vue(content: str) -> str:
    """Extract TypeScript code from a .vue SFC <script> block."""
    m = SCRIPT_SETUP_RE.search(content)
    if m:
        return m.group(1)
    m = SCRIPT_PLAIN_RE.search(content)
    if m:
        return m.group(1)
    return ""


# ── Parse imports from a TS tree ──────────────────────────────────────────

def parse_ts_imports(root, source: bytes) -> dict:
    """
    Returns:
        {
          'api': ['listAnnouncements', 'getUnreadCount', ...],
          'stores': ['useAnnouncementStore', ...],
          'components': ['RichTextRenderer', ...],
          'raw': ['@/api/announcement', ...],   # all import sources
        }
    """
    result = {"api": [], "stores": [], "components": [], "composables": [], "raw": []}

    for imp_node in find_all_descendants(root, "import_statement"):
        imp_src = ""
        # Find string source
        for child in imp_node.children:
            if child.type == "string":
                imp_src = child.text.decode("utf-8", errors="replace").strip("'\"")
                result["raw"].append(imp_src)
                break

        # Collect named imports
        named = []
        for clause in find_all_descendants(imp_node, "import_clause"):
            # named_imports: { foo, bar }
            for named_node in find_all_descendants(clause, "named_imports"):
                for spec in find_all_descendants(named_node, "import_specifier"):
                    for child in spec.children:
                        if child.type == "identifier":
                            named.append(child.text.decode("utf-8", errors="replace"))
                            break
            # default import: import Foo from '...'
            for child in clause.children:
                if child.type == "identifier":
                    named.append(child.text.decode("utf-8", errors="replace"))

        # Classify by source path
        if "@/api/" in imp_src:
            result["api"].extend(named)
        elif "@/stores/" in imp_src:
            result["stores"].extend(named)
        elif imp_src.endswith(".vue") or "@/components/" in imp_src:
            result["components"].extend(named)
        elif "@/composables/" in imp_src:
            result["composables"].extend(named)

    return result


# ── Parse API call functions (from src/api/**) ────────────────────────────

def _get_api_module(file_path: Path) -> str:
    """Given an api file path, return the backend module name."""
    try:
        rel = file_path.relative_to(FRONTEND_SRC / "api")
        parts = rel.parts
        # parts[0] could be 'announcement', 'index.ts', or 'impersonation.ts'
        dir_part = parts[0].replace(".ts", "")
        return API_DIR_TO_MODULE.get(dir_part, dir_part)
    except ValueError:
        return "_infra"


def parse_api_file(file_path: Path) -> list[dict]:
    """
    Parse an API file and extract exported functions with their HTTP call info.
    Returns list of api_function dicts.
    """
    source = file_path.read_bytes()
    tree = parser_ts.parse(source)
    root = tree.root_node
    module = _get_api_module(file_path)
    rel_path = str(file_path.relative_to(PROJECT_ROOT))
    functions = []

    # Find all export statements with const arrow functions / regular functions
    for export_node in find_all_descendants(root, "export_statement"):
        export_text = node_text(export_node, source)

        # Find declared function names
        func_names = []
        for decl in find_all_descendants(export_node, "variable_declarator"):
            for child in decl.children:
                if child.type == "identifier":
                    func_names.append(child.text.decode("utf-8", errors="replace"))
                    break

        # Also check function declaration exports
        for func_decl in find_all_descendants(export_node, "function_declaration"):
            for child in func_decl.children:
                if child.type == "identifier":
                    func_names.append(child.text.decode("utf-8", errors="replace"))
                    break

        if not func_names:
            continue

        # Extract HTTP calls from this export block
        for m in HTTP_METHODS_RE.finditer(export_text):
            http_method = m.group(1).upper()
            # Capture path from whichever group matched
            raw_path = m.group(2) or m.group(3) or m.group(4) or ""
            # Normalize template literals: `/auth/announcements/${id}` → `/auth/announcements/{id}`
            api_path = re.sub(r'\$\{[^}]+\}', '{id}', raw_path)
            # Normalize prefix: most paths are relative to the axios base (/v1)
            if api_path and not api_path.startswith("/"):
                api_path = "/" + api_path

            for func_name in func_names:
                functions.append({
                    "name": func_name,
                    "http_method": http_method,
                    "api_path": api_path,
                    "module": module,
                    "file_path": rel_path,
                })

        # If there's no HTTP call but a function name, still record it
        if func_names and not HTTP_METHODS_RE.search(export_text):
            for func_name in func_names:
                functions.append({
                    "name": func_name,
                    "http_method": "",
                    "api_path": "",
                    "module": module,
                    "file_path": rel_path,
                })

    return functions


# ── Parse Pinia stores (from src/stores/**) ───────────────────────────────

def _get_view_module(file_path: Path) -> str:
    """Given a view file path, detect its module."""
    try:
        rel = file_path.relative_to(FRONTEND_SRC / "views")
        parts = rel.parts
        dir_part = parts[0] if len(parts) > 1 else ""
        return VIEW_DIR_TO_MODULE.get(dir_part, dir_part or "common")
    except ValueError:
        return "common"


def parse_store_file(file_path: Path) -> Optional[dict]:
    """Parse a Pinia store file and extract key info."""
    content = file_path.read_text(encoding="utf-8")
    source = content.encode("utf-8")
    tree = parser_ts.parse(source)
    root = tree.root_node
    rel_path = str(file_path.relative_to(PROJECT_ROOT))

    # Store name from defineStore('name', ...) call
    store_name = file_path.stem  # e.g. announcementStore

    # Find defineStore call
    define_store_match = re.search(r"defineStore\s*\(\s*['\"]([^'\"]+)['\"]", content)
    store_id = define_store_match.group(1) if define_store_match else store_name

    # Module: from which api modules it imports
    imports = parse_ts_imports(root, source)
    api_calls = imports["api"]

    # Detect backend module from api imports
    module = _detect_module_from_imports(imports["raw"])

    return {
        "name": store_name,
        "store_id": store_id,
        "module": module,
        "file_path": rel_path,
        "api_calls": api_calls,
        "imports_raw": imports["raw"],
    }


def _detect_module_from_imports(raw_imports: list[str]) -> str:
    """Guess backend module based on import paths."""
    for imp in raw_imports:
        if "@/api/" in imp:
            # e.g. '@/api/announcement'
            m = re.search(r"@/api/([^/'\"]+)", imp)
            if m:
                api_dir = m.group(1)
                return API_DIR_TO_MODULE.get(api_dir, api_dir)
    return "common"


# ── Parse Vue views (from src/views/**) ───────────────────────────────────

def parse_vue_file(file_path: Path, is_component: bool = False) -> Optional[dict]:
    """Parse a Vue SFC and extract its script setup content."""
    content = file_path.read_text(encoding="utf-8")
    script_content = extract_script_from_vue(content)

    rel_path = str(file_path.relative_to(PROJECT_ROOT))
    name = file_path.stem  # e.g. AnnouncementListView

    if not script_content:
        return {
            "name": name,
            "file_path": rel_path,
            "module": _get_view_module(file_path) if not is_component else "common",
            "api_calls": [],
            "stores_used": [],
            "components_used": [],
            "composables_used": [],
        }

    source = script_content.encode("utf-8")
    tree = parser_ts.parse(source)
    root = tree.root_node
    imports = parse_ts_imports(root, source)

    # Also find API calls directly (axiosIns calls inside the view itself)
    direct_api_calls = []
    for m in HTTP_METHODS_RE.finditer(script_content):
        api_path = re.sub(r'\$\{[^}]+\}', '{id}',
                          m.group(2) or m.group(3) or m.group(4) or "")
        direct_api_calls.append(f"{m.group(1).upper()} {api_path}")

    module = _get_view_module(file_path) if not is_component else "common"
    # If can detect from imports, prefer that
    if not is_component and imports["raw"]:
        detected = _detect_module_from_imports(imports["raw"])
        if detected != "common":
            module = detected

    return {
        "name": name,
        "file_path": rel_path,
        "module": module,
        "api_calls": imports["api"],
        "stores_used": imports["stores"],
        "components_used": imports["components"],
        "composables_used": imports["composables"],
        "direct_api_calls": direct_api_calls,
    }


# ── Parse router ──────────────────────────────────────────────────────────

ROUTE_RE = re.compile(
    r'\{[^{}]*?path:\s*[\'"]([^\'"]+)[\'"][^{}]*?name:\s*[\'"]([^\'"]+)[\'"][^{}]*?\}|'
    r'\{[^{}]*?name:\s*[\'"]([^\'"]+)[\'"][^{}]*?path:\s*[\'"]([^\'"]+)[\'"][^{}]*?\}',
    re.DOTALL,
)

ROUTE_COMPONENT_RE = re.compile(
    r'name:\s*[\'"]([^\'"]+)[\'"].*?import\s*\(\s*[\'"]([^\'"]+)[\'"]',
    re.DOTALL,
)

ROUTE_SCOPE_RE = re.compile(r'requiresScope:\s*[\'"]([^\'"]+)[\'"]')


def parse_router_files() -> list[dict]:
    """Parse router files and extract route definitions."""
    routes = []
    router_dir = FRONTEND_SRC / "router"

    for ts_file in sorted(router_dir.glob("*.ts")):
        content = ts_file.read_text(encoding="utf-8")

        # Extract route entries: path + name + component
        # Use a heuristic block parser for route records
        route_blocks = _extract_route_blocks(content)
        for block in route_blocks:
            path_m = re.search(r"path:\s*['\"]([^'\"]+)['\"]", block)
            name_m = re.search(r"name:\s*['\"]([^'\"]+)['\"]", block)
            comp_m = re.search(r"import\s*\(\s*['\"]([^'\"]+)['\"]", block)
            scope_m = ROUTE_SCOPE_RE.search(block)
            redirect_m = re.search(r"redirect:", block)

            if not path_m:
                continue

            path = path_m.group(1)
            name = name_m.group(1) if name_m else ""
            component = comp_m.group(1) if comp_m else ""
            scope = scope_m.group(1) if scope_m else "TENANT"
            is_redirect = bool(redirect_m)

            if is_redirect and not component:
                continue

            module = _route_path_to_module(path, component)

            routes.append({
                "name": name,
                "path": path,
                "component": component,
                "scope": scope,
                "module": module,
            })

    return routes


def _extract_route_blocks(content: str) -> list[str]:
    """Extract individual route object blocks from router file."""
    blocks = []
    depth = 0
    start = -1
    in_block = False

    # Find route object starts: lines with `path:` inside an object
    for i, char in enumerate(content):
        if char == '{':
            if not in_block:
                start = i
                in_block = True
                depth = 1
            else:
                depth += 1
        elif char == '}' and in_block:
            depth -= 1
            if depth == 0:
                block = content[start:i+1]
                # Only keep blocks that look like route records
                if re.search(r"path:\s*['\"]", block):
                    blocks.append(block)
                in_block = False
                start = -1

    return blocks


def _route_path_to_module(path: str, component: str) -> str:
    """Guess backend module from route path or component path."""
    comp_lower = component.lower()
    path_lower = path.lower()

    for key, mod in VIEW_DIR_TO_MODULE.items():
        if f"views/{key.lower()}" in comp_lower:
            return mod

    if "platform" in path_lower or "platform" in comp_lower:
        return "platform"
    if "announcement" in path_lower:
        return "announcement"
    if "asset" in path_lower or "asset" in comp_lower:
        return "assettransfer"
    if "user" in path_lower:
        return "user"
    if "rbac" in path_lower or "role" in path_lower:
        return "rbac"
    if "audit" in path_lower:
        return "audit"
    if "workflow" in path_lower or "delegate" in path_lower:
        return "workflow"
    if "dept" in path_lower:
        return "dept"
    if "notification" in path_lower:
        return "notification"
    if "setting" in path_lower:
        return "setting"
    if "tenant" in path_lower:
        return "tenant"
    if "login" in path_lower or "password" in path_lower:
        return "auth"

    return "common"


# ── Build fe→be endpoint bindings ─────────────────────────────────────────

def build_fe_be_bindings(api_functions: list[dict], be_endpoints: list[tuple]) -> list[dict]:
    """
    Match frontend API functions to backend endpoints by HTTP method + path.
    Backend endpoints have prefix /v1/ while frontend paths don't have /v1.
    """
    bindings = []

    # Build lookup: (http_method, normalized_path) → backend endpoint id
    be_lookup = {}
    for ep_id, method, path, controller, be_module in be_endpoints:
        # Normalize path params: /v1/auth/dept/{deptId} → /auth/dept/{id}
        norm = re.sub(r'\{[^}]+\}', '{id}', path)
        # Remove /v1 prefix
        norm = re.sub(r'^/v1', '', norm)
        be_lookup[(method.upper(), norm)] = ep_id

    for fn in api_functions:
        if not fn["http_method"] or not fn["api_path"]:
            continue

        # Frontend path is like /auth/announcements or /noauth/login
        norm_fe = re.sub(r'\{[^}]+\}', '{id}', fn["api_path"])
        key = (fn["http_method"].upper(), norm_fe)

        if key in be_lookup:
            bindings.append({
                "fe_function": fn["name"],
                "fe_module": fn["module"],
                "fe_file_path": fn["file_path"],
                "be_endpoint_id": be_lookup[key],
                "http_method": fn["http_method"],
                "api_path": fn["api_path"],
            })

    return bindings


# ── Main walker functions ─────────────────────────────────────────────────

def walk_api_files() -> list[dict]:
    api_dir = FRONTEND_SRC / "api"
    functions = []
    for ts_file in sorted(api_dir.rglob("*.ts")):
        if "__tests__" in str(ts_file):
            continue
        try:
            fns = parse_api_file(ts_file)
            functions.extend(fns)
        except Exception as e:
            print(f"  Error parsing API {ts_file.name}: {e}")
    return functions


def walk_store_files() -> list[dict]:
    store_dir = FRONTEND_SRC / "stores"
    stores = []
    for ts_file in sorted(store_dir.glob("*.ts")):
        if "__tests__" in str(ts_file):
            continue
        try:
            info = parse_store_file(ts_file)
            if info:
                stores.append(info)
        except Exception as e:
            print(f"  Error parsing store {ts_file.name}: {e}")
    return stores


def walk_view_files() -> list[dict]:
    views_dir = FRONTEND_SRC / "views"
    views = []
    for vue_file in sorted(views_dir.rglob("*.vue")):
        if "__tests__" in str(vue_file):
            continue
        try:
            info = parse_vue_file(vue_file, is_component=False)
            if info:
                views.append(info)
        except Exception as e:
            print(f"  Error parsing view {vue_file.name}: {e}")
    return views


def walk_component_files() -> list[dict]:
    comp_dir = FRONTEND_SRC / "components"
    components = []
    for vue_file in sorted(comp_dir.rglob("*.vue")):
        if "__tests__" in str(vue_file):
            continue
        try:
            info = parse_vue_file(vue_file, is_component=True)
            if info:
                components.append(info)
        except Exception as e:
            print(f"  Error parsing component {vue_file.name}: {e}")
    return components


# ── Database ──────────────────────────────────────────────────────────────

def update_database(api_functions, stores, views, components, routes, bindings):
    """Add frontend tables to the existing knowledge.db."""
    if not DB_PATH.exists():
        print(f"Error: {DB_PATH} not found. Run Phase 1 first.")
        sys.exit(1)

    conn = sqlite3.connect(str(DB_PATH))
    conn.execute("PRAGMA journal_mode=WAL")
    cur = conn.cursor()

    # Drop and recreate frontend tables (idempotent)
    cur.executescript("""
        DROP TABLE IF EXISTS fe_be_bindings;
        DROP TABLE IF EXISTS frontend_api_functions;
        DROP TABLE IF EXISTS frontend_stores;
        DROP TABLE IF EXISTS frontend_views;
        DROP TABLE IF EXISTS frontend_components;
        DROP TABLE IF EXISTS frontend_routes;

        CREATE TABLE frontend_api_functions (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            name            TEXT NOT NULL,
            http_method     TEXT DEFAULT '',
            api_path        TEXT DEFAULT '',
            module          TEXT NOT NULL,
            file_path       TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_fe_api_module ON frontend_api_functions(module);
        CREATE INDEX IF NOT EXISTS idx_fe_api_path ON frontend_api_functions(api_path);

        CREATE TABLE frontend_stores (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            name            TEXT NOT NULL,
            store_id        TEXT NOT NULL,
            module          TEXT NOT NULL,
            file_path       TEXT NOT NULL,
            api_calls_json  TEXT DEFAULT '[]',
            imports_json    TEXT DEFAULT '[]'
        );

        CREATE TABLE frontend_views (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            name            TEXT NOT NULL,
            file_path       TEXT NOT NULL,
            module          TEXT NOT NULL,
            api_calls_json  TEXT DEFAULT '[]',
            stores_used_json TEXT DEFAULT '[]',
            components_used_json TEXT DEFAULT '[]',
            composables_used_json TEXT DEFAULT '[]'
        );
        CREATE INDEX IF NOT EXISTS idx_fe_views_module ON frontend_views(module);

        CREATE TABLE frontend_components (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            name            TEXT NOT NULL,
            file_path       TEXT NOT NULL,
            module          TEXT NOT NULL DEFAULT 'common',
            api_calls_json  TEXT DEFAULT '[]',
            stores_used_json TEXT DEFAULT '[]'
        );

        CREATE TABLE frontend_routes (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            name            TEXT NOT NULL,
            path            TEXT NOT NULL,
            component       TEXT NOT NULL DEFAULT '',
            scope           TEXT NOT NULL DEFAULT 'TENANT',
            module          TEXT NOT NULL DEFAULT 'common'
        );
        CREATE INDEX IF NOT EXISTS idx_fe_routes_module ON frontend_routes(module);

        CREATE TABLE fe_be_bindings (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            fe_function     TEXT NOT NULL,
            fe_module       TEXT NOT NULL,
            fe_file_path    TEXT NOT NULL,
            be_endpoint_id  INTEGER NOT NULL REFERENCES endpoints(id),
            http_method     TEXT NOT NULL,
            api_path        TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_binding_fe ON fe_be_bindings(fe_function);
        CREATE INDEX IF NOT EXISTS idx_binding_be ON fe_be_bindings(be_endpoint_id);
    """)

    # Insert API functions (deduplicate on name + http_method + api_path)
    seen_api = set()
    for fn in api_functions:
        key = (fn["name"], fn["http_method"], fn["api_path"])
        if key in seen_api:
            continue
        seen_api.add(key)
        cur.execute(
            "INSERT INTO frontend_api_functions (name, http_method, api_path, module, file_path) VALUES (?, ?, ?, ?, ?)",
            (fn["name"], fn["http_method"], fn["api_path"], fn["module"], fn["file_path"]),
        )

    # Insert stores
    for s in stores:
        cur.execute(
            "INSERT INTO frontend_stores (name, store_id, module, file_path, api_calls_json, imports_json) VALUES (?, ?, ?, ?, ?, ?)",
            (
                s["name"], s["store_id"], s["module"], s["file_path"],
                json.dumps(s["api_calls"], ensure_ascii=False),
                json.dumps(s["imports_raw"], ensure_ascii=False),
            ),
        )

    # Insert views
    for v in views:
        cur.execute(
            """INSERT INTO frontend_views
               (name, file_path, module, api_calls_json, stores_used_json, components_used_json, composables_used_json)
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            (
                v["name"], v["file_path"], v["module"],
                json.dumps(v.get("api_calls", []), ensure_ascii=False),
                json.dumps(v.get("stores_used", []), ensure_ascii=False),
                json.dumps(v.get("components_used", []), ensure_ascii=False),
                json.dumps(v.get("composables_used", []), ensure_ascii=False),
            ),
        )

    # Insert components
    for c in components:
        cur.execute(
            """INSERT INTO frontend_components
               (name, file_path, module, api_calls_json, stores_used_json)
               VALUES (?, ?, ?, ?, ?)""",
            (
                c["name"], c["file_path"], c["module"],
                json.dumps(c.get("api_calls", []), ensure_ascii=False),
                json.dumps(c.get("stores_used", []), ensure_ascii=False),
            ),
        )

    # Insert routes
    for r in routes:
        cur.execute(
            "INSERT INTO frontend_routes (name, path, component, scope, module) VALUES (?, ?, ?, ?, ?)",
            (r["name"], r["path"], r["component"], r["scope"], r["module"]),
        )

    # Insert fe→be bindings
    for b in bindings:
        cur.execute(
            """INSERT INTO fe_be_bindings
               (fe_function, fe_module, fe_file_path, be_endpoint_id, http_method, api_path)
               VALUES (?, ?, ?, ?, ?, ?)""",
            (b["fe_function"], b["fe_module"], b["fe_file_path"],
             b["be_endpoint_id"], b["http_method"], b["api_path"]),
        )

    # Update meta
    cur.execute("INSERT OR REPLACE INTO meta (key, value) VALUES ('fe_api_functions', ?)", (str(len(api_functions)),))
    cur.execute("INSERT OR REPLACE INTO meta (key, value) VALUES ('fe_stores', ?)", (str(len(stores)),))
    cur.execute("INSERT OR REPLACE INTO meta (key, value) VALUES ('fe_views', ?)", (str(len(views)),))
    cur.execute("INSERT OR REPLACE INTO meta (key, value) VALUES ('fe_components', ?)", (str(len(components)),))
    cur.execute("INSERT OR REPLACE INTO meta (key, value) VALUES ('fe_routes', ?)", (str(len(routes)),))
    cur.execute("INSERT OR REPLACE INTO meta (key, value) VALUES ('fe_be_bindings', ?)", (str(len(bindings)),))
    cur.execute(
        "INSERT OR REPLACE INTO meta (key, value) VALUES ('fe_parsed_at', ?)",
        (__import__("datetime").datetime.now().isoformat(),),
    )

    conn.commit()
    conn.close()


# ── Summary ────────────────────────────────────────────────────────────────

def print_summary(api_functions, stores, views, components, routes, bindings):
    print("\n" + "=" * 60)
    print("📊  Frontend Knowledge Database — Summary")
    print("=" * 60)

    print(f"  API functions:  {len(api_functions)}")
    print(f"  Pinia stores:   {len(stores)}")
    print(f"  Views:          {len(views)}")
    print(f"  Components:     {len(components)}")
    print(f"  Routes:         {len(routes)}")
    print(f"  FE→BE bindings: {len(bindings)}")

    # API by module
    api_by_module = defaultdict(int)
    for fn in api_functions:
        api_by_module[fn["module"]] += 1
    print("\n📡  API functions per backend module:")
    for mod, cnt in sorted(api_by_module.items()):
        print(f"  {mod:20s} {cnt:3d} functions")

    # Views by module
    views_by_module = defaultdict(int)
    for v in views:
        views_by_module[v["module"]] += 1
    print("\n🖥️   Views per module:")
    for mod, cnt in sorted(views_by_module.items()):
        print(f"  {mod:20s} {cnt:3d} views")

    # Routes by scope
    routes_by_scope = defaultdict(int)
    for r in routes:
        routes_by_scope[r["scope"]] += 1
    print("\n🛣️   Routes by scope:")
    for scope, cnt in sorted(routes_by_scope.items()):
        print(f"  {scope:20s} {cnt:3d} routes")

    # Matched bindings
    print(f"\n🔗  FE→BE matched endpoints: {len(bindings)}")
    if bindings:
        for b in bindings[:10]:
            print(f"  {b['fe_function']:35s} → {b['http_method']:6s} {b['api_path']}")
        if len(bindings) > 10:
            print(f"  ... ({len(bindings) - 10} more)")

    print("=" * 60)


# ── Main ──────────────────────────────────────────────────────────────────

def main():
    print("🔍  Phase 2: Frontend Analyzer (Vue 3 + TypeScript)")
    print("=" * 60)

    if not FRONTEND_SRC.exists():
        print(f"Error: Frontend source not found: {FRONTEND_SRC}")
        sys.exit(1)

    # Step 1: API functions
    print("\n[1/6] Parsing API files...")
    api_functions = walk_api_files()
    print(f"  ✅ {len(api_functions)} API function entries")

    # Step 2: Stores
    print("\n[2/6] Parsing Pinia stores...")
    stores = walk_store_files()
    print(f"  ✅ {len(stores)} stores")

    # Step 3: Views
    print("\n[3/6] Parsing Vue views...")
    views = walk_view_files()
    print(f"  ✅ {len(views)} views")

    # Step 4: Components
    print("\n[4/6] Parsing Vue components...")
    components = walk_component_files()
    print(f"  ✅ {len(components)} components")

    # Step 5: Routes
    print("\n[5/6] Parsing router...")
    routes = parse_router_files()
    print(f"  ✅ {len(routes)} routes")

    # Step 6: FE→BE bindings
    print("\n[6/6] Building FE→BE endpoint bindings...")
    conn = sqlite3.connect(str(DB_PATH))
    be_endpoints = conn.execute(
        "SELECT id, http_method, path, controller, module FROM endpoints"
    ).fetchall()
    conn.close()
    bindings = build_fe_be_bindings(api_functions, be_endpoints)
    print(f"  ✅ {len(bindings)} bindings matched")

    # Write to DB
    print("\n[7/6] Updating knowledge.db...")
    update_database(api_functions, stores, views, components, routes, bindings)
    print(f"  ✅ Database updated: {DB_PATH}")

    print_summary(api_functions, stores, views, components, routes, bindings)


if __name__ == "__main__":
    main()
