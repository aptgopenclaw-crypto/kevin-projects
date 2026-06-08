#!/usr/bin/env python3
"""
MVP: Generate API → DB tables data flow report.

Scans Spring Boot Java source for:
  - @RestController classes
  - @PostMapping / @GetMapping / @PutMapping / @DeleteMapping endpoints
  - Repository.xxx() calls reachable from each endpoint (1-hop call graph)
  - @Entity → @Table → @Column mapping

Outputs a single HTML page listing every API endpoint with the tables it touches.

Usage:
  python3 tools/gen_api_dataflow.py \
      --src backend/src/main/java \
      --out 01-docs/api-dataflow/index.html
"""

from __future__ import annotations
import argparse
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

import javalang  # type: ignore
from jinja2 import Template

# ---------------------------------------------------------------------------
# Data model
# ---------------------------------------------------------------------------

HTTP_ANNOTATIONS = {
    "GetMapping": "GET",
    "PostMapping": "POST",
    "PutMapping": "PUT",
    "DeleteMapping": "DELETE",
    "PatchMapping": "PATCH",
}

# Heuristic: classify repository methods as Read or Write.
WRITE_PREFIXES = ("save", "delete", "remove", "update", "insert", "revoke", "flush")
READ_PREFIXES = ("find", "get", "exists", "count", "query", "search", "list", "load", "read")


@dataclass
class Endpoint:
    http_method: str
    path: str
    controller: str          # fully qualified class name
    method: str              # method name
    file: str                # file path
    line: int


@dataclass
class RepoCall:
    repo_field: str          # e.g. "userRepository"
    repo_type: str           # e.g. "UserRepository" (simple name)
    method: str              # e.g. "findByEmail"
    op: str                  # "R" or "W"
    in_class: str            # class where the call happens
    in_method: str
    file: str
    line: int


@dataclass
class EntityInfo:
    class_name: str          # simple name
    table: str
    columns: List[str] = field(default_factory=list)
    file: str = ""


@dataclass
class ClassInfo:
    name: str                # simple name
    fq: str                  # fully qualified
    file: str
    # field name → simple type name (for "private final UserRepository userRepository")
    fields: Dict[str, str] = field(default_factory=dict)
    # method name → list of method calls (qualifier_simple, method_name, line) made inside that method
    method_calls: Dict[str, List[Tuple[str, str, int]]] = field(default_factory=lambda: defaultdict(list))
    # method name → line of method declaration
    method_lines: Dict[str, int] = field(default_factory=dict)
    # annotations present on the class (simple names)
    class_annotations: Set[str] = field(default_factory=set)
    # method name → list of annotation dicts {name, value (path or "")}
    method_annotations: Dict[str, List[Dict[str, str]]] = field(default_factory=lambda: defaultdict(list))
    # class-level @RequestMapping base path (if any)
    base_path: str = ""


# ---------------------------------------------------------------------------
# Parsing helpers
# ---------------------------------------------------------------------------

def _annotation_value(anno) -> str:
    """Extract the first string value from a Java annotation, or ''."""
    if anno.element is None:
        return ""
    el = anno.element
    # @PostMapping("/x") → ElementValue is a Literal
    if isinstance(el, javalang.tree.Literal):
        return el.value.strip('"')
    # @PostMapping(value="/x", ...) → list of ElementValuePair
    if isinstance(el, list):
        for pair in el:
            if getattr(pair, "name", None) in (None, "value", "path"):
                v = pair.value
                if isinstance(v, javalang.tree.Literal):
                    return v.value.strip('"')
    return ""


def _simple_type(t) -> str:
    """Get simple name of a parsed Java type."""
    if t is None:
        return ""
    if hasattr(t, "name"):
        return t.name
    return str(t)


def parse_file(path: Path) -> Tuple[List[ClassInfo], List[EntityInfo]]:
    """Parse a single .java file into ClassInfo + EntityInfo lists."""
    try:
        src = path.read_text(encoding="utf-8")
    except Exception:
        return [], []
    try:
        tree = javalang.parse.parse(src)
    except Exception:
        return [], []

    package = tree.package.name if tree.package else ""
    classes: List[ClassInfo] = []
    entities: List[EntityInfo] = []

    for ctype in tree.types:
        if not isinstance(ctype, (javalang.tree.ClassDeclaration, javalang.tree.InterfaceDeclaration)):
            continue
        ci = ClassInfo(name=ctype.name, fq=f"{package}.{ctype.name}", file=str(path))
        ci.class_annotations = {a.name for a in (ctype.annotations or [])}

        # class-level @RequestMapping base
        for a in (ctype.annotations or []):
            if a.name == "RequestMapping":
                ci.base_path = _annotation_value(a)

        # Fields: collect "private XxxRepository foo" type bindings
        for member in ctype.body:
            if isinstance(member, javalang.tree.FieldDeclaration):
                tname = _simple_type(member.type)
                for decl in member.declarators:
                    ci.fields[decl.name] = tname

            elif isinstance(member, javalang.tree.MethodDeclaration):
                ci.method_lines[member.name] = member.position.line if member.position else 0
                # method annotations
                for a in (member.annotations or []):
                    if a.name in HTTP_ANNOTATIONS or a.name == "RequestMapping":
                        ci.method_annotations[member.name].append({
                            "name": a.name,
                            "value": _annotation_value(a),
                        })
                # walk method body for MethodInvocation
                if member.body:
                    for _, node in member.filter(javalang.tree.MethodInvocation):
                        qualifier = node.qualifier or ""
                        line = node.position.line if node.position else 0
                        ci.method_calls[member.name].append((qualifier, node.member, line))

        classes.append(ci)

        # @Entity → extract table name + columns
        if "Entity" in ci.class_annotations:
            table_name = ctype.name.lower()
            for a in (ctype.annotations or []):
                if a.name == "Table":
                    if a.element:
                        if isinstance(a.element, list):
                            for pair in a.element:
                                if pair.name == "name" and isinstance(pair.value, javalang.tree.Literal):
                                    table_name = pair.value.value.strip('"')
                        elif isinstance(a.element, javalang.tree.Literal):
                            table_name = a.element.value.strip('"')
            cols = []
            for member in ctype.body:
                if isinstance(member, javalang.tree.FieldDeclaration):
                    col_name = None
                    for a in (member.annotations or []):
                        if a.name == "Column" and a.element:
                            if isinstance(a.element, list):
                                for pair in a.element:
                                    if pair.name == "name" and isinstance(pair.value, javalang.tree.Literal):
                                        col_name = pair.value.value.strip('"')
                            elif isinstance(a.element, javalang.tree.Literal):
                                col_name = a.element.value.strip('"')
                    for decl in member.declarators:
                        cols.append(col_name or _camel_to_snake(decl.name))
            entities.append(EntityInfo(
                class_name=ctype.name,
                table=table_name,
                columns=cols,
                file=str(path),
            ))

    return classes, entities


def _camel_to_snake(s: str) -> str:
    return re.sub(r'(?<!^)(?=[A-Z])', '_', s).lower()


# ---------------------------------------------------------------------------
# Call graph traversal
# ---------------------------------------------------------------------------

def classify_op(method_name: str) -> str:
    n = method_name.lower()
    for p in WRITE_PREFIXES:
        if n.startswith(p):
            return "W"
    for p in READ_PREFIXES:
        if n.startswith(p):
            return "R"
    return "?"


def find_repo_calls(
    ep: Endpoint,
    classes_by_name: Dict[str, ClassInfo],
    entities_by_class: Dict[str, EntityInfo],
    max_depth: int = 4,
) -> List[RepoCall]:
    """
    Walk from endpoint method outwards, following field-injected dependencies,
    collecting any *Repository.xxx() call sites.
    """
    seen: Set[Tuple[str, str]] = set()
    results: List[RepoCall] = []

    def is_repo_type(t: str) -> bool:
        return t.endswith("Repository")

    def walk(class_name: str, method_name: str, depth: int):
        key = (class_name, method_name)
        if key in seen or depth > max_depth:
            return
        seen.add(key)
        ci = classes_by_name.get(class_name)
        if not ci:
            return
        calls = ci.method_calls.get(method_name, [])
        for qualifier, called_method, line in calls:
            if not qualifier:
                continue
            # qualifier is either "userRepository" or "user.something" — take first segment
            head = qualifier.split(".")[0]
            field_type = ci.fields.get(head)
            if not field_type:
                continue
            if is_repo_type(field_type):
                results.append(RepoCall(
                    repo_field=head,
                    repo_type=field_type,
                    method=called_method,
                    op=classify_op(called_method),
                    in_class=class_name,
                    in_method=method_name,
                    file=ci.file,
                    line=line,
                ))
            else:
                # Recurse into the dependency's method
                walk(field_type, called_method, depth + 1)
                # Also try the *Impl variant (interface → impl)
                impl_name = field_type + "Impl"
                if impl_name in classes_by_name:
                    walk(impl_name, called_method, depth + 1)

    # Endpoint's controller is fully qualified; classes_by_name is keyed by simple name.
    walk(ep.controller.split(".")[-1], ep.method, 0)
    return results


def repo_to_entity(repo_type: str, entities_by_class: Dict[str, EntityInfo]) -> Optional[EntityInfo]:
    """Map UserRepository → UserEntity (heuristic)."""
    candidates = [
        repo_type.replace("Repository", "Entity"),
        repo_type.replace("Repository", ""),
    ]
    for c in candidates:
        if c in entities_by_class:
            return entities_by_class[c]
    return None


# ---------------------------------------------------------------------------
# Endpoint extraction
# ---------------------------------------------------------------------------

def collect_endpoints(classes: List[ClassInfo]) -> List[Endpoint]:
    endpoints: List[Endpoint] = []
    for ci in classes:
        if "RestController" not in ci.class_annotations and "Controller" not in ci.class_annotations:
            continue
        for method_name, annos in ci.method_annotations.items():
            for a in annos:
                if a["name"] in HTTP_ANNOTATIONS:
                    path = (ci.base_path + a["value"]) if a["value"].startswith("/") else (ci.base_path + "/" + a["value"] if a["value"] else ci.base_path)
                    endpoints.append(Endpoint(
                        http_method=HTTP_ANNOTATIONS[a["name"]],
                        path=path or "/",
                        controller=ci.fq,
                        method=method_name,
                        file=ci.file,
                        line=ci.method_lines.get(method_name, 0),
                    ))
    return endpoints


# ---------------------------------------------------------------------------
# HTML rendering
# ---------------------------------------------------------------------------

HTML_TEMPLATE = Template(r"""<!doctype html>
<html lang="zh-Hant"><head><meta charset="utf-8">
<title>API → DB Data Flow (MVP)</title>
<style>
:root{--bg:#0d1117;--panel:#161b22;--border:#30363d;--text:#e6edf3;--muted:#8b949e;
      --accent:#58a6ff;--r:#3fb950;--w:#f85149;--q:#d29922}
*{box-sizing:border-box}
body{margin:0;font-family:-apple-system,Segoe UI,Helvetica,"Noto Sans CJK TC",sans-serif;
     background:var(--bg);color:var(--text);line-height:1.55}
.wrap{max-width:1400px;margin:0 auto;padding:24px}
h1{font-size:1.5rem;margin:0 0 6px;border-bottom:1px solid var(--border);padding-bottom:10px}
.meta{color:var(--muted);font-size:.85rem;margin:4px 0 16px}
.toolbar{display:flex;gap:8px;align-items:center;margin:12px 0 18px;flex-wrap:wrap}
.toolbar input{flex:1;min-width:240px;background:#0a0e13;border:1px solid var(--border);
   border-radius:6px;padding:8px 12px;color:var(--text);font-size:.9rem}
.toolbar .chip{background:#21262d;border:1px solid var(--border);padding:4px 10px;
   border-radius:12px;font-size:.78rem;color:var(--muted);cursor:pointer;user-select:none}
.toolbar .chip.active{background:var(--accent);color:#0d1117;border-color:var(--accent);font-weight:600}
.summary{background:var(--panel);border:1px solid var(--border);border-radius:8px;padding:12px 16px;
   margin-bottom:18px;font-size:.85rem;color:var(--muted)}
.summary b{color:var(--text)}
.ep{background:var(--panel);border:1px solid var(--border);border-radius:8px;padding:14px 16px;
    margin-bottom:10px}
.ep .head{display:flex;align-items:center;gap:10px;flex-wrap:wrap;font-family:ui-monospace,Menlo,monospace;font-size:.9rem}
.ep .verb{padding:2px 8px;border-radius:4px;font-size:.72rem;font-weight:700;letter-spacing:.4px}
.verb.GET{background:#1f6feb33;color:#79c0ff}
.verb.POST{background:#3fb95033;color:#7ee787}
.verb.PUT{background:#d2992233;color:#ffd866}
.verb.DELETE{background:#f8514933;color:#ff7b72}
.verb.PATCH{background:#a371f733;color:#d2a8ff}
.ep .path{color:var(--text);font-weight:600}
.ep .ctrl{color:var(--muted);font-size:.78rem;margin-left:auto}
.ep .body{margin-top:10px}
.ep table{width:100%;border-collapse:collapse;font-size:.82rem;margin-top:6px}
.ep th,.ep td{border:1px solid var(--border);padding:5px 8px;text-align:left;vertical-align:top}
.ep th{background:#21262d;color:#c9d1d9;white-space:nowrap;font-weight:600}
.ep td code{background:#0a0e13;padding:1px 5px;border-radius:3px;font-size:.78rem}
.op{display:inline-block;padding:1px 7px;border-radius:10px;font-size:.7rem;font-weight:700}
.op.R{background:#3fb95033;color:var(--r)}
.op.W{background:#f8514933;color:var(--w)}
.op.\?{background:#d2992233;color:var(--q)}
.empty{color:var(--muted);font-style:italic;font-size:.8rem;padding:6px 0}
.tables{display:flex;gap:6px;flex-wrap:wrap;margin-top:8px}
.tables .t{background:#0a0e13;border:1px solid var(--border);border-radius:4px;
   padding:2px 8px;font-size:.75rem;font-family:ui-monospace,Menlo,monospace}
.hidden{display:none}
</style></head><body>
<div class="wrap">
  <h1>🔌 API → DB Data Flow (MVP, auto-generated)</h1>
  <div class="meta">
    Source: <code>{{ src }}</code> ・
    Generated: <code>{{ ts }}</code> ・
    {{ endpoints|length }} endpoints ・
    {{ entities|length }} JPA entities
  </div>

  <div class="summary">
    <b>偵測法</b>：解析 Java AST，從 @RestController 找端點，沿欄位注入呼叫鏈往下追到任何
    <code>*Repository</code> 呼叫，依方法名 prefix 分類 R / W。
    呼叫鏈最深 4 層、不跨第三方類別、不解析 if 分支。<br>
    <b>限制</b>：抓不到「動態 method reference」「lambda 內回呼」「Stream API 多次轉發」的情境；
    Repository → Entity 用命名慣例對應（XxxRepository → XxxEntity / Xxx）。
  </div>

  <div class="toolbar">
    <input id="q" placeholder="搜尋 path / controller / table / repo method...">
    {% for v in ["ALL","GET","POST","PUT","DELETE","PATCH"] %}
    <span class="chip{% if loop.first %} active{% endif %}" data-verb="{{ v }}">{{ v }}</span>
    {% endfor %}
  </div>

  {% for ep in endpoints %}
  <div class="ep" data-verb="{{ ep.http_method }}"
       data-search="{{ ep.path|lower }} {{ ep.controller|lower }} {{ ep.method|lower }} {{ ep.tables_joined|lower }} {{ ep.repos_joined|lower }}">
    <div class="head">
      <span class="verb {{ ep.http_method }}">{{ ep.http_method }}</span>
      <span class="path">{{ ep.path }}</span>
      <span class="ctrl">{{ ep.controller.split('.')[-1] }}#{{ ep.method }}() — {{ ep.file_short }}:{{ ep.line }}</span>
    </div>
    {% if ep.calls %}
    <div class="body">
      <div class="tables">
        {% for t in ep.tables %}<span class="t">{{ t }}</span>{% endfor %}
      </div>
      <table>
        <tr><th>op</th><th>table</th><th>repository method</th><th>in</th><th>file:line</th></tr>
        {% for c in ep.calls %}
        <tr>
          <td><span class="op {{ c.op }}">{{ c.op }}</span></td>
          <td><code>{{ c.table }}</code></td>
          <td><code>{{ c.repo_type }}.{{ c.method }}()</code></td>
          <td><code>{{ c.in_class }}#{{ c.in_method }}</code></td>
          <td><code>{{ c.file_short }}:{{ c.line }}</code></td>
        </tr>
        {% endfor %}
      </table>
    </div>
    {% else %}
    <div class="empty">（未偵測到任何 Repository 呼叫 — 可能是純讀 cache / 委派給未注入欄位的 helper / lambda 內呼叫）</div>
    {% endif %}
  </div>
  {% endfor %}
</div>
<script>
const q = document.getElementById('q');
const chips = document.querySelectorAll('.chip');
let activeVerb = 'ALL';
function apply() {
  const term = q.value.trim().toLowerCase();
  document.querySelectorAll('.ep').forEach(el => {
    const verbOk = activeVerb === 'ALL' || el.dataset.verb === activeVerb;
    const txtOk  = !term || el.dataset.search.includes(term);
    el.classList.toggle('hidden', !(verbOk && txtOk));
  });
}
q.addEventListener('input', apply);
chips.forEach(c => c.addEventListener('click', () => {
  chips.forEach(x => x.classList.remove('active'));
  c.classList.add('active');
  activeVerb = c.dataset.verb;
  apply();
}));
</script>
</body></html>
""")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", required=True, help="Path to Java source root (e.g. backend/src/main/java)")
    ap.add_argument("--out", required=True, help="Output HTML path")
    args = ap.parse_args()

    src_root = Path(args.src).resolve()
    if not src_root.exists():
        sys.exit(f"src not found: {src_root}")

    java_files = list(src_root.rglob("*.java"))
    print(f"Parsing {len(java_files)} Java files under {src_root}...", file=sys.stderr)

    all_classes: List[ClassInfo] = []
    all_entities: List[EntityInfo] = []
    for f in java_files:
        cls, ents = parse_file(f)
        all_classes.extend(cls)
        all_entities.extend(ents)

    classes_by_name = {c.name: c for c in all_classes}
    entities_by_class = {e.class_name: e for e in all_entities}
    print(f"  → {len(all_classes)} classes, {len(all_entities)} entities", file=sys.stderr)

    endpoints = collect_endpoints(all_classes)
    print(f"  → {len(endpoints)} HTTP endpoints", file=sys.stderr)

    # Build view model
    src_repo_root = src_root.parents[3] if len(src_root.parents) >= 4 else src_root

    def short(p: str) -> str:
        try:
            return str(Path(p).resolve().relative_to(src_repo_root))
        except ValueError:
            return p

    view_endpoints = []
    for ep in sorted(endpoints, key=lambda e: (e.path, e.http_method)):
        calls = find_repo_calls(ep, classes_by_name, entities_by_class)
        # dedupe by (repo_type, method, in_class, in_method, line)
        seen = set()
        unique_calls = []
        for c in calls:
            key = (c.repo_type, c.method, c.in_class, c.in_method, c.line)
            if key in seen:
                continue
            seen.add(key)
            unique_calls.append(c)

        enriched = []
        tables: Set[str] = set()
        for c in unique_calls:
            ent = repo_to_entity(c.repo_type, entities_by_class)
            tbl = ent.table if ent else "?"
            tables.add(tbl)
            enriched.append({
                "op": c.op,
                "table": tbl,
                "repo_type": c.repo_type,
                "method": c.method,
                "in_class": c.in_class,
                "in_method": c.in_method,
                "file_short": short(c.file),
                "line": c.line,
            })

        view_endpoints.append({
            "http_method": ep.http_method,
            "path": ep.path,
            "controller": ep.controller,
            "method": ep.method,
            "file_short": short(ep.file),
            "line": ep.line,
            "calls": enriched,
            "tables": sorted(tables),
            "tables_joined": " ".join(sorted(tables)),
            "repos_joined": " ".join({c["repo_type"] + "." + c["method"] for c in enriched}),
        })

    from datetime import datetime
    html = HTML_TEMPLATE.render(
        src=str(src_root),
        ts=datetime.now().isoformat(timespec="seconds"),
        endpoints=view_endpoints,
        entities=all_entities,
    )

    out = Path(args.out).resolve()
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(html, encoding="utf-8")
    print(f"Wrote {out} ({len(html)} bytes)", file=sys.stderr)

    # Stats summary to stdout
    n_with = sum(1 for e in view_endpoints if e["calls"])
    print(f"Endpoints with detected repo calls: {n_with}/{len(view_endpoints)}", file=sys.stderr)


if __name__ == "__main__":
    main()
