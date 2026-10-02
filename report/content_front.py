# -*- coding: utf-8 -*-
"""Noi dung bao cao.

Tach thanh cac phan de doc va bien dich doc lap. Moi phan la mot danh sach
block; kieu block quy dinh cach python-docx render.
"""

# Kieu block:
#   ("h1"|"h2"|"h3"|"h4", text)
#   ("p", text)
#   ("bul", [item, ...])
#   ("num", [item, ...])
#   ("tbl", caption, header, rows, col_widths_cm)
#   ("fig", path, caption)
#   ("note", text)          <- chu thich nho, in nghieng
#   ("quote", text)
#   ("pagebreak",)
#   ("toc",)                <- Word TOC field
#   ("lof",)                <- List of Figures
#   ("lot",)                <- List of Tables

# ============================================================
# LOI MOC DAU
# ============================================================

LOI_MOC_DAU = [
    ("h1", "LỜI MỞ ĐẦU"),

    ("h2", "Bối cảnh"),
    ("p", "Theo báo cáo phân tích rủi ro của Viện Tiêu chuẩn và Công nghệ Hoa Kỳ, quản lý rủi ro là thành phần nền tảng của mọi tổ chức, vì rủi ro luôn tồn tại và đòi hỏi người quản lý phải xác định, đánh giá, ứng phó và theo dõi một cách có hệ thống [2]. Trong phát triển phần mềm, điều đó càng trở nên rõ ràng bởi hệ thống phần mềm thường đồng thời phải đáp ứng yêu cầu nghiệp vụ, đạt chuẩn chất lượng, bảo đảm an toàn thông tin và vẫn nằm trong giới hạn chi phí và thời gian."),
    ("p", "Quản lý tài chính cá nhân là một bài toán kinh doanh đòi hỏi dữ liệu đa dạng và biến động liên tục. Người dùng cần biết đã chi tiêu bao nhiêu, vào mục nào, có vượt ngân sách dự kiến hay không. Bài toán này có một đặc điểm đáng chú ý: phần lớn người dùng không thể tự phân tích dữ liệu của chính mình. Họ biết con số tổng chi tiêu nhưng không biết xu hướng thay đổi, không nhận ra mục nào đang chi phí lệch chuẩn, và thường chỉ phát hiện vượt ngân sách khi đã quá muộn. Việc đưa phân tích tự động vào sản phẩm vì vậy trở thành một hướng tiếp cận tự nhiên."),
    ("p", "Tuy nhiên, việc áp dụng trí tuệ nhân tạo vào một lĩnh vực mà sai số trực tiếp thành tổn hậu quả tài chính đòi hỏi một tiêu chuẩn khác. Một hệ thống tư vấn tài chính có thể nói “bạn đã chi 5 triệu đồng cho ăn uống” khi dữ liệu thực là 3 triệu đồng, và người dùng sẽ tin. Rủi ro ở đây không chỉ là sai về mặt kỹ thuật mà còn là mất niềm tin không thể khôi phục. Đây là lý do dự án này đặt nguyên tắc cốt lõi là mô-đun AI chỉ được phát biểu điều mà dữ liệu thực chứng minh được, và phải từ chối trả lời khi dữ liệu không đủ."),

    ("h2", "Lý do chọn đề tài"),
    ("p", "Dự án chọn đề tài này dựa trên ba lý do cụ thể. Thứ nhất, quản lý chi tiêu cá nhân là bài toán phổ biến nhưng phần lớn công cụ sẵn có chỉ dừng ở mức ghi nhận và thống kê, chưa hỗ trợ người dùng ra quyết định. Thứ hai, bài toán này vừa đủ phức tạp để minh hoạ nhiều khía cạnh của quản lý dự án phần mềm: có yêu cầu chức năng rõ ràng, có thành phần phụ thuộc nhau, có yêu cầu bảo mật dữ liệu cá nhân, và có rủi ro công nghệ thực sự. Thứ ba, cách dự án xử lý yêu cầu nghiệm thu tạo ra một bài học có thể kiểm chứng: mọi tuyên bố hoàn thành đều phải gắn với bằng chứng thực thi, không chấp nhận tuyên bố dựa trên niềm tin."),
    ("p", "Điểm khác biệt đáng kể nằm ở chỗ dự án không coi phần trí tuệ nhân tạo là một thành phần trang trí cho cuốn báo cáo. Trong toàn bộ mô-đun AI, không có bước nào sinh ra con số bằng suy đoán. Cơ chế này được cài đặt bằng kiểm thử tự động, trong đó mỗi ý định phân tích phải trả về đúng các số liệu lấy từ cơ sở dữ liệu, và khi không có dữ liệu thì hệ thống phải nói rõ là không đủ dữ liệu."),

    ("h2", "Mục tiêu dự án"),
    ("p", "Mục tiêu của dự án được phát biểu theo bốn nhóm, mỗi nhóm gắn với một tiêu chí kiểm chứng cụ thể."),
    ("num", [
        "Xây dựng hệ thống quản lý chi tiêu cá nhân đáp ứng đầy đủ các chức năng cơ bản: ghi nhận giao dịch thu và chi, phân loại theo danh mục, đặt ngân sách theo kỳ, và theo dõi thông báo.",
        "Cung cấp mô-đun phân tích bằng trí tuệ nhân tạo trả lời các câu hỏi về tài chính bằng ngôn ngữ tự nhiên, với nguyên tắc mọi phát biểu đều có căn cứ trong dữ liệu thực.",
        "Xây dựng bảng điều khiển quản trị cho phép quản lý người dùng, danh mục, phản hồi và theo dõi nhật ký kiểm toán.",
        "Bảo đảm hệ thống có thể triển khai lại được trên máy tính khác bằng một lệnh, kèm theo bộ kiem thử tự động có thể chạy lại bất cứ lúc nào.",
    ]),

    ("h2", "Đối tượng và phạm vi"),
    ("p", "Đối tượng sử dụng trực tiếp là cá nhân muốn theo dõi chi tiêu, thông qua ứng dụng Android. Đối tượng sử dụng thứ hai là quản trị viên, thông qua bảng điều khiển web. Bên cạnh hai nhóm này, dự án còn quan tâm đến khía cạnh học thuật, đó là áp dụng các nguyên tắc quản lý dự án và có bằng chứng kiểm chứng."),
    ("p", "Phạm vi công nghệ bao gồm toàn bộ chuỗi sản phẩm: ứng dụng di động, giao diện quản trị, dịch vụ backend, cơ sở dữ liệu, mô-đun phân tích, cùng cấu hình triển khai. Những khía cạnh được nêu tường minh là nằm ngoài phạm vi học thuật của dự án, gồm: giao diện gửi phản hồi trực tiếp trên ứng dụng di động, kiểm thử tự động giao diện người dùng, kiểm thử tải và hiệu năng, cùng kiểm thử xâm nhập. Việc loại trừ này được ghi nhận bằng mã trong ma trận truy vết yêu cầu chứ không bị bỏ qua âm thầm."),

    ("h2", "Phương pháp thực hiện"),
    ("p", "Phương pháp được thực hiện theo nguyên tắc ba lớp, trong đó mỗi tuyên bố quan trọng đều phải vượt qua cả ba lớp."),
    ("num", [
        "Lớp lý thuyết: các nguyên tắc quản lý dự án được trích dẫn từ nguồn chuẩn và học thuật đã được kiểm chứng, bao gồm hướng dẫn của Viện Quản lý Dự án [1], tiêu chuẩn quản lý vòng đời phần mềm [4], và tiêu chuẩn xác minh kiểm chứng [5].",
        "Lớp áp dụng: mỗi nguyên tắc lý thuyết được thể hiện bằng một cơ chế cụ thể trong dự án, chẳng hạn sổ rủi ro cho quản lý rủi ro, hay ma trận truy vết cho quản lý phạm vi.",
        "Lớp bằng chứng: mỗi tuyên bố về kết quả phải truy ngược được về một lệnh đã thực thi hoặc một tệp tài liệu trong dự án.",
    ]),
    ("p", "Rủi ro tiến hành theo NIST SP 800-30 Rev. 1, trong đó rủi ro được định nghĩa là hàm của yếu tố mối đe doạ, điểm yếu, mức độ ảnh hưởng và khả năng xảy ra [2]. Dự án áp dụng cách tính điểm rủi ro đơn giản hóa, nhân khả năng xảy ra với mức độ ảnh hưởng, và ghi rõ phương pháp này là cách đơn giản hoá chứ không phải định nghĩa gốc của tiêu chuẩn."),
    ("p", "Một điểm cần nêu ngay từ đầu: dữ liệu lịch sử của dự án không tồn tại. Quản lý phiên bản chỉ được thiết lập sau giai đoạn phát triển chính, nên không có dữ liệu về nỗ lực, tiến độ hay lịch sử phát triển. Báo cáo này không ước lượng các giá trị đó. Mỗi nơi cần đến số liệu lịch sử, báo cáo ghi rõ là không có đủ dữ liệu để định lượng, và dành riêng mục 1.4.5 để phân tích hậu quả của khoảng trống này."),

    ("h2", "Cấu trúc báo cáo"),
    ("p", "Báo cáo gồm bốn chương và năm phụ lục. Chương 1 trình bày kế hoạch dự án, gồm phạm vi, hạ tầng, quy trình phát triển, ước lượng, kế hoạch chất lượng, quản lý rủi ro, đo lường và quản lý cấu hình. Chương 2 mô tả việc thực hiện và kiểm soát, gồm triển khai từng thành phần, xem xét lại, giám sát, xử lý lỗi và kết quả kiểm thử. Chương 3 trình bày quá trình kết thúc dự án, gồm đánh giá mức độ hoàn thành, bàn giao, các vấn đề còn tồn tại và đánh giá sau dự án. Chương 4 là kết luận. Năm phụ lục đính kèm ma trận truy vết yêu cầu, sổ rủi ro, bằng chứng kiểm thử, danh mục sản phẩm bàn giao và bản nghiệm thu cuối cùng."),
]
