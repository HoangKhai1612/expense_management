import re, sys
from pathlib import Path
sys.stdout.reconfigure(encoding="utf-8")

lines = Path("report-validation/full-text.txt").read_text(encoding="utf-8").splitlines()

# Vietnamese words that MUST carry diacritics. If a line contains any of these
# in its unaccented form, the diacritics were lost.
PLAIN = {
    "cong nghe", "cong bo", "du lieu", "nguoi dung", "ung dung", "mo ta",
    "kiem thu", "xac minh", "xac nhan", "tri tri", "nhan tao", "giao dien",
    "lap trinh", "dich vu", "kien truc", "the he", "bao mat", "tinh nang",
    "chuc nang", "phan tich", "chi tiet", "tong hop", "kiem chung",
    "rui ro", "ke hoach", "tai lieu", "yeu cau", "pham vi", "muc tieu",
    "hien thi", "thong bao", "tinh trang", "nguyen tac", "phuong phap",
    "ket qua", "mo ta", "loi", "thieu", "duoc", "khong", "khai bao",
    "chay", "bien dich", "ung dung", "he thong", "du an", "nhom",
    "danh muc", "chung minh", "khiem khut", "hanh dong", "chinh sach",
}
# unaccented -> accented, only where the pair is unambiguous
PAIRS = [
    ("cong nghe", "công nghệ"), ("du lieu", "dữ liệu"), ("nguoi dung", "người dùng"),
    ("ung dung", "ứng dụng"), ("mo ta", "mô tả"), ("kiem thu", "kiểm thử"),
    ("xac minh", "xác minh"), ("xac nhan", "xác nhận"), ("tri tri", "trí tuệ"),
    ("nhan tao", "nhân tạo"), ("giao dien", "giao diện"), ("lap trinh", "lập trình"),
    ("dich vu", "dịch vụ"), ("kien truc", "kiến trúc"), ("the he", "thế hệ"),
    ("bao mat", "bảo mật"), ("tinh nang", "tính năng"), ("chuc nang", "chức năng"),
    ("phan tich", "phân tích"), ("chi tiet", "chi tiết"), ("tong hop", "tổng hợp"),
    ("kiem chung", "kiểm chứng"), ("rui ro", "rủi ro"), ("ke hoach", "kế hoạch"),
    ("tai lieu", "tài liệu"), ("yeu cau", "yêu cầu"), ("pham vi", "phạm vi"),
    ("muc tieu", "mục tiêu"), ("hien thi", "hiển thị"), ("thong bao", "thông báo"),
    ("tinh trang", "tình trạng"), ("nguyen tac", "nguyên tắc"), ("phuong phap", "phương pháp"),
    ("ket qua", "kết quả"), ("thieu", "thiếu"), ("khong", "không"),
    ("khai bao", "khai báo"), ("bien dich", "biên dịch"), ("he thong", "hệ thống"),
    ("du an", "dự án"), ("danh muc", "danh mục"), ("chung minh", "chứng minh"),
    ("khiem khut", "khiếm khuyết"), ("hanh dong", "hành động"), ("chinh sach", "chính sách"),
]

hits = {}
for i, line in enumerate(lines, 1):
    low = line.lower()
    for plain, acc in PAIRS:
        if plain in low and acc not in low and acc.capitalize() not in line:
            # count only when it looks like plain (unaccented) usage
            hits.setdefault(i, set()).add((plain, acc))

print("=== SOI DOAN THIEU DAU TIENG VIET ===")
n = 0
for i in sorted(hits):
    n += 1
    plain = ", ".join(p for p, a in sorted(hits[i]))
    print(f"{i:4d}  [{plain}]  {lines[i-1][:150]}")
print(f"\nTong so dong bi nghi thieu dau: {n} / {len(lines)}")
