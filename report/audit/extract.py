import re, sys, zipfile, json
from pathlib import Path
from docx import Document
from docx.table import Table
from docx.text.paragraph import Paragraph
from docx.oxml.ns import qn

sys.stdout.reconfigure(encoding="utf-8")
DOCX = Path("baocao.docx")

# ---- 1. ZIP integrity ----
z = zipfile.ZipFile(DOCX)
bad = z.testzip()
names = z.namelist()
print("=== ZIP ===")
print("testzip:", bad or "OK")
media = [n for n in names if n.startswith("word/media/")]
print("media files:", len(media))
for m in sorted(media):
    print("  ", m, z.getinfo(m).file_size, "byte")

# ---- 2. relationships vs media ----
import xml.etree.ElementTree as ET
RNS = "{http://schemas.openxmlformats.org/package/2006/relationships}"
missing = []
for relpart in [n for n in names if n.endswith(".rels")]:
    root = ET.fromstring(z.read(relpart))
    base = relpart.rsplit("_rels/", 1)[0]
    for r in root.findall(RNS + "Relationship"):
        if r.get("TargetMode") == "External":
            continue
        tgt = r.get("Target")
        if tgt.startswith("/"):
            p = tgt.lstrip("/")
        else:
            p = base + tgt
        p = str(Path(p)).replace("\\", "/")
        if p not in names:
            missing.append((relpart, r.get("Id"), tgt, p))
print("\n=== RELATIONSHIPS ===")
print("broken internal rels:", len(missing))
for m in missing:
    print("  MISSING", m)

# ---- 3. document body walk ----
doc = Document(DOCX)
body = doc.element.body

items = []
for child in body.iterchildren():
    if child.tag == qn("w:p"):
        items.append(("p", Paragraph(child, doc)))
    elif child.tag == qn("w:tbl"):
        items.append(("t", Table(child, doc)))

def ptext(p):
    # include field results
    return "".join(t.text or "" for t in p._p.iter(qn("w:t")))

lines = []
idx = 0
tables = []
figs = []
for kind, obj in items:
    idx += 1
    if kind == "p":
        txt = ptext(obj)
        style = obj.style.name if obj.style is not None else "?"
        has_img = bool(obj._p.findall(".//" + qn("a:blip")))
        marker = ""
        if has_img:
            marker = "[IMG]"
        if txt.strip() or marker:
            lines.append(f"{idx:04d}|{style}|{marker}{txt}")
            if style.startswith("Caption") or marker:
                figs.append((idx, style, txt))
    else:
        nrows = len(obj.rows)
        ncols = len(obj.columns)
        tables.append((idx, nrows, ncols))
        lines.append(f"{idx:04d}|TABLE|{nrows}x{ncols}")
        for r in obj.rows:
            cells = [c.text.replace("\n", " / ").strip() for c in r.cells]
            lines.append(f"     |      |  " + " | ".join(cells))

Path("report-validation").mkdir(exist_ok=True)
Path("report-validation/full-text.txt").write_text("\n".join(lines), encoding="utf-8")

print("\n=== BODY ===")
print("blocks:", len(items), "tables:", len(tables), "caption/img paras:", len(figs))

# ---- 4. placeholder hunt ----
alltext = "\n".join(lines)
pats = ["[CẦN BỔ SUNG]", "[cần bổ sung]", "nhấn F9", "[F9]", "TODO", "XXX",
        "Lorem", "PLACEHOLDER", "[N/A]", "<...>", "???", "chưa xác định", "chua xac dinh"]
print("\n=== PLACEHOLDER HUNT ===")
for pat in pats:
    n = alltext.count(pat)
    if n:
        print(f"  FOUND {n}x: {pat}")

# ---- 5. field errors ----
print("\n=== FIELD ERROR STRINGS ===")
for pat in ["Error!", "Lỗi!", "Error ", "Bookmark not defined", "Reference source not found"]:
    n = alltext.count(pat)
    if n:
        print(f"  FOUND {n}x: {pat}")

# ---- 6. hyperlinks ----
print("\n=== HYPERLINKS ===")
ext = 0
for relpart in [n for n in names if n.endswith(".rels")]:
    root = ET.fromstring(z.read(relpart))
    for r in root.findall(RNS + "Relationship"):
        if r.get("TargetMode") == "External":
            ext += 1
            print("  EXT", r.get("Type").rsplit("/", 1)[-1], r.get("Target"))
print("external rels:", ext)
