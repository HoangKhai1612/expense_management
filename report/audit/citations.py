import re, sys
from pathlib import Path
sys.stdout.reconfigure(encoding="utf-8")

lines = Path("report-validation/full-text.txt").read_text(encoding="utf-8").splitlines()

# bibliography entries start with [n] at the start of a Normal paragraph
refs = {}
for i, line in enumerate(lines, 1):
    m = re.match(r"^(\d+)\|Normal\|\[(\d+)\]\s+(.*)$", line)
    if m:
        refs[int(m.group(2))] = (i, m.group(3))

# citations in body = [n] inside any paragraph that is not the bibliography
body_cites = {}
for i, line in enumerate(lines, 1):
    if i in {r[0] for r in refs.values()}:
        continue
    for n in re.findall(r"\[(\d{1,2})\]", line):
        body_cites.setdefault(int(n), []).append(i)

print("=== DANH MUC ===")
print("So tai lieu: %d  (ID: %s)" % (len(refs), ", ".join(str(k) for k in sorted(refs))))
print("\n=== TRICH DAN TRONG THAN ===")
for n in sorted(body_cites):
    locs = body_cites[n]
    print("  [%2d]  %2d lan: dong %s" % (n, len(locs), ", ".join(map(str, locs[:6]))))

cited = set(body_cites)
available = set(refs)
print("\n=== DOI CHIEU ===")
missing_ref = sorted(cited - available)
unused_ref = sorted(available - cited)
print("Trich dan khong co trong danh muc (BROKEN):", missing_ref or "khong co")
print("Tai lieu khong duoc trich dan (UNUSED)   :", unused_ref or "khong co")
print("Trung lap ID                            :", "khong" if len(refs) == len(available) else "CO")

dup = [n for n in refs if sum(1 for k in refs if refs[k][1] == refs[n][1]) > 1]
print("Tai lieu trung lap noi dung            :", "khong" if not dup else dup)

# which numbers are cited, and are any > 14 (i.e. dangling like [15])
print("\n=== SO LON HON DANH MUC ===")
for i, line in enumerate(lines, 1):
    for n in re.findall(r"\[(\d{1,2})\]", line):
        if int(n) > 14:
            print(f"  dong {i}: [{n}]  {line[:150]}")
