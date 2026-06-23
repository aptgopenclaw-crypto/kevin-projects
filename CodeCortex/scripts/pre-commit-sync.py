#!/usr/bin/env python3
"""
CodeCortex Pre-commit Knowledge Sync
=====================================
Called by .githooks/pre-commit with the list of staged Java/Vue/TS files.
Runs incremental updates for each file, then rebuilds FTS5 for affected modules.

Usage (from the git hook):
    python scripts/pre-commit-sync.py \\
        --db /path/to/knowledge.db \\
        --java path/to/Foo.java path/to/Bar.java \\
        --frontend path/to/view.vue path/to/api.ts

Exit codes:
    0  — success
    1  — error (hook will block the commit)
"""

import argparse
import sys
from pathlib import Path

# ── Allow importing from the scripts directory ─────────────────────────────
SCRIPTS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS_DIR))

# ── Import shared updaters from watch.py ──────────────────────────────────
# watch.py uses the same updater functions we want here
import importlib.util

def _load_watch():
    spec = importlib.util.spec_from_file_location("watch", SCRIPTS_DIR / "watch.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def main():
    ap = argparse.ArgumentParser(description="Pre-commit incremental knowledge sync")
    ap.add_argument("--db",       required=True, help="Path to knowledge.db")
    ap.add_argument("--java",     nargs="*", default=[], help="Changed Java files")
    ap.add_argument("--frontend", nargs="*", default=[], help="Changed Vue/TS files")
    args = ap.parse_args()

    db_path = Path(args.db)
    if not db_path.exists():
        print(f"[pre-commit-sync] DB not found: {db_path}", file=sys.stderr)
        sys.exit(1)

    java_files     = [Path(f).resolve() for f in args.java     if f]
    frontend_files = [Path(f).resolve() for f in args.frontend if f]

    if not java_files and not frontend_files:
        print("[pre-commit-sync] No relevant files to sync.")
        sys.exit(0)

    # Override DB_PATH in watch module so it uses the correct db
    watch = _load_watch()
    watch.DB_PATH = db_path

    errors = []

    # ── Process Java files ─────────────────────────────────────────
    for f in java_files:
        if not f.exists():
            print(f"[pre-commit-sync] Skip (deleted): {f.name}")
            continue
        try:
            conn = watch.get_conn()
            watch.DB_PATH = db_path  # ensure override persists after conn open
            watch.update_java_file(f, conn)
            conn.close()
            print(f"[pre-commit-sync] ✓ Java: {f.name}")
        except Exception as e:
            print(f"[pre-commit-sync] ✗ Java: {f.name} — {e}", file=sys.stderr)
            errors.append(str(e))

    # ── Process frontend files ─────────────────────────────────────
    for f in frontend_files:
        if not f.exists():
            print(f"[pre-commit-sync] Skip (deleted): {f.name}")
            continue
        try:
            conn = watch.get_conn()
            watch.update_frontend_file(f, conn)
            conn.close()
            print(f"[pre-commit-sync] ✓ Frontend: {f.name}")
        except Exception as e:
            print(f"[pre-commit-sync] ✗ Frontend: {f.name} — {e}", file=sys.stderr)
            errors.append(str(e))

    if errors:
        print(f"\n[pre-commit-sync] {len(errors)} error(s) — blocking commit.", file=sys.stderr)
        sys.exit(1)

    total = len(java_files) + len(frontend_files)
    print(f"[pre-commit-sync] Synced {total} file(s) ✓")
    sys.exit(0)


if __name__ == "__main__":
    main()
