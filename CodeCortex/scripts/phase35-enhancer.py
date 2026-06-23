#!/usr/bin/env python3
"""
Phase 3.5 Knowledge DB Enhancer
================================
Enhances the existing knowledge.db WITHOUT re-running the full Phase 1 parse.

New tables added:
  feature_contracts  — per-endpoint request/response DTO as TypeScript interface
  module_exports     — which classes each module exposes to other modules
  module_coupling    — which specific classes are imported cross-module
  module_examples    — golden example source code per class type per module

New FTS5 virtual tables:
  modules_fts        — full-text search on module descriptions
  classes_fts        — full-text search on class names + types
  endpoints_fts      — full-text search on paths + controllers
  contracts_fts      — full-text search on feature contracts + DTO schemas

Usage:
  source venv/bin/activate
  python scripts/phase35-enhancer.py
"""

import json
import re
import sqlite3
import sys
from collections import defaultdict
from pathlib import Path

import tree_sitter_java as tsjava
from tree_sitter import Language, Parser

# ── Paths ──────────────────────────────────────────────────────────────────
PROJECT_ROOT = Path(__file__).resolve().parent.parent
BACKEND_SRC = PROJECT_ROOT / "backend" / "src" / "main" / "java" / "com" / "taipei" / "iot"
DB_PATH = PROJECT_ROOT / "knowledge.db"

JAVA_LANGUAGE = Language(tsjava.language())
parser = Parser(JAVA_LANGUAGE)


# ── Java → TypeScript type mapping ────────────────────────────────────────
JAVA_TO_TS: dict[str, str] = {
    "String": "string",
    "string": "string",
    "char": "string",
    "Character": "string",
    "Long": "number",
    "long": "number",
    "Integer": "number",
    "int": "number",
    "Short": "number",
    "short": "number",
    "Double": "number",
    "double": "number",
    "Float": "number",
    "float": "number",
    "BigDecimal": "number",
    "BigInteger": "number",
    "Boolean": "boolean",
    "boolean": "boolean",
    "LocalDateTime": "string",  # ISO 8601
    "LocalDate": "string",
    "LocalTime": "string",
    "Instant": "string",
    "Date": "string",
    "ZonedDateTime": "string",
    "void": "void",
    "Void": "void",
    "Object": "unknown",
    "JsonNode": "unknown",
    "MultipartFile": "File",
    "byte[]": "string",  # base64
}

# Wrapper types whose type parameter is the actual DTO
WRAPPER_TYPES = [
    "ResponseEntity", "BaseResponse", "ApiResponse",
    "CompletableFuture", "Mono", "Flux", "Optional",
]

# Wrapper types that mean "list of T"
LIST_TYPES = ["List", "Set", "Collection", "ArrayList", "HashSet"]

# PageResponse / Page wrappers — keep as-is (meaningful to AI)
PAGE_TYPES = ["PageResponse", "Page", "Slice"]


def map_java_type_to_ts(java_type: str) -> str:
    """Convert a Java type string to TypeScript equivalent."""
    java_type = java_type.strip()

    # Primitive / known mappings
    if java_type in JAVA_TO_TS:
        return JAVA_TO_TS[java_type]

    # List<T> → T[]
    for lt in LIST_TYPES:
        m = re.match(rf"^{lt}<(.+)>$", java_type)
        if m:
            inner = map_java_type_to_ts(m.group(1).strip())
            return f"{inner}[]"

    # Map<K, V> → Record<K, V>
    m = re.match(r"^Map<(.+),\s*(.+)>$", java_type)
    if m:
        k = map_java_type_to_ts(m.group(1).strip())
        v = map_java_type_to_ts(m.group(2).strip())
        return f"Record<{k}, {v}>"

    # PageResponse<T> → PageResponse<T>  (keep - meaningful)
    for pt in PAGE_TYPES:
        m = re.match(rf"^{pt}<(.+)>$", java_type)
        if m:
            inner = map_java_type_to_ts(m.group(1).strip())
            return f"{pt}<{inner}>"

    # Strip simple generics we can't resolve (T remains as T)
    m = re.match(r"^(\w+)<(.+)>$", java_type)
    if m:
        outer = m.group(1)
        inner = map_java_type_to_ts(m.group(2).strip())
        return f"{outer}<{inner}>"

    # Array T[]
    if java_type.endswith("[]"):
        inner = map_java_type_to_ts(java_type[:-2])
        return f"{inner}[]"

    return java_type  # keep as-is (class name)


def unwrap_response_type(type_str: str) -> str:
    """
    Strip response wrapper types to expose the actual DTO.
    BaseResponse<PageResponse<AnnouncementResponse>> → PageResponse<AnnouncementResponse>
    ResponseEntity<ApiResponse<UserVO>> → ApiResponse<UserVO>
    """
    type_str = type_str.strip()
    for w in WRAPPER_TYPES:
        m = re.match(rf"^{w}<(.+)>$", type_str)
        if m:
            return unwrap_response_type(m.group(1).strip())
    return type_str


def fields_to_ts_interface(class_name: str, fields: list[dict], schema_desc: str = "") -> str:
    """Convert a list of Java field dicts to a TypeScript interface string."""
    lines = []
    if schema_desc:
        lines.append(f"/** {schema_desc} */")
    lines.append(f"interface {class_name} {{")
    for f in fields:
        fname = f.get("name", "")
        ftype = f.get("type", "unknown")
        ts_type = map_java_type_to_ts(ftype)
        lines.append(f"  {fname}: {ts_type};")
    lines.append("}")
    return "\n".join(lines)


# ── Tree-sitter helpers (minimal reuse from analyzer) ─────────────────────

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


def get_annotation_value(ann_node, source_bytes: bytes) -> str:
    """Extract the first string literal from an annotation's arguments."""
    args = find_child(ann_node, ["annotation_argument_list"])
    if args:
        args_text = node_text(args, source_bytes)
        m = re.search(r'"([^"]+)"', args_text)
        if m:
            return m.group(1)
    return ""


# ── Feature contract extraction ────────────────────────────────────────────

def build_type_dict(conn: sqlite3.Connection) -> dict[str, dict]:
    """
    Build a mapping: class_name → {fields, description, file_path}
    from the existing `classes` table.
    """
    type_dict: dict[str, dict] = {}
    for row in conn.execute(
        "SELECT class_name, fields_json, annotations, file_path, module FROM classes"
    ):
        try:
            fields = json.loads(row["fields_json"]) or []
        except Exception:
            fields = []
        type_dict[row["class_name"]] = {
            "fields": fields,
            "annotations": row["annotations"] or "",
            "file_path": row["file_path"] or "",
            "module": row["module"] or "",
        }
    return type_dict


def extract_method_contracts(file_path: Path, type_dict: dict[str, dict]) -> list[dict]:
    """
    Parse a controller Java file and extract request/response DTO info
    for each endpoint method.

    Returns list of dicts:
      {method_name, http_method, full_path, request_type, response_type,
       request_schema, response_schema}
    """
    if not file_path.exists():
        return []

    with open(file_path, "rb") as f:
        source_bytes = f.read()

    tree = parser.parse(source_bytes)
    root = tree.root_node
    results = []

    # --- Get base mapping path ---
    base_path = ""
    for ann_node in find_all_descendants(root, "annotation"):
        ann_id = find_child(ann_node, ["identifier"])
        if ann_id and node_text(ann_id, source_bytes) == "RequestMapping":
            val = get_annotation_value(ann_node, source_bytes)
            if val:
                base_path = val
                break

    # --- Get class name ---
    controller_name = ""
    for cls in root.children:
        if cls.type == "class_declaration":
            id_node = find_child(cls, ["identifier"])
            if id_node:
                controller_name = node_text(id_node, source_bytes)

            body = find_child(cls, ["class_body"])
            if body is None:
                continue

            for method in body.children:
                if method.type != "method_declaration":
                    continue

                method_name_node = find_child(method, ["identifier"])
                if not method_name_node:
                    continue
                method_name = node_text(method_name_node, source_bytes)

                # --- Detect HTTP method + sub-path ---
                http_method = None
                sub_path = None
                mapping_anns = {
                    "GetMapping": "GET",
                    "PostMapping": "POST",
                    "PutMapping": "PUT",
                    "DeleteMapping": "DELETE",
                    "PatchMapping": "PATCH",
                }

                for ann in find_all_descendants(method, "annotation"):
                    ann_id = find_child(ann, ["identifier"])
                    if ann_id is None:
                        continue
                    ann_name = node_text(ann_id, source_bytes)
                    if ann_name in mapping_anns:
                        http_method = mapping_anns[ann_name]
                        sub_path = get_annotation_value(ann, source_bytes)
                    elif ann_name == "RequestMapping" and http_method is None:
                        args = find_child(ann, ["annotation_argument_list"])
                        if args:
                            args_text = node_text(args, source_bytes)
                            m = re.search(r'method\s*=\s*(?:RequestMethod\.)?(\w+)', args_text)
                            if m:
                                http_method = m.group(1).upper()
                            m2 = re.search(r'"([^"]+)"', args_text)
                            if m2:
                                sub_path = m2.group(1)

                if http_method is None:
                    continue
                if sub_path is None:
                    sub_path = ""

                full_path = base_path + sub_path

                # --- Extract @RequestBody param type ---
                request_type = None
                formal_params = find_child(method, ["formal_parameters"])
                if formal_params:
                    for param in formal_params.children:
                        if param.type != "formal_parameter":
                            continue
                        # @RequestBody is a marker_annotation (no arguments)
                        has_request_body = any(
                            node_text(find_child(ann, ["identifier"]), source_bytes) == "RequestBody"
                            for ann in (
                                find_all_descendants(param, "marker_annotation")
                                + find_all_descendants(param, "annotation")
                            )
                            if find_child(ann, ["identifier"]) is not None
                        )
                        if has_request_body:
                            for child in param.children:
                                if child.type in (
                                    "type_identifier", "generic_type",
                                    "array_type", "integral_type",
                                ):
                                    request_type = node_text(child, source_bytes).strip()
                                    break

                # --- Extract return type ---
                return_type_raw = ""
                for child in method.children:
                    if child.type in (
                        "type_identifier", "generic_type",
                        "array_type", "void_type"
                    ):
                        return_type_raw = node_text(child, source_bytes).strip()
                        break

                response_type = unwrap_response_type(return_type_raw) if return_type_raw else None

                # --- Build TS schemas from type_dict ---
                request_schema = _resolve_schema(request_type, type_dict) if request_type else ""
                response_schema = _resolve_schema(response_type, type_dict) if response_type else ""

                results.append({
                    "feature_name": f"{controller_name}.{method_name}",
                    "http_method": http_method,
                    "endpoint": full_path,
                    "request_type": request_type or "",
                    "response_type": response_type or "",
                    "request_schema": request_schema,
                    "response_schema": response_schema,
                })

    return results


def _resolve_schema(type_name: str, type_dict: dict[str, dict]) -> str:
    """
    Convert a type name to a TypeScript interface string.
    Handles PageResponse<T>, List<T>, plain DTO names.
    """
    if not type_name or type_name in ("void", "Void", ""):
        return ""

    # PageResponse<T> → describe wrapper + inner type
    for pt in ["PageResponse", "Page", "Slice"]:
        m = re.match(rf"^{pt}<(.+)>$", type_name)
        if m:
            inner = m.group(1).strip()
            inner_schema = _resolve_schema(inner, type_dict)
            wrapper = (
                f"interface {pt}<T> {{\n"
                f"  content: T[];\n"
                f"  total: number;\n"
                f"  page: number;\n"
                f"  size: number;\n"
                f"}}"
            )
            if inner_schema:
                return wrapper + "\n\n" + inner_schema
            return wrapper

    # List<T> → T[]
    for lt in ["List", "Set", "Collection"]:
        m = re.match(rf"^{lt}<(.+)>$", type_name)
        if m:
            inner = m.group(1).strip()
            return _resolve_schema(inner, type_dict)

    # Look up in type_dict
    if type_name in type_dict:
        info = type_dict[type_name]
        fields = info["fields"]
        if fields:
            return fields_to_ts_interface(type_name, fields)

    # Primitive / known mapping
    ts = map_java_type_to_ts(type_name)
    if ts != type_name:
        return f"// {type_name} → {ts}"

    return f"// {type_name} (source not available)"


# ── Module exports / coupling from imports table ───────────────────────────

def build_module_exports_and_coupling(conn: sqlite3.Connection) -> tuple[list, list]:
    """
    Derive from the `imports` table:
    - module_exports: classes used by OTHER modules (import_count ≥ 1)
    - module_coupling: specific (from_module, to_module, class_name, usage_count) rows

    Only tracks imports within com.taipei.iot.*
    """
    base = "com.taipei.iot."

    # class_name → module mapping from classes table
    class_to_module: dict[str, str] = {}
    for row in conn.execute("SELECT class_name, module FROM classes"):
        class_to_module[row["class_name"]] = row["module"]

    # file → module mapping from classes table (by file_path)
    file_to_module: dict[str, str] = {}
    for row in conn.execute("SELECT DISTINCT file_path, module FROM classes"):
        file_to_module[row["file_path"]] = row["module"]
    # Also infer from imports file_path
    for row in conn.execute("SELECT DISTINCT file_path FROM imports"):
        fp = row["file_path"]
        # e.g. backend/src/main/java/com/taipei/iot/announcement/...
        m = re.search(r"com/taipei/iot/([^/]+)/", fp)
        if m:
            file_to_module[fp] = m.group(1)

    # coupling: (from_module, to_module, class_name) → count
    coupling_counts: dict[tuple, int] = defaultdict(int)

    for row in conn.execute("SELECT file_path, import_text FROM imports"):
        imp = row["import_text"].replace("import ", "").replace(";", "").strip()
        if not imp.startswith(base):
            continue
        rest = imp[len(base):]
        parts = rest.split(".")
        if len(parts) < 2:
            continue
        to_module = parts[0]
        class_name = parts[-1]
        if class_name == "*":
            continue

        from_module = file_to_module.get(row["file_path"], "")
        if not from_module or from_module == "_root_" or from_module == to_module:
            continue

        coupling_counts[(from_module, to_module, class_name)] += 1

    # Aggregate exports: class_name, module, how many modules import it
    export_counts: dict[tuple, int] = defaultdict(int)  # (class_name, module) → importers count
    for (from_mod, to_mod, cls_name), cnt in coupling_counts.items():
        export_counts[(cls_name, to_mod)] += 1

    # Look up class_type for exports
    class_type_map: dict[str, str] = {}
    for row in conn.execute("SELECT class_name, class_type FROM classes"):
        class_type_map[row["class_name"]] = row["class_type"]

    module_exports = [
        {
            "module": mod,
            "class_name": cls_name,
            "class_type": class_type_map.get(cls_name, "other"),
            "import_count": count,
        }
        for (cls_name, mod), count in sorted(
            export_counts.items(), key=lambda x: -x[1]
        )
    ]

    module_coupling = [
        {
            "from_module": from_mod,
            "to_module": to_mod,
            "class_name": cls_name,
            "usage_count": cnt,
        }
        for (from_mod, to_mod, cls_name), cnt in sorted(
            coupling_counts.items(), key=lambda x: (-x[1], x[0])
        )
    ]

    return module_exports, module_coupling


# ── Golden example extraction ──────────────────────────────────────────────

def extract_example_code(file_path: Path, class_name: str) -> str:
    """Extract source code for a specific class from a Java file."""
    if not file_path.exists():
        return ""
    with open(file_path, "rb") as f:
        source_bytes = f.read()
    tree = parser.parse(source_bytes)
    root = tree.root_node
    for child in root.children:
        if child.type in ("class_declaration", "interface_declaration", "enum_declaration"):
            id_node = find_child(child, ["identifier"])
            if id_node and node_text(id_node, source_bytes) == class_name:
                code = source_bytes[child.start_byte:child.end_byte].decode("utf-8", errors="replace")
                # Trim to 4000 chars to avoid bloat
                if len(code) > 4000:
                    code = code[:4000] + "\n  // ... (truncated)"
                return code
    return ""


def build_module_examples(conn: sqlite3.Connection) -> list[dict]:
    """
    For each module, pick one golden example per class_type
    (the one with the most methods = most complete).
    """
    examples = []
    type_priority = ["controller", "service", "repository", "entity", "config", "component"]

    for module_row in conn.execute(
        "SELECT name FROM modules WHERE name != '_root_' ORDER BY name"
    ):
        module = module_row["name"]
        for class_type in type_priority:
            # Find the class with the most methods in this module+type
            best = conn.execute(
                """
                SELECT class_name, file_path, methods_json
                FROM classes
                WHERE module = ? AND class_type = ?
                ORDER BY json_array_length(methods_json) DESC
                LIMIT 1
                """,
                (module, class_type),
            ).fetchone()

            if not best:
                continue

            abs_path = PROJECT_ROOT / best["file_path"]
            code = extract_example_code(abs_path, best["class_name"])
            if not code:
                continue

            examples.append({
                "module": module,
                "class_type": class_type,
                "class_name": best["class_name"],
                "file_path": best["file_path"],
                "example_code": code,
            })

    return examples


# ── Database migration ─────────────────────────────────────────────────────

def migrate_schema(conn: sqlite3.Connection):
    """Add new tables to the existing knowledge.db."""
    conn.executescript("""
        -- ── feature_contracts ──────────────────────────────────────────
        CREATE TABLE IF NOT EXISTS feature_contracts (
            id               INTEGER PRIMARY KEY AUTOINCREMENT,
            feature_name     TEXT NOT NULL,       -- e.g. "AnnouncementController.create"
            http_method      TEXT NOT NULL,
            endpoint         TEXT NOT NULL,
            request_type     TEXT DEFAULT '',     -- Java class name
            response_type    TEXT DEFAULT '',     -- Java class name (unwrapped)
            request_schema   TEXT DEFAULT '',     -- TypeScript interface string
            response_schema  TEXT DEFAULT '',     -- TypeScript interface string
            module           TEXT REFERENCES modules(name),
            UNIQUE(feature_name)
        );
        CREATE INDEX IF NOT EXISTS idx_fc_endpoint ON feature_contracts(endpoint);
        CREATE INDEX IF NOT EXISTS idx_fc_module   ON feature_contracts(module);

        -- ── module_exports ─────────────────────────────────────────────
        CREATE TABLE IF NOT EXISTS module_exports (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            module       TEXT NOT NULL REFERENCES modules(name),
            class_name   TEXT NOT NULL,
            class_type   TEXT NOT NULL DEFAULT 'other',
            import_count INTEGER NOT NULL DEFAULT 1,
            UNIQUE(module, class_name)
        );
        CREATE INDEX IF NOT EXISTS idx_me_module ON module_exports(module);

        -- ── module_coupling ────────────────────────────────────────────
        CREATE TABLE IF NOT EXISTS module_coupling (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            from_module  TEXT NOT NULL REFERENCES modules(name),
            to_module    TEXT NOT NULL REFERENCES modules(name),
            class_name   TEXT NOT NULL,
            usage_count  INTEGER NOT NULL DEFAULT 1,
            UNIQUE(from_module, to_module, class_name)
        );
        CREATE INDEX IF NOT EXISTS idx_mc_from ON module_coupling(from_module);
        CREATE INDEX IF NOT EXISTS idx_mc_to   ON module_coupling(to_module);

        -- ── module_examples ────────────────────────────────────────────
        CREATE TABLE IF NOT EXISTS module_examples (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            module       TEXT NOT NULL REFERENCES modules(name),
            class_type   TEXT NOT NULL,
            class_name   TEXT NOT NULL,
            file_path    TEXT NOT NULL,
            example_code TEXT NOT NULL,
            UNIQUE(module, class_type)
        );
        CREATE INDEX IF NOT EXISTS idx_mex_module ON module_examples(module);
    """)
    conn.commit()


def migrate_fts5(conn: sqlite3.Connection):
    """Create standalone FTS5 virtual tables (no content= to avoid sync issues)."""
    tables = [
        """CREATE VIRTUAL TABLE IF NOT EXISTS modules_fts USING fts5(
            source_rowid UNINDEXED, name, description
        )""",
        """CREATE VIRTUAL TABLE IF NOT EXISTS classes_fts USING fts5(
            source_rowid UNINDEXED, class_name, class_type, module, annotations
        )""",
        """CREATE VIRTUAL TABLE IF NOT EXISTS endpoints_fts USING fts5(
            source_rowid UNINDEXED, path, controller, method, module
        )""",
        """CREATE VIRTUAL TABLE IF NOT EXISTS contracts_fts USING fts5(
            source_rowid UNINDEXED, feature_name, endpoint,
            request_type, response_type, request_schema, response_schema
        )""",
    ]
    for stmt in tables:
        conn.execute(stmt)
    conn.commit()


def populate_fts5(conn: sqlite3.Connection):
    """Populate standalone FTS5 tables with current data."""
    pairs = [
        (
            "DELETE FROM modules_fts",
            "INSERT INTO modules_fts(source_rowid, name, description) "
            "SELECT rowid, name, description FROM modules",
        ),
        (
            "DELETE FROM classes_fts",
            "INSERT INTO classes_fts(source_rowid, class_name, class_type, module, annotations) "
            "SELECT id, class_name, class_type, module, annotations FROM classes",
        ),
        (
            "DELETE FROM endpoints_fts",
            "INSERT INTO endpoints_fts(source_rowid, path, controller, method, module) "
            "SELECT id, path, controller, method, module FROM endpoints",
        ),
        (
            "DELETE FROM contracts_fts",
            "INSERT INTO contracts_fts(source_rowid, feature_name, endpoint, "
            "request_type, response_type, request_schema, response_schema) "
            "SELECT id, feature_name, endpoint, request_type, response_type, "
            "request_schema, response_schema FROM feature_contracts",
        ),
    ]
    for delete_stmt, insert_stmt in pairs:
        conn.execute(delete_stmt)
        conn.execute(insert_stmt)
    conn.commit()


# ── Main ───────────────────────────────────────────────────────────────────

def main():
    if not DB_PATH.exists():
        print(f"Error: knowledge.db not found at {DB_PATH}")
        print("Run Phase 1 (tree-sitter-analyzer.py) and Phase 2 (frontend-analyzer.py) first.")
        sys.exit(1)

    conn = sqlite3.connect(str(DB_PATH))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")

    print("🔧  Phase 3.5: Knowledge DB Enhancer")
    print("=" * 60)

    # ── Step 1: Schema migration ────────────────────────────────────
    print("\n[1/5] Migrating schema (new tables)...")
    # Drop old data to allow clean re-run
    conn.executescript("""
        DROP TABLE IF EXISTS feature_contracts;
        DROP TABLE IF EXISTS module_exports;
        DROP TABLE IF EXISTS module_coupling;
        DROP TABLE IF EXISTS module_examples;
        DROP TABLE IF EXISTS modules_fts;
        DROP TABLE IF EXISTS classes_fts;
        DROP TABLE IF EXISTS endpoints_fts;
        DROP TABLE IF EXISTS contracts_fts;
    """)
    migrate_schema(conn)
    print("  ✅ Schema migrated")

    # ── Step 2: Build type dictionary from existing classes ─────────
    print("\n[2/5] Building type dictionary from classes table...")
    type_dict = build_type_dict(conn)
    print(f"  ✅ {len(type_dict)} types loaded")

    # ── Step 3: Extract feature contracts from controller files ──────
    print("\n[3/5] Extracting feature contracts (DTO schemas)...")

    # Get controller files from classes table
    controllers = conn.execute(
        "SELECT class_name, file_path, module FROM classes WHERE class_type = 'controller'"
    ).fetchall()

    all_contracts = []
    for ctrl in controllers:
        abs_path = PROJECT_ROOT / ctrl["file_path"]
        contracts = extract_method_contracts(abs_path, type_dict)
        for c in contracts:
            c["module"] = ctrl["module"]
        all_contracts.extend(contracts)

    # Insert into DB
    for c in all_contracts:
        conn.execute(
            """INSERT OR IGNORE INTO feature_contracts
               (feature_name, http_method, endpoint, request_type, response_type,
                request_schema, response_schema, module)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
            (c["feature_name"], c["http_method"], c["endpoint"],
             c["request_type"], c["response_type"],
             c["request_schema"], c["response_schema"], c["module"]),
        )
    conn.commit()

    with_request = sum(1 for c in all_contracts if c["request_schema"])
    with_response = sum(1 for c in all_contracts if c["response_schema"])
    print(f"  ✅ {len(all_contracts)} feature contracts inserted")
    print(f"     {with_request} have request schema, {with_response} have response schema")

    # ── Step 4: Module exports & coupling ───────────────────────────
    print("\n[4/5] Computing module exports and coupling...")
    exports, coupling = build_module_exports_and_coupling(conn)

    for e in exports:
        conn.execute(
            """INSERT OR IGNORE INTO module_exports
               (module, class_name, class_type, import_count)
               VALUES (?, ?, ?, ?)""",
            (e["module"], e["class_name"], e["class_type"], e["import_count"]),
        )
    for c in coupling:
        # Verify both modules exist
        mods = {r["name"] for r in conn.execute("SELECT name FROM modules")}
        if c["from_module"] in mods and c["to_module"] in mods:
            conn.execute(
                """INSERT OR IGNORE INTO module_coupling
                   (from_module, to_module, class_name, usage_count)
                   VALUES (?, ?, ?, ?)""",
                (c["from_module"], c["to_module"], c["class_name"], c["usage_count"]),
            )
    conn.commit()
    print(f"  ✅ {len(exports)} module exports, {len(coupling)} coupling edges")

    # ── Step 4b: Golden examples ─────────────────────────────────────
    print("\n  Extracting golden example code...")
    examples = build_module_examples(conn)
    for ex in examples:
        conn.execute(
            """INSERT OR REPLACE INTO module_examples
               (module, class_type, class_name, file_path, example_code)
               VALUES (?, ?, ?, ?, ?)""",
            (ex["module"], ex["class_type"], ex["class_name"],
             ex["file_path"], ex["example_code"]),
        )
    conn.commit()
    print(f"  ✅ {len(examples)} golden examples saved")

    # ── Step 5: FTS5 indexing ───────────────────────────────────────
    print("\n[5/5] Building FTS5 full-text search indexes...")
    migrate_fts5(conn)
    populate_fts5(conn)
    print("  ✅ FTS5 indexes built (modules, classes, endpoints, contracts)")

    # ── Update meta ─────────────────────────────────────────────────
    conn.execute(
        "INSERT OR REPLACE INTO meta (key, value) VALUES ('phase35_at', ?)",
        (__import__("datetime").datetime.now().isoformat(),),
    )
    conn.execute(
        "INSERT OR REPLACE INTO meta (key, value) VALUES ('parser_version', '1.1.0')"
    )
    conn.commit()
    conn.close()

    # ── Summary ─────────────────────────────────────────────────────
    print("\n" + "=" * 60)
    print("📊  Phase 3.5 Summary")
    print("=" * 60)
    print(f"  feature_contracts : {len(all_contracts)} rows")
    print(f"    → with request   : {with_request}")
    print(f"    → with response  : {with_response}")
    print(f"  module_exports    : {len(exports)} rows")
    print(f"  module_coupling   : {len(coupling)} rows")
    print(f"  module_examples   : {len(examples)} rows")
    print("  FTS5 tables       : modules, classes, endpoints, contracts")
    print(f"\n  DB: {DB_PATH}")
    print("=" * 60)
    print("\n✅  Done! Run phase35-mcp-update.py to add new MCP tools.")


if __name__ == "__main__":
    main()
