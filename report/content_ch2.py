# -*- coding: utf-8 -*-
"""Chuong 2 - Thuc hien va kiem soat du an."""

CHUONG_2 = [
    ("h1", "CHƯƠNG 2. THỰC HIỆN VÀ KIỂM SOÁT DỰ ÁN"),
    ("p", "Chương 1 mô tả kế hoạch. Chương 2 trình bày việc thực hiện kế hoạch đó và các hoạt động kiểm soát phát sinh trong quá trình thực hiện. Điểm nhấn của chương là phần lớn phát hiện quan trọng không đến từ quá trình xây dựng, mà đến từ quá trình kiểm soát sau đó."),

    ("h2", "2.1. Triển khai kế hoạch dự án"),

    ("h3", "2.1.1. Triển khai phạm vi"),
    ("p", "Phạm vi được chuyển thành danh mục chức năng ở bảng 1.3, và từng chức năng được thực hiện cùng với kiểm thử tương ứng. Quy trình này không cho phép tích lũy việc kiểm thử dồn cuối dự án, vốn là nguyên nhân phổ biến khiến giai đoạn kiểm soát bị rút ngắn. Mười nhóm chức năng trong bảng 1.3 đều có ít nhất một bộ kiểm thử, và tổng số kiểm tra là 385."),
    ("p", "Ba nội dung nằm ngoài phạm vi tại bảng 1.4 không bị bỏ quên mà được ghi nhận bằng trạng thái riêng trong ma trận truy vết. Sự phân biệt này có ý nghĩa thực tế: một chức năng bị loại khỏi phạm vi có chủ đích khác với một chức năng bị quên, và khi đánh giá tiến độ hai trường hợp này phải được xử lý khác nhau."),

    ("h3", "2.1.2. Triển khai quy trình"),
    ("p", "Quy trình bốn giai đoạn tại bảng 1.9 được thực hiện theo thứ tự, nhưng có một điểm đáng chú ý: hoạt động kiểm soát không chờ đến cuối. Bộ kiểm thử hợp đồng API được viết song song với bảng điều khiển quản trị, và bộ kiểm thử đầu cuối được viết trước khi hệ thống được dựng lần đầu. Cách tổ chức này biến kiểm soát từ hoạt động kiểm tra thành hoạt động xây dựng."),

    ("h3", "2.1.3. Triển khai các sản phẩm bàn giao"),
    ("tbl", "Bảng 2.1. Đối chiếu sản phẩm bàn giao với kế hoạch",
     ["Sản phẩm kế hoạch", "Kết quả thực tế", "Trạng thái"],
     [
        ["Mã nguồn backend", "78 tệp Java", "Đạt"],
        ["Mã nguồn ứng dụng di động", "22 tệp Kotlin", "Đạt"],
        ["Mã nguồn bảng quản trị", "13 tệp TypeScript", "Đạt"],
        ["Cơ sở dữ liệu", "9 migration, 11 bảng", "Đạt"],
        ["Mô-đun AI", "6 ý định phân tích, chế độ cục bộ", "Đạt"],
        ["Cấu hình triển khai", "3 hình ảnh, 3 container", "Đạt"],
        ["Bộ kiểm thử", "5 bộ, 385 kiểm tra", "Đạt"],
        ["Giao diện phản hồi trên di động", "Không thực hiện", "Ngoài phạm vi"],
        ["Kiểm thử tự động giao diện", "Không thực hiện", "Ngoài phạm vi"],
     ], [5.0, 5.5, 4.0]),

    ("h2", "2.2. Thực hiện phát triển hệ thống"),
    ("p", "Mục này mô tả việc thực hiện được quản lý như thế nào, không mô tả chi tiết lập trình. Trọng tâm là quyết định thiết kế nào ảnh hưởng đến khả năng kiểm soát chất lượng và vận hành."),

    ("h3", "2.2.1. Backend"),
    ("p", "Backend được xây dựng trên Spring Boot 3.5.16 với Java 21, chia thành mười sáu gói theo miền nghiệp vụ. Cấu trúc phân tách lớp điều khiển, lớp nghiệp vụ và lớp truy cập dữ liệu cho phép kiểm thử lớp nghiệp vụ không cần dựng máy chủ. Đây là điều kiện tiên quyết để có 98 kiểm thử đơn vị chạy nhanh và ổn định."),
    ("p", "Quyết định thiết kế đáng chú ý nhất là cơ chế kiểm soát truy cập. Định danh người dùng được truyền vào lớp nghiệp vụ như một đối số của phương thức dịch vụ, không phải như một tham số do người gọi cung cấp trong nội dung yêu cầu. Cách làm này loại trừ về mặt kiến trúc khả năng truy vấn dữ liệu của người dùng khác, thay vì phụ thuộc vào việc kiểm tra ở tầng điều khiển. Rủi ro R-02 được đánh giá là đã kiểm soát nhờ quyết định này."),

    ("h3", "2.2.2. Ứng dụng di động"),
    ("p", "Ứng dụng di động dùng Kotlin và Jetpack Compose theo kiến trúc MVVM với hai tầng dữ liệu và giao diện, phù hợp với khuyến nghị về phân tầng ứng dụng và luồng dữ liệu một chiều [11]. Mười kiểm thử đơn vị tập trung vào lớp định dạng dữ liệu và quy tắc hiển thị, vì đây là nơi lỗi người dùng nhìn thấy trực tiếp nhất."),
    ("p", "Một rủi ro bảo mật còn mở là token lưu trữ không mã hóa trên thiết bị. Đây là hạn chế ở mức thấp đối với bản dựng gỡ lỗi, nhưng cần xử lý trước khi phát hành sản phẩm thực. Dự án ghi nhận rủi ro này với mã R-15 và không tuyên bố sẵn sàng cho bản phát hành thương mại."),

    ("h3", "2.2.3. Bảng quản trị"),
    ("p", "Bảng quản trị xây dựng mới hoàn toàn trong phạm vi dự án, với các trang quản lý người dùng, danh mục, phản hồi, nhật ký kiểm toán và bảng điều khiển tổng quan. Vì không tồn tại trước đó, mọi quyết định thiết kế đều có thể lựa chọn theo hợp đồng dữ liệu của backend từ đầu."),
    ("p", "Rủi ro đặc thù của thành phần này là lệch kiểu dữ liệu khi hai bên phát triển độc lập. Dự án xử lý bằng cách viết bộ kiểm thử hợp đồng API gồm 102 khẳng định, so khớp lược đề phản hồi của backend với định nghĩa kiểu trong bảng quản trị. Bộ kiểm thử này đạt 102/102 và được duy trì như bộ kiểm thử hồi quy."),

    ("h3", "2.2.4. Cơ sở dữ liệu"),
    ("p", "Cơ sở dữ liệu PostgreSQL 16 gồm 11 bảng, được tạo bởi 9 tệp migration Flyway. Việc dùng migration có đánh số phiên bản kèm kiểm tra mã băm đảm bảo lịch sử thay đổi lược đồ có thể kiểm chứng [10]. Cấu hình đặt chế độ kiểm tra lược đồ thay vì tự động tạo cấu trúc, nhờ đó sai lệch giữa mã ứng dụng và lược đồ bị phát hiện ngay lúc khởi động."),
    ("p", "Hình 2.1 trình bày lược đồ thực tế của cơ sở dữ liệu sau khi áp dụng toàn bộ chín tệp migration. Điểm cần chú ý là bảng lịch sử giao dịch giữ tham chiếu tới danh mục và tài khoản, nên xoá cứng một danh mục sẽ làm hỏng lịch sử; đây là cơ sở của rủi ro R-03 được xử lý bằng cách vô hiệu hoá thay vì xoá."),
    ("fig", "report/figures/e2_database_schema.png", "Hình 2.1. Lược đồ cơ sở dữ liệu thực tế gồm 11 bảng và 9 migration Flyway đã áp dụng"),
    ("p", "Tiền mật được lưu bằng kiểu số chính xác với hai chữ số thập phân, tránh lỗi làm tròn dây chuyền đặc trưng của kiểu số thực dấu phẩy động trong tính toán tiền tệ [13]. Dự án ghi nhận hạn chế là chỉ hỗ trợ một loại tiền tệ, không có mã tiền tệ trong lược đồ."),

    ("h3", "2.2.5. Mô-đun AI"),
    ("p", "Mô-đun AI là thành phần có quyết định thiết kế ảnh hưởng lớn nhất, và cũng là nơi tập trung rủi ro R-01 với điểm cao nhất còn mở trong nhóm rủi ro đã kiểm soát. Quyết định cốt lõi là mô-đun không sinh câu trả lời bằng văn bản tự do, mà chỉ chọn lựa chọn trong tập dữ liệu đã tính sẵn rồi định dạng thành câu trả lời có ràng buộc."),
    ("p", "Quyết định này biến bài toán từ khó sang dễ kiểm chứng. Thay vì phải chứng minh một câu văn bản là đúng, chỉ cần chứng minh các con số trả về khớp với dữ liệu trong cơ sở dữ liệu. Khi dữ liệu không đủ, mô-đun trả lời rằng không đủ dữ liệu thay vì ước lượng. Sáu ý định phân tích được hỗ trợ, mỗi ý định có kiểm thử riêng trong bộ gồm 24 kiểm thử đơn vị và 8 kiểm thử tích hợp."),
    ("fig", "report/figures/e4_ai_grounding.png", "Hình 2.2. Mô-đun AI từ chối đưa ra con số khi dữ liệu chưa đủ, kèm giải thích hành vi"),
    ("p", "Hình 2.2 ghi lại phản hồi thật của hệ thống khi được hỏi về chi tiêu ăn uống trong tháng đối với tài khoản chưa có giao dịch. Hệ thống nói rõ không đủ dữ liệu và đề nghị bổ sung giao dịch, thay vì đưa ra một con số. Đây là hành vi được thiết kế và kiểm thử, không phải lỗi. Đường dẫn nhà cung cấp AI bên ngoài chỉ được ghi nhận là chưa kiểm chứng; chế độ cục bộ là chế độ được hỗ trợ thực tế."),

    ("h3", "2.2.6. Docker và môi trường triển khai"),
    ("p", "Môi trường triển khai gồm ba container: PostgreSQL, backend và bảng quản trị. Khai báo Compose định nghĩa dịch vụ, mạng, khối lưu trữ và kiểm tra sức khoẻ như các thành phần hạng nhất [9]. Quy ước cổng dịch vụ được đặt ngoài vùng mặc định cho phép dựng stack cạnh các dịch vụ đang chạy trên cùng máy."),
    ("p", "Hình 2.3 ghi lại trạng thái ba container ngay sau khi dựng lại từ trạng thái sạch, cả ba đều ở trạng thái khỏe. Ảnh này được lấy từ kết quả lệnh kiểm tra trạng thái, không phải hình vẽ minh hoạ."),
    ("fig", "report/figures/e1_docker_health.png", "Hình 2.3. Trạng thái ba container sau khi dựng từ trạng thái sạch"),
    ("p", "Tín hiệu sức khoẻ của container đóng vai trò quan trọng trong vận hành. Trong quá trình kiểm toán, dự án phát hiện tín hiệu này từng báo sai: container quản trị ở trạng thái không khỏe trong khi vẫn phục vụ yêu cầu bình thường. Nguyên nhân và cách khắc phục được trình bày tại mục 2.5.4. Sự việc này cho thấy tín hiệu sức khoẻ container và tính khả dụng của ứng dụng là hai khái niệm khác nhau, cần được kiểm chứng riêng."),

    ("h2", "2.3. Xem xét lại"),
    ("p", "Hoạt động xem xét lại được thực hiện theo năm lớp: yêu cầu, thiết kế, hiện thực, kiểm thử và tài liệu. Mỗi lớp tìm ra loại sai lệch riêng. Bảng 2.2 tổng hợp các phát hiện theo lớp."),

    ("h3", "2.3.1. Xem xét yêu cầu"),
    ("p", "Xem xét yêu cầu phát hiện ra việc phạm vi hạ tầng chưa được ghi rõ. Ví dụ, yêu cầu bao phủ an toàn thông tin nhưng không định nghĩa rõ phạm vi có bao gồm sao lưu hay không. Dự án xử lý bằng cách tách rõ các hạng mục bắt buộc của sản phẩm khỏi các thực hành vận hành, và ghi rõ trạng thái của từng thực hành vận hành trong ma trận truy vết."),
    ("h3", "2.3.2. Xem xét thiết kế"),
    ("p", "Xem xét thiết kế phát hiện rủi ro R-03: khả năng xoá danh mục làm mất lịch sử giao dịch. Điểm ảnh hưởng của rủi ro này là 4 và điểm rủi ro 16, thuộc nhóm cao. Cách xử lý không phải là bổ sung thêm kiểm tra, mà là thay đổi thiết kế: chỉ cho phép vô hiệu hoá danh mục thay vì xoá. Bằng chứng là một phép kiểm thử khẳng định lịch sử giao dịch vẫn còn sau khi danh mục bị vô hiệu hoá."),
    ("h3", "2.3.3. Xem xét hiện thực"),
    ("p", "Xem xét hiện thực phát hiện lỗi D-1, một lỗi gây lỗi máy chủ ở mọi yêu cầu phân tích chi tiêu, tức là trên ý định được sử dụng nhiều nhất. Nguyên nhân là danh sách khởi tạo bằng phương thức trả về danh sách bất biến rồi bổ sung phần tử vào đó. Chi tiết tại mục 2.5.2."),
    ("h3", "2.3.4. Xem xét kiểm thử"),
    ("p", "Xem xét kiểm thử phát hiện một khoảng trống quan trọng: kiểm thử đơn vị của mô-đun AI chỉ kiểm tra hình thức câu trả lời chứ không kiểm tra giá trị số. Vì vậy lỗi D-1 chưa bị phát hiện bởi kiểm thử đơn vị. Phản ứng là bổ sung bộ kiểm thử tích hợp kiểm chứng giá trị, và đây chính là lý do bộ gồm 8 kiểm thử AiGroundingIT được thêm vào."),
    ("h3", "2.3.5. Xem xét tài liệu"),
    ("p", "Xem xét tài liệu phát hiện tám sai lệch, trong đó có ba sai lệch đáng kể vì chúng mâu thuẫn với hành vi quan sát được. Sai lệch thứ nhất là tài liệu khẳng định cả ba container ở trạng thái khỏe trong khi container quản trị báo không khỏe với chu kỳ lỗi liên tục 22 lần. Sai lệch thứ hai là giả định điểm cuối đo lường công khai trong khi thực tế yêu cầu xác thực. Sai lệch thứ ba là mô tả thông báo trên ứng dụng di động là một màn hình riêng trong khi thực tế nằm trong trang hồ sơ."),
    ("h3", "2.3.6. Kết quả xem xét lại"),
    ("tbl", "Bảng 2.2. Phát hiện chính của từng lớp xem xét lại",
     ["Lớp xem xét lại", "Phát hiện chính", "Cách xử lý", "Bằng chứng xác nhận", "Trạng thái"],
     [
        ["Yêu cầu", "Phạm vi hạ tầng chưa được định nghĩa rõ, ví dụ sao lưu có thuộc phạm vi hay không",
         "Tách hạng mục bắt buộc của sản phẩm khỏi thực hành vận hành",
         "Ma trận truy vết: 7 khoảng trống, 8 xác minh một phần", "Đã phân loại"],
        ["Thiết kế", "Rủi ro R-03: xoá danh mục làm mất lịch sử giao dịch, điểm rủi ro 16",
         "Thay đổi thiết kế sang vô hiệu hoá thay vì xoá",
         "Kiểm thử đầu cuối giữ dữ liệu sau khi vô hiệu hoá", "Đã kiểm soát"],
        ["Hiện thực", "Lỗi D-1: danh sách bất biến bị thêm phần tử, gây lỗi máy chủ ở mọi yêu cầu phân tích chi tiêu",
         "Bọc danh sách khởi tạo bằng bản sao có thể ghi",
         "4 kiểm tra thất bại khi hoàn nguyên bản sửa", "Đã sửa"],
        ["Hiện thực", "Hai mục cấu hình bị loại nhầm: tệp bọc Gradle và thư mục công cụ trợ giúp",
         "Bổ sung ngoại lệ trong danh sách loại trừ của kho mã nguồn",
         "Số tệp trong cam kết giảm từ 238 xuống 232", "Đã sửa"],
        ["Kiểm thử", "Kiểm thử đơn vị mô-đun AI chỉ kiểm tra hình thức câu trả lời, không kiểm tra giá trị số",
         "Bổ sung bộ kiểm thử tích hợp kiểm chứng giá trị",
         "8 kiểm tra kiểm chứng giá trị; 4 lỗi khi cố ý chèn lỗi", "Đã bổ sung"],
        ["Kiểm thử", "Chưa có kiểm thử giao diện tự động và chưa kiểm thử tải",
         "Ghi nhận thành khoảng trống thay vì tuyên bố đã bao phủ",
         "Ma trận truy vết: trạng thái khoảng trống", "Còn mở"],
        ["Tài liệu", "Tám sai lệch giữa tài liệu và hành vi quan sát được",
         "Sửa hoặc phân loại từng sai lệch, không xoá",
         "Kiểm toán nội dung tài liệu; 8 sai lệch đã xử lý", "Đã xử lý"],
        ["Tổng cộng", "5 mục lỗi và cấu hình đã khắc phục; 8 sai lệch tài liệu đã xử lý; 1 nhóm khoảng trống kiểm thử còn mở",
         "Mỗi mục đều có bằng chứng xác nhận riêng",
         "Xem bảng 2.3 và ma trận truy vết yêu cầu", "Có mục còn mở"],
     ], [2.0, 4.2, 3.2, 3.4, 1.8]),
    ("p", "Điểm đáng chú ý trong bảng 2.2 là không có phát hiện nào thuộc nhóm chỉ là lỗi tài liệu. Sai lệch tài liệu phát sinh từ việc tin vào giả định thay vì kiểm chứng, và mỗi sai lệch đều chỉ ra một cơ hội kiểm soát tốt hơn. Sai lệch về trạng thái container, chẳng hạn, cho thấy tài liệu chỉ nên ghi những gì đã chạy lệnh xác nhận. Điểm thứ hai là bảng không ghi số phát hiện theo lớp, vì dự án không lưu sổ đếm phát hiện riêng cho từng lớp; những gì có thể chứng minh thì nêu kèm bằng chứng, những gì không thì ghi nhận là khoảng trống."),

    ("h2", "2.4. Giám sát và kiểm soát dự án"),

    ("h3", "2.4.1. Giám sát phạm vi"),
    ("p", "Giám sát phạm vi thực hiện bằng ma trận truy vết 164 dòng. Mỗi dòng mang đúng một trạng thái, và nguyên tắc không nâng trạng thái nếu chưa có bằng chứng. Kết quả cuối là 148 dòng đã xác minh, 8 dòng xác minh một phần, 7 khoảng trống và 1 nội dung ngoài phạm vi. Bảy khoảng trống đều thuộc nhóm thực hành thiếu, không thuộc nhóm chức năng thiếu."),

    ("h3", "2.4.2. Giám sát tiến độ"),
    ("p", "Không có hoạt động giám sát tiến độ. Dự án ghi nhận đây là khoảng trống không thể khắc phục, và nguyên nhân nằm ở việc quản lý phiên bản chưa tồn tại trong giai đoạn phát triển. Mục này được nêu với tên gọi khoảng trống quản lý dự án, không phải lỗi kỹ thuật."),

    ("h3", "2.4.3. Giám sát chất lượng"),
    ("p", "Giám sát chất lượng là mảng được thực hiện đầy đủ. Năm bộ kiểm thử được chạy lại toàn bộ trước khi xác lập baseline, không kế thừa kết quả từ các lần chạy trước. Cách làm này tốn thời gian hơn nhưng loại bỏ khả năng báo cáo lại kết quả đã cũ. Toàn bộ 385 kiểm tra đều được chạy lại và đều đạt."),

    ("h3", "2.4.4. Giám sát rủi ro"),
    ("p", "Giám sát rủi ro cho thấy sổ rủi ro hoạt động có hiệu quả: năm rủi ro đã thực sự xảy ra và mỗi rủi ro đều được ghi nhận với nguyên nhân và cách khắc phục. Ngược lại, rủi ro R-06 về rò rỉ thông tin qua nhật ký tồn tại trong sổ rủi ro suốt thời gian dài nhưng không được xử lý cho tới khi kiểm toán cuối buộc phải rà soát. Bài học rút ra là chỉ sổ rủi ro không thay thế được việc rà soát theo chu kỳ."),

    ("h3", "2.4.5. Giám sát lỗi"),
    ("p", "Giám sát lỗi tập trung vào chất lượng quy trình khắc phục hơn là khối lượng lỗi. Năm lỗi mã nguồn và cấu hình được phát hiện, mỗi lỗi đều có kiểm thử hồi quy chứng minh được thất bại trên lỗi gốc. Không có lỗi nào bị đóng khi chưa có kiểm thử. Bốn mục thuộc nhóm tài liệu được đóng bằng cách sửa hoặc phân loại, vì bản chất của chúng là sai lệch mô tả chứ không phải hành vi sai."),

    ("h3", "2.4.6. Kiểm soát thay đổi"),
    ("p", "Kiểm soát thay đổi có hiệu lực từ thời điểm thiết lập kho mã nguồn. Trong giai đoạn phát triển, mọi thay đổi đều không có dấu vết phiên bản. Có một phát hiện đáng chú ý trong giai đoạn cuối: tệp bọc Gradle từng bị loại khỏi kho mã nguồn bởi một quy tắc loại trừ theo phần mở rộng tệp, khiến bản sao mới không thể xây dựng phần Android. Không có kiểm thử nào phát hiện được điều này, vì mọi bản dựng cục bộ đều dùng tệp có sẵn trên đĩa. Chi tiết tại mục 2.5.3."),

    ("h3", "2.4.7. Kiểm soát cấu hình"),
    ("p", "Kiểm soát cấu hình được thực hiện ở ba điểm kiểm soát: quét bí mật trước khi ghi, quy tắc loại trừ tệp, và kiểm tra mã băm migration. Kết quả là không có bí mật nào trong kho mã nguồn, không có tệp sinh tự động nào bị ghi, và lịch sử migration có thể kiểm chứng. Tệp bọc Gradle được bổ sung vào kho sau phát hiện ở mục 2.4.6."),

    ("h2", "2.5. Quản lý lỗi và hành động khắc phục"),
    ("p", "Tổng cộng mười mục được phát hiện và xử lý, phân thành ba nhóm: năm lỗi mã nguồn và cấu hình, bốn mục tài liệu, và một lỗi phát hiện trong đợt kiểm chứng cuối. Năm lỗi đầu tiên đều có kiểm thử hồi quy chứng minh thất bại trên lỗi gốc. Bảng 2.3 tổng hợp; các mục 2.5.2 đến 2.5.4 trình bày chi tiết ba lỗi đại diện cho ba loại nguyên nhân khác nhau."),

    ("h3", "2.5.1. Quy trình xử lý lỗi"),
    ("p", "Quy trình gồm sáu bước: ghi nhận hiện tượng, tái hiện, tìm nguyên nhân gốc, khắc phục tối thiểu, kiểm thử hồi quy có kiểm chứng, và xác nhận sau khắc phục. Bước thứ năm là bước phân biệt dự án này với cách xử lý lỗi thông thường: bản sửa được tạm hoàn nguyên để xác nhận kiểm thử thực sự thất bại trên lỗi gốc, rồi mới khôi phục. Một kiểm thử chưa từng được quan sát thất bại thì chưa chứng minh được là kiểm thử."),

    ("h3", "2.5.2. Lỗi D-1: thêm phần tử vào danh sách bất biến"),
    ("p", "Đây là lỗi nghiêm trọng nhất trong năm lỗi, vì nó ảnh hưởng mọi yêu cầu phân tích chi tiêu, tức là ý định được dùng nhiều nhất. Biểu hiện là ngoại lệ không được hỗ trợ do thao tác trên tập hợp bất biến. Nguyên nhân là danh sách sự kiện được khởi tạo bằng phương thức trả về danh sách bất biến rồi bổ sung phần tử vào đó."),
    ("p", "Cách khắc phục là bọc danh sách khởi tạo trong một tập hợp có thể thay đổi. Điểm đáng lưu ý về quy trình là bản sửa đã được tạm hoàn nguyên để xác nhận bộ kiểm thử tạo ra bốn lỗi ngoại lệ không được hỗ trợ, sau đó mới khôi phục và chạy lại. Nếu bỏ qua bước này, không có cơ sở để khẳng định kiểm thử thực sự bảo vệ trước lỗi này."),
    ("note", "Bài học: lỗi D-1 không bị phát hiện bởi kiểm thử đơn vị sẵn có, vì các kiểm thử đó chỉ kiểm tra hình thức câu trả lời chứ không kiểm tra giá trị. Phát hiện được nhờ xem xét mã nguồn. Điều này cho thấy kiểm thử hình thức không thay thế được xem xét mã."),

    ("h3", "2.5.3. Lỗi D-2: định nghĩa trùng hàm định dạng"),
    ("p", "Lỗi này được phát hiện bởi kiểm thử đơn vị trên ứng dụng di động, và là ví dụ rõ ràng về giá trị của phép kiểm thử hồi quy có kiểm chứng. Biểu hiện là chuỗi thời gian không phân tích được bị hiển thị nguyên văn. Nguyên nhân là cùng một tệp có hai định nghĩa cùng tên, trong đó một định nghĩa che khuất định nghĩa còn lại."),
    ("p", "Khắc phục bằng cách xoá định nghĩa trùng và bổ sung kiểm tra định dạng cùng xử lý trường ngày tháng. Hai khẳng định thất bại được quan sát trước khi khắc phục. So với lỗi D-1, lỗi này được phát hiện bởi kiểm thử chứ không phải bằng xem xét mã, cho thấy hai lớp phát hiện bổ trợ cho nhau."),

    ("h3", "2.5.4. Lỗi D-3: kiểm tra sức khoẻ container dùng sai địa chỉ"),
    ("p", "Lỗi này được phát hiện khi đối chiếu tài liệu với trạng thái thực tế của hệ thống đang chạy. Tài liệu khẳng định cả ba container khỏe, trong khi container quản trị báo không khỏe với chu kỳ lỗi liên tục 22 lần, trong khi dịch vụ vẫn phục vụ yêu cầu bình thường từ máy chủ."),
    ("p", "Nguyên nhân là lệnh kiểm tra sức khoẻ truy cập tên máy chủ không xác định, tên này phân giải thành địa chỉ IPv6, trong khi máy chủ web chỉ lắng nghe trên IPv4. Do đó lệnh kiểm tra không bao giờ kết nối được tới dịch vụ dù dịch vụ hoạt động bình thường. Khắc phục bằng cách dùng địa chỉ IPv4 dạng vòng lặp quanh số bốn. Sau khắc phục, chu kỳ lỗi giảm từ 22 xuống 0 và container báo khỏe."),
    ("note", "Bài học: tín hiệu sức khoẻ container khác với tính khả dụng của ứng dụng. Một tín hiệu sai khiến hệ thống giám sát không phân biệt được dịch vụ khỏe với dịch vụ hỏng, và các phép kiểm thử đầu cuối đều đạt vì chúng kiểm tra từ máy chủ chứ không kiểm tra từ trong container."),

    ("h3", "2.5.5. Kiểm thử hồi quy"),
    ("p", "Cơ chế kiểm thử hồi quy có kiểm chứng được áp dụng cho cả năm lỗi. Với mỗi lỗi, bản sửa được tạm hoàn nguyên, bộ kiểm thử chạy và xác nhận thất bại, sau đó bản sửa được khôi phục và kiểm thử lại. Bảng 2.3 tổng hợp kết quả."),
    ("tbl", "Bảng 2.3. Tổng hợp mười mục lỗi và cách khắc phục",
     ["Mã", "Nhóm", "Mô tả", "Biểu hiện", "Nguyên nhân gốc", "Khắc phục",
      "Chứng minh"],
     [], [0.9, 1.9, 2.9, 2.7, 2.7, 2.5, 2.3]),

    ("h3", "2.5.6. Xác nhận sau khắc phục"),
    ("p", "Sau mỗi lần khắc phục, dự án chạy lại toàn bộ năm bộ kiểm thử chứ không chỉ bộ liên quan, nhằm chắc chắn rằng việc khắc phục không gây hồi quy ở nơi khác. Riêng bộ kiểm thử đầu cuối được chạy lại trên toàn bộ stack sau khi dựng lại từ trạng thái sạch, để xác nhận cả hạ tầng lẫn mã nguồn cùng hoạt động đúng."),

    ("h3", "2.5.7. Nhóm mục tài liệu và mục phát hiện khi kiểm chứng"),
    ("p", "Bốn mục thuộc nhóm tài liệu được xử lý khác với lỗi mã nguồn, và sự khác biệt này đáng nêu. Một lỗi mã nguồn được khắc phục bằng cách sửa mã rồi chứng minh bằng kiểm thử hồi quy. Còn với mục tài liệu, bản chất của vấn đề là mô tả không khớp hành vi, nên cách xử lý là sửa mô tả hoặc phân loại lại phạm vi, và cách kiểm chứng là đối chiếu tài liệu với hệ thống đang chạy chứ không phải kiểm thử. Bốn mục gồm: một giao diện chưa được tài liệu hoá, một cảnh báo tài khoản mặc định của Spring đã kiểm chứng không khai thác được, một mô tả tính năng di động bị nói quá, và một giao diện được tài liệu hoá nhưng thực tế ngoài phạm vi học thuật."),
    ("p", "Mục thứ chín được phát hiện trong đợt kiểm chứng cuối ngày 02/10/2026, sau khi cam kết nền tảng đã được xác lập: ứng dụng di động không gọi được tới backend trên Android 16 trở lên, vì nền tảng này chặn lưu lượng tới địa chỉ mạng riêng tư mà dự án chưa khai báo quyền. Điểm đáng chú ý về mục này là nó không thể lộ ra trong kiểm thử đơn vị, vì kiểm thử đơn vị không chạy trên thiết bị thật. Cách khắc phục là khai báo quyền truy cập mạng nội bộ và xin quyền lúc khởi chạy ứng dụng."),
    ("note", "Mức độ kiểm chứng của mục này cần nêu rõ để tránh nói quá: bản sửa đã được kiểm chứng bằng biên dịch thành công và bằng việc mười kiểm thử đơn vị vẫn đạt, nhưng dự án chưa chạy kiểm chứng trên thiết bị hoặc trình giả lập chạy Android 16. Vì vậy mục này được ghi là đã khắc phục ở mức mã nguồn và đã qua kiểm chứng biên dịch, chưa phải đã khắc phục và kiểm chứng đầy đủ trên thiết bị."),

    ("h2", "2.6. Kiểm thử và nghiệm thu"),
    ("p", "Kết quả kiểm thử dưới đây lấy từ lần kiểm chứng cuối thực hiện ngày 02/10/2026. Toàn bộ năm bộ kiểm thử được chạy lại từ đầu, không kế thừa kết quả từ các lần chạy trước. Đây là lần kiểm chứng thứ hai: lần đầu ngày 01/10/2026 cho kết quả 385 phép kiểm tra không lỗi, và lần thứ hai cho đúng kết quả đó. Việc hai lần chạy độc lập cho cùng một kết quả có ý nghĩa riêng, vì nó cho thấy kết quả không phụ thuộc vào trạng thái còn lại của môi trường từ lần chạy trước."),
    ("note", "Một chi tiết cần nêu về môi trường kiểm thử: lần chạy ngày 01/10/2026 phát hiện ra máy dùng mặc định Java 8, trong khi Spring Boot yêu cầu Java 17 trở lên, nên lệnh kiểm thử chỉ chạy được sau khi đặt biến môi trường trỏ tới JDK 21. Đây là đặc điểm của máy chạy, không phải lỗi dự án, nhưng cần ghi lại vì một người đánh giá khác chạy trên máy của họ sẽ gặp cùng khó khăn này nếu không đặt biến môi trường."),

    ("h3", "2.6.1. Kiểm thử backend"),
    ("p", "Backend có 98 kiểm thử đơn vị phân bố theo sáu nhóm nghiệp vụ và 15 kiểm thử tích hợp. Nhóm kiểm thử tích hợp chạy trên cơ sở dữ liệu thật thông qua Testcontainers, bao gồm kiểm thử áp dụng migration, dữ liệu khởi tạo, và tính đúng đắn của căn cứ phân tích AI. Tổng cộng 113 kiểm thu, không có lỗi hay lỗi biên dịch."),
    ("h3", "2.6.2. Kiểm thử ứng dụng di động"),
    ("p", "Ứng dụng di động có 10 kiểm thử đơn vị tập trung vào lớp định dạng và quy tắc nghiệp vụ hiển thị. Quy trình xây dựng bao gồm làm sạch, chạy kiểm thử và đóng gói, cho ra tệp cài đặt 20.318.592 byte. Nhóm kiểm thử này là nơi phát hiện lỗi D-2."),
    ("h3", "2.6.3. Kiểm thử bảng quản trị"),
    ("p", "Bảng quản trị được kiểm thử theo hai hướng. Hướng thứ nhất là kiểm tra kiểu dữ liệu trong lúc biên dịch, đảm bảo không có lỗi kiểu. Hướng thứ hai là bộ kiểm thử hợp đồng API gồm 102 khẳng định, so khớp lược đề phản hồi của backend với định nghĩa kiểu phía bảng quản trị. Quét phụ thuộc không phát hiện lỗ hổng nào."),
    ("h3", "2.6.4. Kiểm thử AI"),
    ("p", "Kiểm thử AI gồm 24 kiểm thử đơn vị kiểm tra hình thức câu trả lời cho sáu ý định phân tích, và 8 kiểm thử tích hợp kiểm chứng các giá trị số trả về khớp dữ liệu trong cơ sở dữ liệu. Bộ kiểm thử tích hợp được thêm sau khi phát hiện rằng bộ kiểm thử đơn vị không bắt được lỗi D-1. Nhóm kiểm thử này cũng xác nhận đường dẫn từ chối khi thiếu dữ liệu."),
    ("h3", "2.6.5. Kiểm thử đầu cuối"),
    ("p", "Bộ kiểm thử đầu cuối gồm 160 khẳng định, chạy trên hệ thống thực sự đang hoạt động trong container. Bộ kiểm thử bao gồm cả các ca kỳ vọng thất bại, chẳng hạn truy cập dữ liệu của người dùng khác phải bị từ chối, và tài khoản bị khoá phải mất quyền truy cập ngay cả khi token còn hiệu lực. Thiết kế có tính lặp lại: mỗi lần chạy sinh dữ liệu duy nhất nên bộ kiểm thử chạy được nhiều lần."),
    ("h3", "2.6.6. Kiểm thử triển khai"),
    ("p", "Kiểm thử triển khai gồm mười một phép kiểm soát trên môi trường container: sức khoẻ ba thành phần, số migration áp dụng thành công, số bảng tạo ra, kết nối backend với cơ sở dữ liệu, trạng thái điểm cuối sức khoẻ, đăng nhập quản trị qua container, trạng thái HTTP của bảng điều khiển, số dòng lỗi trong nhật ký backend, và chạy lại toàn bộ bộ kiểm thử đầu cuối trên bộ chính vừa dựng. Không có phép kiểm soát nào thất bại."),
    ("h3", "2.6.7. Tổng hợp kết quả kiểm thử"),
    ("p", "Bảng 2.4 tổng hợp kết quả năm bộ kiểm thử. Tổng số phép kiểm tra là 385 và tổng số lỗi là 0. Điểm cần lưu ý khi đọc bảng là số phép kiểm tra phân bố không đều: bộ kiểm thử đầu cuối chiếm phần lớn, vì nó là bộ chứng minh hành vi từ góc nhìn người dùng."),
    ("tbl", "Bảng 2.4. Tổng hợp kết quả kiểm thử cuối cùng",
     ["Bộ kiểm thử", "Lệnh thực thi", "Số kiểm tra", "Số lỗi", "Kết quả"],
     [], [3.5, 5.0, 2.2, 1.8, 2.5]),
    ("note", "Tổng 385 kiểm tra được cộng từ năm bộ không trùng lặp: 98 đơn vị backend, 15 tích hợp backend, 10 đơn vị di động, 102 hợp đồng quản trị, và 160 đầu cuối. Các tệp kết quả được lưu tại thư mục tests/results/ với dấu thời gian của lần chạy."),

    ("h3", "2.6.8. Đánh giá tiêu chí nghiệm thu"),
    ("p", "Bảy tiêu chí nghiệm thu tại bảng 1.11 được đánh giá dựa trên kết quả kiểm thử. Cả bảy tiêu chí đều đạt. Đối với tiêu chí triển khai, cần lưu ý rằng kết quả đạt được sau khi khắc phục lỗi D-3; nếu giữ nguyên lỗi, tiêu chí này sẽ không đạt dù toàn bộ kiểm thử chức năng vẫn đạt."),

    ("h3", "2.6.9. Kết quả nghiệm thu cuối cùng"),
    ("fig", "report/figures/07_nghiem_thu_dong_dau.png", "Hình 2.4. Chuỗi nghiệm thu và tuyên bố đóng dự án với hai kết luận tách biệt"),
    ("p", "Kết quả nghiệm thu cuối cùng được trình bày tại Hình 2.4. Sơ đồ tách bạch hai tuyên bố có chủ đích: trạng thái học thuật và mức độ sẵn sàng vận hành. Dự án đạt toàn bộ mười bảy tiêu chí nghiệm thu về mặt học thuật, đồng thời không tuyên bố sẵn sàng vận hành vì bốn khoảng trống vận hành vẫn còn mở. Sơ đồ cũng ghi rõ khoảng trống dữ liệu lịch sử bằng nét đứt, vì khoảng trống này tồn tại xuyên suốt quá trình và không thể đóng lại bằng kỹ thuật."),
]
