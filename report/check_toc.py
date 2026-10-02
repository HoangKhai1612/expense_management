# -*- coding: utf-8 -*-
"""Kiem tra day du muc luc, danh muc hinh, danh muc bang sau khi cap nhat truong."""
import re
import zipfile
from pathlib import Path

from docx import Document

DOCX = Path("baocao.docx")
doc = Document(DOCX)
paras = doc.paragraphs
texts = [p.text for p in paras]

with zipfile.ZipFile(DOCX) as z:
    app = z.read("docProps/app.xml").decode("utf-8", "ignore")
for tag in ("Pages", "Words", "Paragraphs"):
    m = re.search(rf"<{tag}>(\d+)</{tag}>", app)
    print(f"{tag:11}: {m.group(1) if m else '?'}")


def block(start_prefix, stop_prefixes):
    out, on = [], False
    for t in texts:
        s = t.strip()
        if s.startswith(start_prefix):
            on = True
            continue
        if on and any(s.startswith(p) for p in stop_prefixes):
            break
        if on and s:
            out.append(s)
    return out


toc = block("MỤC LỤC", ["DANH MỤC HÌNH"])
print(f"\n=== MUC LUC: {len(toc)} muc ===")
for t in toc[-14:]:
    print("  " + t[:90])
print("  ... 30 muc dau:")
for t in toc[:6]:
    print("  " + t[:90])

figs = block("DANH MỤC HÌNH", ["DANH MỤC BẢNG"])
print(f"\n=== DANH MUC HINH: {len(figs)} muc ===")
for t in figs:
    print("  " + t[:95])

tbls = block("DANH MỤC BẢNG", ["DANH MỤC TỪ VIẾT TẮT"])
print(f"\n=== DANH MUC BANG: {len(tbls)} muc ===")
for t in tbls[:6]:
    print("  " + t[:95])
print("  ...")
for t in tbls[-3:]:
    print("  " + t[:95])

abbr = block("DANH MỤC TỪ VIẾT TẮT", ["LỜI MỞ ĐẦU"])
print(f"\n=== DANH MUC VIET TAT: {len(abbr)} muc ===")

print("\n=== KIEM TRA ===")
need = ["CHƯƠNG 2", "CHƯƠNG 3", "CHƯƠNG 4", "TÀI LIỆU THAM KHẢO", "PHỤ LỤC",
        "Phụ lục E"]
for n in need:
    hit = [t for t in toc if t.startswith(n)]
    print(f"  Muc luc co '{n}': {'CO' if hit else 'THIEU'}"
          + (f"  (trang {hit[0].split()[-1]})" if hit else ""))
bad = [t for t in texts if re.search(r"Error!|Lỗi!|Không tìm thấy mục", t)]
print(f"  Doan chua loi truong: {len(bad)}")
print(f"  Hinh co caption trong danh muc: {len(figs)} / 11")
print(f"  Bang co caption trong danh muc: {len(tbls)} / 28")
