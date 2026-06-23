#!/usr/bin/env python3
"""
CodeCortex File Watcher — Incremental Knowledge DB Updater
===========================================================
Monitors Java / Vue / TypeScript source files. On Ctrl+S (file save),
automatically re-parses ONLY the changed file, updates the affected
rows in knowledge.db, and rebuilds the FTS5 index for those rows.

No full re-parse needed — changes are reflected in < 1 second.

Usage:
    source venv/bin/activate
    python scripts/watch.py

    # Or run in background:
    python scripts/watch.py &

Press Ctrl+C to stop.
"""

import json
import logging
import re
import sqlite3
import sys
import time
from collections import defaultdict
from pathlib import Path

from watchdog.events import FileSystemEventHandler, FileModifiedEvent, FileCreatedEvent
from watchdog.observers import Observer

# ── Add scripts dir to path so we can import analyzer functions ────────────
SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))

import tree_sitter_java as tsjava
import tree_sitter_typescript as tsts
from tree_sitter import Language, Parser

# ── Paths ──────────────────────────────────────────────────────────────────
PROJECT_ROOT = SCRIPTS_DIR.parent
BACKEND_SRC  = PROJECT_ROOT / "backend" / "src" / "main" / "java" / "com" / "taipei" / "iot"
FRONTEND_SRC = PROJECT_ROOT / "frontend" / "src"
DB_PATH      = PROJECT_ROOT / "knowledge.db"

# ── Parsers ────────────────────────────────────────────────────────────────
JAVA_LANGUAGE = Language(tsjava.language())
java_parser   = Parser(JAVA_LANGUAGE)

TS_LANGUAGE   = Language(tsts.language_typescript())
ts_parser     = Parser(TS_LANGUAGE)

# ── Logging ────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [watch] %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("codecortex-watch")


# ══════════════════════════════════════════════════════════════════════════════
# DB helpers
# ══════════════════════════════════════════════════════════════════════════════

def get_conn() -> sqlite3.Connection:
    conn = sqlite3.connect(str(DB_PATH))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    return conn


# ══════════════════════════════════════════════════════════════════════════════
# Shared tree-sitter helpers (mirrors tree-sitter-analyzer.py)
# ══════════════════════════════════════════════════════════════════════════════

def node_text(node, source_bytes: bytes) -> str:
    if node is None:
        return ""
    return source_bytes[node.start_byte:node.end_byte].decode("utf-8", errors="replace")


def find_child(node, types: list[str]):
    if node is None:
        return None
    for child in node.children:
        if child.type in types:
            return child
    return None


def find_all_descendants(node, type_name: str) -> list:
    results = []
    if node is None:
        return results
    if node.type == type_name:
        results.append(node)
    for child in node.children:
        results.extend(find_all_descendants(child, type_name))
    return results


def get_annotation_identifiers(node) -> list[str]:
    result = []
    for ann in find_all_descendants(node, "marker_annotation"):
        for child in ann.children:
            if child.type == "identifier":
                result.append(node_text(child, b"" if not hasattr(ann, '_src') else ann._src))
    for ann in find_all_descendants(node, "annotation"):
        for child in ann.children:
            if child.type == "identifier":
                result.append(node_text(child, b""))
    return result


# ══════════════════════════════════════════════════════════════════════════════
# Java incremental updater
# ══════════════════════════════════════════════════════════════════════════════

SPRING_STEREOTYPES = {
    "Controller": "controller",
    "RestController": "controller",
    "Service": "service",
    "Repository": "repository",
    "RepositoryRestResource": "repository",
    "Component": "component",
    "Configuration": "config",
    "SpringBootApplication": "application",
    "Entity": "entity",
    "Embeddable": "entity",
    "MappedSuperclass": "entity",
    "Advice": "advice",
    "ControllerAdvice": "advice",
    "RestControllerAdvice": "advice",
}


def _detect_class_type(class_node, annotations: list[str], source_bytes: bytes) -> str:
    for ann in annotations:
        if ann in SPRING_STEREOTYPES:
            return SPRING_STEREOTYPES[ann]
    if class_node.type == "enum_declaration":
        return "enum"
    for child in class_node.children:
        if child.type in ("superclass", "super_interfaces", "extends_interfaces"):
            ext_text = node_text(child, source_bytes)
            for kw in ["JpaRepository", "JpaSpecificationExecutor", "CrudRepository",
                        "PagingAndSortingRepository", "TenantScopedRepository"]:
                if kw in ext_text:
                    return "repository"
    return "other"


def _get_ann_identifiers(node, source_bytes: bytes) -> list[str]:
    result = []
    for ann in find_all_descendants(node, "marker_annotation"):
        for c in ann.children:
            if c.type == "identifier":
                result.append(node_text(c, source_bytes))
    for ann in find_all_descendants(node, "annotation"):
        for c in ann.children:
            if c.type == "identifier":
                result.append(node_text(c, source_bytes))
    return result


def _parse_fields(class_node, source_bytes: bytes) -> list[dict]:
    fields = []
    body = find_child(class_node, ["class_body", "enum_body"])
    if body is None:
        return fields
    for child in body.children:
        if child.type != "field_declaration":
            continue
        anns = [node_text(c2, source_bytes)
                for ann in find_all_descendants(child, "marker_annotation")
                for c2 in ann.children if c2.type == "identifier"]
        type_node = find_child(child, ["type_identifier", "generic_type", "array_type"])
        ftype = node_text(type_node, source_bytes) if type_node else ""
        for decl in [c for c in child.children if c.type == "variable_declarator"]:
            name_node = find_child(decl, ["identifier"])
            if name_node:
                fields.append({"name": node_text(name_node, source_bytes),
                                "type": ftype, "annotations": anns})
    return fields


def _parse_methods(class_node, source_bytes: bytes) -> list[dict]:
    methods = []
    body = find_child(class_node, ["class_body", "enum_body"])
    if body is None:
        return methods
    for child in body.children:
        if child.type != "method_declaration":
            continue
        anns = _get_ann_identifiers(child, source_bytes)
        name_node = find_child(child, ["identifier"])
        if not name_node:
            continue
        fps = find_child(child, ["formal_parameters"])
        params = []
        if fps:
            for p in [c for c in fps.children if c.type == "formal_parameter"]:
                pn = node_text(find_child(p, ["identifier"]), source_bytes)
                pt = node_text(find_child(p, ["type_identifier", "generic_type"]), source_bytes)
                params.append(f"{pt} {pn}")
        ret_node = find_child(child, ["type_identifier", "generic_type", "void_type"])
        methods.append({
            "name": node_text(name_node, source_bytes),
            "annotations": anns,
            "params": ", ".join(params),
            "return_type": node_text(ret_node, source_bytes) if ret_node else "",
        })
    return methods


def _parse_endpoints_from_class(class_node, source_bytes: bytes, class_info: dict) -> list[dict]:
    endpoints = []
    base_path = ""
    for ann_node in find_all_descendants(class_node, "annotation"):
        ann_text = node_text(ann_node, source_bytes)
        m = re.search(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?"([^"]+)"', ann_text)
        if m:
            base_path = m.group(1)
            break
        m = re.search(r'@RequestMapping\s*\(\s*path\s*=\s*"([^"]+)"', ann_text)
        if m:
            base_path = m.group(1)
            break

    body = find_child(class_node, ["class_body"])
    if body is None:
        return endpoints

    mapping_map = {
        "GetMapping": "GET", "PostMapping": "POST", "PutMapping": "PUT",
        "DeleteMapping": "DELETE", "PatchMapping": "PATCH",
    }

    for child in body.children:
        if child.type != "method_declaration":
            continue
        name_node = find_child(child, ["identifier"])
        if not name_node:
            continue
        method_name = node_text(name_node, source_bytes)
        http_method = None
        path = None

        for ann_node in find_all_descendants(child, "annotation"):
            ann_id = find_child(ann_node, ["identifier"])
            if ann_id is None:
                continue
            ann_name = node_text(ann_id, source_bytes)
            if ann_name in mapping_map:
                http_method = mapping_map[ann_name]
                args = find_child(ann_node, ["annotation_argument_list"])
                if args:
                    args_text = node_text(args, source_bytes)
                    m2 = re.search(r'(?:value|path)\s*=\s*"([^"]+)"', args_text)
                    path = m2.group(1) if m2 else re.search(r'"([^"]+)"', args_text)
                    if hasattr(path, "group"):
                        path = path.group(1)
                    elif path is None:
                        path = ""
                else:
                    path = ""

        if http_method and path is not None:
            endpoints.append({
                "http_method": http_method,
                "path": base_path + path,
                "controller": class_info["class_name"],
                "method": method_name,
                "module": class_info["module"],
            })
    return endpoints


def get_module_name(file_path: Path) -> str:
    try:
        rel = file_path.relative_to(BACKEND_SRC)
        parts = rel.parts
        if len(parts) >= 2:
            return parts[0]
    except ValueError:
        pass
    return "_root_"


def update_java_file(file_path: Path, conn: sqlite3.Connection):
    """Re-parse a single Java file and update classes, endpoints, imports, feature_contracts."""
    rel_path = str(file_path.relative_to(PROJECT_ROOT))
    module_name = get_module_name(file_path)

    log.info(f"Updating Java: {rel_path}")

    # ── Delete stale data ───────────────────────────────────────────
    # Get old class names for FTS cleanup
    old_class_ids = [r["id"] for r in conn.execute(
        "SELECT id FROM classes WHERE file_path = ?", (rel_path,)
    )]
    old_controllers = [r["class_name"] for r in conn.execute(
        "SELECT class_name FROM classes WHERE file_path = ? AND class_type = 'controller'",
        (rel_path,)
    )]

    conn.execute("DELETE FROM classes WHERE file_path = ?", (rel_path,))
    conn.execute("DELETE FROM imports WHERE file_path = ?", (rel_path,))
    if old_controllers:
        placeholders = ",".join("?" * len(old_controllers))
        # Delete fe_be_bindings first (references endpoints)
        old_ep_ids = [r["id"] for r in conn.execute(
            f"SELECT id FROM endpoints WHERE controller IN ({placeholders})", old_controllers
        )]
        if old_ep_ids:
            ep_ph = ",".join("?" * len(old_ep_ids))
            conn.execute(
                f"DELETE FROM fe_be_bindings WHERE be_endpoint_id IN ({ep_ph})", old_ep_ids
            )
        conn.execute(
            f"DELETE FROM endpoints WHERE controller IN ({placeholders})", old_controllers
        )
        conn.execute(
            f"DELETE FROM feature_contracts WHERE feature_name LIKE ?",
            (f"{old_controllers[0]}.%",)
        )

    # ── Re-parse ────────────────────────────────────────────────────
    try:
        with open(file_path, "rb") as f:
            source_bytes = f.read()
    except OSError:
        log.warning(f"Cannot read {rel_path}")
        return

    tree = java_parser.parse(source_bytes)
    root = tree.root_node

    # Package
    pkg = ""
    for child in root.children:
        if child.type == "package_declaration":
            scoped = find_child(child, ["scoped_identifier", "identifier"])
            pkg = node_text(scoped, source_bytes) if scoped else ""
            break

    # Imports
    new_imports = []
    for child in root.children:
        if child.type == "import_declaration":
            new_imports.append(node_text(child, source_bytes).strip())

    for imp in new_imports:
        conn.execute(
            "INSERT INTO imports (file_path, import_text) VALUES (?, ?)",
            (rel_path, imp)
        )

    # Classes
    new_class_ids = []
    new_endpoints = []
    for child in root.children:
        if child.type not in ("class_declaration", "interface_declaration",
                               "enum_declaration", "record_declaration"):
            continue

        anns = _get_ann_identifiers(child, source_bytes)
        name_node = find_child(child, ["identifier"])
        if not name_node:
            continue
        class_name = node_text(name_node, source_bytes)
        class_type = _detect_class_type(child, anns, source_bytes)
        fqn = f"{pkg}.{class_name}" if pkg else class_name
        fields = _parse_fields(child, source_bytes)
        methods = _parse_methods(child, source_bytes)

        cur = conn.execute(
            """INSERT INTO classes
               (class_name, full_qualified_name, class_type, module, file_path,
                annotations, fields_json, methods_json)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
            (class_name, fqn, class_type, module_name, rel_path,
             ", ".join(anns),
             json.dumps(fields, ensure_ascii=False),
             json.dumps(methods, ensure_ascii=False))
        )
        new_class_ids.append(cur.lastrowid)

        if class_type == "controller":
            class_info = {"class_name": class_name, "module": module_name}
            eps = _parse_endpoints_from_class(child, source_bytes, class_info)
            new_endpoints.extend(eps)

    for ep in new_endpoints:
        conn.execute(
            "INSERT INTO endpoints (http_method, path, controller, method, module) "
            "VALUES (?, ?, ?, ?, ?)",
            (ep["http_method"], ep["path"], ep["controller"], ep["method"], ep["module"])
        )

    conn.commit()

    # ── Update feature_contracts for re-parsed controllers ──────────
    if old_controllers or any(e for e in new_endpoints):
        _update_contracts_for_file(file_path, conn)

    # ── Update code_constraints (Phase 5a) ──────────────────────────
    _update_constraints_for_file(file_path, conn)

    # ── Incremental FTS5 update ─────────────────────────────────────
    # Remove stale FTS rows
    if old_class_ids:
        for cid in old_class_ids:
            conn.execute("DELETE FROM classes_fts WHERE source_rowid = ?", (cid,))

    # Insert new FTS rows
    for cid in new_class_ids:
        r = conn.execute(
            "SELECT class_name, class_type, module, annotations FROM classes WHERE id = ?",
            (cid,)
        ).fetchone()
        if r:
            conn.execute(
                "INSERT INTO classes_fts(source_rowid, class_name, class_type, module, annotations) "
                "VALUES (?, ?, ?, ?, ?)",
                (cid, r["class_name"], r["class_type"], r["module"], r["annotations"])
            )

    # Rebuild endpoints_fts for this module (simpler than tracking individual IDs)
    conn.execute(
        "DELETE FROM endpoints_fts WHERE module = ?", (module_name,)
    )
    for r in conn.execute(
        "SELECT id, path, controller, method, module FROM endpoints WHERE module = ?",
        (module_name,)
    ):
        conn.execute(
            "INSERT INTO endpoints_fts(source_rowid, path, controller, method, module) "
            "VALUES (?, ?, ?, ?, ?)",
            (r["id"], r["path"], r["controller"], r["method"], r["module"])
        )

    conn.commit()
    log.info(f"  → {len(new_class_ids)} class(es), {len(new_endpoints)} endpoint(s) updated")


def _update_contracts_for_file(file_path: Path, conn: sqlite3.Connection):
    """Re-extract feature_contracts for controllers in the changed file."""
    # Import the contract extractor from phase35-enhancer
    try:
        from phase35_enhancer import build_type_dict, extract_method_contracts
    except ImportError:
        # Fallback: load directly
        import importlib.util
        spec = importlib.util.spec_from_file_location(
            "phase35_enhancer",
            SCRIPTS_DIR / "phase35-enhancer.py"
        )
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
        build_type_dict = mod.build_type_dict
        extract_method_contracts = mod.extract_method_contracts

    type_dict = build_type_dict(conn)
    contracts = extract_method_contracts(file_path, type_dict)

    module = get_module_name(file_path)
    for c in contracts:
        c["module"] = module

    for c in contracts:
        conn.execute(
            "DELETE FROM feature_contracts WHERE feature_name = ?",
            (c["feature_name"],)
        )
        conn.execute(
            """INSERT INTO feature_contracts
               (feature_name, http_method, endpoint, request_type, response_type,
                request_schema, response_schema, module)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
            (c["feature_name"], c["http_method"], c["endpoint"],
             c["request_type"], c["response_type"],
             c["request_schema"], c["response_schema"], c["module"])
        )

    # Rebuild contracts_fts for this module
    # contracts_fts has no module column — delete by feature_name prefix match
    existing_names = [r["feature_name"] for r in conn.execute(
        "SELECT feature_name FROM feature_contracts WHERE module = ?", (module,)
    )]
    # Clear all contracts_fts rows whose feature_name starts with known controllers
    # (simpler: rebuild entire contracts_fts for the module's contracts)
    for fc in conn.execute(
        "SELECT id FROM feature_contracts WHERE module = ?", (module,)
    ):
        conn.execute("DELETE FROM contracts_fts WHERE source_rowid = ?", (fc["id"],))

    for r in conn.execute(
        "SELECT id, feature_name, endpoint, request_type, response_type, "
        "request_schema, response_schema FROM feature_contracts WHERE module = ?",
        (module,)
    ):
        conn.execute(
            "INSERT INTO contracts_fts(source_rowid, feature_name, endpoint, "
            "request_type, response_type, request_schema, response_schema) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            (r["id"], r["feature_name"], r["endpoint"],
             r["request_type"], r["response_type"],
             r["request_schema"], r["response_schema"])
        )

    conn.commit()
    if contracts:
        log.info(f"  → {len(contracts)} feature contract(s) updated")


def _update_constraints_for_file(file_path: Path, conn: sqlite3.Connection):
    """Re-extract code_constraints for a changed Java file (Phase 5a)."""
    try:
        import importlib.util
        spec = importlib.util.spec_from_file_location(
            "phase5a", SCRIPTS_DIR / "phase5a-constraints.py"
        )
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)
    except Exception as e:
        log.warning(f"  Cannot load phase5a-constraints.py: {e}")
        return

    constraints = mod.extract_constraints(file_path)
    rel_path = str(file_path.relative_to(PROJECT_ROOT))

    # Ensure tables exist (idempotent)
    mod.migrate_schema(conn)
    mod.upsert_constraints(conn, constraints, rel_path)
    if constraints:
        log.info(f"  → {len(constraints)} constraint(s) updated")


# ══════════════════════════════════════════════════════════════════════════════
# Frontend incremental updater (Vue / TypeScript)
# ══════════════════════════════════════════════════════════════════════════════

API_DIR_TO_MODULE = {
    "announcement": "announcement",
    "asset-transfer": "assettransfer",
    "audit": "audit",
    "auth": "auth",
    "common": "common",
    "dept": "dept",
    "notification": "notification",
    "platform": "platform",
    "rbac": "rbac",
    "setting": "setting",
    "tenant": "tenant",
    "user": "user",
    "workflow": "workflow",
}

VIEW_DIR_TO_MODULE = {
    "announcement": "announcement",
    "asset-transfer": "assettransfer",
    "audit": "audit",
    "dept": "dept",
    "notification": "notification",
    "rbac": "rbac",
    "setting": "setting",
    "tenant": "tenant",
    "user": "user",
    "workflow": "workflow",
    "auth": "auth",
    "dashboard": "platform",
    "platform": "platform",
}


def _infer_fe_module(file_path: Path) -> str:
    """Guess backend module from frontend file path."""
    try:
        rel = file_path.relative_to(FRONTEND_SRC)
        parts = rel.parts
        for p in parts:
            for mapping in (API_DIR_TO_MODULE, VIEW_DIR_TO_MODULE):
                if p in mapping:
                    return mapping[p]
    except ValueError:
        pass
    return "common"


def _extract_api_calls(source: str) -> list[str]:
    """Extract function names called via axiosIns from a TS/Vue file."""
    return re.findall(r'(?:import\s+\{[^}]*\b(\w+)\b[^}]*\})', source)


def update_frontend_file(file_path: Path, conn: sqlite3.Connection):
    """Incremental update for a changed Vue or TypeScript frontend file."""
    rel_path = str(file_path.relative_to(PROJECT_ROOT))
    suffix = file_path.suffix.lower()
    log.info(f"Updating frontend: {rel_path}")

    # ── API functions (src/api/**/*.ts) ─────────────────────────────
    if "src/api" in rel_path.replace("\\", "/") and suffix == ".ts":
        _update_api_functions(file_path, rel_path, conn)

    # ── Vue views / components ──────────────────────────────────────
    elif suffix == ".vue":
        _update_vue_component(file_path, rel_path, conn)


def _update_api_functions(file_path: Path, rel_path: str, conn: sqlite3.Connection):
    """Update frontend_api_functions for a changed API file."""
    try:
        source = file_path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return

    module = _infer_fe_module(file_path)

    # Delete stale rows for this file
    old_ids = [r["id"] for r in conn.execute(
        "SELECT id FROM frontend_api_functions WHERE file_path = ?", (rel_path,)
    )]
    conn.execute("DELETE FROM frontend_api_functions WHERE file_path = ?", (rel_path,))

    # Re-extract: match export function / const declarations with axiosIns calls
    fn_pattern = re.compile(
        r'export\s+(?:async\s+)?(?:function|const)\s+(\w+)',
        re.MULTILINE
    )
    http_pattern = re.compile(
        r'axiosIns\.(get|post|put|delete|patch|head)[^(]*\(\s*(?:`([^`]+)`|\'([^\']+)\'|"([^"]+)")',
        re.IGNORECASE
    )

    new_ids = []
    for fn_match in fn_pattern.finditer(source):
        fn_name = fn_match.group(1)
        # Look for axiosIns call within the next 400 chars
        snippet = source[fn_match.start():fn_match.start() + 400]
        http_match = http_pattern.search(snippet)
        http_method = http_match.group(1).upper() if http_match else ""
        api_path = next((g for g in (http_match.group(2), http_match.group(3),
                                      http_match.group(4)) if g), "") if http_match else ""

        cur = conn.execute(
            "INSERT INTO frontend_api_functions (name, http_method, api_path, module, file_path) "
            "VALUES (?, ?, ?, ?, ?)",
            (fn_name, http_method, api_path, module, rel_path)
        )
        new_ids.append(cur.lastrowid)

    conn.commit()

    # Update FE→BE bindings for changed functions (rebuild for module)
    _rebuild_fe_be_bindings_for_module(module, conn)
    log.info(f"  → {len(new_ids)} API function(s) updated")


def _update_vue_component(file_path: Path, rel_path: str, conn: sqlite3.Connection):
    """Update frontend_views or frontend_components for a changed Vue file."""
    try:
        source = file_path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return

    module = _infer_fe_module(file_path)
    name = file_path.stem

    # Determine if view or component
    is_view = "views" in rel_path.replace("\\", "/")
    table = "frontend_views" if is_view else "frontend_components"

    # Extract used stores, api calls, components from imports
    stores_used = re.findall(r'use(\w+Store)\(\)', source)
    api_calls = re.findall(r'\b((?:get|post|put|delete|patch|fetch|create|update|remove)\w*)\s*\(', source)
    components_used = re.findall(r'import\s+(\w+)\s+from\s+[\'"].*components', source)

    conn.execute(f"DELETE FROM {table} WHERE file_path = ?", (rel_path,))
    conn.execute(
        f"INSERT INTO {table} "
        f"(name, file_path, module, api_calls_json, stores_used_json, components_used_json) "
        f"VALUES (?, ?, ?, ?, ?, ?)",
        (name, rel_path, module,
         json.dumps(list(set(api_calls[:20])), ensure_ascii=False),
         json.dumps(list(set(stores_used)), ensure_ascii=False),
         json.dumps(list(set(components_used)), ensure_ascii=False))
    )
    conn.commit()
    log.info(f"  → {table}: '{name}' updated")


def _rebuild_fe_be_bindings_for_module(module: str, conn: sqlite3.Connection):
    """Rebuild fe_be_bindings for a specific module after API function changes."""
    conn.execute("DELETE FROM fe_be_bindings WHERE fe_module = ?", (module,))

    api_fns = conn.execute(
        "SELECT id, name, http_method, api_path FROM frontend_api_functions "
        "WHERE module = ? AND http_method != ''",
        (module,)
    ).fetchall()

    for fn in api_fns:
        # Try to match to a backend endpoint
        path = fn["api_path"]
        if not path:
            continue
        # Normalize path variables: /api/v1/user/{id} → pattern match
        path_pattern = re.sub(r'\$\{[^}]+\}', '%', path)  # template literals
        path_pattern = re.sub(r':\w+', '%', path_pattern)  # :id style

        ep = conn.execute(
            "SELECT id FROM endpoints WHERE http_method = ? AND path LIKE ? LIMIT 1",
            (fn["http_method"], path_pattern)
        ).fetchone()

        if ep:
            conn.execute(
                "INSERT OR IGNORE INTO fe_be_bindings "
                "(fe_function, fe_module, http_method, api_path, be_endpoint_id) "
                "VALUES (?, ?, ?, ?, ?)",
                (fn["name"], module, fn["http_method"], fn["api_path"], ep["id"])
            )

    conn.commit()


# ══════════════════════════════════════════════════════════════════════════════
# Watchdog event handler
# ══════════════════════════════════════════════════════════════════════════════

class KnowledgeUpdateHandler(FileSystemEventHandler):
    """Handles file system events and triggers incremental DB updates."""

    # Debounce: avoid double-firing on rapid saves (vim writes temp file, etc.)
    _last_event: dict[str, float] = {}
    DEBOUNCE_SECONDS = 0.5

    def on_modified(self, event):
        self._handle(event)

    def on_created(self, event):
        self._handle(event)

    def _handle(self, event):
        if event.is_directory:
            return

        path = Path(event.src_path)
        suffix = path.suffix.lower()

        # Filter to relevant file types only
        if suffix not in (".java", ".vue", ".ts"):
            return
        # Skip generated / build / test files
        rel = str(path).replace("\\", "/")
        if any(skip in rel for skip in [
            "/target/", "/dist/", "/node_modules/", ".min.",
            "test/java", "test-classes", "__pycache__",
        ]):
            return

        # Debounce
        now = time.monotonic()
        if now - self._last_event.get(str(path), 0) < self.DEBOUNCE_SECONDS:
            return
        self._last_event[str(path)] = now

        # Route to correct updater
        if not DB_PATH.exists():
            log.warning("knowledge.db not found — run the full analyzers first")
            return

        try:
            conn = get_conn()
            if suffix == ".java" and BACKEND_SRC in path.parents:
                update_java_file(path, conn)
            elif suffix in (".vue", ".ts") and FRONTEND_SRC in path.parents:
                update_frontend_file(path, conn)
            conn.close()
        except Exception as e:
            log.error(f"Failed to update {path.name}: {e}", exc_info=True)


# ══════════════════════════════════════════════════════════════════════════════
# Entry point
# ══════════════════════════════════════════════════════════════════════════════

def main():
    if not DB_PATH.exists():
        print(f"Error: knowledge.db not found at {DB_PATH}")
        print("Run the full analyzers first:")
        print("  python scripts/tree-sitter-analyzer.py")
        print("  python scripts/frontend-analyzer.py")
        print("  python scripts/phase35-enhancer.py")
        sys.exit(1)

    handler  = KnowledgeUpdateHandler()
    observer = Observer()

    watch_dirs = []
    if BACKEND_SRC.exists():
        observer.schedule(handler, str(BACKEND_SRC), recursive=True)
        watch_dirs.append(str(BACKEND_SRC.relative_to(PROJECT_ROOT)))
    if FRONTEND_SRC.exists():
        observer.schedule(handler, str(FRONTEND_SRC), recursive=True)
        watch_dirs.append(str(FRONTEND_SRC.relative_to(PROJECT_ROOT)))

    observer.start()
    log.info(f"Watching: {', '.join(watch_dirs)}")
    log.info(f"DB: {DB_PATH}")
    log.info("Save any .java / .vue / .ts file to trigger incremental update. Ctrl+C to stop.")

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
        log.info("Stopped.")

    observer.join()


if __name__ == "__main__":
    main()
