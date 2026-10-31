"""Convierte los errores de Gradle/Kotlin en anotaciones de GitHub Actions."""
import re
import sys

ROOT_MARKERS = ("/rocola/rocola/",)
kotlin = re.compile(r"^e: file://(?P<path>[^:]+):(?P<line>\d+):(?P<col>\d+) (?P<msg>.*)$")
shown = 0
with open(sys.argv[1], encoding="utf-8", errors="replace") as f:
    for raw in f:
        line = raw.rstrip("\n")
        m = kotlin.match(line)
        if m:
            path = m.group("path")
            for marker in ROOT_MARKERS:
                if marker in path:
                    path = path.split(marker, 1)[1]
            print(f"::error file={path},line={m.group('line')},col={m.group('col')}::{m.group('msg')}")
            shown += 1
        elif re.match(r"^(ERROR|FAILURE|e:|AAPT: error|> )", line.strip()) and len(line.strip()) > 3:
            print(f"::error::{line.strip()[:900]}")
            shown += 1
        if shown >= 45:
            break
