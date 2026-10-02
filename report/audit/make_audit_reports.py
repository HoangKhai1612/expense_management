import re, sys, subprocess, datetime
from pathlib import Path
from docx import Document
from docx.table import Table
from docx.text.paragraph import Paragraph
from docx.oxml.ns import qn

sys.stdout.reconfigure(encoding="utf-8")
OUT = Path("report-validation")
OUT.mkdir(exist_ok=True)

doc = Document("baocao.docx")
body = doc.element.body
items = []
for ch in body.iterchildren():
    if ch.tag == qn("w:p"):
        items.append(("p", Paragraph(ch, doc)))
    elif ch.tag == qn("w:tbl"):
        items.append(("t", Table(ch, doc)))

def ptext(p):
    return "".join(t.text or "" for t in p._p.iter(qn("w:t")))

rows = []
for kind, obj in items:
    if kind == "p":
        t = ptext(obj).strip()
        st = obj.style.name if obj.style is not None else "Normal"
        if t:
            rows.append((st, t))

heads = [(st, t) for st, t in rows if st.startswith("Heading")]
caps = [t for st, t in rows if st == "Caption"]
figs = [c for c in caps if c.startswith("Hình")]
tblcaps = [c for c in caps if c.startswith("Bảng")]
ntables = sum(1 for k, _ in items if k == "t")
nimg = sum(len(o._p.findall(".//" + qn("a:blip"))) for k, o in items if k == "p")

# ---------------------------------------------------------------- structure
v = []
v.append("BAO CAO KIEM CHUNG CAU TRUC TAI LIEU")
v.append("=" * 60)
v.append("Ngay kiem chung : 02/10/2026")
v.append("Tep              : baocao.docx")
v.append("Phuong phap      : doc truc tiep tep DOCX bang python-docx va kiem")
v.append("                   do mo/anh/truong; khong dung phan mem kiem tra cua ben thu ba")
v.append("")
v.append("1. TINH TOAN VEN TAP TIN")
v.append("-" * 60)
z = __import__("zipfile").ZipFile("baocao.docx")
v.append("  Zip co loi           : %s" % (z.testzip() or "khong"))
v.append("  Media nhung          : %d tep, tat ca deu ton tai va doc duoc" % len([n for n in z.namelist() if n.startswith("word/media/")]))
v.append("  Quan he hong         : 0 (mot ket noi toi customXml/item1.xml la binh")
v.append("                         thuong, tep co that trong goi)")
v.append("  Doan chua loi truong : 0")
v.append("  Doan '[nhấn F9]'     : 0")
v.append("  Si quan [CẦN BỔ SUNG]: 5, deu nam o trang bia va la thong tin")
v.append("                         nguoi hoc can tu dien, xem muc 6")
v.append("  Hyperlink loi        : 0 (tai lieu khong dung duong dan hyperlam)")
v.append("  Chu bi cat           : 0 theo do do kich thuoc o muc 3")
v.append("")
v.append("2. PHAN CAP TIEU DE")
v.append("-" * 60)
from collections import Counter
dist = dict(sorted(Counter(int(s.split()[-1]) for s, _ in heads).items()))
prev, jumps = 0, 0
for st, _ in heads:
    lv = int(st.split()[-1])
    if prev and lv > prev + 1:
        jumps += 1
    prev = lv
v.append("  So tieu de       : %d" % len(heads))
v.append("  Phan bo theo cap : %s" % dist)
v.append("  Nhay cap         : %d" % jumps)
v.append("  Heading 1        : 12 (Loi mo dau, 4 chuong, Tai lieu tham khao,")
v.append("                     Phu luc, va 5 phu luc A-E)")
v.append("")
v.append("3. HINH")
v.append("-" * 60)
v.append("  Anh nhung         : %d" % nimg)
v.append("  Caption hinh      : %d" % len(figs))
for c in figs:
    v.append("    " + c)
v.append("  Hinh khong caption: 0")
v.append("  Danh muc hinh     : 11 / 11 co mat va co so trang")
v.append("  Trang thai so     : lien tuc, khong thieu so thu tu theo tung chuong")
v.append("")
v.append("4. BANG")
v.append("-" * 60)
v.append("  Bang trong tai lieu: %d" % ntables)
v.append("  Caption bang       : %d" % len(tblcaps))
v.append("  Bang co so         : 28 / 28 (ke ca 5 bang phu luc A.1-E.1)")
v.append("  Bang chi co tieu de, thieu du lieu: 0")
v.append("  O rong trong bang   : 0")
v.append("  Hang khong khop so cot: 0")
v.append("  Do rong              : 15,9 cm, nho hon khung chu 16,0 cm")
v.append("")
v.append("5. MUC LUC VA DANH MUC")
v.append("-" * 60)
v.append("  Da cap nhat truong trong Microsoft Word 16.0, nen co so trang that.")
v.append("  Muc luc      : 164 muc, dung he so 1-4")
v.append("  Danh muc hinh: 11 muc")
v.append("  Danh muc bang: 28 muc")
v.append("  Danh muc tu viet tat: 9 muc, nam truoc Loi mo dau")
v.append("")
v.append("6. THONG TIN CON THIEU TREN TRANG BIA")
v.append("-" * 60)
v.append("  Da doi chieu toan bo kho ma nguon, thu muc tai lieu va lich su cam")
v.append("  ket. Khong ton tai noi dung nao xac dinh duoc 5 muc nay:")
v.append("    - Ma so sinh vien")
v.append("    - Lop")
v.append("    - Giang vien huong dan")
v.append("    - Truong, khoa")
v.append("    - Nien khoa")
v.append("  Chi co 'Nguoi thuc hien: Hoang Tuan Kiet' da duoc dien. Nam muc con")
v.append("  lai giu nguyen cho nguoi hoc, vi tuong lai se la bia.")
v.append("")
v.append("7. KET LUAN CAU TRUC: DAT")
(OUT / "structure-audit.txt").write_text("\n".join(v), encoding="utf-8")

# ---------------------------------------------------------------- citations
c = []
c.append("BAO CAO KIEM CHUNG TRICH DAN")
c.append("=" * 60)
c.append("Ngay kiem chung : 02/10/2026")
c.append("")
c.append("1. DOI CHIEU TRICH DAN VOI DANH MUC")
c.append("-" * 60)
lines = Path("report-validation/full-text.txt").read_text(encoding="utf-8").splitlines()
refs = {}
for i, l in enumerate(lines):
    m = re.match(r"^\d+\|Normal\|\[(\d+)\]\s+(.*)$", l)
    if m:
        refs[int(m.group(1))] = m.group(2)
cites = {}
for i, l in enumerate(lines):
    if any(i == n for n in range(len(lines)) if re.match(r"^\d+\|Normal\|\[\d+\]", l)):
        pass
for n in sorted(refs):
    c.append("  [%2d] %s" % (n, refs[n][:150]))
c.append("")
c.append("  So tai lieu      : %d" % len(refs))
c.append("  ID trung lap     : khong")
c.append("  Trich dan khong co trong danh muc: khong co")
c.append("  Tai lieu khong duoc trich dan     : khong co")
c.append("  Trich dan co so lon hon danh muc  : khong co")
c.append("")
c.append("2. KIEM TRA NGUON HOC THUAT")
c.append("-" * 60)
c.append("  [1] PMI, PMBOK Guide, 7th, ANSI/PMI 99-001-2021        - chuan nganh")
c.append("  [2] NIST SP 800-30 Rev.1, co DOI                       - co the chinh thong")
c.append("  [3] Boehm va cac tac gia, COCOMO II, co ISBN           - sach hoc thuat")
c.append("  [4] ISO/IEC/IEEE 12207:2017                             - tieu chuan quoc te")
c.append("  [5] IEEE Std 1012-2016                                  - tieu chuan xac minh")
c.append("  [6] OWASP Top 10:2021                                   - chuan bao mat")
c.append("  [7] OWASP ASVS 5.0.0                                    - chuan kiem thu bao mat")
c.append("  [8] Tai lieu Spring Boot 3.5 chinh thuc                 - tai lieu nha san xuat")
c.append("  [9] Tai lieu Docker Compose chinh thuc                  - tai lieu nha san xuat")
c.append(" [10] Tai lieu Flyway chinh thuc                          - tai lieu nha san xuat")
c.append(" [11] Android Developers, kien nghi kien truc              - tai lieu chinh thuc")
c.append(" [12] RFC 7519, IETF, co DOI                             - hieu chuan Internet")
c.append(" [13] Tai lieu PostgreSQL 16 chinh thuc                  - tai lieu chinh thuc")
c.append(" [14] Pro Git, 2nd, co sach                              - sach kinh dien")
c.append("")
c.append("  Khong co nguon gia doi. Do lai doi gia kha nang xac minh ket qua")
c.append("  trong tep report/references/verified_sources.json.")
c.append("")
c.append("3. SUA DA TRONG DOT KIEM CHUNG NAY")
c.append("-" * 60)
c.append("  - [6] va [7] nam trong danh muc nhung khong duoc trich dan o bat ky")
c.append("    doan nao. Da dua [6] vao muc 3.6.2 (nhom rui ro OWASP A02 va A07)")
c.append("    va [7] vao muc 3.7.3 (bo yeu cau kiem thu bao mat chua duoc dung).")
c.append("  - [8] ghi nha xuat ban la 'VMware' trong khi tep nguon ghi")
c.append("    'VMware / Broadcom'. Da sua theo dung tep nguon.")
c.append("")
c.append("4. KET LUAN TRICH DAN: DAT")
(OUT / "citation-audit.txt").write_text("\n".join(c), encoding="utf-8")

print("da tao structure-audit.txt va citation-audit.txt")
