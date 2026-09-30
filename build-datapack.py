#!/usr/bin/env python3
"""Rebuild dist/BetterRecipeMemory.zip from datapack/ — deterministic (sorted entries, fixed
timestamps, deflate), so the same source always yields the same SHA-256.

    python build-datapack.py            # writes dist/BetterRecipeMemory.zip and prints its SHA-256
"""
import hashlib
import pathlib
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent
SRC = ROOT / "datapack"
OUT = ROOT / "dist" / "BetterRecipeMemory.zip"
EPOCH = (1980, 1, 1, 0, 0, 0)

files = sorted(p for p in SRC.rglob("*") if p.is_file())
OUT.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(OUT, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for p in files:
        info = zipfile.ZipInfo(p.relative_to(SRC).as_posix(), date_time=EPOCH)
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o644 << 16
        z.writestr(info, p.read_bytes())
print(OUT.relative_to(ROOT).as_posix(), hashlib.sha256(OUT.read_bytes()).hexdigest())
for p in files:
    print("  ", p.relative_to(SRC).as_posix())
