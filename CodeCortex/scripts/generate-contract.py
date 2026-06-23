#!/usr/bin/env python3
"""
CodeCortex — AI Contract Generator
=====================================
After the AI (or a developer) writes a new Controller + DTO classes, run this
script to:
  1. Parse the new Java files with tree-sitter
  2. Write request/response TypeScript interfaces to knowledge.db
     (feature_contracts table)
  3. Emit concrete TypeScript declaration files to:
       frontend/src/types/generated/<module>.contracts.ts
     that the frontend can import directly

The script is intentionally fast (< 2 s per controller) and idempotent —
running it twice on the same file just overwrites the same rows.

Usage:
    # Single controller file (most common after AI code-gen):
    python scripts/generate-contract.py --file path/to/FooController.java

    # Multiple files at once:
    python scripts/generate-contract.py \\
        --file path/to/FooController.java path/to/BarController.java

    # All controllers in one module:
    python scripts/generate-contract.py --module announcement

    # Regenerate every module (full refresh):
    python scripts/generate-contract.py --all

    # Dry-run: print TypeScript interfaces without writing to disk/DB:
    python scripts/generate-contract.py --file ... --dry-run

VS Code task shortcut: ⇧⌘B → "CodeCortex: Generate Contract (active file)"
"""

import argparse
import importlib.util
import json
import re
import sqlite3
import sys
from pathlib import Path

# ── Paths ──────────────────────────────────────────────────────────────────
SCRIPTS_DIR  = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPTS_DIR.parent
BACKEND_SRC  = PROJECT_ROOT / "backend" / "src" / "main" / "java" / "com" / "taipei" / "iot"
GENERATED_TS = PROJECT_ROOT / "frontend" / "src" / "types" / "generated"
DB_PATH      = PROJECT_ROOT / "knowledge.db"


# ── Load shared helpers from watch.py ─────────────────────────────────────

def _load_watch():
    spec = importlib.util.spec_from_file_location("watch", SCRIPTS_DIR / "watch.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def _load_phase35():
    spec = importlib.util.spec_from_file_location(
        "phase35_enhancer", SCRIPTS_DIR / "phase35-enhancer.py"
    )
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


# ══════════════════════════════════════════════════════════════════════════════
# TypeScript file emitter
# ══════════════════════════════════════════════════════════════════════════════

_TS_FILE_HEADER = """\
// =============================================================================
// AUTO-GENERATED — do not edit manually.
// Source: scripts/generate-contract.py
// Re-generate: python scripts/generate-contract.py --module {module}
// =============================================================================
//
// Request / Response TypeScript interfaces for the `{module}` module.
// These are derived from the Java @RequestBody and return types in the
// corresponding Spring Boot Controllers.
//
// Usage:
//   import type {{ UserCreateRequest, UserResponse }} from '@/types/generated/{module}.contracts';
// =============================================================================

"""


def _format_ts_block(contracts: list[dict]) -> str:
    """
    Build the full TypeScript source for one module's contracts.

    Each endpoint produces an export block:
        // POST /api/users  (UserController.create)
        export interface UserCreateRequest { ... }
        export interface UserResponse { ... }
    """
    emitted_interfaces: set[str] = set()  # avoid duplicate interface declarations
    blocks: list[str] = []

    for c in contracts:
        comment = f"// {c['http_method']} {c['endpoint']}  ({c['feature_name']})"
        lines = [comment]
        has_content = False

        for schema_field, type_field in [
            # Prefer JSDoc-enriched schema if present (Phase 5b), fall back to plain
            ("request_schema", "request_type"),
            ("response_schema", "response_type"),
        ]:
            # Use JSDoc variant when available
            jsdoc_key = schema_field.replace("_schema", "_schema_jsdoc")
            schema = (c.get(jsdoc_key) or c.get(schema_field, "")).strip()
            type_name = c.get(type_field, "").strip()

            if not schema or not type_name:
                continue

            # If it's a comment-only line (e.g. "// String → string"), skip
            if schema.startswith("// ") and "\n" not in schema:
                continue

            # Extract each `interface Foo { ... }` block from the schema
            # The schema may contain multiple interfaces (e.g. PageResponse<T> + inner DTO)
            for iface_match in re.finditer(
                r'((?:/\*\*[^*]*\*/\s*)?interface\s+\w[\w<>, ]*\s*\{[^}]*\})',
                schema,
                re.DOTALL,
            ):
                iface_text = iface_match.group(1).strip()

                # Extract interface name to dedup
                name_match = re.search(r'interface\s+(\w+)', iface_text)
                iface_name = name_match.group(1) if name_match else ""

                if iface_name and iface_name in emitted_interfaces:
                    continue
                if iface_name:
                    emitted_interfaces.add(iface_name)

                # Add export keyword
                exported = iface_text.replace("interface ", "export interface ", 1)
                lines.append(exported)
                has_content = True

        if has_content:
            blocks.append("\n".join(lines))

    return "\n\n".join(blocks)


def emit_ts_file(module: str, contracts: list[dict], dry_run: bool = False) -> Path | None:
    """
    Write TypeScript interfaces to frontend/src/types/generated/<module>.contracts.ts
    Returns the written path, or None on dry-run.
    """
    ts_body = _format_ts_block(contracts)
    if not ts_body.strip():
        return None

    header = _TS_FILE_HEADER.format(module=module)
    full_content = header + ts_body + "\n"

    if dry_run:
        print(f"\n{'─'*60}")
        print(f"[dry-run] Would write: frontend/src/types/generated/{module}.contracts.ts")
        print(full_content)
        return None

    GENERATED_TS.mkdir(parents=True, exist_ok=True)
    out_path = GENERATED_TS / f"{module}.contracts.ts"
    out_path.write_text(full_content, encoding="utf-8")
    return out_path


# ══════════════════════════════════════════════════════════════════════════════
# Source-based type enrichment
# ══════════════════════════════════════════════════════════════════════════════

def _enrich_type_dict_from_source(
    module_dir: Path,
    type_dict: dict,
    watch_mod,
) -> dict:
    """
    Re-parse all Java files in the module directory with tree-sitter and merge
    their fields into type_dict.  This is needed for two reasons:
      1. The initial DB parse may have stored empty types for some classes.
      2. AI-generated DTO files are NEW and not yet in the DB at all.

    Returns the enriched type_dict (mutates in place and returns for convenience).
    """
    import tree_sitter_java as tsjava
    from tree_sitter import Language, Parser

    java_lang   = Language(tsjava.language())
    java_parser = Parser(java_lang)

    node_text        = watch_mod.node_text
    find_child       = watch_mod.find_child
    find_all_descendants = watch_mod.find_all_descendants

    def _parse_fields_from_source(class_node, source_bytes: bytes) -> list[dict]:
        """Minimal field parser that reliably captures the Java type."""
        fields = []
        body = find_child(class_node, ["class_body", "record_declaration",
                                       "interface_body"])
        if body is None:
            return fields
        for child in body.children:
            if child.type not in ("field_declaration", "record_component"):
                continue
            # For record_component: type is a direct child type_identifier/generic_type
            if child.type == "record_component":
                type_node = find_child(child, [
                    "type_identifier", "generic_type", "array_type", "integral_type",
                    "floating_point_type", "boolean_type",
                ])
                name_node = find_child(child, ["identifier"])
                if name_node:
                    fields.append({
                        "name": node_text(name_node, source_bytes),
                        "type": node_text(type_node, source_bytes) if type_node else "",
                        "annotations": [],
                    })
                continue

            # field_declaration: modifiers? type declarator+
            type_node = None
            for c in child.children:
                if c.type in ("type_identifier", "generic_type", "array_type",
                              "integral_type", "floating_point_type", "boolean_type"):
                    type_node = c
                    break
            if type_node is None:
                continue
            ftype = node_text(type_node, source_bytes)

            for decl in child.children:
                if decl.type == "variable_declarator":
                    name_node = find_child(decl, ["identifier"])
                    if name_node:
                        fields.append({
                            "name": node_text(name_node, source_bytes),
                            "type": ftype,
                            "annotations": [],
                        })
        return fields

    if not module_dir.exists():
        return type_dict

    for java_file in module_dir.rglob("*.java"):
        try:
            source_bytes = java_file.read_bytes()
        except OSError:
            continue

        tree = java_parser.parse(source_bytes)
        for node in tree.root_node.children:
            if node.type not in ("class_declaration", "record_declaration",
                                 "interface_declaration", "enum_declaration"):
                continue
            name_node = find_child(node, ["identifier"])
            if not name_node:
                continue
            class_name = node_text(name_node, source_bytes)
            fields = _parse_fields_from_source(node, source_bytes)

            # Only enrich if we actually got typed fields (avoids wiping good
            # DB data with a bad re-parse of a different class kind)
            if fields and any(f["type"] for f in fields):
                entry = type_dict.setdefault(class_name, {
                    "fields": [], "annotations": "", "file_path": "", "module": "",
                })
                entry["fields"] = fields   # prefer fresh source over DB

    return type_dict


# ══════════════════════════════════════════════════════════════════════════════
# Core processor
# ══════════════════════════════════════════════════════════════════════════════

def get_module_name(file_path: Path) -> str:
    """Derive module name from Java source file path."""
    try:
        rel = file_path.relative_to(BACKEND_SRC)
        return rel.parts[0] if rel.parts else "_root_"
    except ValueError:
        return "_root_"


def process_controller_file(
    file_path: Path,
    conn: sqlite3.Connection,
    phase35,
    watch_mod,
    dry_run: bool = False,
) -> dict:
    """
    Parse one Controller Java file, update DB, emit .ts file.
    Returns a result summary dict.
    """
    file_path = file_path.resolve()
    if not file_path.exists():
        return {"file": str(file_path), "status": "missing", "contracts": 0}

    module = get_module_name(file_path)
    type_dict = phase35.build_type_dict(conn)

    # Enrich type_dict by re-parsing source files in this module.
    # This captures: (a) new AI-generated DTOs not yet in DB,
    # (b) classes whose types were stored empty by the initial analyzer.
    module_dir = BACKEND_SRC / module
    _enrich_type_dict_from_source(module_dir, type_dict, watch_mod)

    contracts = phase35.extract_method_contracts(file_path, type_dict)

    if not contracts:
        return {"file": file_path.name, "status": "no_contracts", "contracts": 0, "module": module}

    for c in contracts:
        c["module"] = module

    if not dry_run:
        # ── Update DB ─────────────────────────────────────────────────
        for c in contracts:
            # Preserve Phase 5b JSDoc columns if they already exist
            existing = conn.execute(
                "SELECT request_schema_jsdoc, response_schema_jsdoc "
                "FROM feature_contracts WHERE feature_name = ?",
                (c["feature_name"],),
            ).fetchone()
            req_jsdoc  = (existing["request_schema_jsdoc"]  if existing else "") or ""
            resp_jsdoc = (existing["response_schema_jsdoc"] if existing else "") or ""

            conn.execute(
                "DELETE FROM feature_contracts WHERE feature_name = ?",
                (c["feature_name"],)
            )
            conn.execute(
                """INSERT INTO feature_contracts
                   (feature_name, http_method, endpoint, request_type, response_type,
                    request_schema, response_schema, module,
                    request_schema_jsdoc, response_schema_jsdoc)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (
                    c["feature_name"], c["http_method"], c["endpoint"],
                    c["request_type"], c["response_type"],
                    c["request_schema"], c["response_schema"], c["module"],
                    req_jsdoc, resp_jsdoc,
                ),
            )

        # ── Rebuild contracts_fts for this module ──────────────────────
        for fc in conn.execute(
            "SELECT id FROM feature_contracts WHERE module = ?", (module,)
        ):
            conn.execute("DELETE FROM contracts_fts WHERE source_rowid = ?", (fc["id"],))

        for r in conn.execute(
            "SELECT id, feature_name, endpoint, request_type, response_type, "
            "request_schema, response_schema FROM feature_contracts WHERE module = ?",
            (module,),
        ):
            conn.execute(
                "INSERT INTO contracts_fts(source_rowid, feature_name, endpoint, "
                "request_type, response_type, request_schema, response_schema) "
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                (
                    r["id"], r["feature_name"], r["endpoint"],
                    r["request_type"], r["response_type"],
                    r["request_schema"], r["response_schema"],
                ),
            )
        conn.commit()

    # ── Emit TypeScript declaration file ──────────────────────────────
    # Re-load contracts from DB to pick up JSDoc columns written by Phase 5b
    if not dry_run:
        db_contracts = [
            dict(r) for r in conn.execute(
                "SELECT feature_name, http_method, endpoint, request_type, response_type, "
                "request_schema, response_schema, "
                "COALESCE(NULLIF(request_schema_jsdoc,''), request_schema) AS request_schema_jsdoc, "
                "COALESCE(NULLIF(response_schema_jsdoc,''), response_schema) AS response_schema_jsdoc "
                "FROM feature_contracts WHERE module = ?",
                (module,),
            )
        ]
        ts_path = emit_ts_file(module, db_contracts, dry_run=dry_run)
    else:
        ts_path = emit_ts_file(module, contracts, dry_run=dry_run)

    return {
        "file": file_path.name,
        "module": module,
        "status": "ok",
        "contracts": len(contracts),
        "ts_file": str(ts_path.relative_to(PROJECT_ROOT)) if ts_path else None,
    }


def find_controllers_in_module(module_name: str) -> list[Path]:
    """Return all Controller Java files under a given module directory."""
    module_dir = BACKEND_SRC / module_name
    if not module_dir.exists():
        return []
    controllers = []
    for java_file in module_dir.rglob("*.java"):
        name = java_file.stem
        if name.endswith("Controller"):
            controllers.append(java_file)
    return controllers


def find_all_controllers() -> list[Path]:
    """Return all Controller Java files in the backend src tree."""
    controllers = []
    for java_file in BACKEND_SRC.rglob("*.java"):
        if java_file.stem.endswith("Controller"):
            controllers.append(java_file)
    return sorted(controllers)


# ══════════════════════════════════════════════════════════════════════════════
# CLI entry point
# ══════════════════════════════════════════════════════════════════════════════

def main():
    ap = argparse.ArgumentParser(
        description="Generate TypeScript contracts from Java Controllers",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    group = ap.add_mutually_exclusive_group(required=True)
    group.add_argument(
        "--file", "-f", nargs="+", metavar="JAVA_FILE",
        help="One or more Java Controller file paths"
    )
    group.add_argument(
        "--module", "-m", metavar="MODULE",
        help="Process all controllers in this module (e.g. announcement)"
    )
    group.add_argument(
        "--all", "-a", action="store_true",
        help="Regenerate contracts for every module"
    )
    ap.add_argument(
        "--db", default=str(DB_PATH),
        help="Path to knowledge.db (default: project root)"
    )
    ap.add_argument(
        "--dry-run", action="store_true",
        help="Print output without writing to DB or disk"
    )
    args = ap.parse_args()

    db_path = Path(args.db)
    if not db_path.exists():
        print(f"[generate-contract] ERROR: DB not found: {db_path}", file=sys.stderr)
        print("  Run the analyzers first:  python scripts/tree-sitter-analyzer.py", file=sys.stderr)
        sys.exit(1)

    # ── Collect target files ───────────────────────────────────────────────
    if args.file:
        targets = [Path(f).resolve() for f in args.file]
    elif args.module:
        targets = find_controllers_in_module(args.module)
        if not targets:
            print(f"[generate-contract] No controllers found in module: {args.module}")
            sys.exit(0)
    else:  # --all
        targets = find_all_controllers()
        if not targets:
            print("[generate-contract] No controllers found in backend src")
            sys.exit(0)

    # ── Load shared modules ────────────────────────────────────────────────
    watch_mod  = _load_watch()
    phase35    = _load_phase35()

    # ── Connect to DB ──────────────────────────────────────────────────────
    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")

    # ── Process each file ──────────────────────────────────────────────────
    results = []
    errors  = []

    for target in targets:
        try:
            result = process_controller_file(
                target, conn, phase35, watch_mod, dry_run=args.dry_run
            )
            results.append(result)
            status_icon = "✓" if result["status"] == "ok" else "─"
            ts_hint = f"  →  {result['ts_file']}" if result.get("ts_file") else ""
            print(
                f"[generate-contract] {status_icon} {result['file']}"
                f"  ({result.get('contracts', 0)} contracts){ts_hint}"
            )
        except Exception as e:
            errors.append((str(target), str(e)))
            print(f"[generate-contract] ✗ {target.name} — {e}", file=sys.stderr)

    conn.close()

    # ── Summary ────────────────────────────────────────────────────────────
    ok_count    = sum(1 for r in results if r["status"] == "ok")
    total_contracts = sum(r.get("contracts", 0) for r in results)
    ts_files    = [r["ts_file"] for r in results if r.get("ts_file")]

    print()
    if args.dry_run:
        print(f"[generate-contract] [dry-run] Would write {total_contracts} contracts for {ok_count} controller(s)")
    else:
        print(f"[generate-contract] Done: {ok_count} controller(s), {total_contracts} contracts")
        if ts_files:
            print(f"[generate-contract] TypeScript files written:")
            for ts in sorted(set(ts_files)):
                print(f"  {ts}")

    if errors:
        print(f"\n[generate-contract] {len(errors)} error(s):", file=sys.stderr)
        for path, err in errors:
            print(f"  {path}: {err}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
