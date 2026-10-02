# -*- coding: utf-8 -*-
"""Chuong 3 va Chuong 4."""

CHUONG_3 = [
    ("h1", "CHƯƠNG 3. KẾT THÚC DỰ ÁN"),

    ("h2", "3.1. Đánh giá mức độ hoàn thành"),

    ("h3", "3.1.1. Mức độ hoàn thành yêu cầu"),
    ("p", "Đánh giá mức độ hoàn thành dựa trên ma trận truy vết 164 dòng, trong đó mỗi dòng mang đúng một trạng thái. Nguyên tắc áp dụng xuyên suốt là không nâng trạng thái lên đã xác minh nếu chưa có kiểm thử đã chạy hoặc kết quả lệnh trực tiếp. Kết quả tổng hợp nêu tại bảng 1.17, tỷ lệ đã xác minh là 90,2%."),
    ("note", "Tỷ lệ 90,2% không phải chỉ số chất lượng. Trong số 164 dòng, 148 dòng có thể chứng minh bằng bằng chứng chạy được, 8 dòng đã triển khai nhưng chưa phủ kiểm thử đầy đủ, 7 dòng là thực hành thiếu, và 1 dòng cố ý nằm ngoài phạm vi. Tỷ lệ chỉ có ý nghĩa khi đi kèm phân tích thành phần."),

    ("h3", "3.1.2. Các yêu cầu hoàn thành"),
    ("p", "148 dòng đã xác minh bao gồm toàn bộ chức năng nghiệp vụ cốt lõi, toàn bộ mô-đun phân tích AI, toàn bộ chức năng bảng quản trị, và toàn bộ yêu cầu về tính tái lập. Các nhóm chức năng và bằng chứng thực thi tương ứng được nêu tại bảng 1.3 và bảng 2.1. Điểm cần lưu ý là mức độ xác minh không đồng đều theo chiều sâu: các chức năng nghiệp vụ có kiểm thử đầu cuối bao phủ, còn giao diện người dùng chỉ có kiểm thử hợp đồng ở tầng dữ liệu chứ không có kiểm thử giao diện."),

    ("h3", "3.1.3. Các yêu cầu hoàn thành một phần"),
    ("p", "Tám dòng hoàn thành một phần đều thuộc cùng một dạng vấn đề: chức năng đã triển khai và hoạt động đúng, nhưng chưa có kiểm thử tự động bao phủ đầy đủ. Dạng điển hình là các quy tắc hiển thị trên ứng dụng di động, vốn được kiểm thử ở tầng định dạng dữ liệu nhưng chưa kiểm thử ở tầng giao diện. Dự án ghi nhận tám dòng này thay vì tuyên bố hoàn thành, vì tuyên bố hoàn thành cho chức năng chưa có bằng chứng kiểm thử sẽ phá vỡ nguyên tắc áp dụng xuyên suốt báo cáo."),

    ("h3", "3.1.4. Các yêu cầu còn khoảng trống"),
    ("p", "Bảy khoảng trống được phân loại thành hai nhóm. Nhóm thứ nhất gồm năm thực hành thiếu: đường dẫn nhà cung cấp AI bên ngoài chưa kiểm chứng, chưa có tích hợp liên tục, chưa tự động hóa kiểm thử giao diện, chưa kiểm thử tải, và chưa quét hình ảnh container. Nhóm thứ hai gồm hai khoảng trống vận hành: chưa giới hạn tần suất và chưa chứng thực bảo mật đường truyền."),
    ("p", "Không khoảng trống nào trong bảy này là hành vi nghiệp vụ chưa triển khai. Tất cả đều là thực hành vận hành hoặc kỹ thuật chưa thực hiện, và phần lớn thuộc phạm vi quyết định hạ tầng của chủ dự án chứ không phải phạm vi mã nguồn. Sự phân loại này có ý nghĩa thực tế: nó cho biết khoảng trống có thể đóng bằng cách nào, thay vì mọi khoảng trống đều được xử lý như nhau."),

    ("h3", "3.1.5. Các nội dung ngoài phạm vi"),
    ("p", "Một dòng được ghi nhận là ngoài phạm vi có chủ đích, đó là giao diện gửi phản hồi trên ứng dụng di động. Lý do ghi kèm: người dùng gửi phản hồi qua giao diện lập trình và tiếp nhận trong bảng quản trị, còn giao diện di động không thuộc phạm vi học thuật của dự án. Sự phân biệt giữa ngoài phạm vi và khoảng trống là quan trọng vì ngoài phạm vi là quyết định phạm vi có lý do, còn khoảng trống là thiếu sót."),

    ("h2", "3.2. Bàn giao sản phẩm"),
    ("p", "Hình 3.1 ghi lại kết quả đăng nhập quản trị và trang tài liệu giao diện lập trình tự sinh của hệ thống đang chạy. Cả hai đều là bằng chứng cho điều kiện tiếp nhận của bảng quản trị: xác thực hoạt động và giao diện lập trình được mô tả đầy đủ."),
    ("fig", "report/figures/e3_api_login.png", "Hình 3.1. Xác thực đăng nhập quản trị và tài liệu API tự sinh của hệ thống đang chạy"),
    ("p", "Sản phẩm bàn giao gồm tám nhóm, nêu chi tiết tại bảng 1.6. Mỗi nhóm đều có cách xác minh độc lập, cho phép người nhận bàn giao kiểm tra độc lập thay vì tin theo tuyên bố. Bảng 3.1 tóm tắt các nhóm sản phẩm kèm điều kiện tiếp nhận."),

    ("h3", "3.2.1. Mã nguồn"),
    ("p", "Mã nguồn gồm 78 tệp Java, 22 tệp Kotlin và 13 tệp TypeScript, tổng cộng 232 tệp trong cam kết nền tảng. Điều kiện tiếp nhận là bản sao mới có thể dựng toàn bộ stack bằng một lệnh và chạy được năm bộ kiểm thử. Điều kiện này đã được kiểm chứng sau khi khắc phục lỗi D-4 về tệp bọc Gradle."),
    ("h3", "3.2.2. Ứng dụng di động"),
    ("p", "Ứng dụng di động được bàn giao dưới dạng mã nguồn và tệp cài đặt đã biên dịch. Điều kiện tiếp nhận là mã nguồn biên dịch được bằng hệ thống xây dựng của dự án mà không cần thao tác sửa tay."),
    ("h3", "3.2.3. Bảng quản trị"),
    ("p", "Bảng quản trị được bàn giao dưới dạng mã nguồn và cấu hình máy chủ web. Điều kiện tiếp nhận là đóng gói không có lỗi kiểu và bộ kiểm thử hợp đồng đạt 102/102."),
    ("h3", "3.2.4. Backend"),
    ("p", "Backend được bàn giao dưới dạng mã nguồn và tệp thực thi. Điều kiện tiếp nhận là 113 kiểm thử đạt và các tệp migration áp dụng được trên cơ sở dữ liệu trống."),
    ("h3", "3.2.5. Cơ sở dữ liệu"),
    ("p", "Cơ sở dữ liệu được bàn giao dưới dạng chín tệp migration có đánh số phiên bản. Điều kiện tiếp nhận là dựng lại trên cơ sở dữ liệu trống tạo ra đủ 11 bảng với lịch sử migration hợp lệ."),
    ("h3", "3.2.6. Mô-đun AI"),
    ("p", "Mô-đun AI được bàn giao dưới dạng mã nguồn kèm cấu hình chế độ. Điều kiện tiếp nhận là sáu ý định phân tích có kiểm thử và đường dẫn từ chối hoạt động đúng. Đường dẫn nhà cung cấp bên ngoài được bàn giao kèm ghi chú là chưa kiểm chứng."),
    ("h3", "3.2.7. Cấu hình triển khai"),
    ("p", "Cấu hình triển khai gồm ba tệp mô tả hình ảnh và mạng. Điều kiện tiếp nhận là dựng lại từ trạng thái sạch cho ba container ở trạng thái khỏe và bộ kiểm thử đầu cuối đạt trên bộ chính vừa dựng."),
    ("h3", "3.2.8. Tài liệu"),
    ("p", "Tài liệu gồm 28 tệp Markdown chia thành tài liệu kỹ thuật và tài liệu quản lý dự án. Điều kiện tiếp nhận là mọi khẳng định về kết quả đều truy được về bằng chứng, điều kiện này đã được kiểm toán và dẫn tới việc sửa ba sai lệch."),

    ("h2", "3.3. Cấu hình nền tảng cuối cùng"),
    ("p", "Cấu hình nền tảng cuối cùng được xác định tại cam kết gắn thẻ phát hành. Bảng 1.18 trình bày các thành phần. Phần này tập trung vào cách thiết lập và giới hạn của cấu hình nền tảng."),

    ("h3", "3.3.1. Nền tảng mã nguồn"),
    ("p", "Nền tảng mã nguồn được thiết lập sau giai đoạn phát triển chính, và đây là sự kiện cần được trình bày trung thực vì nó ảnh hưởng lớn đến giá trị của bằng chứng. Kho mã nguồn hiện có ba cam kết: một cam kết mẫu ban đầu của GitHub, một cam kết nền tảng đã kiểm chứng, và một cam kết cập nhật tài liệu."),
    ("p", "Nền tảng mã nguồn cung cấp ba năng lực có giá trị: khả năng đối chiếu thay đổi, khả năng phân tách công việc, và khả năng truy vết cấu hình từ điểm nền tảng trở đi. Nền tảng không cung cấp năng lực chứng minh quá trình phát triển, vì không có bản ghi nào trong giai đoạn đó."),
    ("p", "Thao tác thiết lập được thực hiện mà không sửa lịch sử từ xa. Cụ thể, lệnh đẩy là đẩy tiến nhanh, không ép đẩy, và cam kết mẫu ban đầu được giữ nguyên. Kết quả là lịch sử từ xa và lịch sử cục bộ không bị phân kỳ. Dự án cũng phát hiện và loại trừ một bản sao kho lồng thừa phát sinh trong quá trình đối chiếu, trước khi nó có thể bị ghi nhận như mô-đun con."),

    ("h3", "3.3.2. Nền tảng cơ sở dữ liệu"),
    ("p", "Nền tảng cơ sở dữ liệu gồm 9 tệp migration và lịch sử áp dụng ghi lại trong bảng lịch sử. Tính bất biến của tệp migration đã áp dụng được bảo đảm bằng cơ chế kiểm tra mã băm. Nền tảng này đủ để dựng lại cơ sở dữ liệu từ trạng thái trống, đã được kiểm chứng nhiều lần trong quá trình phát triển và kiểm toán."),

    ("h3", "3.3.3. Nền tảng triển khai"),
    ("p", "Nền tảng triển khai gồm ba hình ảnh đã đánh số phiên bản và tệp mô tả dịch vụ. Nền tảng này đã được kiểm chứng bằng cách hạ toàn bộ rồi dựng lại, cho thấy khả năng tái lập độc lập với trạng thái trước đó. Đây là bằng chứng quan trọng hơn so với việc chỉ xác nhận container đang chạy, vì nó chứng minh được khả năng tái tạo."),

    ("h3", "3.3.4. Nền tảng tài liệu"),
    ("p", "Nền tảng tài liệu gồm 28 tệp, trong đó 11 tệp ở cấp tài liệu kỹ thuật và 17 tệp ở cấp quản lý dự án. Nền tảng này đã qua một vòng kiểm toán nội dung dẫn tới việc sửa tám sai lệch và bổ sung hai tài liệu chuyên biệt về rà soát nhật ký và quét bí mật."),

    ("h3", "3.3.5. Thẻ phát hành và phát hành"),
    ("p", "Dự án có một thẻ phát hành duy nhất, đặt tên là bản học thuật cuối cùng đã kiểm chứng. Sự tồn tại của chỉ một bản phát hành là hệ quả trực tiếp của việc thiếu lịch sử phát triển: không có các bản phát hành trung gian để đối chiếu tiến độ hoặc chất lượng qua từng mốc. Thẻ phát hành cho phép dựng lại chính xác trạng thái đã kiểm chứng, đây là giá trị chính của nó."),

    ("h2", "3.4. Các vấn đề còn tồn tại"),
    ("p", "Bốn khoảng trống vận hành còn mở được trình bày riêng biệt trong chương này. Điểm cần nhấn mạnh: đây là khoảng trống vận hành, không phải lỗi mã nguồn. Phân biệt này quyết định cách tiếp cận: lỗi mã nguồn được khắc phục ngay trong phạm vi dự án, còn khoảng trống vận hành đòi hỏi quyết định hạ tầng và ngân sách mà dự án chưa đưa ra."),
    ("tbl", "Bảng 3.1. Bốn khoảng trống vận hành còn mở",
     ["Mã", "Khoảng trống", "Hệ quả cụ thể", "Điều kiện đóng"],
     [
        ["L-01", "Không có tích hợp liên tục", "Một thay đổi sai có thể được ghi mà không bị phát hiện; xác minh phụ thuộc kỷ luật cá nhân", "Thêm đường dẫn chạy năm bộ kiểm thử hiện có"],
        ["L-02", "Không chứng thực bảo mật đường truyền", "Dịch vụ chỉ dùng giao thức không mã hóa; không thể công khai", "Quyết định đích triển khai rồi thêm chứng thư đầu cuốe"],
        ["L-03", "Không có chiến lược sao lưu", "Mất dữ liệu là không khôi phục được", "Thêm kịch bản trích xuất và hướng dẫn khôi phục"],
        ["L-04", "Không giới hạn tần suất", "Nguy cơ dò mật khẩu và lạm dụng giao diện phân tích", "Thêm kiểm soát tần suất ở tầng ứng dụng"],
     ], [1.2, 3.6, 5.2, 5.0]),
    ("p", "Bốn khoảng trống này không nằm trong tiêu chí nghiệm thu học thuật của dự án, và dự án không tuyên bố sẵn sàng cho vận hành. Điểm cần nhấn mạnh là ba trong bốn khoảng trống có thể đóng bằng công sức hạn chế, vì năm bộ kiểm thử đã tồn tại và chỉ cần cấu hình để chạy tự động. Đề xuất cụ thể được trình bày tại mục 3.6."),

    ("h2", "3.5. Đánh giá sau dự án"),

    ("h3", "3.5.1. Kết quả đạt được"),
    ("p", "Dự án đạt được bốn kết quả chính. Thứ nhất, toàn bộ chức năng trong phạm vi được triển khai và kiểm chứng bằng 385 phép kiểm tra tự động không có lỗi. Thứ hai, hệ thống phân tích AI hoạt động theo nguyên tắc có ràng buộc, được chứng minh bằng kiểm thử giá trị chứ không chỉ bằng kiểm thử hình thức. Thứ ba, hệ thống tái lập được, đã kiểm chứng bằng cách dựng lại từ trạng thái sạch. Thứ tư, mọi khẳng định trong báo cáo này đều truy được về bằng chứng thực thi."),

    ("h3", "3.5.2. Khó khăn thực tế"),
    ("p", "Bốn khó khăn thực sự gặp phải đáng ghi nhận. Khó khăn thứ nhất là thiếu dữ liệu lịch sử, đã phân tích tại mục 1.4.5. Khó khăn thứ hai là phát hiện rằng kiểm thử hình thức không đủ: bộ kiểm thử đơn vị của mô-đun AI kiểm tra hình thức câu trả lời nên không bắt được lỗi nghiêm trọng nhất của dự án. Khó khăn thứ ba là thực tế rằng một bản làm việc cục bộ có thể che giấu lỗi nền tảng, minh hoạ bằng trường hợp tệp bọc Gradle bị loại khỏi kho mã nguồn trong khi mọi bản dựng cục bộ vẫn đạt. Khó khăn thứ tư là sự khác biệt giữa trạng thái container và tính khả dụng thực tế của dịch vụ, khiến tín hiệu giám sát báo sai suốt nhiều giờ mà không ai phát hiện."),

    ("h3", "3.5.3. Các vấn đề đã được xử lý"),
    ("p", "Mười mục lỗi và cấu hình đã được xử lý, mỗi mục kèm bằng chứng xử lý riêng: năm mục thuộc nhóm mã nguồn và cấu hình, bốn mục thuộc nhóm tài liệu, và một mục phát hiện trong đợt kiểm chứng cuối. Bảng 2.3 liệt kê chi tiết. Điểm đáng chú ý về cách xử lý là dự án không chỉ khắc phục mà còn chứng minh hiệu quả của việc khắc phục bằng kiểm thử hồi quy có kiểm chứng. Đối với lỗi tệp bọc Gradle, việc khắc phục được xác minh bằng cách kiểm tra điều kiện loại trừ, không phải bằng kiểm thử vì không có kiểm thử nào bắt được lỗi này."),

    ("h3", "3.5.4. Bài học rút ra"),
    ("p", "Bài học quan trọng nhất liên quan đến việc dữ liệu lịch sử không tồn tại. Bài học không phải là quản lý phiên bản là tốt, mà là bằng chứng không được ghi lại tại thời điểm nó tồn tại thì sẽ không bao giờ khôi phục được. Dự án có thể chứng minh chất lượng sản phẩm bằng 385 phép kiểm tra, nhưng không thể chứng minh được dự án đã được quản lý tốt về tiến độ và nỗ lực. Chi phí của việc ghi nhận dữ liệu lịch sử gần như bằng không nếu thực hiện sớm, nhưng không thể trả giá sau này."),
    ("p", "Bài học thứ hai: khẳng định chưa được thực thi chưa phải là bằng chứng. Ba sai lệch tài liệu được phát hiện trong dự án đều có đặc điểm chung là chúng dựa trên giả định thay vì hành vi quan sát được. Cả ba đều lộ ra khi đối chiếu tài liệu với hệ thống đang chạy, không phải khi đọc lại tài liệu."),
    ("p", "Bài học thứ ba: một bản làm việc cục bộ hoạt động bình thường có thể che giấu lỗi khiến người khác không thể dùng được. Tệp bọc Gradle bị loại khỏi kho mã nguồn trong suốt dự án, mọi bản dựng cục bộ đều đạt, và lỗi chỉ lộ ra khi kiểm tra điều kiện loại trừ trước khi ghi. Khả năng tái lập phải được kiểm chứng từ bản sao sạch, không phải từ thư mục đang làm việc."),
    ("p", "Bài học thứ tư: kiểm thử hồi quy chỉ có giá trị nếu đã được quan sát thất bại. Một kiểm thử viết sau khi khắc phục mà chưa từng chạy trên lỗi gốc thì chưa chứng minh được là kiểm thử. Dự án áp dụng tạm hoàn nguyên bản sửa để xác nhận kiểm thử thất bại, và bước này đã phát hiện ra rằng lỗi nghiêm trọng nhất chưa hề có bảo vệ kiểm thử."),
    ("p", "Bài học thứ năm: tín hiệu sức khoẻ và tính khả dụng là hai khái niệm khác nhau. Container quản trị phục vụ mọi yêu cầu từ máy chủ nhưng vẫn báo không khỏe suốt nhiều giờ. Nếu chỉ kiểm tra từ máy chủ, toàn bộ phép kiểm thử đều đạt và hệ thống giám sát vẫn báo sai. Kiểm chứng từ bên trong đơn vị được triển khai là cần thiết để kiểm tra tín hiệu giám sát."),

    ("h3", "3.5.5. Kinh nghiệm về quản lý dự án"),
    ("p", "Kinh nghiệm thu được về phương diện quản lý dự án tập trung vào quan hệ giữa bằng chứng và quyết định. Dự án chứng minh rằng bằng chứng thực thi làm thay đổi bản chất của quyết định quản lý. Khi mọi tuyên bố đều gắn với lệnh đã chạy, cuộc họp đánh giá trở thành việc rà soát bằng chứng thay vì tranh luận về ý kiến. Ngược lại, ba sai lệch tài liệu đều bắt nguồn từ việc ra quyết định dựa trên giả định."),
    ("p", "Kinh nghiệm thứ hai liên quan đến ranh giới giữa phạm vi học thuật và phạm vi sản phẩm. Dự án dành nhiều công sức xác định rõ điều gì ngoài phạm vi và vì sao, thay vì âm thầm bỏ qua. Nhờ vậy, tám sai lệch tài liệu được phân loại thành lỗi, cập nhật, hoặc phân loại ngoài phạm vi, và mỗi cách xử lý đều hợp lý. Một danh sách ngoài phạm vi rõ ràng có giá trị hơn một danh sách rỗng, vì nó thể hiện việc cân nhắc có chủ đích."),

    ("h2", "3.6. Đề xuất cải tiến"),
    ("p", "Các đề xuất dưới đây là đề xuất, không phải tuyên bố đã triển khai. Thứ tự được sắp xếp theo tỷ lệ giá trị trên công sức."),

    ("h3", "3.6.1. Cải tiến tích hợp liên tục"),
    ("p", "Năm bộ kiểm thử đã tồn tại và hoạt động, nên việc thêm tích hợp liên tục là công việc cấu hình chứ không phải phát triển. Đề xuất là tạo đường dẫn chạy tuần tự năm bộ kiểm thử theo đúng thứ tự đã xác minh trong báo cáo này. Đây là đề xuất có tỷ lệ giá trị trên công sức cao nhất trong toàn bộ danh sách, vì nó đóng được khoảng trống vận hành L-01 mà không cần mã nguồn mới."),

    ("h3", "3.6.2. Cải tiến bảo mật vận hành"),
    ("p", "Hai đề xuất được nêu. Một là bổ sung giới hạn tần suất trên đăng nhập và giao diện phân tích, đóng khoảng trống L-04 và giảm rủi ro dò mật khẩu. Hai là chuyển token lưu trên thiết bị di động sang lưu trữ được bảo vệ, cần thiết trước khi phát hành sản phẩm thực. Ngoài ra, dự án khuyến nghị thay thế mật khẩu quản trị đang dùng từ dữ liệu môi trường, vì mật khẩu này đã được công bố trong tài liệu dạng bản dùng thử. Hai đề xuất đầu lần lượt thuộc nhóm rủi ro nhận diện và xác thực hỏng và nhóm lưu trữ dữ liệu không an toàn trong danh mục rủi ro bảo mật của OWASP [6], nên cách xử lý được đề xuất dựa trên phân loại rủi ro có sẵn thay vì đặt ra từ suy đoán."),

    ("h3", "3.6.3. Sao lưu và khôi phục"),
    ("p", "Thêm kịch bản trích xuất cơ sở dữ liệu kèm hướng dẫn khôi phục đã kiểm thử là công việc có giới hạn rõ ràng. Khi tiến hành, cần kiểm tra khôi phục thực tế chứ không chỉ trích xuất, vì sao lưu chưa từng được khôi phục thì chưa phải sao lưu."),

    ("h3", "3.6.4. Giới hạn tần suất"),
    ("p", "Đề xuất này được nêu riêng vì tác động của nó lớn hơn mức nhỏ: không giới hạn tần suất trên đăng nhập làm suy yếu biện pháp kiểm soát truy cập đã được xây dựng, vì biện pháp này chỉ hiệu quả nếu số lần thử bị giới hạn. Cần áp dụng giới hạn theo địa chỉ nguồn và có cơ chế tạm khóa khi vượt ngưỡng."),

    ("h3", "3.6.5. Giám sát và khả năng quan sát"),
    ("p", "Đề xuất gồm ba phần: tổng hợp nhật ký ra nơi lưu trữ tập trung, thu thập chỉ số hiệu năng, và cảnh báo khi tín hiệu sức khoẻ chuyển trạng thái. Phần thứ ba có thêm giá trị đặc biệt sau lỗi D-03, vì dự án đã trải nghiệm trường hợp tín hiệu sai mà không ai phát hiện. Ngoài ra, dự án khuyến nghị bổ sung kiểm thử tự động giao diện và kiểm thử tải, hiện nằm ngoài phạm vi nhưng nên cân nhắc khi hệ thống tiến tới vận hành thực."),

    ("h2", "3.7. Tuyên bố đóng dự án"),
    ("p", "Dự án phân biệt rõ hai tuyên bố có phạm vi khác nhau, và sự phân biệt này là nội dung quan trọng nhất của mục này."),

    ("h3", "3.7.1. Trạng thái học thuật"),
    ("p", "Về mặt học thuật, dự án được tuyên bố là hoàn thành. Cơ sở của tuyên bố là bảy tiêu chí hoàn thành tại bảng 1.11 đều đạt với bằng chứng thực thi, 385 phép kiểm tra tự động không có lỗi, hệ thống triển khai lại được từ trạng thái sạch, và toàn bộ chức năng trong phạm vi đã có kiểm thử hoặc lệnh xác minh trực tiếp. Ở cấp yêu cầu, 148 trong 164 dòng của ma trận truy vết đã xác minh bằng bằng chứng chạy được; tám dòng hoàn thành một phần, bảy dòng là khoảng trống và một dòng nằm ngoài phạm vi học thuật."),

    ("h3", "3.7.2. Mức độ sẵn sàng vận hành"),
    ("p", "Về mặt vận hành, dự án không tuyên bố sẵn sàng cho vận hành. Bốn khoảng trống tại bảng 3.1 vẫn còn mở, gồm thiếu tích hợp liên tục, thiếu chứng thực bảo mật đường truyền, thiếu chiến lược sao lưu, và thiếu giới hạn tần suất. Bốn khoảng trống này cần được xem xét và xử lý phù hợp trước khi triển khai trong môi trường vận hành thực tế. Trong bốn khoảng trống, thiếu chứng thực bảo mật đường truyền và thiếu giới hạn tần suất là hai rủi ro tác động trực tiếp đến người dùng, vì dịch vụ hiện truyền dữ liệu không mã hóa và không chặn được các lần thử đăng nhập lặp lại. Báo cáo không đặt ra ngưỡng định lượng về số khoảng trống tối thiểu phải đóng, vì sổ rủi ro không xác lập ngưỡng như vậy và mọi ngưỡng như vậy sẽ là ước lượng không có căn cứ."),

    ("h3", "3.7.3. Điều kiện ghi nhận thêm"),
    ("p", "Có hai điều kiện cần được nêu cùng với tuyên bố. Điều kiện thứ nhất: dữ liệu lịch sử phát triển không tồn tại, nên dự án không thể chứng minh về tiến độ, nỗ lực hay quy trình. Điều kiện thứ hai: dự án chưa có kiểm thử xâm nhập, kiểm thử tải và quét mã độc hình thức, nên mức độ an toàn thông tin được đánh giá dựa trên rà soát mã nhai và nhật ký chứ không phải kiểm thử chuyên sâu. Có những bộ yêu cầu kiểm thử bảo mật được công bố để đối chiếu, chẳng hạn tiêu chuẩn xác minh bảo mật ứng dụng của OWASP với các mã yêu cầu có phiên bản [7], nhưng dự án chưa thực hiện đối chiếu theo bộ yêu cầu đó, nên không được tuyên bố tuân thủ bộ tiêu chuẩn này."),
]

CHUONG_4 = [
    ("h1", "CHƯƠNG 4. KẾT LUẬN"),
    ("p", "Báo cáo trình bày quá trình quản lý một dự án phần mềm, trong đó sản phẩm phần mềm là đối tượng được quản lý và kết quả được chứng minh bằng bằng chứng thực tế."),

    ("h2", "4.1. Kết quả đạt được"),
    ("p", "Toàn bộ chức năng trong phạm vi đã được triển khai và kiểm chứng. Một trăm bốn mươi tám dòng yêu cầu trong tổng số 164 dòng được xác minh bằng kiểm thử đã chạy hoặc lệnh trực tiếp, tương đương 90,2%. Năm bộ kiểm thử tự động chạy tổng cộng 385 phép kiểm tra không có lỗi. Hệ thống triển khai lại được từ trạng thái sạch bằng một lệnh, cho ba container ở trạng thái khỏe. Mô-đun phân tích AI hoạt động theo nguyên tắc có ràng buộc, được chứng minh bằng kiểm thử kiểm chứng giá trị chứ không chỉ hình thức."),
    ("p", "Về mặt quản lý, dự án đã xác lập đầy đủ sổ rủi ro gồm 18 mục, ma trận truy vết yêu cầu, bộ tiêu chí nghiệm thu, và bộ tài liệu gồm 28 tệp. Mười mục lỗi và cấu hình đã được phát hiện và xử lý, mỗi mục đều có bằng chứng xử lý; tám sai lệch tài liệu đã được sửa hoặc phân loại."),

    ("h2", "4.2. Hạn chế"),
    ("p", "Hạn chế lớn nhất là thiếu dữ liệu lịch sử phát triển. Quản lý phiên bản chỉ được thiết lập sau giai đoạn phát triển chính, nên dự án không thể chứng minh về tiến độ, nỗ lực, quy mô nhóm, hay quy trình đã sử dụng. Khoảng trống này không thể khôi phục và là bài học quan trọng nhất của dự án về quản lý dự án."),
    ("p", "Bảy dòng yêu cầu còn khoảng trống và tám dòng hoàn thành một phần, tất cả đều thuộc nhóm thực hành chứ không phải chức năng nghiệp vụ. Bốn khoảng trống vận hành vẫn còn mở gồm thiếu tích hợp liên tục, thiếu chứng thực bảo mật đường truyền, thiếu sao lưu và thiếu giới hạn tần suất. Dự án chưa có kiểm thử xâm nhập, kiểm thử tải, kiểm thử giao diện tự động và quét mã độc hình thức."),

    ("h2", "4.3. Hướng phát triển"),
    ("p", "Ưu tiên đầu tiên là bổ sung tích hợp liên tục, vì năm bộ kiểm thử đã sẵn sàng và công việc chỉ là cấu hình. Tiếp theo là thêm giới hạn tần suất và giải pháp sao lưu, hai việc có giới hạn rõ ràng. Quyết định đích triển khai cần được đưa ra trước khi bổ sung chứng thực bảo mật đường truyền, vì cách thực hiện phụ thuộc vào đích này. Cuối cùng, dự án cần bổ sung kiểm thử giao diện tự động và kiểm thử tải khi hệ thống tiến gần vận hành thực."),
    ("p", "Với mọi dự án phần mềm tiếp theo, bài học đầu tiên cần áp dụng là khởi tạo quản lý phiên bản từ ngày đầu tiên. Chi phí gần như bằng không, nhưng thiếu nó thì không thể khôi phục được bất cứ điều gì về sau. Bài học thứ hai là chỉ ghi vào tài liệu những gì đã được thực thi kiểm chứng, vì ba sai lệch tài liệu của dự án đều bắt nguồn từ việc ghi những điều chưa kiểm chứng."),
]
