#!/usr/bin/env python3
"""Write or verify SHA256SUMS.txt for every file tracked by Git (excluding itself).

  python scripts/update_checksums.py          rewrite SHA256SUMS.txt
  python scripts/update_checksums.py --check  exit 1 if any hash or file list differs
"""
from __future__ import annotations

import hashlib
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'SHA256SUMS.txt'


def tracked() -> list[str]:
    out = subprocess.run(['git', '-C', str(ROOT), 'ls-files', '-z'], capture_output=True, check=True).stdout
    return sorted(p for p in out.decode('utf-8').split('\0') if p and p != OUT.name and (ROOT / p).is_file())


def committed_bytes(path: str) -> bytes:
    """Hash the bytes Git stores, so Windows line-ending conversion cannot change the result."""
    blob = subprocess.run(['git', '-C', str(ROOT), 'show', f':{path}'], capture_output=True)
    return blob.stdout if blob.returncode == 0 else (ROOT / path).read_bytes()


def render() -> str:
    return ''.join(f'{hashlib.sha256(committed_bytes(p)).hexdigest()}  ./{p}\n' for p in tracked())


def main(argv: list[str]) -> int:
    text = render()
    if '--check' in argv:
        if OUT.exists() and OUT.read_text(encoding='utf-8') == text:
            print('SHA256SUMS=PASS')
            return 0
        print('SHA256SUMS=FAIL: run python scripts/update_checksums.py and commit the result')
        return 1
    OUT.write_text(text, encoding='utf-8', newline='\n')
    print(f'wrote {OUT.name} ({text.count(chr(10))} files)')
    return 0


if __name__ == '__main__':
    raise SystemExit(main(sys.argv[1:]))
