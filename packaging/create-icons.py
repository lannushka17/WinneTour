#!/usr/bin/env python3
"""Placeholder-іконки для jpackage / javapackager (винна палітра #5c1a33)."""
from __future__ import annotations

import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent
RGB = (92, 26, 51)
SIZE = 128


def write_png(path: Path, width: int, height: int, rgb: tuple[int, int, int]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + tag
            + data
            + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    row = b"\x00" + bytes(rgb) * width
    raw = row * height
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", ihdr)
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


def main() -> None:
    png = ROOT / "linux" / "winerytours.png"
    write_png(png, SIZE, SIZE, RGB)
    # javapackager: базове ім'я без розширення
    write_png(ROOT / "app-icon.png", SIZE, SIZE, RGB)
    print(f"Created {png} and {ROOT / 'app-icon.png'}")


if __name__ == "__main__":
    main()
