# -*- coding: utf-8 -*-
"""Du lieu bang chung da kiem chung cho bao cao.

Moi so lieu o day deu truy ve duoc mot tep cua du an hoac mot lenh da chay.
Khong co so lieu uoc luong.
"""

PROJECT = {
    # Ten chinh thuc lay tu README.md, backend/pom.xml va admin-web/package.json.
    # "troi long" (tinh te) la loi chinh ta; dung "troi ly" (tro gia nhiem vu).
    "ten": "Personal Finance AI System",
    "ten_viet": "H\u1ec7 th\u1ed1ng qu\u1ea3n l\u00fd chi ti\u00eau c\u00e1 nh\u00e2n c\u00f3 tr\u1ee1 l\u00fd AI",
    "ten_khoa": "finai",
    "muc_dich": "Qu\u1ea3n l\u00fd t\u00e0i ch\u00ednh c\u00e1 nh\u00e2n k\u00e8m ph\u00e2n t\u00edch AI c\u00f3 r\u00e0ng bu\u1ed9c d\u1eef li\u1ec7u th\u1eadt",
    "nguon_git": "https://github.com/HoangKhai1612/expense_management.git",
    "nhanh": "main",
    "commit": "175ce3bd96cf68251151be20a8e722520274bf71",
    "commit_baseline": "59cc2a039c310f7ae4ca09a74bdc250a4b6ea546",
    "commit_cha": "2c4371be5e69446fdb26979e2eb4ae2b505bb308",
    "the": "v1.0.0-academic-final",
    "so_file": 232,
    "so_commit": 3,
    "bi_mat_commit": 0,
    "ngay_kiem_chung": "01/10/2026",
}

CONG_NGHE = [
    ("Backend", "Spring Boot 3.5.16, Java 21, Spring Security, Spring Data JPA", "Tieu chuc he thong, tai dayu"),
    ("Du lieu", "PostgreSQL 16, Flyway (9 migration)", "Luu tru giao dich, ngan sach, danh muc"),
    ("Ung dung di dong", "Kotlin, Jetpack Compose, MVVM", "Giao dien nguoi dung"),
    ("Ban quan tri", "React 18, TypeScript, Vite, Nginx", "Giao dien quan tri"),
    ("AI", "Mo-duun phan tich noi bo, che do LOCAL", "Phan tich chi tieu co rang buoc du lieu"),
    ("Trien khai", "Docker Compose, 3 container", "Moi truong chay lap lai duoc"),
    ("Kiem thu", "JUnit 5, JUnit, PowerShell E2E", "5 bo kiem thu tu dong"),
    ("Quan ly cau hinh", "Git, file .env, tu dong hoa", "Theo doi cau hinh va phat hanh"),
]

YEU_CAU = {
    "tong": 164,
    "verified": 148,
    "partial": 8,
    "gap": 7,
    "out_of_scope": 1,
}

KIEM_THU = [
    ("Kiểm thử đơn vị - Backend", "mvn verify", 98, 0),
    ("Kiểm thử tích hợp - Backend", "mvn verify", 15, 0),
    ("Kiểm thử đơn vị - Android", "gradlew testDebugUnitTest", 10, 0),
    ("Kiểm thử hợp đồng API - Admin", "tests/admin-web-contract.ps1", 102, 0),
    ("Kiểm thử đầu cuối (E2E)", "tests/e2e-api-tests.ps1", 160, 0),
]

RUI_RO = [
    ("R-01", "AI đưa ra con số không có cơ sở", 3, 5, 15, "Đã kiểm soát", "Kiểm thử AiGroundingIT kiểm chứng giá trị trả về"),
    ("R-02", "Rò rỉ dữ liệu giữa những người dùng", 2, 5, 10, "Đã kiểm soát", "Lọc theo chủ sở hữu ở mọi truy vấn"),
    ("R-03", "Xóa danh mục làm mất lịch sử giao dịch", 4, 4, 16, "Tránh theo thiết kế", "Vô hiệu hoá thay vì xóa"),
    ("R-04", "Tài khoản bị khoá vẫn truy cập được bằng phiên cũ", 3, 4, 12, "Đã kiểm soát", "Kiểm tra trạng thái tài khoản ở mỗi yêu cầu"),
    ("R-05", "Migration thất bại trên cơ sở dữ liệu đã chuyển một phần", 2, 5, 10, "Đã kiểm soát", "Kiểm tra mã băm và khởi tạo dữ liệu"),
    ("R-06", "Rò rỉ thông tin bí mật qua nhật ký", 3, 4, 12, "Đã nghiệm thu", "Rà soát 20 dòng nhật ký, không ghi bí mật"),
    ("R-07", "Bí mật bị lẫn vào kho mã nguồn", 1, 5, 5, "Đã kiểm soát", "Tệp môi trường không được theo dõi"),
    ("R-08", "Tín cảnh báo người dùng bị trùng lặp", 3, 3, 9, "Đã kiểm soát", "Kiểm thử đầu cuối chỉ sinh một cảnh báo"),
    ("R-09", "Số trang không bị giới hạn", 2, 3, 6, "Chấp nhận", "Đã kiểm tra kích thước trang"),
    ("R-10", "Không có tích hợp liên tục, thay đổi lỗi có thể lọt", 3, 4, 12, "Còn mở", "Chưa có đường dẫn kiểm thử tự động"),
    ("R-11", "Đường dẫn AI bên ngoài chưa kiểm chứng", 2, 3, 6, "Chấp nhận", "Chỉ hỗ trợ chế độ chạy cục bộ"),
    ("R-12", "Không sao lưu, mất dữ liệu là không khôi phục được", 2, 5, 10, "Còn mở", "Chỉ có ổ đĩa thể hiện dữ liệu"),
    ("R-13", "Không giới hạn tần suất trên đăng nhập và trò chuyện AI", 3, 4, 12, "Còn mở", "Chưa triển khai biện pháp giới hạn"),
    ("R-14", "Ranh giới ngày tháng theo giờ quốc tế lệch kỳ báo cáo", 2, 3, 6, "Chấp nhận", "Tính theo giờ quốc tế, đã tài liệu hoá"),
    ("R-15", "Thiếu lịch sử phát triển không thể tái hiện", 3, 4, 12, "Đã giảm nhẹ cho tương lai", "Kho và thẻ tồn tại, lịch sử phát triển không có"),
    ("R-16", "Tín hiệu sức khoẻ container báo sai", 2, 3, 6, "Đã xảy ra và đã sửa", "Sửa cách dò trên địa chỉ bản lặp IPv4"),
    ("R-17", "Tài liệu lệch với thực tế hệ thống", 3, 2, 6, "Còn mở", "Đã điều chỉnh một số mô tả"),
    ("R-18", "Tệp bọc Gradle bị loại khỏi kho mã nguồn", 2, 5, 10, "Đã xảy ra và đã sửa", "Bản sao mới không thể biên dịch phần Android"),
]

LOI = [
    ("D-1", "AiAnalyst thêm phần tử vào danh sách bất biến", "List.of không cho phép thêm phần tử",
     "Ném UnsupportedOperationException ở mọi yêu cầu phân tích chi tiêu",
     "Bọc bằng new ArrayList<>(...)", "4 lỗi, kiểm chứng bằng cách hoàn nguyên bản sửa"),
    ("D-2", "Định nghĩa trùng hàm định dạng thời gian", "Định nghĩa bị che khuất",
     "Chuỗi thời gian không phân tích được về nguyên văn",
     "Xoá bản trùng, bổ sung kiểm tra mẫu và xử lý ngày tháng", "2 khẳng định kiểm tra, quan sát trước khi sửa"),
    ("D-3", "Kiểm tra sức khoẻ dùng địa chỉ localhost", "Phân giải ra IPv6 ::1, máy chủ chỉ lắng nghe IPv4",
     "Container báo không khỏe trong khi dịch vụ vẫn phục vụ bình thường",
     "Kiểm tra trên 127.0.0.1 thay vì localhost", "Chu kỳ lỗi giảm từ 22 xuống 0"),
    ("D-4", "Tệp bọc Gradle bị quy tắc loại trừ", "Quy tắc *.jar chưa có ngoại lệ",
     "Bản sao mới không biên dịch được phần Android",
     "Thêm ngoại lệ !android/gradle/wrapper/gradle-wrapper.jar",
     "Không kiểm thử nào bắt được vì máy đã có sẵn tệp"),
    ("D-5", "Thư mục công cụ trợ giúp bị ghi nhận", "Quy tắc loại trừ chưa phủ thư mục công cụ",
     "Kho mã nguồn ghi nhận nhiều mô-đun con không mong muốn",
     "Thêm .kilo, .claude, .cursor vào danh sách loại trừ", "Số tệp trong cam kết giảm từ 238 xuống 232"),
]

# Nhom hai: muc tai lieu va cau hinh da dong, khong phai loi ma nguon.
# Nguon: FINAL_PROJECT_STATUS.md muc 9.
LOI_TAI_LIEU = [
    ("T-1", "Giao diện /api/auth/me chưa được tài liệu hoá", "Tài liệu thiếu mô tả giao diện",
     "Tài liệu được viết theo ý định thay vì sinh từ hệ thống đang chạy",
     "Bổ sung mô tả giao diện vào tài liệu kỹ thuật", "Đã đóng, đã tài liệu hoá"),
    ("T-2", "Cảnh báo tài khoản mặc định của Spring", "Thông tin, không phải lỗ hổng",
     "Bộ khởi tạo của Spring sinh tài khoản mặc định khi thiếu cấu hình",
     "Kiểm chứng hai giao diện trả về 401, không khai thác được",
     "Đã đóng, kiểm chứng không khai thác được"),
    ("T-3", "Mô tả màn hình thông báo trên ứng dụng di động bị nói quá",
     "Tài liệu khẳng định có màn hình trong khi không có",
     "Tài liệu mô tả theo dự kiến thay vì theo mã nguồn đã triển khai",
     "Sửa lại mô tả cho khớp với mức độ phủ kiểm thử thực tế", "Đã đóng, đã sửa mô tả"),
    ("T-4", "Giao diện phản hồi trên ứng dụng di động chưa có",
     "Tài liệu mô tả một giao diện không tồn tại",
     "Yêu cầu bị hiểu sai phạm vi khi lập tài liệu",
     "Phân loại rõ là ngoài phạm vi học thuật, ghi trong ma trận truy vết",
     "Đã đóng bằng cách phân loại ngoài phạm vi"),
]

# Phat hien trong kiem chung cuoi ngay 02/10/2026, sau khi xac lap cam ket nen tang.
LOI_KIEM_CHUNG = [
    ("K-1", "Ứng dụng di động không gọi được backend trên Android 16 trở lên",
     "Quyền truy cập mạng nội bộ chưa khai báo, nên lưu lượng tới 10.0.2.2 bị chặn",
     "Nền tảng Android 16 chặn lưu lượng tới địa chỉ mạng riêng tư nếu không khai báo quyền",
     "Khai báo quyền truy cập mạng nội bộ và xin quyền lúc khởi chạy ứng dụng",
     "Biên dịch thành công và 10 kiểm thử đơn vị vẫn đạt; chưa kiểm chứng trên thiết bị chạy Android 16"),
]

HANH_DONG = [
    ("L-01", "Khong co pipeline CI/CD", "Dang mo"),
    ("L-02", "Khong chung thuc TLS", "Dang mo"),
    ("L-03", "Khong co chien luoc sao luu san xuat", "Dang mo"),
    ("L-04", "Khong gioi han tan so", "Dang mo"),
    ("L-05", "Nghi nghieu che do che gia nhat ky", "Da dong 01/10/2026"),
    ("L-06", "Khong tu dong hoa kiem thu giao dien", "Dang mo"),
    ("L-10", "Khong kiem thu tai tai", "Dang mo"),
    ("L-11", "Khong quet bao mat hay kiem thuc xam nhap", "Dang mo"),
    ("L-12", "Chi mot loai tien te", "Dang mo"),
    ("L-13", "Khong co token lam moi", "Dang mo"),
    ("L-14", "Token Android luu khong ma hoa", "Dang mo"),
    ("L-15", "Duong dan AI ben ngoai chua kiem chung", "Dang mo"),
    ("L-16", "Khong co manifest Kubernetes", "Dang mo"),
    ("L-17", "Khong tong hop nhat ky", "Dang mo"),
    ("L-18", "Khong xac thuc email hay dat lai mat khau", "Dang mo"),
]

TAI_LIEU_THAM_KHAO = [
    "Project Management Institute, A Guide to the Project Management Body of Knowledge (PMBOK Guide), Seventh Edition, ANSI/PMI 99-001-2021, Project Management Institute, Inc., Newtown Square, PA, 2021.",
    "Joint Task Force Transformation Initiative, Guide for Conducting Risk Assessments, NIST Special Publication 800-30 Revision 1, National Institute of Standards and Technology, Gaithersburg, MD, 2012. doi:10.6028/NIST.SP.800-30r1.",
    "Barry W. Boehm, Chris Abts, A. Winsor Brown, Sunita Chulani, Bradford K. Clark, Ellis Horowitz, Ray Madachy, Donald J. Reifer, Bert Steece, Software Cost Estimation with COCOMO II, Prentice Hall PTR, Upper Saddle River, NJ, 2000. ISBN 978-0-13-702576-3.",
    "ISO/IEC JTC 1/SC 7, ISO/IEC/IEEE 12207:2017 Systems and software engineering - Software life cycle processes, International Organization for Standardization, Geneva, 2017.",
    "IEEE, IEEE Std 1012-2016 IEEE Standard for System, Software, and Hardware Verification and Validation, Institute of Electrical and Electronics Engineers, Piscataway, NJ, 2016. ISBN 978-1-5044-1812-6.",
    "OWASP Foundation, OWASP Top 10:2021, OWASP Foundation, 2021. [Online]. Available: https://owasp.org/Top10/en/",
    "OWASP Foundation, OWASP Application Security Verification Standard 5.0.0, OWASP Foundation, 2025. [Online]. Available: https://owasp.org/www-project-application-security-verification-standard/",
    "VMware / Broadcom, Spring Boot 3.5 System Requirements and Reference Documentation, 2025. [Online]. Available: https://docs.spring.io/spring-boot/3.5/system-requirements.html",
    "Docker Inc., Compose Specification and Compose File Reference, 2025. [Online]. Available: https://docs.docker.com/compose/",
    "Redgate, Flyway Documentation, 2025. [Online]. Available: https://documentation.red-gate.com/flyway",
    "Android Open Source Project, Android Developers - Architecture Recommendations, Google LLC, 2025. [Online]. Available: https://developer.android.com/topic/architecture",
    "P. Jones, B. Bradley, J. Jones, RFC 7519: JSON Web Token (JWT), Internet Engineering Task Force, 2015. doi:10.17487/RFC7519.",
    "PostgreSQL Global Development Group, PostgreSQL 16 Documentation, 2023. [Online]. Available: https://www.postgresql.org/docs/16/",
    "S. Chacon, B. Straub, Pro Git, 2nd Edition, Apress, 2014. [Online]. Available: https://git-scm.com/book/en/v2",
]

VIET_TAT = [
    ("AI", "Artificial Intelligence - Trí tuệ nhân tạo"),
    ("API", "Application Programming Interface - Giao diện lập trình ứng dụng"),
    ("CSDL", "Cơ sở dữ liệu"),
    ("E2E", "End-to-End - Kiểm thử đầu toàn hệ thống"),
    ("JWT", "JSON Web Token - Token xác thực người dùng"),
    ("REST", "Representational State Transfer - Kiến trúc dịch vụ web"),
    ("SPA", "Single Page Application - Ứng dụng một trang"),
    ("UC", "Use Case - Tình huống sử dụng"),
    ("V&V", "Verification and Validation - Xác minh và xác nhận"),
]
