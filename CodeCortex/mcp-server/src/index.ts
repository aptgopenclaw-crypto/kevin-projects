#!/usr/bin/env node
/**
 * CodeCortex Knowledge MCP Server
 * ================================
 * Exposes the knowledge.db (SQLite) as MCP tools so that
 * GitHub Copilot Agent / Claude can query the IoT platform's
 * module structure, API endpoints, frontend components, and more.
 *
 * Tools exposed:
 *   list_modules          — list all backend modules with descriptions
 *   get_module_info       — full info for a specific module
 *   resolve_dependencies  — transitive deps for a set of modules
 *   list_endpoints        — HTTP endpoints, optionally filtered by module
 *   list_classes          — classes/entities/repositories by module & type
 *   list_migrations       — DB migration files for a module
 *   list_config_keys      — application config keys by module
 *   list_frontend_views   — Vue views by module
 *   list_frontend_api     — Frontend API functions by module
 *   list_routes           — Frontend routes by module / scope
 *   get_fe_be_bindings    — Show FE API → BE endpoint pairs for a module
 *   query_db              — Raw read-only SQL (SELECT only, safety-checked)
 */

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import Database from "better-sqlite3";
import path from "path";
import { fileURLToPath } from "url";

// ── DB connection ──────────────────────────────────────────────────────────
const DB_PATH =
  process.env.DB_PATH ??
  path.join(fileURLToPath(import.meta.url), "../../../knowledge.db");

let db: Database.Database;
try {
  db = new Database(DB_PATH, { readonly: true });
  db.pragma("journal_mode = WAL");
} catch (err) {
  process.stderr.write(`[codecortex-mcp] Failed to open database: ${DB_PATH}\n`);
  process.stderr.write(`[codecortex-mcp] Error: ${err}\n`);
  process.exit(1);
}

// ── Helpers ────────────────────────────────────────────────────────────────
function rows<T = Record<string, unknown>>(sql: string, params: unknown[] = []): T[] {
  return db.prepare(sql).all(...params) as T[];
}

function row<T = Record<string, unknown>>(sql: string, params: unknown[] = []): T | undefined {
  return db.prepare(sql).get(...params) as T | undefined;
}

/** Resolve transitive module dependencies via BFS */
function resolveTransitiveDeps(seeds: string[]): string[] {
  const visited = new Set<string>(seeds);
  const queue = [...seeds];
  while (queue.length > 0) {
    const mod = queue.shift()!;
    const deps = rows<{ depends_on: string }>(
      "SELECT depends_on FROM module_deps WHERE module = ?",
      [mod]
    );
    for (const { depends_on } of deps) {
      if (!visited.has(depends_on) && depends_on !== "_root_") {
        visited.add(depends_on);
        queue.push(depends_on);
      }
    }
  }
  seeds.forEach((s) => visited.delete(s)); // exclude the seeds themselves
  return [...visited].sort();
}

/** Safety check: only allow SELECT / WITH queries */
function isSafeQuery(sql: string): boolean {
  const trimmed = sql.trim().toLowerCase();
  return (
    trimmed.startsWith("select") ||
    trimmed.startsWith("with") ||
    trimmed.startsWith("explain")
  );
}

// ── MCP Server ─────────────────────────────────────────────────────────────
const server = new McpServer({
  name: "codecortex-knowledge",
  version: "1.0.0",
});

// ── Tool: list_modules ─────────────────────────────────────────────────────
server.tool(
  "list_modules",
  "List all backend modules with their description and file counts. Use this to see what modules are available in the IoT platform template.",
  {},
  async () => {
    const mods = rows(
      "SELECT name, package, description, file_count FROM modules WHERE name != '_root_' ORDER BY name"
    );
    const lines = mods.map(
      (m: any) => `• ${m.name} (${m.file_count} files)\n  Package: ${m.package}\n  Description: ${m.description}`
    );
    return {
      content: [{ type: "text", text: lines.join("\n\n") }],
    };
  }
);

// ── Tool: get_module_info ──────────────────────────────────────────────────
server.tool(
  "get_module_info",
  "Get detailed information about a single module: its classes, endpoints, migrations, config keys, and frontend views.",
  { module: z.string().describe("Module name, e.g. 'announcement', 'auth', 'workflow'") },
  async ({ module }) => {
    const mod = row("SELECT * FROM modules WHERE name = ?", [module]);
    if (!mod) {
      return { content: [{ type: "text", text: `Module '${module}' not found.` }] };
    }

    const classes = rows(
      "SELECT class_name, class_type FROM classes WHERE module = ? ORDER BY class_type, class_name",
      [module]
    );
    const endpoints = rows(
      "SELECT http_method, path, method FROM endpoints WHERE module = ? ORDER BY path",
      [module]
    );
    const migrations = rows(
      "SELECT version, filename FROM migrations WHERE module = ? ORDER BY version",
      [module]
    );
    const configKeys = rows(
      "SELECT key, value_sample FROM config_keys WHERE module = ?",
      [module]
    );
    const views = rows(
      "SELECT name, file_path FROM frontend_views WHERE module = ?",
      [module]
    );
    const apiFns = rows(
      "SELECT name, http_method, api_path FROM frontend_api_functions WHERE module = ? AND http_method != ''",
      [module]
    );
    const directDeps = rows<{ depends_on: string }>(
      "SELECT depends_on FROM module_deps WHERE module = ? ORDER BY depends_on",
      [module]
    );
    const transitive = resolveTransitiveDeps([module]);

    const out: string[] = [];
    out.push(`## Module: ${module}`);
    out.push(`Package: ${(mod as any).package}`);
    out.push(`Description: ${(mod as any).description}`);
    out.push(`Files: ${(mod as any).file_count}`);

    out.push(`\n### Direct Dependencies\n${directDeps.map((d) => `  • ${d.depends_on}`).join("\n") || "  (none)"}`);
    out.push(`\n### Transitive Dependencies\n${transitive.map((d) => `  • ${d}`).join("\n") || "  (none)"}`);

    const byType: Record<string, string[]> = {};
    for (const c of classes as any[]) {
      (byType[c.class_type] ??= []).push(c.class_name);
    }
    out.push("\n### Classes");
    for (const [type, names] of Object.entries(byType)) {
      out.push(`  [${type}] ${names.join(", ")}`);
    }

    out.push("\n### REST Endpoints");
    out.push(
      endpoints.length
        ? (endpoints as any[]).map((e) => `  ${e.http_method.padEnd(6)} ${e.path}  → ${e.method}`).join("\n")
        : "  (none)"
    );

    out.push("\n### DB Migrations");
    out.push(
      migrations.length
        ? (migrations as any[]).map((m) => `  ${m.version}: ${m.filename}`).join("\n")
        : "  (none)"
    );

    out.push("\n### Config Keys");
    out.push(
      configKeys.length
        ? (configKeys as any[]).map((c) => `  ${c.key}: ${c.value_sample}`).join("\n")
        : "  (none)"
    );

    out.push("\n### Frontend Views");
    out.push(views.length ? (views as any[]).map((v) => `  • ${v.name}`).join("\n") : "  (none)");

    out.push("\n### Frontend API Functions");
    out.push(
      apiFns.length
        ? (apiFns as any[]).map((f) => `  ${f.http_method.padEnd(6)} ${f.api_path}  → ${f.name}()`).join("\n")
        : "  (none)"
    );

    return { content: [{ type: "text", text: out.join("\n") }] };
  }
);

// ── Tool: resolve_dependencies ─────────────────────────────────────────────
server.tool(
  "resolve_dependencies",
  "Given a list of desired modules, resolve ALL required transitive dependencies. Returns the full set of modules needed (including infrastructure like common, config, tenant).",
  {
    modules: z
      .array(z.string())
      .describe("List of module names you want to include, e.g. ['announcement', 'workflow']"),
  },
  async ({ modules }) => {
    const transitive = resolveTransitiveDeps(modules);
    const allModules = [...new Set([...modules, ...transitive])].filter(
      (m) => m !== "_root_"
    ).sort();

    // Get migrations for all
    const placeholders = allModules.map(() => "?").join(",");
    const migrations = rows(
      `SELECT module, filename, version FROM migrations WHERE module IN (${placeholders}) ORDER BY version`,
      allModules
    );

    // Group migrations by module
    const migsByMod: Record<string, string[]> = {};
    for (const m of migrations as any[]) {
      (migsByMod[m.module] ??= []).push(m.filename);
    }

    const out: string[] = [];
    out.push(`## Dependency Resolution`);
    out.push(`\nRequested: ${modules.join(", ")}`);
    out.push(`Additional (transitive): ${transitive.join(", ") || "(none)"}`);
    out.push(`\n### Full Module Set (${allModules.length} modules)`);
    out.push(allModules.map((m) => `  • ${m}`).join("\n"));

    out.push(`\n### DB Migrations to Include (${migrations.length} files)`);
    for (const mod of allModules) {
      const files = migsByMod[mod] ?? [];
      if (files.length) {
        out.push(`  [${mod}]`);
        files.forEach((f) => out.push(`    ${f}`));
      }
    }

    return { content: [{ type: "text", text: out.join("\n") }] };
  }
);

// ── Tool: list_endpoints ───────────────────────────────────────────────────
server.tool(
  "list_endpoints",
  "List REST API endpoints, optionally filtered by module.",
  {
    module: z.string().optional().describe("Filter by module name (optional)"),
    http_method: z.enum(["GET", "POST", "PUT", "DELETE", "PATCH", "ALL"]).optional().describe("Filter by HTTP method (optional)"),
  },
  async ({ module, http_method }) => {
    let sql = "SELECT http_method, path, controller, method, module FROM endpoints WHERE 1=1";
    const params: unknown[] = [];
    if (module) { sql += " AND module = ?"; params.push(module); }
    if (http_method && http_method !== "ALL") { sql += " AND http_method = ?"; params.push(http_method); }
    sql += " ORDER BY module, path";

    const eps = rows(sql, params);
    if (!eps.length) {
      return { content: [{ type: "text", text: "No endpoints found." }] };
    }
    const lines = (eps as any[]).map(
      (e) => `${e.http_method.padEnd(6)} ${e.path.padEnd(55)} [${e.module}]  → ${e.controller}.${e.method}()`
    );
    return { content: [{ type: "text", text: lines.join("\n") }] };
  }
);

// ── Tool: list_classes ─────────────────────────────────────────────────────
server.tool(
  "list_classes",
  "List Java classes by module and/or type (controller, service, repository, entity, enum, component, config).",
  {
    module: z.string().optional().describe("Filter by module name"),
    class_type: z
      .enum(["controller", "service", "repository", "entity", "enum", "component", "config", "other", "all"])
      .optional()
      .describe("Filter by class type"),
  },
  async ({ module, class_type }) => {
    let sql = "SELECT class_name, class_type, module, file_path, annotations FROM classes WHERE 1=1";
    const params: unknown[] = [];
    if (module) { sql += " AND module = ?"; params.push(module); }
    if (class_type && class_type !== "all") { sql += " AND class_type = ?"; params.push(class_type); }
    sql += " ORDER BY module, class_type, class_name";

    const cls = rows(sql, params);
    const lines = (cls as any[]).map(
      (c) =>
        `[${c.module}] ${c.class_type.padEnd(12)} ${c.class_name.padEnd(40)} ${c.file_path}`
    );
    return { content: [{ type: "text", text: lines.join("\n") || "No classes found." }] };
  }
);

// ── Tool: list_migrations ──────────────────────────────────────────────────
server.tool(
  "list_migrations",
  "List Flyway DB migration SQL files for one or more modules.",
  {
    modules: z
      .array(z.string())
      .optional()
      .describe("List of module names. Omit for all modules."),
  },
  async ({ modules }) => {
    let sql = "SELECT version, filename, description, module, file_path FROM migrations";
    const params: unknown[] = [];
    if (modules && modules.length > 0) {
      const ph = modules.map(() => "?").join(",");
      sql += ` WHERE module IN (${ph})`;
      params.push(...modules);
    }
    sql += " ORDER BY version";

    const migs = rows(sql, params);
    const lines = (migs as any[]).map(
      (m) => `${m.version.padEnd(12)} [${m.module.padEnd(15)}] ${m.filename}`
    );
    return {
      content: [{ type: "text", text: `${migs.length} migration files:\n\n${lines.join("\n")}` }],
    };
  }
);

// ── Tool: list_config_keys ─────────────────────────────────────────────────
server.tool(
  "list_config_keys",
  "List application.yml configuration keys, optionally filtered by module.",
  {
    module: z.string().optional().describe("Filter by module name"),
  },
  async ({ module }) => {
    let sql = "SELECT key, value_sample, module FROM config_keys WHERE 1=1";
    const params: unknown[] = [];
    if (module) { sql += " AND module = ?"; params.push(module); }
    sql += " ORDER BY module, key";

    const keys = rows(sql, params);
    const lines = (keys as any[]).map(
      (k) => `[${k.module.padEnd(10)}] ${k.key}: ${k.value_sample}`
    );
    return { content: [{ type: "text", text: lines.join("\n") || "No config keys found." }] };
  }
);

// ── Tool: list_frontend_views ──────────────────────────────────────────────
server.tool(
  "list_frontend_views",
  "List Vue frontend views grouped by backend module, showing which stores and API functions they use.",
  {
    module: z.string().optional().describe("Filter by backend module name"),
  },
  async ({ module }) => {
    let sql =
      "SELECT name, file_path, module, api_calls_json, stores_used_json, components_used_json FROM frontend_views WHERE 1=1";
    const params: unknown[] = [];
    if (module) { sql += " AND module = ?"; params.push(module); }
    sql += " ORDER BY module, name";

    const views = rows(sql, params);
    const lines = (views as any[]).map((v) => {
      const apis = JSON.parse(v.api_calls_json) as string[];
      const stores = JSON.parse(v.stores_used_json) as string[];
      const comps = JSON.parse(v.components_used_json) as string[];
      const details: string[] = [];
      if (stores.length) details.push(`stores: ${stores.join(", ")}`);
      if (apis.length) details.push(`api: ${apis.slice(0, 4).join(", ")}${apis.length > 4 ? "..." : ""}`);
      if (comps.length) details.push(`components: ${comps.join(", ")}`);
      return `[${v.module.padEnd(15)}] ${v.name.padEnd(40)}\n    ${v.file_path}\n    ${details.join(" | ")}`;
    });
    return { content: [{ type: "text", text: lines.join("\n\n") || "No views found." }] };
  }
);

// ── Tool: list_frontend_api ────────────────────────────────────────────────
server.tool(
  "list_frontend_api",
  "List frontend API call functions (from src/api/**), optionally filtered by module.",
  {
    module: z.string().optional().describe("Filter by backend module name"),
  },
  async ({ module }) => {
    let sql =
      "SELECT name, http_method, api_path, module, file_path FROM frontend_api_functions WHERE 1=1";
    const params: unknown[] = [];
    if (module) { sql += " AND module = ?"; params.push(module); }
    sql += " ORDER BY module, name";

    const fns = rows(sql, params);
    const lines = (fns as any[]).map(
      (f) =>
        `[${f.module.padEnd(14)}] ${(f.http_method || "–").padEnd(7)} ${(f.api_path || "–").padEnd(50)} → ${f.name}()`
    );
    return { content: [{ type: "text", text: lines.join("\n") || "No API functions found." }] };
  }
);

// ── Tool: list_routes ──────────────────────────────────────────────────────
server.tool(
  "list_routes",
  "List Vue Router routes, optionally filtered by module or scope (TENANT / PLATFORM).",
  {
    module: z.string().optional().describe("Filter by module name"),
    scope: z.enum(["TENANT", "PLATFORM", "ALL"]).optional().describe("Filter by scope"),
  },
  async ({ module, scope }) => {
    let sql = "SELECT name, path, component, scope, module FROM frontend_routes WHERE 1=1";
    const params: unknown[] = [];
    if (module) { sql += " AND module = ?"; params.push(module); }
    if (scope && scope !== "ALL") { sql += " AND scope = ?"; params.push(scope); }
    sql += " ORDER BY scope, module, path";

    const routes = rows(sql, params);
    const lines = (routes as any[]).map(
      (r) =>
        `[${r.scope.padEnd(8)}][${r.module.padEnd(14)}] ${r.path.padEnd(50)} → ${r.name}\n    ${r.component}`
    );
    return { content: [{ type: "text", text: lines.join("\n\n") || "No routes found." }] };
  }
);

// ── Tool: get_fe_be_bindings ───────────────────────────────────────────────
server.tool(
  "get_fe_be_bindings",
  "Show the mapping between frontend API functions and backend REST endpoints for a module. Essential for understanding full-stack feature scope.",
  {
    module: z.string().describe("Module name, e.g. 'announcement', 'workflow'"),
  },
  async ({ module }) => {
    const bindings = rows(
      `SELECT b.fe_function, b.http_method, b.api_path,
              e.controller, e.method as be_method, e.path as be_path
       FROM fe_be_bindings b
       JOIN endpoints e ON e.id = b.be_endpoint_id
       WHERE b.fe_module = ?
       ORDER BY b.api_path`,
      [module]
    );

    if (!bindings.length) {
      return { content: [{ type: "text", text: `No FE→BE bindings found for module '${module}'.` }] };
    }

    const lines = (bindings as any[]).map(
      (b) =>
        `${b.http_method.padEnd(6)} ${b.api_path.padEnd(55)}\n  FE: ${b.fe_function}()\n  BE: ${b.controller}.${b.be_method}()`
    );
    return {
      content: [{ type: "text", text: `${bindings.length} bindings for module '${module}':\n\n${lines.join("\n\n")}` }],
    };
  }
);

// ── Tool: query_db ─────────────────────────────────────────────────────────
server.tool(
  "query_db",
  "Execute a read-only SQL SELECT query against the knowledge.db. Use this for custom analysis not covered by other tools. Available tables: modules, classes, endpoints, module_deps, migrations, config_keys, imports, frontend_api_functions, frontend_stores, frontend_views, frontend_components, frontend_routes, fe_be_bindings, meta.",
  {
    sql: z.string().describe("A read-only SQL SELECT or WITH query"),
    limit: z.number().optional().default(50).describe("Max rows to return (default 50)"),
  },
  async ({ sql: rawSql, limit }) => {
    if (!isSafeQuery(rawSql)) {
      return {
        content: [
          {
            type: "text",
            text: "Error: Only SELECT / WITH / EXPLAIN queries are allowed.",
          },
        ],
      };
    }

    // Inject LIMIT if not present
    let sql = rawSql.trim().replace(/;+$/, "");
    if (!/\bLIMIT\b/i.test(sql)) {
      sql += ` LIMIT ${limit ?? 50}`;
    }

    try {
      const result = rows(sql);
      if (!result.length) {
        return { content: [{ type: "text", text: "(no rows returned)" }] };
      }
      const header = Object.keys(result[0] as object).join(" | ");
      const separator = header.replace(/[^|]/g, "-");
      const dataRows = result.map((r) => Object.values(r as object).join(" | "));
      const table = [header, separator, ...dataRows].join("\n");
      return {
        content: [{ type: "text", text: `${result.length} row(s):\n\n${table}` }],
      };
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      return { content: [{ type: "text", text: `SQL Error: ${msg}` }] };
    }
  }
);

// ── Start server ───────────────────────────────────────────────────────────
const transport = new StdioServerTransport();
await server.connect(transport);
process.stderr.write("[codecortex-mcp] Server started, waiting for requests...\n");
