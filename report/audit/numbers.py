import re, sys
from pathlib import Path
sys.stdout.reconfigure(encoding="utf-8")

text = Path("report-validation/full-text.txt").read_text(encoding="utf-8")
lines = text.splitlines()

CLAIMS = {
    "164 requirements": [r"\b164\b"],
    "148 verified":     [r"\b148\b"],
    "8 partial":        [r"\b8\b.{0,30}ho\u00e0n th\u1ea5n|ho\u00e0n th\u1ea5n.{0,30}\b8\b", r"PARTIAL.{0,12}8\b"],
    "7 gap":            [r"\b7\b.{0,25}(kho\u1ea3ng tr\u1ed1ng|GAP)|\bGAP\b.{0,12}7\b"],
    "1 out of scope":   [r"ngo\u00e0i ph\u1ea1m vi.{0,40}1\b|\b1\b.{0,25}ngo\u00e0i ph\u1ea1m vi h\u1ec1c th\u1ea1p"],
    "385 checks":       [r"\b385\b"],
    "98 backend unit":  [r"\b98\b"],
    "15 integration":   [r"\b15\b.{0,40}ki\u1ec3m th\u1eed t\u00edch h\u1ee3p|ki\u1ec3m th\u1eed t\u00edch h\u1ee3p.{0,40}\b15\b", r"\b15\b.{0,20}(t\u00edch h\u1ee3p|integration)"],
    "10 android":       [r"\b10\b.{0,30}ki\u1ec3m th\u1eed \u0111\u01a1n v\u1ec5|ki\u1ec3m th\u1eed \u0111\u01a1n v\u1ec5.{0,30}\b10\b|m\u01b0\u1eddi ki\u1ec3m th\u1eed \u0111\u01a1n v\u1ec5.{0,20}\b10\b"],
    "102 contract":     [r"\b102\b"],
    "160 E2E":          [r"\b160\b"],
    "3/3 containers":   [r"3/3|ba container|ba th\u00e0nh ph\u1ea7n.{0,30}kh\u1ecfe"],
    "9 migrations":     [r"\b9\b.{0,25}migration|migration.{0,25}\b9\b|V1.V9|V1\u2013V9"],
    "11 tables":        [r"\b11\b.{0,25}b\u1ea3ng|b\u1ea3ng.{0,25}\b11\b"],
    "18 risks":         [r"\b18\b.{0,25}r\u1ee7i ro|r\u1ee7i ro.{0,25}\b18\b"],
    "10 defects":       [r"\b10\b.{0,30}m\u1ee5c l\u1ed7i|m\u1ee7c l\u1ed7i.{0,30}\b10\b|m\u01b0\u1eddi m\u1ee5c l\u1ed7i|m\u01b0\u1eddi m\u1ee5c"],
    "7 criteria":       [r"b\u1ea3y ti\u00eau ch\u00ed|7 ti\u00eau ch\u00ed|b\u1ea3y ti\u00eau ch\u00ed nghi\u1ec7m thu"],
    "17 criteria":      [r"m\u01b0\u1eddi b\u1ea3y ti\u00eau ch\u00ed|17 ti\u00eau ch\u00ed"],
}

print("=== VI TRI XUAT HIEN ===")
for name, pats in CLAIMS.items():
    found = []
    for i, line in enumerate(lines, 1):
        for p in pats:
            if re.search(p, line, re.I):
                found.append(i)
                break
    print(f"\n[{name}]  {len(found)} noi dung")
    for i in found[:6]:
        print(f"   {i}: {lines[i-1][:190]}")

# ---- look for contradictory numbers near key totals ----
print("\n\n=== SOI TUONG PHAN KHAC ===")
for i, line in enumerate(lines, 1):
    if re.search(r"t\u1ed5ng (s\u1ed1|c\u1ed9ng).{0,25}(\d{2,4})", line, re.I) or re.search(r"t\u1ed5ng c\u1ed9ng.{0,20}(\d{2,4})", line, re.I):
        print(f"{i}: {line[:200]}")
