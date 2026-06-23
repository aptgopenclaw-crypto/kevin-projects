#!/usr/bin/env python3
"""
Phase 1: tree-sitter Java Code Analyzer
=========================================
Parses a Spring Boot backend project using tree-sitter-java,
extracts structured module information, and stores it in SQLite.

Usage: python scripts/tree-sitter-analyzer.py
"""

import json
import os
import re
import sqlite3
import sys
from collections import defaultdict
from dataclasses import dataclass, field, asdict
from pathlib import Path
from typing import Optional

import tree_sitter_java as tsjava
from tree_sitter import Language, Parser

# ── Paths ──────────────────────────────────────────────────────────────────
PROJECT_ROOT = Path(__file__).resolve().parent.parent
BACKEND_SRC = PROJECT_ROOT / "backend" / "src" / "main" / "java" / "com" / "taipei" / "iot"
BACKEND_RESOURCES = PROJECT_ROOT / "backend" / "src" / "main" / "resources"
DB_PATH = PROJECT_ROOT / "knowledge.db"

# ── Load tree-sitter Java grammar ──────────────────────────────────────────
JAVA_LANGUAGE = Language(tsjava.language())
parser = Parser(JAVA_LANGUAGE)

# ── Data structures ────────────────────────────────────────────────────────
MODULES = {}  # module_name -> ModuleInfo
CLASSES = []  # list of ClassInfo
ENDPOINTS = []
MODULE_DEPS = defaultdict(set)  # module_name -> {dep_module_name}
IMPORTS_BY_FILE = {}  # file_path -> [import strings]


@dataclass
class ModuleInfo:
    name: str
    package: str
    description: str = ""
    file_count: int = 0


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
    "Enum": "enum",
    "Advice": "advice",
    "ControllerAdvice": "advice",
    "RestControllerAdvice": "advice",
}

# Module name -> possible keywords in migration filename
MIGRATION_MODULE_KEYWORDS = {
    "auth": ["auth", "user", "password", "session", "login", "reset_token"],
    "rbac": ["rbac", "role", "permission", "menu"],
    "tenant": ["tenant"],
    "dept": ["dept"],
    "user": ["user"],
    "audit": ["audit", "rev_info", "login_log"],
    "announcement": ["announcement", "announce"],
    "notification": ["notify", "notification"],
    "setting": ["setting"],
    "assettransfer": ["asset_transfer"],
    "workflow": ["workflow"],
    "platform": ["platform"],
}


# ── Tree-sitter helpers ────────────────────────────────────────────────────


def node_text(node, source_bytes: bytes) -> str:
    """Get the text of a tree-sitter node from source bytes."""
    if node is None:
        return ""
    return source_bytes[node.start_byte : node.end_byte].decode("utf-8")


def find_child(node, types: list[str]) -> Optional:
    """Find first direct child node matching one of the given types."""
    if node is None:
        return None
    for child in node.children:
        if child.type in types:
            return child
    return None


def find_children(node, type_name: str) -> list:
    """Find all direct children matching a type."""
    if node is None:
        return []
    return [c for c in node.children if c.type == type_name]


def find_all_descendants(node, type_name: str) -> list:
    """Find ALL descendants (not just direct children) matching a type."""
    results = []
    if node is None:
        return results
    if node.type == type_name:
        results.append(node)
    for child in node.children:
        results.extend(find_all_descendants(child, type_name))
    return results


def find_annotation_by_name(node, annotation_name: str) -> Optional:
    """Find a marker annotation like @Entity, @Service within a node."""
    annotations = find_all_descendants(node, "marker_annotation")
    for ann in annotations:
        for child in ann.children:
            if child.type == "identifier" and child.text.decode("utf-8") == annotation_name:
                return ann
    return None


def get_annotation_identifiers(node) -> list[str]:
    """Get all annotation names from a node (class declaration)."""
    annotations = find_all_descendants(node, "marker_annotation")
    result = []
    for ann in annotations:
        for child in ann.children:
            if child.type == "identifier":
                result.append(child.text.decode("utf-8"))
    # Also check annotation with arguments
    ann_with_args = find_all_descendants(node, "annotation")
    for ann in ann_with_args:
        for child in ann.children:
            if child.type == "identifier":
                result.append(child.text.decode("utf-8"))
    return result


def get_annotation_argument(annotation_node, source_bytes: bytes) -> str:
    """Extract the string value from an annotation like @RequestMapping("/v1/auth/dept")."""
    ann_body = find_child(annotation_node, ["annotation_argument_list"])
    if ann_body:
        return node_text(ann_body, source_bytes)
    return ""


# ── Mapping module name from package ───────────────────────────────────────


def get_module_name(file_path: Path) -> str:
    """Given a Java file path, determine which module it belongs to."""
    try:
        rel_path = file_path.relative_to(BACKEND_SRC)
        parts = rel_path.parts
        # Must be in a subdirectory (module folder), not directly under iot/
        if len(parts) >= 2:
            return parts[0]  # top-level directory = module name
    except ValueError:
        pass
    return "_root_"


# ── Parsing imports ────────────────────────────────────────────────────────


def parse_imports(tree, source_bytes: bytes) -> list[str]:
    """Extract all import statements from a compilation unit."""
    imports = []
    root = tree.root_node
    for child in root.children:
        if child.type == "import_declaration":
            imp_text = node_text(child, source_bytes)
            imports.append(imp_text)
    return imports


# ── Parsing class declarations ─────────────────────────────────────────────


def _get_extended_types(class_node, source_bytes: bytes) -> list[str]:
    """Get the list of types this class/interface extends or implements."""
    types = []
    for child in class_node.children:
        if child.type in ("superclass", "super_interfaces", "extends_interfaces"):
            types.append(node_text(child, source_bytes))
    return types


def _detect_class_type(class_node, annotations: list[str], source_bytes: bytes) -> str:
    """Determine the Spring stereotype of a class node."""
    # 1. Check annotations first
    for ann in annotations:
        if ann in SPRING_STEREOTYPES:
            return SPRING_STEREOTYPES[ann]

    # 2. Check declaration type
    if class_node.type == "enum_declaration":
        return "enum"

    # 3. Check inheritance for repositories
    extended = _get_extended_types(class_node, source_bytes)
    repo_keywords = ["JpaRepository", "JpaSpecificationExecutor", "MongoRepository",
                     "CrudRepository", "PagingAndSortingRepository", "ReactiveCrudRepository",
                     "TenantScopedRepository"]
    for ext in extended:
        for kw in repo_keywords:
            if kw in ext:
                return "repository"

    return "other"


def parse_class_declaration(class_node, source_bytes: bytes, file_path: Path, module_name: str, pkg: str):
    """
    Parse a class/interface/enum declaration and return a ClassInfo dict.
    """
    annotations = get_annotation_identifiers(class_node)
    class_name_node = find_child(class_node, ["identifier"])
    if class_name_node is None:
        return None
    class_name = node_text(class_name_node, source_bytes)

    # Determine class type using annotations + inheritance
    class_type = _detect_class_type(class_node, annotations, source_bytes)

    full_qualified_name = f"{pkg}.{class_name}" if pkg else class_name

    # Parse fields
    fields = parse_fields(class_node, source_bytes)

    # Parse methods
    methods = parse_methods(class_node, source_bytes)

    class_info = {
        "class_name": class_name,
        "full_qualified_name": full_qualified_name,
        "class_type": class_type,
        "module": module_name,
        "file_path": str(file_path.relative_to(PROJECT_ROOT)),
        "annotations": ", ".join(annotations),
        "fields": fields,
        "methods": methods,
    }
    CLASSES.append(class_info)

    # If it's a controller, also parse endpoints
    if class_type == "controller":
        parse_endpoints(class_node, source_bytes, class_info, annotations)


def get_package(tree, source_bytes: bytes) -> str:
    """Extract package declaration."""
    root = tree.root_node
    for child in root.children:
        if child.type == "package_declaration":
            # The package name is the scoped_identifier child
            scoped = find_child(child, ["scoped_identifier", "identifier"])
            return node_text(scoped, source_bytes) if scoped else ""
    return ""


def parse_fields(class_node, source_bytes: bytes) -> list[dict]:
    """Parse field declarations from a class body."""
    fields = []
    body = find_child(class_node, ["class_body", "enum_body"])
    if body is None:
        return fields

    for child in body.children:
        if child.type == "field_declaration":
            field_info = parse_field(child, source_bytes)
            if field_info:
                fields.append(field_info)
    return fields


def parse_field(field_node, source_bytes: bytes) -> Optional[dict]:
    """Parse a single field declaration."""
    # Get annotations
    annotations = []
    for ann_node in find_children(field_node, "marker_annotation"):
        for c in ann_node.children:
            if c.type == "identifier":
                annotations.append(node_text(c, source_bytes))
    for ann_node in find_children(field_node, "annotation"):
        for c in ann_node.children:
            if c.type == "identifier":
                annotations.append(node_text(c, source_bytes))

    # Get type
    type_node = find_child(field_node, ["type", "generic_type", "array_type"])
    field_type = node_text(type_node, source_bytes) if type_node else ""

    # Get declarators (variable names)
    declarators = find_children(field_node, "variable_declarator")
    names = []
    for decl in declarators:
        name_node = find_child(decl, ["identifier"])
        if name_node:
            names.append(node_text(name_node, source_bytes))

    if not names:
        return None

    return {
        "name": names[0],  # primary field name
        "type": field_type,
        "annotations": annotations,
        "column_name": _extract_column_name(annotations, field_node, source_bytes),
    }


def _extract_column_name(annotations: list[str], field_node, source_bytes: bytes) -> Optional[str]:
    """Try to extract @Column(name='xxx') value."""
    ann_nodes = find_children(field_node, "annotation")
    for ann in ann_nodes:
        for c in ann.children:
            if c.type == "identifier" and node_text(c, source_bytes) == "Column":
                args = find_child(ann, ["annotation_argument_list"])
                if args:
                    args_text = node_text(args, source_bytes)
                    m = re.search(r'name\s*=\s*"([^"]+)"', args_text)
                    if m:
                        return m.group(1)
    return None


def parse_methods(class_node, source_bytes: bytes) -> list[dict]:
    """Parse method declarations from a class body."""
    methods = []
    body = find_child(class_node, ["class_body", "enum_body"])
    if body is None:
        return methods

    for child in body.children:
        if child.type == "method_declaration":
            annotations = get_annotation_identifiers(child)
            name_node = find_child(child, ["identifier"])
            if name_node is None:
                continue
            method_name = node_text(name_node, source_bytes)

            # Get parameters
            params = []
            formal_params = find_child(child, ["formal_parameters"])
            if formal_params:
                for param in find_children(formal_params, "formal_parameter"):
                    p_name = node_text(
                        find_child(param, ["identifier"]), source_bytes
                    ) or ""
                    p_type = node_text(find_child(param, ["type"]), source_bytes) or ""
                    params.append(f"{p_type} {p_name}")

            # Return type
            ret_node = find_child(child, ["type", "void_type"])
            ret_type = node_text(ret_node, source_bytes) if ret_node else ""

            methods.append(
                {
                    "name": method_name,
                    "annotations": annotations,
                    "params": ", ".join(params),
                    "return_type": ret_type,
                }
            )

    return methods


# ── Parsing REST endpoints ─────────────────────────────────────────────────


def parse_endpoints(class_node, source_bytes: bytes, class_info: dict, class_annotations: list[str]):
    """Extract HTTP endpoints from a controller class."""
    # Get base path from class-level @RequestMapping
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
        return

    for child in body.children:
        if child.type == "method_declaration":
            anns = get_annotation_identifiers(child)
            name_node = find_child(child, ["identifier"])
            if name_node is None:
                continue
            method_name = node_text(name_node, source_bytes)

            http_method = None
            path = None

            # Check @GetMapping, @PostMapping, etc.
            request_map = {
                "GetMapping": "GET",
                "PostMapping": "POST",
                "PutMapping": "PUT",
                "DeleteMapping": "DELETE",
                "PatchMapping": "PATCH",
                "RequestMapping": None,  # determined later
            }

            for ann_node in find_all_descendants(child, "annotation"):
                ann_id = find_child(ann_node, ["identifier"])
                if ann_id is None:
                    continue
                ann_name = node_text(ann_id, source_bytes)

                if ann_name in request_map:
                    http_method = request_map[ann_name]
                    # Extract path from annotation argument
                    args = find_child(ann_node, ["annotation_argument_list"])
                    if args:
                        args_text = node_text(args, source_bytes)
                        # Try to find path value: value="..." or path="..."
                        m = re.search(r'(?:value|path)\s*=\s*"([^"]+)"', args_text)
                        if m:
                            path = m.group(1)
                        else:
                            # Single string argument: @GetMapping("/path")
                            m = re.search(r'"([^"]+)"', args_text)
                            if m:
                                path = m.group(1)
                            else:
                                path = ""
                    else:
                        path = ""

                # For @RequestMapping, get method if not already set
                if ann_name == "RequestMapping" and http_method is None:
                    args = find_child(ann_node, ["annotation_argument_list"])
                    if args:
                        args_text = node_text(args, source_bytes)
                        m = re.search(r'method\s*=\s*(RequestMethod\.)?(\w+)', args_text)
                        if m:
                            http_method = m.group(2).upper()
                        # Also get path if not already extracted
                        if path is None:
                            m = re.search(r'(?:value|path)\s*=\s*"([^"]+)"', args_text)
                            if m:
                                path = m.group(1)
                            else:
                                m = re.search(r'"([^"]+)"', args_text)
                                if m:
                                    path = m.group(1)
                        if path is None:
                            path = ""

            if http_method and path is not None:
                # Handle path variables by extracting them
                full_path = base_path + path if path else base_path
                ENDPOINTS.append(
                    {
                        "http_method": http_method,
                        "path": full_path,
                        "controller": class_info["class_name"],
                        "method": method_name,
                        "module": class_info["module"],
                    }
                )


# ── Module dependency resolution ───────────────────────────────────────────


def resolve_module_deps(imports: list[str], current_module: str, source_bytes: bytes):
    """Given a list of import statements, figure out cross-module dependencies."""
    if current_module == "_root_":
        return  # skip root-level files like Application.java
    base_pkg = "com.taipei.iot"
    for imp in imports:
        imp_text = imp.replace("import ", "").replace(";", "").strip()
        if imp_text.startswith(f"{base_pkg}."):
            rest = imp_text[len(base_pkg) + 1 :]
            parts = rest.split(".")
            if parts and parts[0] != current_module:
                MODULE_DEPS[current_module].add(parts[0])


# ── Parsing a single Java file ─────────────────────────────────────────────


def parse_java_file(file_path: Path):
    """Parse a single Java file and extract all relevant information."""
    with open(file_path, "rb") as f:
        source_bytes = f.read()

    tree = parser.parse(source_bytes)
    root = tree.root_node
    module_name = get_module_name(file_path)

    # Parse imports
    imports = parse_imports(tree, source_bytes)
    IMPORTS_BY_FILE[str(file_path)] = imports
    resolve_module_deps(imports, module_name, source_bytes)

    # Update module file count
    MODULES[module_name].file_count += 1

    # Get the package from the file
    pkg = get_package(tree, source_bytes)

    # Parse all top-level type declarations
    for child in root.children:
        if child.type in (
            "class_declaration",
            "interface_declaration",
            "enum_declaration",
            "record_declaration",
        ):
            parse_class_declaration(child, source_bytes, file_path, module_name, pkg)


# ── Collecting module descriptions ─────────────────────────────────────────


def collect_module_descriptions():
    """Extract module descriptions from package structure and key class names."""
    module_descriptions = {
        "announcement": "公告系統 - 公告 CRUD、分類、附件、版本管理、已讀回執、置頂排序",
        "assettransfer": "資產移轉 - 資產轉移申請與審核流程",
        "audit": "稽核日誌 - 操作審計、登入日誌、資料變更追蹤",
        "auth": "認證授權 - JWT token 管理、登入/登出、Session 管理、OAuth/LDAP 認證",
        "common": "共用工具 - 通用工具類、異常處理、事件系統、多租戶過濾、Response 封裝",
        "config": "系統配置 - Spring Boot 自動配置、安全配置、CORS、Swagger/OpenAPI",
        "dept": "部門管理 - 部門樹狀結構、階層路徑、CRUD",
        "notification": "通知系統 - 站內通知、Email/SMS 發送、已讀狀態",
        "platform": "平台管理 - 平台層級管理功能、跨租戶操作",
        "rbac": "角色權限 - 角色管理、選單管理、權限控制、Scope 過濾",
        "setting": "系統設定 - 系統級別設定、密碼策略設定",
        "tenant": "多租戶 - 租戶管理、租戶隔離、多租戶資料過濾",
        "user": "使用者 - 使用者 CRUD、密碼管理、軟刪除",
        "workflow": "工作流程 - 可配置工作流程引擎、審批流程、SLA 管理",
    }
    for module_name, info in MODULES.items():
        if module_name in module_descriptions:
            MODULES[module_name].description = module_descriptions[module_name]
        else:
            MODULES[module_name].description = f"{module_name} 模組"


# ── Discover modules ───────────────────────────────────────────────────────


def discover_modules():
    """Find all top-level Java packages under the backend source."""
    if not BACKEND_SRC.exists():
        print(f"Error: Backend source path not found: {BACKEND_SRC}")
        sys.exit(1)

    for item in sorted(BACKEND_SRC.iterdir()):
        if item.is_dir() and not item.name.startswith("."):
            module_name = item.name
            MODULES[module_name] = ModuleInfo(
                name=module_name, package=f"com.taipei.iot.{module_name}", file_count=0
            )

    # Pseudo-module for root-level files (e.g., Application.java)
    MODULES["_root_"] = ModuleInfo(
        name="_root_", package="com.taipei.iot", description="根層級檔案（Application.java 等）"
    )

    print(f"Discovered {len(MODULES)} modules: {', '.join(sorted(MODULES.keys()))}")


# ── Walk and parse all Java files ──────────────────────────────────────────


def walk_and_parse():
    """Walk through all Java source files and parse each one."""
    java_files = list(BACKEND_SRC.rglob("*.java"))
    print(f"Found {len(java_files)} Java files to parse")

    for i, file_path in enumerate(java_files):
        if i % 50 == 0 and i > 0:
            print(f"  Progress: {i}/{len(java_files)}")
        try:
            parse_java_file(file_path)
        except Exception as e:
            print(f"  Error parsing {file_path.relative_to(PROJECT_ROOT)}: {e}")

    return len(java_files)


# ── Parse DB migrations ────────────────────────────────────────────────────


def parse_migrations():
    """
    Parse Flyway migration files and assign them to modules.
    Uses heuristics based on filename keywords.
    """
    migration_dir = BACKEND_RESOURCES / "db" / "migration"
    if not migration_dir.exists():
        print(f"Warning: Migration directory not found: {migration_dir}")
        return []

    migrations = []
    for f in sorted(migration_dir.glob("*.sql")):
        filename = f.name
        # Determine module from filename heuristics
        module = _classify_migration(filename)

        # Extract version
        version_match = re.match(r"(V\d+(?:_\d+)?)__", filename)
        version = version_match.group(1) if version_match else filename

        # Extract description
        desc_match = re.match(r"V\d+(?:_\d+)?__(.+)\.sql", filename)
        description = desc_match.group(1).replace("_", " ") if desc_match else filename

        migrations.append(
            {
                "filename": filename,
                "version": version,
                "description": description,
                "module": module,
                "file_path": f"backend/src/main/resources/db/migration/{filename}",
            }
        )

    return migrations


def _classify_migration(filename: str) -> str:
    """Classify a migration filename to the most likely module."""
    fname_lower = filename.lower()

    for module, keywords in MIGRATION_MODULE_KEYWORDS.items():
        for kw in keywords:
            if kw in fname_lower:
                return module

    # Fallback: check common patterns
    if "streetlight" in fname_lower:
        return "assettransfer"
    if "platform" in fname_lower:
        return "platform"

    return "common"


# ── Parse config keys from application yml ────────────────────────────────


def parse_config_keys():
    """Parse config keys from semantic-map.md if available."""
    config_keys = []
    sm_path = PROJECT_ROOT / "semantic-map.md"
    if sm_path.exists():
        with open(sm_path) as f:
            content = f.read()
        for line in content.split("\n"):
            line = line.strip()
            if line.startswith("- `") and "`: " in line:
                m = re.match(r"- `([^`]+)`:\s*(.*)", line)
                if m:
                    key = m.group(1)
                    value = m.group(2)
                    # Determine likely module from key prefix
                    module = _classify_config_key(key)
                    config_keys.append(
                        {
                            "key": key,
                            "value_sample": value[:100] if len(value) > 100 else value,
                            "module": module,
                        }
                    )
    return config_keys


def _classify_config_key(key: str) -> str:
    """Guess which module a config key belongs to."""
    prefix_module_map = {
        "jwt.": "auth",
        "auth.": "auth",
        "captcha.": "auth",
        "user.": "user",
        "app.security.": "auth",
        "app.auth.": "auth",
        "tenant.": "tenant",
        "announcement.": "announcement",
        "file.": "common",
        "mqtt.": "common",
        "virus-scan.": "common",
        "spring.": "config",
        "server.": "config",
        "management.": "config",
        "springdoc.": "config",
        "cors.": "config",
        "spring.datasource.": "config",
        "spring.data.redis.": "config",
        "spring.flyway.": "config",
        "iot.": "common",
    }
    for prefix, module in prefix_module_map.items():
        if key.startswith(prefix):
            return module
    return "config"


# ── Build SQLite database ──────────────────────────────────────────────────


def build_database(migrations: list, config_keys: list):
    """Create and populate the SQLite knowledge database."""
    if DB_PATH.exists():
        os.remove(DB_PATH)
        print(f"Removed existing database: {DB_PATH}")

    conn = sqlite3.connect(str(DB_PATH))
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    cur = conn.cursor()

    # ── Schema ──────────────────────────────────────────────────────────
    cur.executescript("""
        CREATE TABLE IF NOT EXISTS modules (
            name            TEXT PRIMARY KEY,
            package         TEXT NOT NULL,
            description     TEXT DEFAULT '',
            file_count      INTEGER DEFAULT 0
        );

        CREATE TABLE IF NOT EXISTS classes (
            id                  INTEGER PRIMARY KEY AUTOINCREMENT,
            class_name          TEXT NOT NULL,
            full_qualified_name TEXT NOT NULL,
            class_type          TEXT NOT NULL,  -- controller, service, repository, entity, config, enum, other
            module              TEXT NOT NULL REFERENCES modules(name),
            file_path           TEXT NOT NULL,
            annotations         TEXT DEFAULT '',
            fields_json         TEXT DEFAULT '[]',
            methods_json        TEXT DEFAULT '[]'
        );
        CREATE INDEX IF NOT EXISTS idx_classes_module ON classes(module);
        CREATE INDEX IF NOT EXISTS idx_classes_type ON classes(class_type);

        CREATE TABLE IF NOT EXISTS endpoints (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            http_method     TEXT NOT NULL,
            path            TEXT NOT NULL,
            controller      TEXT NOT NULL,
            method          TEXT NOT NULL,
            module          TEXT NOT NULL REFERENCES modules(name)
        );
        CREATE INDEX IF NOT EXISTS idx_endpoints_module ON endpoints(module);
        CREATE INDEX IF NOT EXISTS idx_endpoints_path ON endpoints(path);

        CREATE TABLE IF NOT EXISTS module_deps (
            module          TEXT NOT NULL REFERENCES modules(name),
            depends_on      TEXT NOT NULL REFERENCES modules(name),
            PRIMARY KEY (module, depends_on)
        );

        CREATE TABLE IF NOT EXISTS migrations (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            filename        TEXT NOT NULL,
            version         TEXT NOT NULL,
            description     TEXT NOT NULL,
            module          TEXT NOT NULL REFERENCES modules(name),
            file_path       TEXT NOT NULL
        );
        CREATE INDEX IF NOT EXISTS idx_migrations_module ON migrations(module);

        CREATE TABLE IF NOT EXISTS config_keys (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            key             TEXT NOT NULL UNIQUE,
            value_sample    TEXT DEFAULT '',
            module          TEXT NOT NULL REFERENCES modules(name)
        );
        CREATE INDEX IF NOT EXISTS idx_config_keys_module ON config_keys(module);

        CREATE TABLE IF NOT EXISTS imports (
            id              INTEGER PRIMARY KEY AUTOINCREMENT,
            file_path       TEXT NOT NULL,
            import_text     TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS meta (
            key             TEXT PRIMARY KEY,
            value           TEXT NOT NULL
        );
    """)

    # ── Insert modules ──────────────────────────────────────────────────
    for module_info in MODULES.values():
        cur.execute(
            "INSERT INTO modules (name, package, description, file_count) VALUES (?, ?, ?, ?)",
            (module_info.name, module_info.package, module_info.description, module_info.file_count),
        )

    # ── Insert classes ──────────────────────────────────────────────────
    for cls in CLASSES:
        fields = cls.pop("fields", [])
        methods = cls.pop("methods", [])
        fields_json = json.dumps(fields, ensure_ascii=False)
        methods_json = json.dumps(methods, ensure_ascii=False)
        cur.execute(
            """INSERT INTO classes (class_name, full_qualified_name, class_type, module,
                                    file_path, annotations, fields_json, methods_json)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
            (
                cls["class_name"],
                cls["full_qualified_name"],
                cls["class_type"],
                cls["module"],
                cls["file_path"],
                cls["annotations"],
                fields_json,
                methods_json,
            ),
        )

    # ── Insert endpoints ────────────────────────────────────────────────
    for ep in ENDPOINTS:
        cur.execute(
            "INSERT INTO endpoints (http_method, path, controller, method, module) VALUES (?, ?, ?, ?, ?)",
            (ep["http_method"], ep["path"], ep["controller"], ep["method"], ep["module"]),
        )

    # ── Insert module dependencies ──────────────────────────────────────
    for module_name, deps in sorted(MODULE_DEPS.items()):
        if module_name not in MODULES:
            continue
        for dep in sorted(deps):
            if dep in MODULES:  # only track deps to known modules
                cur.execute(
                    "INSERT OR IGNORE INTO module_deps (module, depends_on) VALUES (?, ?)",
                    (module_name, dep),
                )

    # ── Insert migrations ───────────────────────────────────────────────
    for m in migrations:
        cur.execute(
            "INSERT INTO migrations (filename, version, description, module, file_path) VALUES (?, ?, ?, ?, ?)",
            (m["filename"], m["version"], m["description"], m["module"], m["file_path"]),
        )

    # ── Insert config keys ──────────────────────────────────────────────
    for ck in config_keys:
        cur.execute(
            "INSERT OR IGNORE INTO config_keys (key, value_sample, module) VALUES (?, ?, ?)",
            (ck["key"], ck["value_sample"], ck["module"]),
        )

    # ── Insert imports ──────────────────────────────────────────────────
    for file_path, imp_list in IMPORTS_BY_FILE.items():
        rel_path = str(Path(file_path).relative_to(PROJECT_ROOT))
        for imp in imp_list:
            cur.execute(
                "INSERT INTO imports (file_path, import_text) VALUES (?, ?)",
                (rel_path, imp),
            )

    # ── Meta info ───────────────────────────────────────────────────────
    cur.execute(
        "INSERT INTO meta (key, value) VALUES ('project', ?)",
        ("taipei-iot backend - Spring Boot + JPA multi-tenant IoT platform",),
    )
    cur.execute("INSERT INTO meta (key, value) VALUES ('java_version', '21')")
    cur.execute("INSERT INTO meta (key, value) VALUES ('total_classes', ?)", (len(CLASSES),))
    cur.execute("INSERT INTO meta (key, value) VALUES ('total_endpoints', ?)", (len(ENDPOINTS),))
    cur.execute("INSERT INTO meta (key, value) VALUES ('total_modules', ?)", (len(MODULES),))
    cur.execute("INSERT INTO meta (key, value) VALUES ('total_migrations', ?)", (len(migrations),))
    cur.execute(
        "INSERT INTO meta (key, value) VALUES ('parser_version', '1.0.0')"
    )
    cur.execute("INSERT INTO meta (key, value) VALUES ('parsed_at', ?)", (__import__('datetime').datetime.now().isoformat(),))

    conn.commit()
    conn.close()


# ── Summary ────────────────────────────────────────────────────────────────


def print_summary():
    """Print a human-readable summary of what was extracted."""
    print("\n" + "=" * 60)
    print("📊  Knowledge Database Summary")
    print("=" * 60)
    print(f"  Modules:      {len(MODULES)}")
    print(f"  Classes:      {len(CLASSES)}")
    print(f"  Endpoints:    {len(ENDPOINTS)}")
    print(f"  DB:           {DB_PATH}")

    print("\n📦  Modules:")
    for name in sorted(MODULES.keys()):
        info = MODULES[name]
        deps = MODULE_DEPS.get(name, set())
        deps_str = f" → depends on: {', '.join(sorted(deps))}" if deps else ""
        print(f"  {name:20s} ({info.file_count:3d} files){deps_str}")

    print("\n🔗  Dependencies (cross-module):")
    for module in sorted(MODULES.keys()):
        deps = sorted(MODULE_DEPS.get(module, set()) & set(MODULES.keys()))
        if deps:
            print(f"  {module:20s} → {', '.join(deps)}")

    # Count by type
    type_counts = defaultdict(int)
    for cls in CLASSES:
        type_counts[cls["class_type"]] += 1
    print("\n🏷️  Classes by type:")
    for t, count in sorted(type_counts.items()):
        print(f"  {t:15s} {count}")

    print(f"\n📡  Endpoints: {len(ENDPOINTS)}")
    print(f"🗄️   Migrations: {len(migrations_cache) if 'migrations_cache' in dir() else '?'}")
    print("=" * 60)


# ── Main ───────────────────────────────────────────────────────────────────


def main():
    print("🔍  Phase 1: tree-sitter Java Code Analyzer")
    print("=" * 60)

    # Step 1: Discover modules
    print("\n[1/5] Discovering modules...")
    discover_modules()

    # Step 2: Collect module descriptions
    collect_module_descriptions()

    # Step 3: Parse all Java files
    print("\n[2/5] Parsing Java files with tree-sitter...")
    total_files = walk_and_parse()
    print(f"  ✅ Parsed {total_files} files")

    # Step 4: Parse migrations
    print("\n[3/5] Parsing DB migrations...")
    global migrations_cache
    migrations_cache = parse_migrations()
    print(f"  ✅ Found {len(migrations_cache)} migration files")

    # Step 5: Parse config keys
    print("\n[4/5] Parsing configuration keys...")
    config_keys = parse_config_keys()
    print(f"  ✅ Found {len(config_keys)} config keys")

    # Step 6: Build database
    print("\n[5/5] Building SQLite knowledge database...")
    build_database(migrations_cache, config_keys)
    print(f"  ✅ Database created at: {DB_PATH}")

    # Summary
    print_summary()


if __name__ == "__main__":
    migrations_cache = []
    main()
