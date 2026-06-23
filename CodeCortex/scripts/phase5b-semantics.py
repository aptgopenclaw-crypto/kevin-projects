#!/usr/bin/env python3
"""
Phase 5b — Business Semantics Extractor
=========================================
Extracts "what the system SHOULD do" from two sources that developers
are guaranteed to keep up-to-date:

  1. Swagger annotations (@Operation, @Schema) from src/main/java
     → Updates feature_contracts (api_summary, api_description columns)
     → Enriches DTO field metadata for JSDoc-aware TS generation

  2. JUnit test method names (@Test) from src/test/java
     → Populates test_rules table
     → Gives AI knowledge of existing behavioral boundaries

New DB schema:
  feature_contracts.api_summary       TEXT  (new column)
  feature_contracts.api_description   TEXT  (new column)
  feature_contracts.request_schema_jsdoc  TEXT  (new column, schema with JSDoc)
  test_rules                          table (new)
  test_rules_fts                      FTS5 virtual table (standalone, no content=)

Usage:
    source venv/bin/activate
    python scripts/phase5b-semantics.py

    python scripts/phase5b-semantics.py --module workflow   # one module only
    python scripts/phase5b-semantics.py --dry-run           # preview, no writes
"""

import argparse
import re
import sqlite3
import sys
from pathlib import Path

import tree_sitter_java as tsjava
from tree_sitter import Language, Parser

# ── Paths ──────────────────────────────────────────────────────────────────
PROJECT_ROOT = Path(__file__).resolve().parent.parent
BACKEND_MAIN = PROJECT_ROOT / "backend" / "src" / "main" / "java" / "com" / "taipei" / "iot"
BACKEND_TEST = PROJECT_ROOT / "backend" / "src" / "test" / "java" / "com" / "taipei" / "iot"
DB_PATH      = PROJECT_ROOT / "knowledge.db"

JAVA_LANGUAGE = Language(tsjava.language())
java_parser   = Parser(JAVA_LANGUAGE)

# ── Java→TS type map (same as phase35-enhancer) ────────────────────────────
JAVA_TO_TS = {
    "String": "string", "char": "string", "Character": "string",
    "Long": "number", "long": "number", "Integer": "number", "int": "number",
    "Short": "number", "short": "number", "Double": "number", "double": "number",
    "Float": "number", "float": "number", "BigDecimal": "number", "BigInteger": "number",
    "Boolean": "boolean", "boolean": "boolean",
    "LocalDateTime": "string", "LocalDate": "string", "LocalTime": "string",
    "Instant": "string", "Date": "string", "ZonedDateTime": "string",
    "void": "void", "Void": "void", "Object": "unknown",
}


def map_java_type_to_ts(jtype: str) -> str:
    jtype = jtype.strip()
    if jtype in JAVA_TO_TS:
        return JAVA_TO_TS[jtype]
    for lt in ["List", "Set", "Collection", "ArrayList"]:
        m = re.match(rf"^{lt}<(.+)>$", jtype)
        if m:
            return f"{map_java_type_to_ts(m.group(1).strip())}[]"
    return jtype


# ══════════════════════════════════════════════════════════════════════════════
# Tree-sitter helpers
# ══════════════════════════════════════════════════════════════════════════════

def node_text(node, src: bytes) -> str:
    if node is None:
        return ""
    return src[node.start_byte:node.end_byte].decode("utf-8", errors="replace")


def find_child(node, types: list[str]):
    if node is None:
        return None
    for c in node.children:
        if c.type in types:
            return c
    return None


def find_all_descendants(node, type_name: str) -> list:
    out = []
    if node is None:
        return out
    if node.type == type_name:
        out.append(node)
    for c in node.children:
        out.extend(find_all_descendants(c, type_name))
    return out


def _get_annotation_attr(ann_node, key: str, src: bytes) -> str:
    """
    From an annotation node like @Operation(summary="...", description="..."),
    extract the string value of the given key.
    Also handles single-value annotations like @Schema("description text").
    """
    args = find_child(ann_node, ["annotation_argument_list"])
    if args is None:
        return ""

    args_text = node_text(args, src)

    # Named pair: key = "value"
    m = re.search(rf'\b{re.escape(key)}\s*=\s*"([^"]*)"', args_text)
    if m:
        return m.group(1)

    # Single-value shorthand: @Schema("some description")
    if key == "description":
        m = re.search(r'^\s*"([^"]+)"', args_text)
        if m:
            return m.group(1)

    return ""


def _get_annotation_name(ann_node, src: bytes) -> str:
    """Return the identifier name of an annotation node."""
    id_node = find_child(ann_node, ["identifier"])
    return node_text(id_node, src) if id_node else ""


# ══════════════════════════════════════════════════════════════════════════════
# Part 1: @Operation / @Schema extraction
# ══════════════════════════════════════════════════════════════════════════════

def extract_operation_semantics(file_path: Path) -> list[dict]:
    """
    Parse a Controller file and return list of dicts:
      {controller_method, api_summary, api_description}
    """
    try:
        src = file_path.read_bytes()
    except OSError:
        return []

    tree = java_parser.parse(src)
    root = tree.root_node

    # Find base RequestMapping path
    base_path = ""
    for ann in find_all_descendants(root, "annotation"):
        if _get_annotation_name(ann, src) == "RequestMapping":
            val = _get_annotation_attr(ann, "value", src) or _get_annotation_attr(ann, "path", src)
            if not val:
                args = find_child(ann, ["annotation_argument_list"])
                if args:
                    m = re.search(r'"([^"]+)"', node_text(args, src))
                    if m:
                        val = m.group(1)
            if val:
                base_path = val
                break

    results = []
    mapping_map = {
        "GetMapping": "GET", "PostMapping": "POST", "PutMapping": "PUT",
        "DeleteMapping": "DELETE", "PatchMapping": "PATCH",
    }

    for class_node in root.children:
        if class_node.type != "class_declaration":
            continue
        body = find_child(class_node, ["class_body"])
        if not body:
            continue

        for method_node in body.children:
            if method_node.type != "method_declaration":
                continue

            method_id = find_child(method_node, ["identifier"])
            method_name = node_text(method_id, src) if method_id else ""

            # Find the @Operation annotation on this method
            summary = ""
            description = ""
            for ann in find_all_descendants(method_node, "annotation"):
                ann_name = _get_annotation_name(ann, src)
                if ann_name == "Operation":
                    summary     = _get_annotation_attr(ann, "summary", src)
                    description = _get_annotation_attr(ann, "description", src)
                    break

            if not summary and not description:
                continue

            # Reconstruct the full endpoint path to match feature_contracts.endpoint
            http_method = None
            sub_path = ""
            for ann in find_all_descendants(method_node, "annotation"):
                ann_name = _get_annotation_name(ann, src)
                if ann_name in mapping_map:
                    http_method = mapping_map[ann_name]
                    args = find_child(ann, ["annotation_argument_list"])
                    if args:
                        args_text = node_text(args, src)
                        m = re.search(r'(?:value|path)\s*=\s*"([^"]+)"', args_text)
                        sub_path = m.group(1) if m else re.search(r'"([^"]+)"', args_text)
                        if hasattr(sub_path, "group"):
                            sub_path = sub_path.group(1)
                        elif sub_path is None:
                            sub_path = ""
                    break

            # Feature name = ClassName.methodName (matches feature_contracts.feature_name)
            cls_id = find_child(class_node, ["identifier"])
            cls_name = node_text(cls_id, src) if cls_id else ""
            feature_name = f"{cls_name}.{method_name}" if cls_name else method_name

            results.append({
                "feature_name": feature_name,
                "endpoint": base_path + sub_path if http_method else "",
                "api_summary": summary,
                "api_description": description,
            })

    return results


def extract_schema_field_docs(file_path: Path) -> dict[str, dict[str, str]]:
    """
    Parse a DTO class file and return:
      { class_name: { field_name: "@Schema description text" } }
    """
    try:
        src = file_path.read_bytes()
    except OSError:
        return {}

    tree = java_parser.parse(src)
    root = tree.root_node
    result: dict[str, dict[str, str]] = {}

    for class_node in root.children:
        if class_node.type not in ("class_declaration", "record_declaration"):
            continue
        cls_id = find_child(class_node, ["identifier"])
        cls_name = node_text(cls_id, src) if cls_id else ""
        if not cls_name:
            continue

        field_docs: dict[str, str] = {}
        body = find_child(class_node, ["class_body"])
        if not body:
            continue

        for child in body.children:
            if child.type not in ("field_declaration", "record_component"):
                continue

            # Get field name
            if child.type == "field_declaration":
                for decl in child.children:
                    if decl.type == "variable_declarator":
                        name_node = find_child(decl, ["identifier"])
                        field_name = node_text(name_node, src) if name_node else ""
                        break
                else:
                    continue
            else:
                name_node = find_child(child, ["identifier"])
                field_name = node_text(name_node, src) if name_node else ""

            if not field_name:
                continue

            # Find @Schema annotation on this field
            for ann in find_all_descendants(child, "annotation"):
                if _get_annotation_name(ann, src) == "Schema":
                    desc = _get_annotation_attr(ann, "description", src)
                    if desc:
                        field_docs[field_name] = desc
                    break

        if field_docs:
            result[cls_name] = field_docs

    return result


def rebuild_schema_with_jsdoc(
    original_schema: str,
    field_docs: dict[str, str],
    class_name: str,
) -> str:
    """
    Re-emit a TypeScript interface string, inserting JSDoc comments
    before each field that has a @Schema description.

    Input:
      interface Foo {
        id: number;
        title: string;
      }

    Output (if field_docs = {"id": "公告 ID", "title": "標題"}):
      interface Foo {
        /** 公告 ID */
        id: number;
        /** 標題 */
        title: string;
      }
    """
    if not original_schema or not field_docs:
        return original_schema

    lines = original_schema.split("\n")
    new_lines: list[str] = []
    for line in lines:
        # Detect a field line: "  fieldName: type;"
        m = re.match(r'^(\s+)(\w+)\s*:\s*.+;', line)
        if m:
            indent = m.group(1)
            fname  = m.group(2)
            if fname in field_docs:
                new_lines.append(f"{indent}/** {field_docs[fname]} */")
        new_lines.append(line)
    return "\n".join(new_lines)


# ══════════════════════════════════════════════════════════════════════════════
# Part 2: JUnit test rule extraction
# ══════════════════════════════════════════════════════════════════════════════

def _camel_to_words(name: str) -> str:
    """Convert camelCase or snake_case method name to readable English."""
    # First split on underscores
    parts = name.split("_")
    expanded = []
    for part in parts:
        # Then split camelCase
        words = re.sub(r'([a-z])([A-Z])', r'\1 \2', part)
        expanded.append(words)
    readable = " ".join(expanded)
    # Collapse multiple spaces, strip, capitalize first letter
    readable = re.sub(r'\s+', ' ', readable).strip()
    return readable.capitalize() if readable else name


def extract_test_rules(file_path: Path) -> list[dict]:
    """
    Parse a JUnit test file and return list of dicts:
      {module_name, class_name, test_class_name, test_method_name, readable_rule, file_path}
    """
    try:
        src = file_path.read_bytes()
    except OSError:
        return []

    tree = java_parser.parse(src)
    root = tree.root_node

    try:
        rel = file_path.relative_to(BACKEND_TEST)
        module = rel.parts[0] if rel.parts else "_root_"
    except ValueError:
        module = "_root_"

    rel_path = str(file_path.relative_to(PROJECT_ROOT))
    results = []

    for class_node in root.children:
        if class_node.type not in ("class_declaration",):
            continue

        cls_id = find_child(class_node, ["identifier"])
        test_class_name = node_text(cls_id, src) if cls_id else ""
        if not test_class_name:
            continue

        # Derive the class under test (strip Test/Tests suffix)
        target_class = re.sub(r'Tests?$', '', test_class_name)

        body = find_child(class_node, ["class_body"])
        if not body:
            continue

        for method_node in body.children:
            if method_node.type != "method_declaration":
                continue

            # Must have @Test marker annotation
            has_test = any(
                _get_annotation_name(ann, src) == "Test"
                for ann in (
                    find_all_descendants(method_node, "marker_annotation") +
                    find_all_descendants(method_node, "annotation")
                )
            )
            if not has_test:
                continue

            method_id = find_child(method_node, ["identifier"])
            method_name = node_text(method_id, src) if method_id else ""
            if not method_name:
                continue

            readable = _camel_to_words(method_name)
            results.append({
                "module_name":      module,
                "class_name":       target_class,
                "test_class_name":  test_class_name,
                "test_method_name": method_name,
                "readable_rule":    readable,
                "file_path":        rel_path,
            })

    return results


# ══════════════════════════════════════════════════════════════════════════════
# DB schema migration
# ══════════════════════════════════════════════════════════════════════════════

def migrate_schema(conn: sqlite3.Connection):
    """Add Phase 5b columns/tables idempotently."""

    # 1. Extend feature_contracts
    existing_cols = {
        row[1] for row in conn.execute("PRAGMA table_info(feature_contracts)")
    }
    for col, coltype in [
        ("api_summary",          "TEXT DEFAULT ''"),
        ("api_description",      "TEXT DEFAULT ''"),
        ("request_schema_jsdoc", "TEXT DEFAULT ''"),
        ("response_schema_jsdoc","TEXT DEFAULT ''"),
    ]:
        if col not in existing_cols:
            conn.execute(
                f"ALTER TABLE feature_contracts ADD COLUMN {col} {coltype}"
            )

    # 2. test_rules (standalone — no content= FTS5 trap)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS test_rules (
            id                INTEGER PRIMARY KEY AUTOINCREMENT,
            module_name       TEXT NOT NULL,
            class_name        TEXT NOT NULL,
            test_class_name   TEXT NOT NULL,
            test_method_name  TEXT NOT NULL,
            readable_rule     TEXT NOT NULL,
            file_path         TEXT NOT NULL,
            UNIQUE(test_method_name, file_path)
        )
    """)
    conn.execute("CREATE INDEX IF NOT EXISTS idx_tr_class  ON test_rules(class_name)")
    conn.execute("CREATE INDEX IF NOT EXISTS idx_tr_module ON test_rules(module_name)")

    # 3. Standalone FTS5 (no content= — avoids "malformed DB" bug from Phase 3.5)
    conn.execute("""
        CREATE VIRTUAL TABLE IF NOT EXISTS test_rules_fts USING fts5(
            source_rowid UNINDEXED,
            module_name,
            class_name,
            test_method_name,
            readable_rule
        )
    """)

    conn.commit()


# ══════════════════════════════════════════════════════════════════════════════
# DB writers
# ══════════════════════════════════════════════════════════════════════════════

def write_operation_semantics(
    conn: sqlite3.Connection,
    semantics: list[dict],
    dry_run: bool,
):
    """Patch api_summary / api_description into feature_contracts rows."""
    updated = 0
    for s in semantics:
        if dry_run:
            print(f"  [@Operation] {s['feature_name']}")
            if s["api_summary"]:
                print(f"    summary:     {s['api_summary']}")
            if s["api_description"]:
                print(f"    description: {s['api_description']}")
            continue

        conn.execute(
            """UPDATE feature_contracts
               SET api_summary = ?, api_description = ?
               WHERE feature_name = ?""",
            (s["api_summary"], s["api_description"], s["feature_name"]),
        )
        updated += conn.execute(
            "SELECT changes()"
        ).fetchone()[0]

    if not dry_run:
        conn.commit()
    return updated


def write_schema_jsdoc(
    conn: sqlite3.Connection,
    all_field_docs: dict[str, dict[str, str]],
    module: str,
    dry_run: bool,
):
    """
    For each feature_contract row in this module, rebuild request/response
    schema strings with JSDoc inserted.
    """
    contracts = conn.execute(
        "SELECT id, feature_name, request_type, request_schema, "
        "response_type, response_schema "
        "FROM feature_contracts WHERE module = ?",
        (module,),
    ).fetchall()

    updated = 0
    for c in contracts:
        req_jsdoc  = rebuild_schema_with_jsdoc(
            c["request_schema"] or "",
            all_field_docs.get(c["request_type"] or "", {}),
            c["request_type"] or "",
        )
        resp_jsdoc = rebuild_schema_with_jsdoc(
            c["response_schema"] or "",
            all_field_docs.get(c["response_type"] or "", {}),
            c["response_type"] or "",
        )
        if req_jsdoc == (c["request_schema"] or "") and resp_jsdoc == (c["response_schema"] or ""):
            continue

        if dry_run:
            print(f"  [JSDoc] {c['feature_name']}")
            continue

        conn.execute(
            "UPDATE feature_contracts SET request_schema_jsdoc = ?, response_schema_jsdoc = ? WHERE id = ?",
            (req_jsdoc, resp_jsdoc, c["id"]),
        )
        updated += 1

    if not dry_run:
        conn.commit()
    return updated


def write_test_rules(
    conn: sqlite3.Connection,
    rules: list[dict],
    file_path: str,
    dry_run: bool,
):
    """Upsert test_rules rows for one test file."""
    if dry_run:
        for r in rules:
            print(f"  [@Test] {r['test_class_name']}.{r['test_method_name']}()")
            print(f"    Rule: {r['readable_rule']}")
        return len(rules)

    # Delete stale rows
    old_ids = [row[0] for row in conn.execute(
        "SELECT id FROM test_rules WHERE file_path = ?", (file_path,)
    )]
    for oid in old_ids:
        conn.execute("DELETE FROM test_rules_fts WHERE source_rowid = ?", (oid,))
    conn.execute("DELETE FROM test_rules WHERE file_path = ?", (file_path,))

    for r in rules:
        cur = conn.execute(
            """INSERT OR IGNORE INTO test_rules
               (module_name, class_name, test_class_name,
                test_method_name, readable_rule, file_path)
               VALUES (?, ?, ?, ?, ?, ?)""",
            (r["module_name"], r["class_name"], r["test_class_name"],
             r["test_method_name"], r["readable_rule"], r["file_path"]),
        )
        rowid = cur.lastrowid
        if rowid:
            conn.execute(
                """INSERT INTO test_rules_fts
                   (source_rowid, module_name, class_name,
                    test_method_name, readable_rule)
                   VALUES (?, ?, ?, ?, ?)""",
                (rowid, r["module_name"], r["class_name"],
                 r["test_method_name"], r["readable_rule"]),
            )
    conn.commit()
    return len(rules)


# ══════════════════════════════════════════════════════════════════════════════
# Main
# ══════════════════════════════════════════════════════════════════════════════

def main():
    ap = argparse.ArgumentParser(
        description="Phase 5b — Extract Swagger semantics and JUnit test rules"
    )
    ap.add_argument("--module",   help="Only process this module (e.g. workflow)")
    ap.add_argument("--dry-run",  action="store_true", help="Preview without writing")
    ap.add_argument("--db",       default=str(DB_PATH), help="Path to knowledge.db")
    ap.add_argument(
        "--skip-tests",  action="store_true",
        help="Skip scanning src/test (only process Swagger annotations)"
    )
    ap.add_argument(
        "--skip-swagger", action="store_true",
        help="Skip Swagger extraction (only process test rules)"
    )
    args = ap.parse_args()

    db_path = Path(args.db)
    if not db_path.exists():
        print(f"[phase5b] DB not found: {db_path}", file=sys.stderr)
        sys.exit(1)

    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")

    if not args.dry_run:
        migrate_schema(conn)
        print("[phase5b] Schema migrated ✓")

    # ── Determine scan scope ───────────────────────────────────────────
    if args.module:
        main_dirs = [BACKEND_MAIN / args.module]
        test_dirs = [BACKEND_TEST / args.module]
    else:
        main_dirs = [d for d in BACKEND_MAIN.iterdir() if d.is_dir()] if BACKEND_MAIN.exists() else []
        test_dirs = [d for d in BACKEND_TEST.iterdir() if d.is_dir()] if BACKEND_TEST.exists() else []

    total_operations = 0
    total_jsdoc      = 0
    total_tests      = 0

    # ── Part 1: Swagger @Operation ─────────────────────────────────────
    if not args.skip_swagger:
        print("[phase5b] Extracting @Operation semantics from controllers...")
        for mdir in main_dirs:
            module = mdir.name
            for java_file in mdir.rglob("*Controller.java"):
                semantics = extract_operation_semantics(java_file)
                if not semantics:
                    continue
                n = write_operation_semantics(conn, semantics, args.dry_run)
                if not args.dry_run:
                    total_operations += n
                    if n:
                        print(f"  ✓ {module}/{java_file.name}  ({n} @Operation updated)")
                else:
                    total_operations += len(semantics)
                    if semantics:
                        print(f"\n── {java_file.relative_to(PROJECT_ROOT)} ──")

        # ── Part 1b: @Schema field docs → JSDoc ────────────────────────
        print("[phase5b] Extracting @Schema descriptions for JSDoc...")
        for mdir in main_dirs:
            module = mdir.name
            all_field_docs: dict[str, dict[str, str]] = {}
            for java_file in mdir.rglob("*.java"):
                docs = extract_schema_field_docs(java_file)
                all_field_docs.update(docs)

            n = write_schema_jsdoc(conn, all_field_docs, module, args.dry_run)
            total_jsdoc += n
            if n and not args.dry_run:
                print(f"  ✓ {module}  ({n} contract(s) enriched with JSDoc)")

    # ── Part 2: JUnit test rules ────────────────────────────────────────
    if not args.skip_tests and BACKEND_TEST.exists():
        print("[phase5b] Extracting @Test method rules from test directory...")
        for tdir in test_dirs:
            if not tdir.exists():
                continue
            module = tdir.name
            for java_file in tdir.rglob("*.java"):
                rules = extract_test_rules(java_file)
                if not rules:
                    continue
                n = write_test_rules(conn, rules, str(java_file.relative_to(PROJECT_ROOT)), args.dry_run)
                total_tests += n
                if not args.dry_run and n:
                    print(f"  ✓ {module}/{java_file.name}  ({n} test rule(s))")
                elif args.dry_run and rules:
                    print(f"\n── {java_file.relative_to(PROJECT_ROOT)} ──")
    elif not BACKEND_TEST.exists():
        print("[phase5b] No src/test directory found — skipping test rules")

    conn.close()

    print()
    if args.dry_run:
        print(f"[phase5b] [dry-run] Would update: {total_operations} @Operation, "
              f"{total_jsdoc} JSDoc, {total_tests} test rules")
    else:
        print(f"[phase5b] Done:")
        print(f"  @Operation semantics : {total_operations} feature_contracts updated")
        print(f"  @Schema JSDoc        : {total_jsdoc} contract(s) enriched")
        print(f"  Test rules           : {total_tests} @Test method(s) indexed")


if __name__ == "__main__":
    main()
