#!/usr/bin/env python3
"""
CodeCortex Knowledge MCP Server (Python)
==========================================
Exposes knowledge.db (SQLite) as MCP tools for GitHub Copilot Agent / Claude.

Tools:
  list_modules            — all backend modules with descriptions
  get_module_info         — full info for one module
  resolve_dependencies    — transitive deps for a set of modules
  list_endpoints          — REST endpoints, filter by module / method
  list_classes            — Java classes by module & type
  list_migrations         — DB migration files by module
  list_config_keys        — application.yml keys by module
  list_frontend_views     — Vue views by module
  list_frontend_api       — Frontend API functions by module
  list_routes             — Vue Router routes by module / scope
  get_fe_be_bindings      — FE API function → BE endpoint map for a module
  query_db                — Raw read-only SELECT query

  [Phase 3.5 new tools]
  search_code_entity      — FTS5 full-text search across classes / endpoints / contracts
  get_feature_contract    — Request + response DTO schemas for an endpoint (eliminates DTO guessing)
  get_entity_details      — Full class info + example source code by class name
  get_module_coupling     — Which classes a module imports from another module
  get_module_exports      — Which classes a module exposes to others
  get_example_code        — Golden example source code per module + class type

  [Phase 5a new tools]
  get_code_constraints    — Implicit business rules from exceptions/logs (eliminates missing validation)

  [Phase 5b new tools]
  get_test_rules          — JUnit test method rules (behavioral boundaries AI must not break)
"""

import json
import os
import sqlite3
import sys
from pathlib import Path
from collections import defaultdict

from mcp.server.fastmcp import FastMCP

# ── DB ─────────────────────────────────────────────────────────────────────
DB_PATH = os.environ.get(
    "DB_PATH",
    str(Path(__file__).resolve().parent.parent / "knowledge.db"),
)

if not Path(DB_PATH).exists():
    sys.stderr.write(f"[codecortex-mcp] DB not found: {DB_PATH}\n")
    sys.exit(1)


def get_conn() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def rows(sql: str, params: tuple = ()) -> list[dict]:
    conn = get_conn()
    try:
        cur = conn.execute(sql, params)
        return [dict(r) for r in cur.fetchall()]
    finally:
        conn.close()


def row(sql: str, params: tuple = ()) -> dict | None:
    result = rows(sql, params)
    return result[0] if result else None


def resolve_transitive_deps(seeds: list[str]) -> list[str]:
    """BFS: collect all transitive dependencies for the given modules."""
    visited: set[str] = set(seeds)
    queue = list(seeds)
    while queue:
        mod = queue.pop(0)
        for r in rows("SELECT depends_on FROM module_deps WHERE module = ?", (mod,)):
            dep = r["depends_on"]
            if dep != "_root_" and dep not in visited:
                visited.add(dep)
                queue.append(dep)
    for s in seeds:
        visited.discard(s)
    return sorted(visited)


def is_safe_query(sql: str) -> bool:
    t = sql.strip().lower()
    return t.startswith(("select", "with", "explain"))


# ── MCP server ─────────────────────────────────────────────────────────────
mcp = FastMCP("codecortex-knowledge")


@mcp.tool()
def list_modules() -> str:
    """
    List all backend modules in the IoT platform template with their
    descriptions and file counts. Use this first to see what modules exist.
    """
    mods = rows(
        "SELECT name, package, description, file_count FROM modules "
        "WHERE name != '_root_' ORDER BY name"
    )
    lines = [
        f"• {m['name']} ({m['file_count']} files)\n"
        f"  Package: {m['package']}\n"
        f"  Description: {m['description']}"
        for m in mods
    ]
    return "\n\n".join(lines)


@mcp.tool()
def get_module_info(module: str) -> str:
    """
    Get detailed information about a single module: classes, REST endpoints,
    DB migrations, config keys, frontend views, and all dependencies.

    Args:
        module: Module name, e.g. 'announcement', 'auth', 'workflow'
    """
    mod = row("SELECT * FROM modules WHERE name = ?", (module,))
    if not mod:
        return f"Module '{module}' not found."

    direct_deps = [r["depends_on"] for r in rows(
        "SELECT depends_on FROM module_deps WHERE module = ? ORDER BY depends_on",
        (module,)
    )]
    transitive = resolve_transitive_deps([module])

    classes = rows(
        "SELECT class_name, class_type FROM classes WHERE module = ? ORDER BY class_type, class_name",
        (module,)
    )
    endpoints = rows(
        "SELECT http_method, path, method FROM endpoints WHERE module = ? ORDER BY path",
        (module,)
    )
    migrations = rows(
        "SELECT version, filename FROM migrations WHERE module = ? ORDER BY version",
        (module,)
    )
    config_keys = rows(
        "SELECT key, value_sample FROM config_keys WHERE module = ?", (module,)
    )
    views = rows("SELECT name FROM frontend_views WHERE module = ?", (module,))
    api_fns = rows(
        "SELECT name, http_method, api_path FROM frontend_api_functions "
        "WHERE module = ? AND http_method != '' ORDER BY name",
        (module,)
    )

    by_type: dict[str, list[str]] = defaultdict(list)
    for c in classes:
        by_type[c["class_type"]].append(c["class_name"])

    out = [
        f"## Module: {module}",
        f"Package: {mod['package']}",
        f"Description: {mod['description']}",
        f"Files: {mod['file_count']}",
        f"\n### Direct Dependencies",
        "\n".join(f"  • {d}" for d in direct_deps) or "  (none)",
        f"\n### Transitive Dependencies",
        "\n".join(f"  • {d}" for d in transitive) or "  (none)",
        "\n### Classes",
    ]
    for t, names in sorted(by_type.items()):
        out.append(f"  [{t}] {', '.join(names)}")

    out.append("\n### REST Endpoints")
    out.append(
        "\n".join(f"  {e['http_method']:<6} {e['path']:<50} → {e['method']}()" for e in endpoints)
        or "  (none)"
    )

    out.append("\n### DB Migrations")
    out.append(
        "\n".join(f"  {m['version']}: {m['filename']}" for m in migrations) or "  (none)"
    )

    out.append("\n### Config Keys")
    out.append(
        "\n".join(f"  {c['key']}: {c['value_sample']}" for c in config_keys) or "  (none)"
    )

    out.append("\n### Frontend Views")
    out.append("\n".join(f"  • {v['name']}" for v in views) or "  (none)")

    out.append("\n### Frontend API Functions")
    out.append(
        "\n".join(
            f"  {f['http_method']:<6} {f['api_path']:<50} → {f['name']}()"
            for f in api_fns
        ) or "  (none)"
    )

    return "\n".join(out)


@mcp.tool()
def resolve_dependencies(modules: list[str]) -> str:
    """
    Given a list of desired modules, find ALL transitive dependencies required.
    Returns the complete module set + DB migration files to include.

    Args:
        modules: List of module names, e.g. ['announcement', 'workflow']
    """
    transitive = resolve_transitive_deps(modules)
    all_mods = sorted(set(modules) | set(transitive) - {"_root_"})

    placeholders = ",".join("?" * len(all_mods))
    migrations = rows(
        f"SELECT module, filename, version FROM migrations "
        f"WHERE module IN ({placeholders}) ORDER BY version",
        tuple(all_mods),
    )

    migs_by_mod: dict[str, list[str]] = defaultdict(list)
    for m in migrations:
        migs_by_mod[m["module"]].append(m["filename"])

    out = [
        "## Dependency Resolution",
        f"\nRequested: {', '.join(modules)}",
        f"Additional (transitive): {', '.join(transitive) or '(none)'}",
        f"\n### Full Module Set ({len(all_mods)} modules)",
        "\n".join(f"  • {m}" for m in all_mods),
        f"\n### DB Migrations to Include ({len(migrations)} files)",
    ]
    for mod in all_mods:
        files = migs_by_mod.get(mod, [])
        if files:
            out.append(f"  [{mod}]")
            out.extend(f"    {f}" for f in files)

    return "\n".join(out)


@mcp.tool()
def list_endpoints(module: str = "", http_method: str = "") -> str:
    """
    List REST API endpoints. Optionally filter by module and/or HTTP method.

    Args:
        module: Filter by module name, e.g. 'auth' (empty = all)
        http_method: Filter by method: GET, POST, PUT, DELETE, PATCH (empty = all)
    """
    sql = "SELECT http_method, path, controller, method, module FROM endpoints WHERE 1=1"
    params: list = []
    if module:
        sql += " AND module = ?"
        params.append(module)
    if http_method:
        sql += " AND http_method = ?"
        params.append(http_method.upper())
    sql += " ORDER BY module, path"

    eps = rows(sql, tuple(params))
    if not eps:
        return "No endpoints found."
    lines = [
        f"{e['http_method']:<6} {e['path']:<55} [{e['module']}]  → {e['controller']}.{e['method']}()"
        for e in eps
    ]
    return "\n".join(lines)


@mcp.tool()
def list_classes(module: str = "", class_type: str = "") -> str:
    """
    List Java classes filtered by module and/or type.

    Args:
        module: Module name (empty = all)
        class_type: One of: controller, service, repository, entity, enum, component, config, other (empty = all)
    """
    sql = "SELECT class_name, class_type, module, file_path FROM classes WHERE 1=1"
    params: list = []
    if module:
        sql += " AND module = ?"
        params.append(module)
    if class_type and class_type != "all":
        sql += " AND class_type = ?"
        params.append(class_type)
    sql += " ORDER BY module, class_type, class_name"

    cls = rows(sql, tuple(params))
    lines = [
        f"[{c['module']}] {c['class_type']:<12} {c['class_name']:<40} {c['file_path']}"
        for c in cls
    ]
    return "\n".join(lines) or "No classes found."


@mcp.tool()
def list_migrations(modules: list[str] | None = None) -> str:
    """
    List Flyway DB migration SQL files for one or more modules.

    Args:
        modules: List of module names. Pass null/empty for all modules.
    """
    if modules:
        ph = ",".join("?" * len(modules))
        sql = f"SELECT version, filename, module FROM migrations WHERE module IN ({ph}) ORDER BY version"
        migs = rows(sql, tuple(modules))
    else:
        migs = rows("SELECT version, filename, module FROM migrations ORDER BY version")

    lines = [f"{m['version']:<12} [{m['module']:<15}] {m['filename']}" for m in migs]
    return f"{len(migs)} migration files:\n\n" + "\n".join(lines)


@mcp.tool()
def list_config_keys(module: str = "") -> str:
    """
    List application.yml configuration keys, optionally filtered by module.

    Args:
        module: Module name (empty = all)
    """
    sql = "SELECT key, value_sample, module FROM config_keys WHERE 1=1"
    params: list = []
    if module:
        sql += " AND module = ?"
        params.append(module)
    sql += " ORDER BY module, key"

    keys = rows(sql, tuple(params))
    lines = [f"[{k['module']:<10}] {k['key']}: {k['value_sample']}" for k in keys]
    return "\n".join(lines) or "No config keys found."


@mcp.tool()
def list_frontend_views(module: str = "") -> str:
    """
    List Vue frontend views, showing which Pinia stores and API functions they use.

    Args:
        module: Backend module name (empty = all)
    """
    sql = (
        "SELECT name, file_path, module, api_calls_json, stores_used_json, components_used_json "
        "FROM frontend_views WHERE 1=1"
    )
    params: list = []
    if module:
        sql += " AND module = ?"
        params.append(module)
    sql += " ORDER BY module, name"

    views = rows(sql, tuple(params))
    blocks = []
    for v in views:
        apis = json.loads(v["api_calls_json"])
        stores = json.loads(v["stores_used_json"])
        comps = json.loads(v["components_used_json"])
        details = []
        if stores:
            details.append(f"stores: {', '.join(stores)}")
        if apis:
            snippet = ", ".join(apis[:4]) + ("..." if len(apis) > 4 else "")
            details.append(f"api: {snippet}")
        if comps:
            details.append(f"components: {', '.join(comps)}")
        blocks.append(
            f"[{v['module']:<15}] {v['name']}\n"
            f"    {v['file_path']}\n"
            f"    {' | '.join(details)}"
        )
    return "\n\n".join(blocks) or "No views found."


@mcp.tool()
def list_frontend_api(module: str = "") -> str:
    """
    List frontend API call functions (src/api/**), optionally filtered by module.

    Args:
        module: Backend module name (empty = all)
    """
    sql = (
        "SELECT name, http_method, api_path, module, file_path "
        "FROM frontend_api_functions WHERE 1=1"
    )
    params: list = []
    if module:
        sql += " AND module = ?"
        params.append(module)
    sql += " ORDER BY module, name"

    fns = rows(sql, tuple(params))
    lines = [
        f"[{f['module']:<14}] {(f['http_method'] or '–'):<7} {(f['api_path'] or '–'):<50} → {f['name']}()"
        for f in fns
    ]
    return "\n".join(lines) or "No API functions found."


@mcp.tool()
def list_routes(module: str = "", scope: str = "") -> str:
    """
    List Vue Router routes, optionally filtered by module or scope.

    Args:
        module: Backend module name (empty = all)
        scope: 'TENANT', 'PLATFORM', or empty for all
    """
    sql = "SELECT name, path, component, scope, module FROM frontend_routes WHERE 1=1"
    params: list = []
    if module:
        sql += " AND module = ?"
        params.append(module)
    if scope:
        sql += " AND scope = ?"
        params.append(scope.upper())
    sql += " ORDER BY scope, module, path"

    routes = rows(sql, tuple(params))
    blocks = [
        f"[{r['scope']:<8}][{r['module']:<14}] {r['path']:<50} → {r['name']}\n"
        f"    {r['component']}"
        for r in routes
    ]
    return "\n\n".join(blocks) or "No routes found."


@mcp.tool()
def get_fe_be_bindings(module: str) -> str:
    """
    Show the mapping between frontend API functions and backend REST endpoints
    for a module. Essential for full-stack feature scope planning.

    Args:
        module: Module name, e.g. 'announcement', 'workflow'
    """
    bindings = rows(
        """
        SELECT b.fe_function, b.http_method, b.api_path,
               e.controller, e.method as be_method
        FROM fe_be_bindings b
        JOIN endpoints e ON e.id = b.be_endpoint_id
        WHERE b.fe_module = ?
        ORDER BY b.api_path
        """,
        (module,),
    )
    if not bindings:
        return f"No FE→BE bindings found for module '{module}'."

    blocks = [
        f"{b['http_method']:<6} {b['api_path']}\n"
        f"  FE: {b['fe_function']}()\n"
        f"  BE: {b['controller']}.{b['be_method']}()"
        for b in bindings
    ]
    return f"{len(bindings)} bindings for module '{module}':\n\n" + "\n\n".join(blocks)


@mcp.tool()
def query_db(sql: str, limit: int = 50) -> str:
    """
    Execute a read-only SQL SELECT query against knowledge.db for custom analysis.

    Available tables:
      modules, classes, endpoints, module_deps, migrations, config_keys, imports,
      frontend_api_functions, frontend_stores, frontend_views, frontend_components,
      frontend_routes, fe_be_bindings, meta,
      feature_contracts, module_exports, module_coupling, module_examples,
      code_constraints

    FTS5 search tables (use MATCH keyword):
      modules_fts, classes_fts, endpoints_fts, contracts_fts

    Args:
        sql: A read-only SELECT or WITH query
        limit: Max rows to return (default 50)
    """
    if not is_safe_query(sql):
        return "Error: Only SELECT / WITH / EXPLAIN queries are allowed."

    clean = sql.strip().rstrip(";")
    if "limit" not in clean.lower():
        clean += f" LIMIT {limit}"

    try:
        result = rows(clean)
        if not result:
            return "(no rows returned)"
        header = " | ".join(result[0].keys())
        separator = "-" * len(header)
        data = [" | ".join(str(v) for v in r.values()) for r in result]
        return f"{len(result)} row(s):\n\n" + "\n".join([header, separator, *data])
    except Exception as e:
        return f"SQL Error: {e}"


# ── Phase 3.5 Tools ────────────────────────────────────────────────────────


@mcp.tool()
def search_code_entity(keyword: str, entity_type: str = "all") -> str:
    """
    Full-text search across classes, endpoints, and feature contracts using FTS5.
    Returns compact summaries (no example_code) to save tokens.
    Use get_entity_details() or get_feature_contract() to get full info.

    Args:
        keyword: Search term, e.g. 'announcement', 'login', 'approval', 'tenant'
        entity_type: 'class', 'endpoint', 'contract', or 'all'
    """
    results = []
    safe_kw = keyword.replace('"', '""')

    if entity_type in ("class", "all"):
        hits = rows(
            "SELECT source_rowid, class_name, class_type, module "
            "FROM classes_fts WHERE classes_fts MATCH ? LIMIT 20",
            (safe_kw,),
        )
        for h in hits:
            results.append(
                f"[class/{h['class_type']}] {h['module']}.{h['class_name']}  (id={h['source_rowid']})"
            )

    if entity_type in ("endpoint", "all"):
        hits = rows(
            "SELECT endpoints.id as source_rowid, endpoints.path, endpoints.controller, "
            "endpoints.http_method, endpoints.module "
            "FROM endpoints_fts "
            "JOIN endpoints ON endpoints.id = endpoints_fts.source_rowid "
            "WHERE endpoints_fts MATCH ? LIMIT 20",
            (safe_kw,),
        )
        for h in hits:
            results.append(
                f"[endpoint] {h['http_method']:<6} {h['path']}  [{h['module']}] (id={h['source_rowid']})"
            )

    if entity_type in ("contract", "all"):
        hits = rows(
            "SELECT source_rowid, feature_name, endpoint, request_type, response_type "
            "FROM contracts_fts WHERE contracts_fts MATCH ? LIMIT 20",
            (safe_kw,),
        )
        for h in hits:
            req = h["request_type"] or "–"
            res = h["response_type"] or "–"
            results.append(
                f"[contract] {h['feature_name']}  {h['endpoint']}\n"
                f"           request={req}  response={res}  (id={h['source_rowid']})"
            )

    if not results:
        return f"No results found for '{keyword}' in {entity_type}."

    return f"{len(results)} result(s) for '{keyword}':\n\n" + "\n".join(results)


@mcp.tool()
def get_feature_contract(endpoint_or_feature: str) -> str:
    """
    Get the EXACT request and response DTO schemas for an API endpoint,
    expressed as TypeScript interfaces. Use this to ELIMINATE DTO guessing
    when generating frontend code — never fabricate field names.

    Args:
        endpoint_or_feature: API path (e.g. '/v1/auth/announcements') or
                             feature name (e.g. 'AnnouncementController.create')
    """
    hits = rows(
        """
        SELECT feature_name, http_method, endpoint, request_type, response_type,
               request_schema, response_schema, module,
               COALESCE(NULLIF(api_summary,''), '')         AS api_summary,
               COALESCE(NULLIF(api_description,''), '')     AS api_description,
               COALESCE(NULLIF(request_schema_jsdoc,''),  request_schema)  AS req_schema,
               COALESCE(NULLIF(response_schema_jsdoc,''), response_schema) AS resp_schema
        FROM feature_contracts
        WHERE endpoint LIKE ? OR feature_name LIKE ?
        ORDER BY feature_name
        LIMIT 10
        """,
        (f"%{endpoint_or_feature}%", f"%{endpoint_or_feature}%"),
    )

    if not hits:
        return f"No feature contract found for '{endpoint_or_feature}'."

    blocks = []
    for h in hits:
        block = [
            f"## {h['feature_name']}",
            f"Endpoint: {h['http_method']} {h['endpoint']}  [{h['module']}]",
        ]
        if h.get("api_summary"):
            block.append(f"Summary: {h['api_summary']}")
        if h.get("api_description"):
            block.append(f"Business rule: {h['api_description']}")
        req_schema  = h.get("req_schema")  or h.get("request_schema")  or ""
        if req_schema:
            block.append(f"\n### Request DTO ({h['request_type']})")
            block.append("```typescript")
            block.append(req_schema)
            block.append("```")
        else:
            block.append("\n### Request: (no @RequestBody — query params or path variable only)")

        if resp_schema:
            block.append(f"\n### Response DTO ({h['response_type']})")
            block.append("```typescript")
            block.append(resp_schema)
            block.append("```")
        else:
            block.append("\n### Response: (void or unknown)")

        blocks.append("\n".join(block))

    return "\n\n---\n\n".join(blocks)


@mcp.tool()
def get_entity_details(class_name: str) -> str:
    """
    Get full details for a Java class including all fields, methods,
    and golden example source code.

    Args:
        class_name: Exact class name, e.g. 'AnnouncementController', 'UserService'
    """
    cls = row(
        "SELECT * FROM classes WHERE class_name = ?",
        (class_name,),
    )
    if not cls:
        return f"Class '{class_name}' not found."

    fields = json.loads(cls["fields_json"]) if cls["fields_json"] else []
    methods = json.loads(cls["methods_json"]) if cls["methods_json"] else []

    example = row(
        "SELECT example_code FROM module_examples WHERE class_name = ?",
        (class_name,),
    )

    out = [
        f"## {cls['class_name']}  [{cls['class_type']}]",
        f"Module   : {cls['module']}",
        f"Package  : {cls['full_qualified_name']}",
        f"File     : {cls['file_path']}",
        f"Annotations: {cls['annotations']}",
        f"\n### Fields ({len(fields)})",
    ]
    for f in fields:
        anns = " ".join(f"@{a}" for a in f.get("annotations", []))
        out.append(f"  {f.get('type', '')} {f.get('name', '')}  {anns}")

    out.append(f"\n### Methods ({len(methods)})")
    for m in methods:
        anns = " ".join(f"@{a}" for a in m.get("annotations", []))
        out.append(f"  {m.get('return_type', '')} {m.get('name', '')}({m.get('params', '')})  {anns}")

    if example and example.get("example_code"):
        out.append("\n### Source Code")
        out.append("```java")
        out.append(example["example_code"])
        out.append("```")

    return "\n".join(out)


@mcp.tool()
def get_module_coupling(from_module: str, to_module: str = "") -> str:
    """
    Show which classes a module imports from other modules.
    Use this to understand actual cross-module API usage — much more precise
    than just knowing the dependency exists.

    Args:
        from_module: The module that imports (e.g. 'announcement')
        to_module: Filter to a specific dependency (empty = show all)
    """
    sql = (
        "SELECT to_module, class_name, usage_count "
        "FROM module_coupling WHERE from_module = ?"
    )
    params: list = [from_module]
    if to_module:
        sql += " AND to_module = ?"
        params.append(to_module)
    sql += " ORDER BY to_module, usage_count DESC"

    coupling = rows(sql, tuple(params))
    if not coupling:
        return f"No coupling data found for module '{from_module}'."

    by_target: dict[str, list] = {}
    for c in coupling:
        by_target.setdefault(c["to_module"], []).append(
            f"  • {c['class_name']} (used {c['usage_count']}x)"
        )

    out = [f"## Module Coupling: {from_module} → ..."]
    for target, lines in sorted(by_target.items()):
        out.append(f"\n### → {target}")
        out.extend(lines)

    return "\n".join(out)


@mcp.tool()
def get_module_exports(module: str) -> str:
    """
    Show which classes a module exposes that are imported by other modules.
    This defines the module's PUBLIC API boundary.

    Args:
        module: Module name, e.g. 'common', 'auth'
    """
    exports = rows(
        "SELECT class_name, class_type, import_count "
        "FROM module_exports WHERE module = ? "
        "ORDER BY import_count DESC, class_name",
        (module,),
    )
    if not exports:
        return f"No exports data for module '{module}' (no other module imports from it)."

    out = [f"## Public API of module '{module}' ({len(exports)} exported classes)\n"]
    for e in exports:
        out.append(
            f"  [{e['class_type']:<12}] {e['class_name']:<45} imported by {e['import_count']} module(s)"
        )

    return "\n".join(out)


@mcp.tool()
def get_example_code(module: str, class_type: str) -> str:
    """
    Get the golden example source code for a specific class type in a module.
    Use this for few-shot prompting when generating new code — the example
    shows exact naming conventions, annotation patterns, and code style.

    Args:
        module: Module name, e.g. 'announcement', 'workflow'
        class_type: One of: controller, service, repository, entity, config, component
    """
    ex = row(
        "SELECT class_name, file_path, example_code "
        "FROM module_examples WHERE module = ? AND class_type = ?",
        (module, class_type),
    )
    if not ex:
        # Try any module for the same class_type
        ex = row(
            "SELECT module, class_name, file_path, example_code "
            "FROM module_examples WHERE class_type = ? ORDER BY module LIMIT 1",
            (class_type,),
        )
        if not ex:
            return f"No golden example found for class_type='{class_type}'."
        return (
            f"No example in module '{module}', showing '{ex.get('module','?')}' instead:\n\n"
            f"# {ex['class_name']} ({ex['file_path']})\n```java\n{ex['example_code']}\n```"
        )

    return (
        f"# Golden example: {module}/{class_type} → {ex['class_name']}\n"
        f"# File: {ex['file_path']}\n\n"
        f"```java\n{ex['example_code']}\n```"
    )


# ── Phase 5a Tools ─────────────────────────────────────────────────────────


@mcp.tool()
def get_code_constraints(
    class_name: str = "",
    module: str = "",
    keyword: str = "",
) -> str:
    """
    Query the implicit business constraints extracted from Java source code:
    exception messages, log.warn/error calls, and assertions.

    These reveal the business rules and validation boundaries that the original
    developer encoded — use this BEFORE writing Service layer logic to avoid
    missing critical validations.

    At least one argument is required.

    Args:
        class_name: Filter by class name, e.g. 'AnnouncementService', 'DeptService'
        module:     Filter by module, e.g. 'announcement', 'auth'
        keyword:    Full-text search on constraint messages (FTS5), e.g. '庫存 不足'

    Examples:
        get_code_constraints(class_name="AnnouncementService")
        get_code_constraints(module="auth")
        get_code_constraints(keyword="permission denied")
    """
    if not class_name and not module and not keyword:
        return "Provide at least one of: class_name, module, keyword."

    results: list[dict] = []

    if keyword:
        # FTS5 handles ASCII/English well; for CJK use LIKE fallback
        has_cjk = any('\u4e00' <= ch <= '\u9fff' for ch in keyword)
        if has_cjk:
            results = rows(
                """
                SELECT module_name, class_name, method_name,
                       constraint_type, constraint_message, line_number
                FROM code_constraints
                WHERE constraint_message LIKE ?
                  OR class_name LIKE ?
                ORDER BY module_name, class_name, line_number
                LIMIT 40
                """,
                (f"%{keyword}%", f"%{keyword}%"),
            )
        else:
            safe_kw = keyword.replace('"', '""')
            results = rows(
                """
                SELECT c.module_name, c.class_name, c.method_name,
                       c.constraint_type, c.constraint_message, c.line_number
                FROM code_constraints_fts f
                JOIN code_constraints c ON c.id = f.source_rowid
                WHERE code_constraints_fts MATCH ?
                ORDER BY c.module_name, c.class_name, c.line_number
                LIMIT 40
                """,
                (safe_kw,),
            )
        if module:
            results = [r for r in results if r["module_name"] == module]
        if class_name:
            results = [r for r in results if r["class_name"] == class_name]
    else:
        sql = (
            "SELECT module_name, class_name, method_name, "
            "constraint_type, constraint_message, line_number "
            "FROM code_constraints WHERE 1=1"
        )
        params: list = []
        if class_name:
            sql += " AND class_name = ?"
            params.append(class_name)
        if module:
            sql += " AND module_name = ?"
            params.append(module)
        sql += " ORDER BY class_name, line_number LIMIT 60"
        results = rows(sql, tuple(params))

    if not results:
        label = class_name or module or f'"{keyword}"'
        return f"No code constraints found for {label}."

    # Group by class for readable output
    by_class: dict[str, list[dict]] = {}
    for r in results:
        key = f"{r['module_name']}.{r['class_name']}"
        by_class.setdefault(key, []).append(r)

    out = [f"## Code Constraints ({len(results)} found)\n"]
    for cls_key, items in sorted(by_class.items()):
        out.append(f"### {cls_key}")
        for item in items:
            icon = "🚫" if item["constraint_type"] == "exception" else "⚠️"
            out.append(
                f"  {icon} `{item['method_name']}()`  [{item['constraint_type']}]  L{item['line_number']}"
            )
            out.append(f"     \"{item['constraint_message']}\"")
        out.append("")

    return "\n".join(out)


@mcp.tool()
def get_test_rules(
    class_name: str = "",
    module: str = "",
    keyword: str = "",
) -> str:
    """
    Query JUnit test method names extracted from src/test/java as human-readable
    behavioral rules. Use this BEFORE modifying any Service logic to understand
    what behavioral contracts the tests enforce.

    If any returned rule says "should throw when X", your implementation MUST
    include that guard, or the existing tests will fail.

    At least one argument is required.

    Args:
        class_name: The class under test, e.g. 'WorkflowEngine', 'AnnouncementService'
        module:     Filter by module, e.g. 'workflow', 'announcement'
        keyword:    Full-text search on readable rules, e.g. 'permission', 'cancel'

    Examples:
        get_test_rules(class_name="WorkflowEngine")
        get_test_rules(module="workflow")
        get_test_rules(keyword="should throw permission")
    """
    if not class_name and not module and not keyword:
        return "Provide at least one of: class_name, module, keyword."

    result: list[dict] = []

    if keyword:
        has_cjk = any('\u4e00' <= ch <= '\u9fff' for ch in keyword)
        if has_cjk:
            result = rows(
                """SELECT module_name, class_name, test_method_name, readable_rule
                   FROM test_rules WHERE readable_rule LIKE ? OR class_name LIKE ?
                   ORDER BY module_name, class_name LIMIT 50""",
                (f"%{keyword}%", f"%{keyword}%"),
            )
        else:
            safe_kw = keyword.replace('"', '""')
            result = rows(
                """SELECT t.module_name, t.class_name, t.test_method_name, t.readable_rule
                   FROM test_rules_fts f
                   JOIN test_rules t ON t.id = f.source_rowid
                   WHERE test_rules_fts MATCH ?
                   ORDER BY t.module_name, t.class_name LIMIT 50""",
                (safe_kw,),
            )
        if module:
            result = [r for r in result if r["module_name"] == module]
        if class_name:
            result = [r for r in result if r["class_name"] == class_name]
    else:
        sql = (
            "SELECT module_name, class_name, test_method_name, readable_rule "
            "FROM test_rules WHERE 1=1"
        )
        params: list = []
        if class_name:
            sql += " AND class_name = ?"
            params.append(class_name)
        if module:
            sql += " AND module_name = ?"
            params.append(module)
        sql += " ORDER BY class_name, test_method_name LIMIT 80"
        result = rows(sql, tuple(params))

    if not result:
        label = class_name or module or f'"{keyword}"'
        return f"No test rules found for {label}."

    # Group by class
    by_class: dict[str, list[dict]] = {}
    for r in result:
        key = f"{r['module_name']}.{r['class_name']}"
        by_class.setdefault(key, []).append(r)

    out = [f"## Test Rules ({len(result)} behavioral contracts)\n"]
    for cls_key, items in sorted(by_class.items()):
        out.append(f"### {cls_key}")
        for item in items:
            out.append(f"  ✓ {item['readable_rule']}")
            out.append(f"    (test: `{item['test_method_name']}`)")
        out.append("")

    return "\n".join(out)


# ── Entry ──────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    sys.stderr.write(f"[codecortex-mcp] Starting, DB: {DB_PATH}\n")
    mcp.run(transport="stdio")
