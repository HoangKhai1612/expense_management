# -*- coding: utf-8 -*-
"""Kiem chuong truc truc baocao.docx bang Python.

Kiem tra: goi tai lieu, muc tieu dau, so bang, so hinh, trich danh,
dam chieu trich danh voi tai lieu tham khao, caption, va tinh nhat quan
giua du lieu bien dich va tep da sinh.
Chay: python report/validate_docx.py
"""
import re
import sys
import zipfile
from collections import Counter, defaultdict
from pathlib import Path

from docx import Document

DOCX = Path("baocao.docx")
CAPTIONS = Path("report/validation/captions.txt")
OUT = Path("report/validation")
OUT.mkdir(parents=True, exist_ok=True)

CAP_RE = re.compile(r"^(Hình|Bảng)\s+(\d+|[A-E])\.(\d+)\.\s*(.+)$")

fail, warn, info = [], [], []


def check(cond, msg):
    (info if cond else fail).append(msg)


def main():
    if not DOCX.exists():
        print("Khong tim thay baocao.docx")
        sys.exit(1)

    # 1. Tinh trang goi tai lieu
    with zipfile.ZipFile(DOCX) as z:
        check(z.testzip() is None, "Goi tai lieu hop le (zip integrity ok)")
        names = z.namelist()
        media = [n for n in names if n.startswith("word/media/")]
        check("word/_rels/document.xml.rels" in names,
              "Co quan he tai lieu (document.xml.rels)")

    src_figs = sorted(Path("report/figures").glob("*.png"))
    check(len(media) == len(src_figs),
          f"So anh nhung = so file nguon: {len(media)} / {len(src_figs)}")

    doc = Document(DOCX)
    paras = doc.paragraphs
    texts = [p.text for p in paras]
    full = "\n".join(texts)
    tbl_text = "\n".join(c.text for t in doc.tables for r in t.rows for c in r.cells)
    everywhere = full + "\n" + tbl_text

    # 2. Muc tieu dau
    h1 = [p.text for p in paras if p.style.name == "Heading 1"]
    h2 = [p.text for p in paras if p.style.name == "Heading 2"]
    h3 = [p.text for p in paras if p.style.name == "Heading 3"]
    h4 = [p.text for p in paras if p.style.name == "Heading 4"]
    check(len(h1) >= 8, f"So muc cap 1 (Chuong/Phu luc): {len(h1)}")
    check(len(h2) >= 30, f"So muc cap 2: {len(h2)}")
    check(len(h3) >= 50, f"So muc cap 3: {len(h3)}")
    check(len(h4) >= 2, f"So muc cap 4: {len(h4)}")
    print("Muc cap 1:")
    for t in h1:
        print("   -", t)

    # 3. Cau truc bat buoc
    for m in ["LỜI MỞ ĐẦU", "CHƯƠNG 1", "CHƯƠNG 2", "CHƯƠNG 3",
              "CHƯƠNG 4", "TÀI LIỆU THAM KHẢO", "PHỤ LỤC"]:
        check(any(m in t for t in h1), f"Co muc: {m}")
    for ap in "ABCDE":
        check(f"Phụ lục {ap}." in everywhere, f"Co Phu luc {ap}")

    # 4. Caption: kiem tra theo kieu Word, khong theo regex tren toan van
    caps = []
    for p in paras:
        if p.style.name == "Caption":
            m = CAP_RE.match(p.text.strip())
            check(m is not None, f"Co caption hop le: {p.text.strip()[:50]}")
            if m:
                caps.append((m.group(1), m.group(2), int(m.group(3))))

    per = defaultdict(list)
    for label, chap, num in caps:
        per[(label, chap)].append(num)
    for key, nums in sorted(per.items()):
        check(nums == list(range(1, len(nums) + 1)),
              f"Danh so {key[0]} chuong {key[1]}: {nums}")

    nfig = sum(1 for c in caps if c[0] == "Hình")
    ntbl = sum(1 for c in caps if c[0] == "Bảng")
    check(nfig == len(src_figs), f"So hinh co caption: {nfig} / {len(src_figs)} file")
    check(ntbl >= 25, f"So bang co caption: {ntbl}")

    # 5. Khop caption voi tep da sinh khi dung
    if CAPTIONS.exists():
        built = CAPTIONS.read_text(encoding="utf-8").splitlines()
        check(len(built) == len(caps),
              f"CAPTION khop: tep={len(built)}, docx={len(caps)}")
        built_nums = [tuple(CAP_RE.match(b).groups()[:3]) for b in built]
        docx_nums = [(a, b, str(c)) for a, b, c in caps]
        check(built_nums == docx_nums, "CAPTION dung thu tu docx")

    # 6. Doan so cua tieu de phai khop voi chuong dang xuat hien
    cur = None
    bad_num = []
    for p in paras:
        st = p.style.name
        if st == "Heading 1":
            m = re.match(r"CHƯƠNG\s+(\d+)", p.text.strip())
            cur = f"CH{m.group(1)}" if m else (
                "PL" if p.text.strip().startswith("Phụ lục") else None)
            continue
        if st in ("Heading 2", "Heading 3", "Heading 4") and cur:
            m = re.match(r"(\d+)\.(\d+)", p.text.strip())
            if m and not m.group(1) == cur[2:]:
                bad_num.append(f"{cur}: {p.text.strip()[:50]}")
    check(not bad_num, f"Tieu de khong khop so chuong: {bad_num or 'khong co'}")

    # 7. Bang: khong duoc co bang chi co tieu de
    tables = doc.tables
    check(len(tables) >= 30, f"So bang: {len(tables)}")
    header_only = [i + 1 for i, t in enumerate(tables) if len(t.rows) < 2]
    check(not header_only, f"Bang chi co tieu de (khong co du lieu): {header_only}")

    empty = sum(1 for t in tables for r in t.rows for c in r.cells
                if not c.text.strip())
    check(empty == 0, f"So o rong trong bang: {empty}")

    # 7. Moi hang phai dung so cot
    ragged = [i + 1 for i, t in enumerate(tables)
              for r in t.rows if len(r.cells) != len(t.columns)]
    check(not ragged, f"Hang khong khop so cot: {ragged}")

    # 8. Tham chieu cheo: moi "Hinh x.y" / "Bang x.y" trong van ban
    # phai co caption tuong ung trong tai lieu
    defined = {f"{a} {b}.{c}" for a, b, c in caps}
    # Quet tung doan rieng: noi cac doan lai se tao tham chieu gia
    # khi mot doan ket thuc bang "hinh" va doan sau bat dau bang "1.9".
    refs = set()
    for t in texts:
        if CAP_RE.match(t.strip()):
            continue
        for a, b, c in re.findall(r"\b(hình|bảng)\s+(\d+|[A-E])\.(\d+)",
                                  t, re.IGNORECASE):
            refs.add(f"{a.capitalize()} {b}.{c}")
    missing = sorted(refs - defined)
    check(not missing, f"Tham chieu khong ton tai: {missing or 'khong co'}")
    unused = sorted(defined - refs)
    info.append(f"So tham chieu chinh xac: {len(refs)}; "
                f"hinh/bang chua duoc tham chieu: {len(unused)} {unused}")

    # 9. Trich danh
    cites = sorted({int(x) for x in re.findall(r"\[(\d+)\]", everywhere)})
    refs = sorted({int(x) for x in re.findall(r"^\[(\d+)\]\s", full, re.M)})
    check(cites == refs, f"Khop trich danh {cites} voi danh muc {refs}")
    check(refs == list(range(1, len(refs) + 1)),
          f"Danh muc lien tuc tu 1 den {len(refs)}")

    # 9. Placeholder tren trang bia (nam trong bang)
    ph = everywhere.count("[THÔNG TIN CẦN BỔ SUNG]") + everywhere.count("[CẦN BỔ SUNG]")
    check(ph == 5, f"So o trong bia can nop: {ph} (nen la 5 truoc khi nop)")
    check("Hoàng Tuấn Kiệt" in everywhere, "Da dien ten nguoi thuc hien")

    # 9b. Khong duoc con chu huong dan nhap F9 trong chan trang
    with zipfile.ZipFile(DOCX) as z:
        foot = [z.read(n).decode("utf-8", "ignore") for n in z.namelist()
                if "footer" in n and n.endswith(".xml")]
    shown = re.findall(r"<w:t[^>]*>([^<]*)</w:t>", " ".join(foot))
    check(not any("F9" in t for t in shown),
          f"Chan trang khong con huong dan nhap F9: {shown}")

    # 10. Bo cuc: khong duoc vuot khung trang
    sec = doc.sections[0]
    text_w = sec.page_width.cm - sec.left_margin.cm - sec.right_margin.cm
    text_h = sec.page_height.cm - sec.top_margin.cm - sec.bottom_margin.cm
    wide = []
    for i, t in enumerate(tables, 1):
        w = sum((c.width.cm if c.width else 0) for c in t.columns)
        if w > text_w + 0.05:
            wide.append(f"Bang {i}: {w:.1f} cm > {text_w:.1f} cm")
    check(not wide, f"Bang vuot khung chu: {wide or 'khong co'}")

    tall = []
    for i, sh in enumerate(doc.inline_shapes, 1):
        if sh.width.cm > text_w + 0.05:
            tall.append(f"Hinh {i}: rong {sh.width.cm:.1f} cm")
        if sh.height.cm > text_h + 0.05:
            tall.append(f"Hinh {i}: cao {sh.height.cm:.1f} cm")
    check(not tall, f"Hinh vuot khung trang: {tall or 'khong co'}")
    info.append(f"Khung chu: {text_w:.1f} x {text_h:.1f} cm; "
                f"rong nhat cua bang: "
                f"{max((sum((c.width.cm if c.width else 0) for c in t.columns) for t in tables), default=0):.1f} cm")

    # 11. Mau sac va khoi trang
    colors = {str(r.font.color.rgb) for p in paras for r in p.runs
              if r.font.color and r.font.color.rgb is not None}
    check(colors <= {"000000"}, f"Mau chu: {colors or 'mac dinh'}")
    s = doc.sections[0]
    check(abs(s.page_width.cm - 21.0) < 0.2 and abs(s.page_height.cm - 29.7) < 0.2,
          f"Khoi trang: {s.page_width.cm:.1f} x {s.page_height.cm:.1f} cm")

    # 11. Nhat quan so lieu da xiem minh
    for k, v in {"385": "tong so kiem tra", "164": "so dong yeu cau",
                 "148": "so dong da xiem minh", "98": "kiem thu don vi backend",
                 "102": "hop dong admin", "160": "E2E",
                 "v1.0.0-academic-final": "the phat hanh"}.items():
        check(k in everywhere, f"Nhat quan so lieu [{k}] ({v})")

    # 12. Khong con so lieu cu
    stale = [x for x in ["147 đã xác minh", "9 khoảng trống", "17 rủi ro",
                         "26 tài liệu", "5 khoảng trống vận hành còn mở"]
             if x in everywhere]
    check(not stale, f"Khong con so lieu cu: {stale or 'khong co'}")

    # 13. Chu khong dau (du lieu chua kiem duyet)
    undia = [x for x in ["Kiem thu don vi", "Roi ro du lieu", "Dang mo",
                         "Da kiem soat", "khong co co so", "Khong sao luu"]
             if x in everywhere]
    check(not undia, f"Khong con chu tieng Viet khong dau: {undia or 'khong co'}")

    # 14. Truong Word
    fld = sum(1 for p in paras if "fldChar" in (p._p.xml or ""))
    check(fld >= 3, f"So truong Word (TOC/PAGE/SEQ): {fld}")
    seq = sum(1 for p in paras if "SEQ " in (p._p.xml or ""))
    check(seq == len(caps), f"Truong SEQ cho caption: {seq} / {len(caps)}")

    # 15. Khong con mau do chan
    print("\n" + "=" * 68)
    print("KET QUA KIEM CHUNG")
    print("=" * 68)
    print(f"Hinh: {nfig}   Bang: {ntbl}   Muc cap 1/2/3/4: "
          f"{len(h1)}/{len(h2)}/{len(h3)}/{len(h4)}")
    for m in info:
        print("  [OK]  " + m)
    for m in warn:
        print("  [CANH BAO] " + m)
    for m in fail:
        print("  [LOI] " + m)
    print("-" * 68)
    print(f"Tong so muc: {len(info)+len(warn)+len(fail)} | "
          f"Dat: {len(info)} | Canh bao: {len(warn)} | Loi: {len(fail)}")
    print("=" * 68)
    (OUT / "validate_docx.txt").write_text(
        "\n".join(["[OK] " + m for m in info] +
                  ["[LOI] " + m for m in fail]), encoding="utf-8")
    return 1 if fail else 0


if __name__ == "__main__":
    sys.exit(main())
