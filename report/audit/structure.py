import re, sys
from pathlib import Path
from docx import Document
from docx.table import Table
from docx.text.paragraph import Paragraph
from docx.oxml.ns import qn
sys.stdout.reconfigure(encoding="utf-8")

doc = Document("baocao.docx")
body = doc.element.body
items = []
for child in body.iterchildren():
    if child.tag == qn("w:p"):
        items.append(("p", Paragraph(child, doc)))
    elif child.tag == qn("w:tbl"):
        items.append(("t", Table(child, doc)))

def ptext(p):
    return "".join(t.text or "" for t in p._p.iter(qn("w:t")))

figs, tbls, heads = [], [], []
img_count = 0
for i, (kind, obj) in enumerate(items):
    if kind != "p":
        continue
    t = ptext(obj).strip()
    st = obj.style.name if obj.style is not None else ""
    imgs = obj._p.findall(".//" + qn("a:blip"))
    if imgs:
        img_count += len(imgs)
        figs.append((i, "IMG", t))
    if st == "Caption":
        if t.startswith("Hình"):
            figs.append((i, "CAP", t))
        elif t.startswith("Bảng"):
            tbls.append((i, t))
    if st.startswith("Heading"):
        heads.append((i, int(st.split()[-1]), t))

print("=== HINH ===")
print("so anh nhung:", img_count, " | so caption Hinh:", sum(1 for f in figs if f[1] == "CAP"))
for i, k, t in figs:
    if k == "CAP":
        print("  ", t[:88])
orphan_img = [f for f in figs if f[1] == "IMG" and not f[2]]
print("anh khong caption:", len(orphan_img))

# figure/table numbering continuity
def nums(lst, pre):
    out = []
    for _, t in lst:
        m = re.search(pre + r"\s*(\d+)\.(\d+)", t)
        if m:
            out.append((int(m.group(1)), int(m.group(2)), t))
    return out

fn = nums([(i, t) for i, k, t in figs if k == "CAP"], r"H\u00ecnh")
tn = nums(tbls, r"B\u1ea3ng")
print("\nHinh numbering:", [(f"{a}.{b}") for a, b, _ in fn])
print("Bang numbering:", [(f"{a}.{b}") for a, b, _ in tn])

for label, seq in (("HINH", fn), ("BANG", tn)):
    per = {}
    for ch, n, t in seq:
        per.setdefault(ch, []).append(n)
    bad = []
    for ch, ns in per.items():
        for k in range(1, len(ns) + 1):
            if k not in ns:
                bad.append(f"{label} ch.{ch} thieu so {k}")
    print(f"{label} theo chuong:", {k: v for k, v in sorted(per.items())}, "| thieu:", bad or "khong")

print("\n=== PHAN CAP TIEU DE ===")
prev = 0
jumps = []
for i, lvl, t in heads:
    if prev and lvl > prev + 1:
        jumps.append((i, prev, lvl, t[:60]))
    prev = lvl
print("so heading:", len(heads), "| nhay cap:", len(jumps))
for j in jumps:
    print("   NHAY:", j)
from collections import Counter
print("phan bo cap:", dict(Counter(l for _, l, _ in heads)))

# H1 must be chapters / back matter
print("\n=== H1 ===")
for i, l, t in heads:
    if l == 1:
        print("  ", t[:70])
