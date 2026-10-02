# -*- coding: utf-8 -*-
"""Chuong 1 (tiep) - Chat luong, rui ro, do luong, quan ly du an va cau hinh."""

CHUONG_1B = [
    ("h3", "1.5.1. Mục tiêu chất lượng"),
    ("p", "Tiêu chuẩn xác minh kiểm chứng phân biệt hai loại bằng chứng: xác minh cho thấy sản phẩm đáp ứng yêu cầu, còn xác nhận cho thấy sản phẩm giải quyết đúng bài toán [5]. Dự án đặt mục tiêu chất lượng theo đúng hai tiêu chí này, thay vì theo khẩu hiệu chung chung."),
    ("p", "Với tiêu chí xác minh, dự án yêu cầu mọi chức năng trong phạm vi đều có ít nhất một kiểm thử tự động. Với tiêu chí xác nhận, dự án yêu cầu mỗi nhóm chức năng được kiểm tra qua một bộ kiểm thử chạy trên hệ thống thật, không chỉ qua kiểm thử đơn vị. Hai yêu cầu này giải thích vì sao dự án có tới năm bộ kiểm thử thay vì một: mỗi bộ phục vụ một tầng khác nhau của bằng chứng."),

    ("h3", "1.5.2. Tiêu chí chất lượng"),
    ("p", "Bảng 1.13 nêu bốn chiều chất lượng kèm phép đo tương ứng và kết quả thực tế. Nguyên tắc áp dụng là mỗi tiêu chí phải có một phép đo kiểm chứng được, nếu không thì tiêu chí chỉ là khẩu hiệu."),
    ("tbl", "Bảng 1.13. Tiêu chí chất lượng và phép đo",
     ["Chiều chất lượng", "Tiêu chí", "Phép đo", "Kết quả thực tế"],
     [
        ["Tính đúng đắn", "Mọi chức năng trong phạm vi có kiểm thử", "Số kiểm thu trên chức năng", "Đạt"],
        ["Tính ổn định", "Không có lỗi khi dự án lặp lại từ trạng thái sạch", "Số lần chạy liên tiếp thành công", "Đạt"],
        ["Tính bảo mật", "Không rò rỉ dữ liệu giữa người dùng", "Số ca kiểm thử 403 và 404", "Đạt"],
        ["Tính có căn cứ của AI", "Mọi con số phải lấy từ dữ liệu thật", "Số ý định phân tích có kiểm thử", "6/6"],
        ["Tính tái lập", "Triển khai được bằng một lệnh", "Số container khỏe", "3/3"],
        ["Tính an toàn thông tin", "Không có bí mật trong kho mã nguồn", "Số bí mật phát hiện", "0"],
        ["Tính truy vết", "Yêu cầu truy được về bằng chứng", "Tỷ lệ dòng có bằng chứng", "148/164"],
     ], [3.2, 4.8, 4.0, 3.0]),

    ("h3", "1.5.3. Chiến lược kiểm thử"),
    ("p", "Chiến lược kiểm thử của dự án dựa trên nguyên tắc phân tầng, theo đó mỗi tầng bắt lỗi khác nhau và chi phí phát hiện tăng dần theo chiều cao. Bốn tầng được sử dụng là kiểm thử đơn vị, kiểm thử tích hợp, kiểm thử hợp đồng API và kiểm thử đầu cuối."),
    ("p", "Có một tầng thứ năm đóng vai trò đặc biệt: kiểm thử hồi quy có kiểm chứng. Với mỗi lỗi được khắc phục, dự án tạm hoàn nguyên bản sửa để xác nhận kiểm thử thực sự thất bại trên lỗi gốc, rồi mới khôi phục bản sửa. Cách làm này đảm bảo kiểm thử không trở thành hình thức. Mục 2.5.5 trình bày chi tiết cơ chế này cùng kết quả quan sát được."),

    ("h3", "1.5.4. Kế hoạch kiểm thử"),
    ("p", "Bảng 1.14 trình bày kế hoạch kiểm thử gồm năm bộ, phạm vi, kỹ thuật dùng để kiểm thử, số kiểm tra và lệnh chạy. Việc ghi kèm lệnh chạy ngay từ kế hoạch là điều kiện để kế hoạch này có thể trở thành đường dẫn kiểm chứng tự động sau này."),
    ("tbl", "Bảng 1.14. Kế hoạch kiểm thử chi tiết",
     ["Bộ kiểm thử", "Phạm vi", "Kỹ thuật", "Số kiểm tra", "Lệnh chạy"],
     [
        ["Đơn vị backend", "Nghiệp vụ giao dịch, ngân sách, danh mục, xác thực, AI", "JUnit 5, khẳng định về giá trị", "98", "mvn verify"],
        ["Tích hợp backend", "Migration, dữ liệu khởi tạo, căn cứ AI", "JUnit 5 với CSDL thật qua Testcontainers", "15", "mvn verify"],
        ["Đơn vị Android", "Định dạng dữ liệu, quy tắc nghiệp vụ hiển thị", "JUnit, dữ liệu cố định", "10", "gradlew testDebugUnitTest"],
        ["Hợp đồng API", "Kiểu dữ liệu giao diện quản trị khớp API", "PowerShell, so khớp lược đồ", "102", "tests/admin-web-contract.ps1"],
        ["Đầu cuối", "Luồng nghiệp vụ trên hệ thống đang chạy", "PowerShell, dữ liệu duy nhất mỗi lần chạy", "160", "tests/e2e-api-tests.ps1"],
     ], [2.6, 4.0, 3.6, 1.8, 3.0]),
    ("p", "Hình 1.3 trình bày quy trình kiểm thử của dự án, trong đó vòng lặp phát hiện lỗi và khắc phục là thành phần trung tâm chứ không phải bước cuối cùng."),
    ("fig", "report/figures/04_quy_trinh_kiem_thu.png", "Hình 1.3. Quy trình kiểm thử và vòng lặp xử lý lỗi của dự án"),
    ("p", "Bộ kiểm thử đầu cuối được thiết kế để chạy lại được nhiều lần: mỗi lần chạy sinh dữ liệu người dùng và mã danh mục duy nhất, và các phép đối chiếu được tính tương đối so với dữ liệu nền. Nếu thiết kế không như vậy, bộ kiểm thử sẽ chỉ chạy được một lần và nhanh chóng trở nên vô dụng. Đây là điểm được ghi nhận khi rà soát rủi ro, với mã R-11 trong bảng 1.15."),

    ("h3", "1.5.5. Tiêu chí nghiệm thu"),
    ("p", "Tiêu chí nghiệm thu được xác định trước khi viết mã và gồm bảy tiêu chí tại bảng 1.11. Nguyên tắc áp dụng là tiêu chí phải kiểm tra được bằng lệnh, không chấp nhận tiêu chí dạng đánh giá cảm tính. Chẳng hạn, tiêu chí “hệ thống ổn định” không dùng được, còn tiêu chí “bộ kiểm thử E2E đạt 160/160” thì có. Bảy tiêu chí ở đây là cấp dự án; nghiệm thu ở cấp yêu cầu được đếm riêng trên ma trận truy vết 164 dòng, và hai cấp này không cộng vào nhau."),

    ("h3", "1.5.6. Kế hoạch xử lý lỗi"),
    ("p", "Quy trình xử lý lỗi gồm sáu bước: ghi nhận hiện tượng, tái hiện, tìm nguyên nhân gốc, khắc phục tối thiểu, kiểm thử hồi quy có kiểm chứng, và xác nhận sau khắc phục. Bước thứ năm là bước đặc thù của dự án và cũng là bước hay bị bỏ qua nhất trong thực tế."),
    ("fig", "report/figures/06_quy_trinh_xu_ly_loi.png", "Hình 1.4. Quy trình xử lý lỗi áp dụng cho các mục lỗi của dự án"),
    ("p", "Bước tái hiện trước khi tìm nguyên nhân gốc nhằm loại trừ khả năng lỗi không xác định. Với lỗi liên quan tới môi trường triển khai, tái hiện đòi hỏi dựng lại từ trạng thái sạch, và dự án thực hiện điều này bằng cách hạ toàn bộ stack rồi dựng lại. Quy trình đầy đủ được minh hoạ trong hình 1.4."),

    ("h3", "1.5.7. Chỉ số chất lượng"),
    ("p", "Dự án theo dõi bốn nhóm chỉ số. Nhóm thứ nhất là độ bao phủ kiểm thử, đo bằng tỷ lệ chức năng trong phạm vi có kiểm thử. Nhóm thứ hai là chất lượng kiểm thử, đo bằng tỷ lệ kiểm tra thất bại và số lỗi phát hiện được bởi từng bộ. Nhóm thứ ba là chất lượng cấu hình, đo bằng số bí mật phát hiện trong kho mã nguồn. Nhóm thứ tư là tính tái lập, đo bằng số lần dựng lại thành công từ trạng thái sạch."),
    ("note", "Dự án không công bố tỷ lệ độ bao phủ mã nguồn. Không có công cụ đo độ bao phủ nào được cấu hình, và báo cáo một con số độ bao phủ khi chưa đo sẽ là ước lượng. Bảng 1.12 nêu rõ những đại lượng nào đo được và những đại lượng nào không."),

    ("h2", "1.6. Quản lý rủi ro"),
    ("p", "Quản lý rủi ro được tổ chức theo mô hình bốn bước của NIST SP 800-30 Rev. 1, gồm định khung, đánh giá, ứng phó và theo dõi; trong đó đánh giá rủi ro dựa trên bốn yếu tố mối đe doạ, điểm yếu, mức độ ảnh hưởng và khả năng khai thác điểm yếu [2]. Dự án giữ nguyên khung bốn bước này, đồng thời đơn giản hóa bước đánh giá xuống còn tích số giữa khả năng xảy ra và mức độ ảnh hưởng."),
    ("p", "Cần nói rõ sự đơn giản hóa này. Tích số nhân là cách chấm điểm rủi ro phổ biến trong thực hành quản lý dự án, nhưng nó không phải định nghĩa gốc trong tiêu chuẩn. NIST xác định rủi ro qua phân tích định tính hoặc định lượng trên bốn yếu tố, và khẳng định rủi ro là “hàm của khả năng xảy ra và mức độ ảnh hưởng” chứ không phải tích số cộng hay nhân cố định [2]. Dự án chọn tích số nhân vì nó đơn giản, dễ kiểm tra lại bằng tay và phù hợp với quy mô sổ rủi ro mười tám mục. Đây là lựa chọn công cụ, không phải khẳng định về mô hình rủi ro."),
    ("p", "Hình 1.5 thể hiện bốn bước này kèm điểm dừng quyết định sau mỗi bước, nhờ đó một rủi ro chỉ chuyển sang bước kế tiếp khi đã có đủ thông tin để đánh giá."),
    ("fig", "report/figures/03_quan_ly_rui_ro.png", "Hình 1.5. Quy trình quản lý rủi ro áp dụng cho dự án"),
    ("h3", "1.6.1. Nhận diện rủi ro"),
    ("p", "Rủi ro được nhận diện từ bốn nguồn: yêu cầu nghiệp vụ, thiết kế kiến trúc, hoạt động kiểm thử, và rà soát bảo mật. Nguồn đáng chú ý nhất là hoạt động kiểm thử, vì phần lớn rủi ro trong bảng 1.15 được phát hiện khi hệ thống đã được dựng và hành vi thực tế khác với dự kiến."),
    ("h3", "1.6.2. Phân tích rủi ro"),
    ("tbl", "Bảng 1.15. Sổ rủi ro của dự án",
     ["Mã", "Rủi ro", "K", "H", "Điểm", "Trạng thái", "Kiểm soát chính"],
     [], [1.3, 4.6, 0.9, 0.9, 1.1, 2.4, 3.8]),
    ("note", "K là khả năng xảy ra, H là mức độ ảnh hưởng, điểm rủi ro bằng K × H. Mức điểm 15 trở lên được coi là cao, từ 10 đến 14 là trung bình, dưới 10 là thấp. Bảng lấy từ tài liệu rủi ro của dự án; chi tiết kiểm soát được nêu ở Phụ lục B."),
    ("h3", "1.6.3. Đánh giá mức độ ảnh hưởng"),
    ("p", "Mức độ ảnh hưởng được chấm theo hệ quả thực tế. Với dự án này, mức 5 được dành cho rủi ro có thể làm hỏng dữ liệu người dùng hoặc vi phạm điều khiển truy cập, mức 4 cho rủi ro làm mất chức năng đã cam kết, mức 3 cho rủi ro làm giảm chất lượng dịch vụ, mức 2 cho bất tiện và mức 1 cho vấn đề hình thức. Ba rủi ro đạt mức ảnh hưởng 5 là rò rỉ dữ liệu giữa người dùng, thất bại migration, và thiếu sao lưu."),
    ("h3", "1.6.4. Kế hoạch ứng phó"),
    ("p", "Bốn chiến lược ứng phó được sử dụng. Giảm thiểu áp dụng khi có thể thêm kiểm soát kỹ thuật, ví dụ thêm kiểm tra trạng thái tài khoản ở mỗi yêu cầu để chặn truy cập sau khi khóa tài khoản. Tránh áp dụng khi thiết kế lại loại bỏ được rủi ro, ví dụ chỉ cho phép vô hiệu hóa danh mục thay vì xóa để bảo toàn lịch sử giao dịch. Chuyển giao áp dụng khi giới hạn phạm vi, ví dụ giới hạn mô-đun AI ở chế độ cục bộ và ghi nhận đường dẫn bên ngoài là chưa kiểm chứng. Chấp nhận áp dụng khi rủi ro thấp và việc xử lý tốn kém hơn giá trị thu được."),
    ("h3", "1.6.5. Theo dõi và cập nhật rủi ro"),
    ("p", "Sổ rủi ro được cập nhật ở ba thời điểm: khi thiết kế, khi phát hiện lỗi, và khi kiểm toán cuối. Kết quả theo dõi cho thấy năm rủi ro đã thực sự xảy ra và được xử lý, không rủi ro cao nào còn mở. Bốn rủi ro vẫn đang mở là thiếu tích hợp liên tục, thiếu sao lưu, thiếu giới hạn tần suất, và tài liệu lệch đối với thực tế. Bốn rủi ro này được trình bày như khoảng trống vận hành chứ không phải lỗi mã nguồn, vì chúng thuộc phạm vi quyết định hạ tầng của chủ dự án."),

    ("h2", "1.7. Đo lường và kế hoạch theo dõi"),

    ("h3", "1.7.1. Mục tiêu đo lường"),
    ("p", "Mục tiêu đo lường là phát hiện sớm khi sản phẩm lệch khỏi tiêu chí đã định, để còn thời gian sửa. Điều này đặt ra yêu cầu mỗi chỉ số phải đo được bằng lệnh tự động. Chỉ số không đo được tự động thường trở thành ghi nhận chủ quan, và báo cáo đó không có tác dụng theo dõi."),

    ("h3", "1.7.2. Các chỉ số được sử dụng"),
    ("tbl", "Bảng 1.16. Các chỉ số theo dõi",
     ["Nhóm", "Chỉ số", "Đơn vị", "Nguồn dữ liệu", "Tần suất"],
     [
        ["Phạm vi", "Số dòng truy vết theo trạng thái", "dòng", "Ma trận truy vết yêu cầu", "Mỗi kỳ nghiệm thu"],
        ["Chất lượng", "Số kiểm tra tự động", "kiểm tra", "5 bộ kiểm thử", "Mỗi lần kiểm chứng"],
        ["Chất lượng", "Số lỗi phát hiện", "lỗi", "Nhật ký kiểm thử", "Mỗi lần kiểm chứng"],
        ["Vận hành", "Số container khỏe", "container", "docker compose ps", "Mỗi lần triển khai"],
        ["Bảo mật", "Số bí mật trong kho mã nguồn", "bí mật", "Quét trước khi ghi", "Trước mỗi lần ghi"],
        ["Rủi ro", "Số rủi ro cao còn mở", "rủi ro", "Sổ rủi ro", "Mỗi kỳ rà soát"],
        ["Cấu hình", "Trạng thái đồng bộ kho mã nguồn", "—", "git rev-list", "Mỗi lần phát hành"],
     ], [2.0, 4.5, 1.8, 4.0, 2.7]),

    ("h3", "1.7.3. Phương pháp thu thập dữ liệu"),
    ("p", "Mọi chỉ số trong bảng 1.16 được thu thập bằng lệnh cho ra kết quả trực tiếp, không qua nhập liệu thủ công. Riêng chỉ số rủi ro được cập nhật bằng phân tích định tính đối chiếu với bằng chứng, nên đây là chỉ số duy nhất phụ thuộc đánh giá của con người. Dự án ghi nhận điểm hạn chế này thay vì coi mọi chỉ số là cùng loại."),

    ("h3", "1.7.4. Tần suất theo dõi"),
    ("p", "Tần suất thực tế được điều chỉnh theo tính chất chỉ số. Chỉ số vận hành và chất lượng được kiểm tra ở mỗi lần triển khai. Chỉ số bảo mật được kiểm tra trước mỗi lần ghi thay đổi. Chỉ số phạm vi và rủi ro được rà soát ở các mốc nghiệm thu. Do dự án không có vòng lặp phát triển theo chu kỳ, không có tần suất theo tuần hoặc theo tháng."),

    ("h3", "1.7.5. Tiêu chí đánh giá"),
    ("p", "Tiêu chí đánh giá được đặt trước khi đo. Đối với chất lượng, tiêu chí là không có lỗi trong bất kỳ bộ kiểm thử nào. Đối với vận hành, tiêu chí là cả ba container ở trạng thái khỏe. Đối với bảo mật, tiêu chí là không có bí mật nào trong kho mã nguồn. Đối với rủi ro, tiêu chí là không có rủi ro mức cao nào còn mở. Cả bốn tiêu chí này đều đạt tại thời điểm kiểm chứng cuối. Cần nói rõ rằng tiêu chí rủi ro vẫn đạt dù bốn rủi ro còn mở, vì ba trong số đó thuộc mức trung bình và một thuộc mức thấp theo thang điểm tại chú thích bảng 1.15; tiêu chí được đặt ở mức cao chứ không đặt ở mức trung bình."),

    ("h2", "1.8. Kế hoạch quản lý dự án"),
    ("p", "Mục này mô tả cách tổ chức quản lý từng mảng công việc. Nguyên tắc áp dụng là chỉ mô tả những gì thực sự đã thực hiện và có bằng chứng. Những mảng không có hoạt động thực tế được ghi nhận là không có, thay vì trình bày một kế hoạch lý thuyết."),

    ("h3", "1.8.1. Quản lý phạm vi"),
    ("p", "Quản lý phạm vi được thực hiện bằng ma trận truy vết gồm 164 dòng, mỗi dòng mang đúng một trạng thái. Nguyên tắc bất di bất dịch của dự án là không nâng trạng thái lên đã xác minh nếu chưa có kiểm thử đã chạy hoặc kết quả lệnh trực tiếp. Bảng 1.17 tóm tắt kết quả."),
    ("tbl", "Bảng 1.17. Tình trạng truy vết yêu cầu",
     ["Trạng thái", "Số dòng", "Tỷ lệ", "Ý nghĩa"],
     [
        ["Đã xác minh", "148", "90,2%", "Có kiểm thử đã chạy hoặc lệnh trực tiếp"],
        ["Xác minh một phần", "8", "4,9%", "Đã triển khai nhưng chưa phủ kiểm thử đầy đủ"],
        ["Khoảng trống", "7", "4,3%", "Chưa thực hiện"],
        ["Ngoài phạm vi", "1", "0,6%", "Cố ý loại khỏi phạm vi, có lý do ghi kèm"],
        ["Tổng", "164", "100%", "Toàn bộ dòng yêu cầu trong ma trận truy vết"],
     ], [3.5, 2.0, 2.0, 7.5]),
    ("p", "Bảy khoảng trống còn lại đều là thực hành thiếu chứ không phải hành vi nghiệp vụ thiếu, cụ thể là: đường dẫn nhà cung cấp AI bên ngoài chưa kiểm chứng, chưa có tích hợp liên tục, chưa tự động hóa kiểm thử giao diện, chưa kiểm thử tải, chưa giới hạn tần suất, chưa chứng thực TLS, và chưa quét hình ảnh container. Bảy khoảng trống này được thừa nhận trong mọi tài liệu liên quan."),

    ("h3", "1.8.2. Quản lý tiến độ"),
    ("p", "Không có hoạt động quản lý tiến độ thực sự. Dự án không có biểu đồ tiến trình, không có mốc thời gian, và không có cơ chế báo cáo trạng thái định kỳ. Nguyên nhân là quản lý phiên bản chưa tồn tại trong giai đoạn phát triển, nên không có dữ liệu để theo dõi. Mục này được ghi nhận là khoảng trống quản lý, và là một trong những lý do chính của mục 1.4.5."),

    ("h3", "1.8.3. Quản lý chất lượng"),
    ("p", "Quản lý chất lượng là mảng được thực hiện đầy đủ nhất. Năm bộ kiểm thử được duy trì như bộ kiểm thử hồi quy, mỗi bộ phục vụ một tầng. Bộ kiểm thử hợp đồng API tồn tại để ngăn giao diện quản trị lệch khỏi hợp đồng dữ liệu, đây là rủi ro thường gặp khi hai bên phát triển độc lập. Chất lượng được kiểm soát bằng tiêu chuẩn chấp nhận nêu tại bảng 1.11, không phải bằng đánh giá cảm tính."),

    ("h3", "1.8.4. Quản lý rủi ro"),
    ("p", "Quản lý rủi ro được thực hiện theo quy trình bốn bước nêu tại mục 1.6, với sổ rủi ro gồm 18 mục. Điểm đáng chú ý là năm rủi ro đã thực sự xảy ra, mỗi rủi ro đều có cách phát hiện và bằng chứng xử lý. Một sổ rủi ro chỉ ghi “đã giảm thiểu” mà không có sự kiện thực tế sẽ không cho biết biện pháp có hiệu quả hay không; sự kiện thực tế là phép thử duy nhất chứng minh hiệu quả của biện pháp."),

    ("h3", "1.8.5. Quản lý thay đổi"),
    ("p", "Quản lý thay đổi trong giai đoạn phát triển không có kiểm soát vì không có quản lý phiên bản. Từ thời điểm thiết lập kho mã nguồn, mọi thay đổi được ghi bằng thông điệp cam kết mô tả mục đích, và mỗi lần phát hành được đánh dấu bằng thẻ. Cơ chế này giúp truy vết cấu hình từ điểm baseline trở đi, nhưng không thể tái tạo lịch sử trước đó."),

    ("h3", "1.8.6. Quản lý tài liệu"),
    ("p", "Tài liệu gồm 28 tệp Markdown được tổ chức theo hai nhóm: tài liệu kỹ thuật mô tả hệ thống, và tài liệu quản lý dự án mô tả bằng chứng. Quy tắc áp dụng là mọi khẳng định trong tài liệu kỹ thuật phải truy được về bằng chứng thực thi. Quy tắc này bị vi phạm và được phát hiện trong quá trình kiểm toán: ba khẳng định tài liệu sai so với thực tế, gồm tuyên bố ba container khỏe trong khi container quản trị báo không khỏe, giả định điểm cuối đo lường công khai trong khi thực tế yêu cầu xác thực, và mô tả sai vị trí thông báo trên ứng dụng di động. Cả ba đã được sửa."),

    ("h3", "1.8.7. Quản lý giao tiếp"),
    ("p", "Không có bản ghi giao tiếp dự án. Không có biên bản họp, không có biên bản quyết định, và không có nhật ký trao đổi với các bên liên quan. Giống tiến độ, đây là khoảng trống không thể khôi phục. Tác động của nó đến khả năng truy vết các quyết định thiết kế được ghi nhận tại mục 1.4.5."),

    ("h2", "1.9. Quản lý cấu hình"),
    ("p", "Hình 1.6 thể hiện dòng chảy quản lý cấu hình: từ việc xác định mục tiêu cấu hình, qua công cụ và định dạng lưu trữ, đến việc kiểm soát thay đổi và đóng băng cấu hình. Dòng chảy này áp dụng cho cả cấu hình sản phẩm lẫn cấu hình quy trình."),
    ("fig", "report/figures/05_quan_ly_cau_hinh.png", "Hình 1.6. Dòng chảy quản lý cấu hình của dự án"),

    ("h3", "1.9.1. Đối tượng cấu hình"),
    ("p", "Đối tượng cấu hình của dự án gồm mã nguồn, cấu hình xây dựng, cấu hình chạy, cấu hình cơ sở dữ liệu, cấu hình triển khai và tài liệu. Sáu nhóm này dùng chung một cơ chế kiểm soát phiên bản và cùng một mốc phát hành. Phạm vi quản lý cấu hình được giới hạn ở cấu hình của phần mềm; dữ liệu người dùng không nằm trong phạm vi này."),

    ("h3", "1.9.2. Quản lý phiên bản"),
    ("p", "Quản lý phiên bản được thiết lập sau giai đoạn phát triển chính, và đây là khoảng trống có hậu quả nặng nhất trong toàn bộ dự án. Cần nêu chính xác: kho mã nguồn hiện có ba cam kết, trong đó một cam kết là mẫu ban đầu của GitHub, một cam kết là baseline đã kiểm chứng, và một cam kết là bản cập nhật tài liệu. Kho này cung cấp khả năng truy vết cấu hình từ điểm baseline trở đi, nhưng không tái tạo được quá trình phát triển trước đó."),
    ("p", "Việc thiết lập kho mã nguồn được thực hiện mà không sửa lịch sử từ xa. Lệnh đẩy là đẩy tiến nhanh, không ép đẩy, và cam kết mẫu ban đầu được giữ nguyên. Chi tiết kỹ thuật của thao tác này được trình bày tại mục 3.3.1."),

    ("h3", "1.9.3. Cấu hình nền tảng"),
    ("p", "Cấu hình nền tảng gồm cam kết mã nguồn, lịch sử chín tương ứng, cấu hình triển khai và tệp tài liệu. Cấu hình nền tảng được xác định tại cam kết gắn thẻ phát hành, cho phép dựng lại đúng trạng thái đã kiểm chứng."),

    ("h3", "1.9.4. Quản lý thay đổi cấu hình"),
    ("p", "Thay đổi cấu hình được phân loại theo mức độ ảnh hưởng. Thay đổi cấu hình chạy và bí mật chỉ tồn tại ngoài kho mã nguồn. Thay đổi lược đồ cơ sở dữ liệu bắt buộc qua tệp migration mới, không sửa tệp migration đã áp dụng, nhờ cơ chế kiểm tra mã băm của Flyway [10]. Thay đổi cấu hình triển khai thực hiện qua tệp Compose. Mỗi nhóm đều có kiểm soát tương ứng."),

    ("h3", "1.9.5. Quản lý phát hành"),
    ("p", "Phát hành được thực hiện bằng thẻ gắn với cam kết cụ thể. Thẻ hiện tại là v1.0.0-academic-final, chỉ định bản học thuật cuối cùng đã kiểm chứng. Dự án chỉ có một bản phát hành, và đây là hệ quả trực tiếp của việc thiếu lịch sử phát triển: không có các bản phát hành trung gian để đối chiếu."),

    ("h3", "1.9.6. Trạng thái cấu hình nền tảng cuối cùng"),
    ("tbl", "Bảng 1.18. Cấu hình nền tảng cuối cùng",
     ["Hạng mục", "Giá trị", "Cách xác minh"],
     [
        ["Cam kết nền tảng", "175ce3bd96cf68251151be20a8e722520274bf71", "git rev-parse HEAD"],
        ["Thẻ phát hành", "v1.0.0-academic-final", "git tag"],
        ["Số tệp trong nền tảng", "232", "git ls-tree -r HEAD"],
        ["Trạng thái đồng bộ", "Khác biệt 0/0", "git rev-list --left-right --count"],
        ["Số bí mật được ghi", "0", "Quét trước khi ghi"],
        ["Tệp sinh tự động được ghi", "0", "Kiểm tra quy tắc loại trừ"],
        ["Lịch sử phát triển", "Không tồn tại", "Xem mục 1.4.5"],
     ], [4.0, 6.5, 4.5]),
]
