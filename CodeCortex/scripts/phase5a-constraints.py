#!/usr/bin/env python3
"""
Phase 5a — Code Constraints Extractor
========================================
Scans all Java source files and extracts implicit business rules from:
  1. throw statements:  throw new BusinessException("庫存不足")
  2. log.warn / log.error calls:  log.warn("VIP 折扣不可與優惠券疊加")
  3. assert / Objects.requireNonNull with messages

These strings ARE the business constraints — they tell you exactly what
conditions the original developer decided to enforce and why.

New tables added to knowledge.db:
  code_constraints     — one row per constraint message
  code_constraints_fts — FTS5 index for keyword search

Usage:
    source venv/bin/activate
    python scripts/phase5a-constraints.py

    # Only re-scan one module:
    python scripts/phase5a-constraints.py --module announcement

    # Dry-run (print without writing to DB):
    python scripts/phase5a-constraints.py --dry-run
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
BACKEND_SRC  = PROJECT_ROOT / "backend" / "src" / "main" / "java" / "com" / "taipei" / "iot"
DB_PATH      = PROJECT_ROOT / "knowledge.db"

JAVA_LANGUAGE = Language(tsjava.language())
java_parser   = Parser(JAVA_LANGUAGE)


# ══════════════════════════════════════════════════════════════════════════════
# Tree-sitter helpers  (minimal, no import from other scripts)
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


def _first_string_arg(node, src: bytes) -> str:
    """
    Recursively find the first string_literal inside *node* and return its
    content (with surrounding quotes stripped and escape sequences resolved).
    Returns "" if none found.
    """
    for lit in find_all_descendants(node, "string_literal"):
        raw = node_text(lit, src)
        # strip outer quotes
        if len(raw) >= 2 and raw[0] == '"' and raw[-1] == '"':
            return raw[1:-1]
        return raw
    return ""


def _first_string_arg_concat(node, src: bytes) -> str:
    """
    Like _first_string_arg but also handles simple string concatenation:
      "前綴 " + someVar + " 後綴"
    Returns the concatenated *literal* parts only (ignores variable parts).
    """
    parts = []
    for child in _walk_concat(node, src):
        parts.append(child)
    result = "".join(parts).strip()
    return result if result else _first_string_arg(node, src)


def _walk_concat(node, src: bytes) -> list[str]:
    """Yield string literal pieces from a string concatenation expression."""
    if node is None:
        return []
    if node.type == "string_literal":
        raw = node_text(node, src)
        if len(raw) >= 2:
            yield raw[1:-1]
        return
    if node.type == "binary_expression":
        op = find_child(node, ["+"])
        if op is not None:
            for child in node.children:
                if child.type != "+":
                    yield from _walk_concat(child, src)
            return
    # For any other node, recurse
    for child in node.children:
        yield from _walk_concat(child, src)


# ══════════════════════════════════════════════════════════════════════════════
# Constraint extraction
# ══════════════════════════════════════════════════════════════════════════════

# Log method names we care about
LOG_WARN_ERROR = {"warn", "error"}
LOG_ALL_LEVELS = {"trace", "debug", "info", "warn", "error"}

# Exception class names that signal business constraints
BUSINESS_EXCEPTION_NAMES = {
    "BusinessException", "AppException", "ApiException",
    "ServiceException", "DomainException", "IllegalArgumentException",
    "IllegalStateException", "ValidationException", "AccessDeniedException",
    "ForbiddenException", "NotFoundException", "ConflictException",
    "BadRequestException",
}


def _enclosing_method_name(node) -> str:
    """Walk parent chain to find the enclosing method_declaration name."""
    # tree-sitter nodes don't have a .parent attribute in the Python bindings
    # so we pass method_name explicitly from the caller instead.
    return ""


def extract_constraints(file_path: Path) -> list[dict]:
    """
    Parse one Java file and return a list of constraint dicts:
      {module, class_name, method_name, constraint_type, constraint_message,
       file_path, line_number}
    """
    try:
        src = file_path.read_bytes()
    except OSError:
        return []

    tree = java_parser.parse(src)
    root = tree.root_node

    # Module name from directory
    try:
        rel = file_path.relative_to(BACKEND_SRC)
        module = rel.parts[0] if rel.parts else "_root_"
    except ValueError:
        module = "_root_"

    rel_path = str(file_path.relative_to(PROJECT_ROOT))
    constraints: list[dict] = []

    def _add(class_name, method_name, ctype, msg, node):
        if not msg.strip():
            return
        # Filter out pure format strings with no meaningful text
        if re.match(r'^\{.*\}$', msg.strip()):
            return
        constraints.append({
            "module": module,
            "class_name": class_name,
            "method_name": method_name,
            "constraint_type": ctype,
            "constraint_message": msg.strip(),
            "file_path": rel_path,
            "line_number": node.start_point[0] + 1,
        })

    # ── Walk all class/method declarations ────────────────────────────
    for class_node in root.children:
        if class_node.type not in ("class_declaration", "record_declaration"):
            continue
        class_id = find_child(class_node, ["identifier"])
        class_name = node_text(class_id, src) if class_id else ""

        body = find_child(class_node, ["class_body"])
        if body is None:
            continue

        for method_node in body.children:
            if method_node.type != "method_declaration":
                continue
            method_id = find_child(method_node, ["identifier"])
            method_name = node_text(method_id, src) if method_id else ""

            _scan_method(method_node, class_name, method_name, src, _add)

    return constraints


def _scan_method(method_node, class_name: str, method_name: str, src: bytes, _add):
    """Recursively scan a method body for constraints."""

    def walk(node):
        # ── 1. throw new SomeException("message") ─────────────────────
        if node.type == "throw_statement":
            _handle_throw(node, class_name, method_name, src, _add)

        # ── 2. log.warn("msg") / log.error("msg") / logger.warn(...) ──
        elif node.type == "method_invocation":
            _handle_log(node, class_name, method_name, src, _add)

        # ── 3. Objects.requireNonNull(x, "msg") ───────────────────────
        elif node.type == "method_invocation":
            pass  # handled inside _handle_log with method name check

        # Recurse
        for child in node.children:
            walk(child)

    walk(method_node)


def _handle_throw(node, class_name, method_name, src, _add):
    """
    throw new XxxException("message")
    throw new XxxException(ErrorCode.FOO, "message")
    """
    # Find object_creation_expression inside the throw
    for oc in find_all_descendants(node, "object_creation_expression"):
        type_node = find_child(oc, ["type_identifier"])
        exc_name = node_text(type_node, src) if type_node else ""
        if not exc_name:
            continue

        # Determine constraint type
        if exc_name in BUSINESS_EXCEPTION_NAMES:
            ctype = "exception"
        elif "Exception" in exc_name or "Error" in exc_name:
            ctype = "exception"
        else:
            continue

        arg_list = find_child(oc, ["argument_list"])
        if arg_list is None:
            # throw new XxxException() with no message — still worth recording
            _add(class_name, method_name, ctype, f"[{exc_name}] (no message)", node)
            return

        msg = _first_string_arg_concat(arg_list, src)
        if not msg:
            # Try to grab any identifier that looks like an error code enum
            for child in arg_list.children:
                if child.type in ("field_access", "identifier"):
                    msg = node_text(child, src)
                    break
        if msg:
            _add(class_name, method_name, ctype, msg, node)
        else:
            _add(class_name, method_name, ctype, f"[{exc_name}]", node)
        return  # only process first object_creation per throw


def _handle_log(node, class_name, method_name, src, _add):
    """
    log.warn("msg")  /  log.error("msg")
    logger.warn("msg ...", arg)
    Objects.requireNonNull(x, "msg")
    Assert.notNull(x, "msg")
    """
    # Method name being called
    invoked = find_child(node, ["identifier"])
    invoked_name = node_text(invoked, src) if invoked else ""

    # For log.warn / log.error: object is 'log' or 'logger', method is warn/error
    if invoked_name in LOG_WARN_ERROR:
        arg_list = find_child(node, ["argument_list"])
        if arg_list:
            msg = _first_string_arg_concat(arg_list, src)
            if msg:
                # Trim SLF4J format placeholders for readability
                msg_clean = re.sub(r'\{\}', '…', msg).strip(" -:")
                _add(class_name, method_name, f"{invoked_name}_log", msg_clean, node)

    # Objects.requireNonNull(x, "field must not be null")
    elif invoked_name == "requireNonNull":
        arg_list = find_child(node, ["argument_list"])
        if arg_list:
            args = [c for c in arg_list.children if c.type != ","]
            if len(args) >= 2:
                # second arg is the message
                msg = _first_string_arg_concat(args[1], src)
                if msg:
                    _add(class_name, method_name, "require_nonnull", msg, node)

    # Assert.notNull(x, "msg") / Assert.isTrue(x, "msg")
    elif invoked_name in {"notNull", "notEmpty", "notBlank", "isTrue", "state"}:
        arg_list = find_child(node, ["argument_list"])
        if arg_list:
            args = [c for c in arg_list.children if c.type != ","]
            if len(args) >= 2:
                msg = _first_string_arg_concat(args[-1], src)
                if msg:
                    _add(class_name, method_name, "assertion", msg, node)


# ══════════════════════════════════════════════════════════════════════════════
# DB helpers
# ══════════════════════════════════════════════════════════════════════════════

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS code_constraints (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    module_name        TEXT    NOT NULL,
    class_name         TEXT    NOT NULL,
    method_name        TEXT    NOT NULL,
    constraint_type    TEXT    NOT NULL,
    constraint_message TEXT    NOT NULL,
    file_path          TEXT    NOT NULL,
    line_number        INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_cc_module
    ON code_constraints(module_name);
CREATE INDEX IF NOT EXISTS idx_cc_class
    ON code_constraints(class_name);

-- Standalone FTS5 (no content= to avoid "malformed DB" issues)
CREATE VIRTUAL TABLE IF NOT EXISTS code_constraints_fts USING fts5(
    source_rowid UNINDEXED,
    module_name,
    class_name,
    method_name,
    constraint_type,
    constraint_message
);
"""


def migrate_schema(conn: sqlite3.Connection):
    """Add Phase 5a tables if they don't already exist."""
    for stmt in SCHEMA_SQL.strip().split(";"):
        s = stmt.strip()
        if s:
            conn.execute(s)
    conn.commit()


def upsert_constraints(conn: sqlite3.Connection, constraints: list[dict], file_path: str):
    """Delete old rows for a file, insert new ones, rebuild FTS5."""
    # 1. Collect IDs to delete from FTS5
    old_ids = [r[0] for r in conn.execute(
        "SELECT id FROM code_constraints WHERE file_path = ?", (file_path,)
    )]
    for oid in old_ids:
        conn.execute("DELETE FROM code_constraints_fts WHERE source_rowid = ?", (oid,))
    conn.execute("DELETE FROM code_constraints WHERE file_path = ?", (file_path,))

    # 2. Insert new rows
    for c in constraints:
        cur = conn.execute(
            """INSERT INTO code_constraints
               (module_name, class_name, method_name, constraint_type,
                constraint_message, file_path, line_number)
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            (c["module"], c["class_name"], c["method_name"],
             c["constraint_type"], c["constraint_message"],
             c["file_path"], c["line_number"]),
        )
        rowid = cur.lastrowid
        conn.execute(
            """INSERT INTO code_constraints_fts
               (source_rowid, module_name, class_name, method_name,
                constraint_type, constraint_message)
               VALUES (?, ?, ?, ?, ?, ?)""",
            (rowid, c["module"], c["class_name"], c["method_name"],
             c["constraint_type"], c["constraint_message"]),
        )
    conn.commit()


# ══════════════════════════════════════════════════════════════════════════════
# Main
# ══════════════════════════════════════════════════════════════════════════════

def main():
    ap = argparse.ArgumentParser(description="Phase 5a — Extract code constraints from Java")
    ap.add_argument("--module",   help="Only scan this module (e.g. announcement)")
    ap.add_argument("--file",     help="Only scan this single Java file")
    ap.add_argument("--dry-run",  action="store_true",
                    help="Print constraints without writing to DB")
    ap.add_argument("--db",       default=str(DB_PATH), help="Path to knowledge.db")
    args = ap.parse_args()

    db_path = Path(args.db)
    if not db_path.exists():
        print(f"[phase5a] DB not found: {db_path}", file=sys.stderr)
        sys.exit(1)

    # ── Collect target files ───────────────────────────────────────────
    if args.file:
        targets = [Path(args.file).resolve()]
    elif args.module:
        module_dir = BACKEND_SRC / args.module
        if not module_dir.exists():
            print(f"[phase5a] Module dir not found: {module_dir}", file=sys.stderr)
            sys.exit(1)
        targets = list(module_dir.rglob("*.java"))
    else:
        targets = list(BACKEND_SRC.rglob("*.java"))

    print(f"[phase5a] Scanning {len(targets)} file(s)...")

    if not args.dry_run:
        conn = sqlite3.connect(str(db_path))
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode=WAL")
        migrate_schema(conn)

    total_constraints = 0
    total_files_with_constraints = 0

    for f in targets:
        constraints = extract_constraints(f)
        if not constraints:
            continue

        total_constraints += len(constraints)
        total_files_with_constraints += 1

        if args.dry_run:
            rel = str(f.relative_to(PROJECT_ROOT))
            print(f"\n── {rel} ({len(constraints)} constraints) ──")
            for c in constraints:
                print(f"  [{c['constraint_type']:<16}] {c['class_name']}.{c['method_name']}()  L{c['line_number']}")
                print(f"    \"{c['constraint_message']}\"")
        else:
            rel_path = str(f.relative_to(PROJECT_ROOT))
            upsert_constraints(conn, constraints, rel_path)

    if not args.dry_run:
        conn.close()

    print(f"\n[phase5a] Done: {total_constraints} constraints in {total_files_with_constraints} file(s)")

    if not args.dry_run:
        print(f"[phase5a] Tables updated: code_constraints, code_constraints_fts")


if __name__ == "__main__":
    main()
