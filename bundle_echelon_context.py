#!/usr/bin/env python3
import os
import hashlib
from pathlib import Path

# -----------------------
# Config
# -----------------------
EXCLUDE_DIR_NAMES = {
    ".git", ".gradle", "build", "out", "bin", "target", "runs", "run",
    ".idea", ".vscode", "logs", "crash-reports"
}

# Include by extension (text-like)
INCLUDE_EXTS = {
    ".java", ".kt", ".groovy",
    ".gradle", ".properties", ".toml", ".json", ".mcmeta",
    ".md", ".txt", ".cfg", ".yml", ".yaml",
    ".mixins.json"
}

# Also include these exact filenames even if extension filtering misses
INCLUDE_BASENAMES = {
    "settings.gradle", "settings.gradle.kts",
    "gradle.properties",
    "mods.toml", "pack.mcmeta",
    "fabric.mod.json",
    "tiered.mixins.json", "tiered.fabric-compat.mixins.json",
}

MAX_BYTES_PER_FILE = 200_000          # truncate any single file beyond this
MAX_TOTAL_BYTES_DUMP = 8_000_000      # safety cap for the whole dump output

TREE_OUT = "ECHELON_PROJECT_TREE_FULL.txt"
DUMP_OUT = "ECHELON_PROJECT_DUMP.txt"

# -----------------------
# Helpers
# -----------------------
def sha256_of_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def is_excluded_dir(path: Path) -> bool:
    return any(part in EXCLUDE_DIR_NAMES for part in path.parts)

def should_include_file(path: Path) -> bool:
    if path.name in INCLUDE_BASENAMES:
        return True
    ext = path.suffix.lower()
    if ext in INCLUDE_EXTS:
        return True
    # Special-case mixin configs that end with .json but are named *.mixins.json
    if path.name.endswith(".mixins.json"):
        return True
    return False

def read_file_bytes(path: Path) -> bytes:
    with open(path, "rb") as f:
        return f.read()

def safe_decode(data: bytes) -> str:
    # Decode as UTF-8 first; fall back to latin-1 to avoid crashing on odd files.
    try:
        return data.decode("utf-8")
    except UnicodeDecodeError:
        return data.decode("latin-1", errors="replace")

def write_tree(root: Path, out_path: Path):
    lines = []
    def walk(dir_path: Path, prefix: str = ""):
        try:
            entries = sorted(dir_path.iterdir(), key=lambda p: (p.is_file(), p.name.lower()))
        except PermissionError:
            return

        entries = [e for e in entries if not (e.is_dir() and e.name in EXCLUDE_DIR_NAMES)]

        for i, entry in enumerate(entries):
            is_last = (i == len(entries) - 1)
            connector = "└── " if is_last else "├── "
            lines.append(f"{prefix}{connector}{entry.name}")
            if entry.is_dir():
                walk(entry, prefix + ("    " if is_last else "│   "))

    lines.append(str(root.resolve()))
    walk(root)
    out_path.write_text("\n".join(lines), encoding="utf-8")

def write_dump(root: Path, out_path: Path):
    total_written = 0
    with open(out_path, "w", encoding="utf-8") as out:
        for path in sorted(root.rglob("*")):
            if path.is_dir():
                continue
            rel = path.relative_to(root)

            if is_excluded_dir(rel):
                continue
            if not should_include_file(path):
                continue

            try:
                data = read_file_bytes(path)
            except Exception as e:
                out.write(f"\nFILE: {rel}\nERROR: {e}\n")
                continue

            size = len(data)
            sha = sha256_of_bytes(data)

            truncated = False
            if size > MAX_BYTES_PER_FILE:
                data = data[:MAX_BYTES_PER_FILE]
                truncated = True

            text = safe_decode(data)

            header = []
            header.append("\n" + "=" * 89)
            header.append(f"FILE: {rel}")
            header.append(f"SIZE: {size} bytes")
            header.append(f"SHA256: {sha}")
            header.append("-" * 89)
            if truncated:
                header.append(f"[TRUNCATED] Showing first {MAX_BYTES_PER_FILE} bytes")
                header.append("-" * 89)

            block = "\n".join(header) + "\n" + text + "\n"

            block_bytes = block.encode("utf-8", errors="replace")
            if total_written + len(block_bytes) > MAX_TOTAL_BYTES_DUMP:
                out.write("\n" + "=" * 89 + "\n")
                out.write("DUMP STOPPED: reached MAX_TOTAL_BYTES_DUMP cap.\n")
                break

            out.write(block)
            total_written += len(block_bytes)

def main():
    root = Path(os.getcwd())
    write_tree(root, root / TREE_OUT)
    write_dump(root, root / DUMP_OUT)
    print(f"Wrote: {TREE_OUT}")
    print(f"Wrote: {DUMP_OUT}")

if __name__ == "__main__":
    main()
