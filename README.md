# Electric Billing – Hệ thống tính tiền điện cho hộ cá nhân

Bài tập lớn môn *Đánh giá và kiểm định chất lượng phần mềm* – đề tài 1 (nhóm chức năng 1.1 và 1.2).
Đặc tả yêu cầu: [docs/SRS.md](docs/SRS.md). Đặc tả use-case một trang: [docs/usecase.html](docs/usecase.html) (biểu đồ UML: [docs/usecase.puml](docs/usecase.puml)).

## Công nghệ

Java 21 · Spring Boot 4.1.1 (MVC, Data JPA, Security, Mail) · Thymeleaf + Bootstrap 5 · MySQL 8 (H2 cho profile `h2`) · Maven Wrapper.

## Chạy ứng dụng

Yêu cầu: JDK 21 và MySQL 8 đang chạy, đã tạo sẵn database `electricbilling`. Các bảng được Hibernate tự tạo từ entity (`ddl-auto: update`), dữ liệu mặc định (admin, biểu giá, tham số) được tạo khi khởi động lần đầu.

```powershell
.\mvnw.cmd spring-boot:run
```

Mở http://localhost:8080 (nếu cổng bận: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"`).

Chạy nhanh không cần MySQL (dữ liệu mất khi tắt):

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

### Tài khoản mặc định

| Vai trò | Tên đăng nhập | Mật khẩu |
|---|---|---|
| Quản trị viên | `admin` | `Admin@123` |

Hộ dùng điện tự đăng ký tại `/register`. Hãy đổi mật khẩu admin khi triển khai thật (biến `ADMIN_PASSWORD` chỉ có tác dụng khi chưa có tài khoản ADMIN nào).

### Biến môi trường (đều có giá trị mặc định)

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/electricbilling?...` | Chuỗi kết nối MySQL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `123456` | Tài khoản MySQL |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | Máy chủ SMTP |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | (trống) | Tài khoản SMTP nếu cần đăng nhập |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `Admin@123` | ADMIN tạo lần đầu |

Nếu chưa có máy chủ SMTP, email thất bại sẽ được ghi vào *Thông báo* với trạng thái "Thất bại" (nghiệp vụ chính không bị ảnh hưởng). Để thử gửi email thật khi phát triển, dùng một SMTP giả như MailHog hoặc smtp4dev lắng nghe cổng 1025.

## Cấu trúc

```
docs/SRS.md                         Đặc tả yêu cầu (YC 1)
docs/usecase.html                   Đặc tả use-case một trang (biểu đồ + 13 use-case)
docs/usecase.puml                   Biểu đồ use-case bản UML (PlantUML)
src/main/java/.../domain            Thực thể JPA, enum
src/main/java/.../repository        Spring Data JPA
src/main/java/.../service           Nghiệp vụ; BillingCalculator = logic tính tiền thuần
src/main/java/.../web               Controller (Thymeleaf)
src/main/java/.../scheduler         Nhắc nợ hằng ngày 08:00
src/main/resources/templates        Giao diện
```

Các phần tử giao diện quan trọng đều có `id` ổn định (ví dụ `username`, `password`, `btn-login`, `bill-total`) để dùng cho kiểm thử tự động bằng Selenium.
