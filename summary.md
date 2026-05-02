# Tóm Tắt Dự Án: NextJs_SpringBoot

## Tổng Quan
Đây là ứng dụng full-stack kết hợp Spring Boot backend với Next.js frontend. Hệ thống cung cấp xác thực người dùng, tính năng chat thời gian thực, và giám sát telemetry xe với kiểm soát truy cập dựa trên vai trò.

---

## Backend (Spring Boot)

### Công Nghệ Sử Dụng
- **Framework**: Spring Boot 4.0.5
- **Phiên bản Java**: 21
- **Cơ sở dữ liệu**: SQLite với JPA/Hibernate
- **Công cụ build**: Maven
- **Thư viện chính**:
  - Spring Security (xác thực JWT, mã hóa mật khẩu BCrypt)
  - Spring WebSocket + STOMP (nhắn tin thời gian thực)
  - Spring GraphQL (truy vấn telemetry)
  - Lombok (tự động sinh code)
  - SQLite JDBC Driver + Hibernate Community Dialects

### Cấu Trúc Database
- **users**: Tài khoản người dùng với quyền hạn
- **permissions**: Định nghĩa vai trò (member, admin, guest)
- **refresh_tokens**: Quản lý refresh token JWT
- **behicle_telemetry_master**: Dữ liệu telemetry xe (55+ cột)

### Các Entity Chính

#### User
- Các trường: id, name, email, phone, password, permission (FK)
- Quan hệ: Many-to-one với Permission

#### Permission
- Vai trò: member, admin, guest
- Dùng cho kiểm soát truy cập

#### RefreshToken
- Quản lý refresh token JWT với thời hạn hết hạn
- Quan hệ one-to-many với User

#### VehicleTelemetry
- Ánh xạ tới bảng `behicle_telemetry_master`
- 55+ điểm dữ liệu chia thành 6 nhóm:
  1. **Nhận diện phần cứng**: deviceId, vinNumber, plateNumber, firmwareVersion, hardwareSerial
  2. **Vị trí thời gian thực**: latitude, longitude, altitude, speed, heading, dữ liệu GPS, thành phố/quốc gia
  3. **Động cơ & Nhiên liệu**: RPM, tải động cơ, nhiệt độ làm mát, mức nhiên liệu, tiêu thụ, áp suất dầu, pin, odometer
  4. **An toàn & Cảm biến**: trạng thái động cơ, chuyển động, vượt tốc, phanh, dây đai an toàn, áp suất lốp, cảm biến va chạm
  5. **Môi trường**: nhiệt độ, độ ẩm, chỉ số chất lượng không khí, mưa, đèn pha, trạng thái cửa
  6. **Chẩn đoán**: timestamps, mã lỗi, nút khẩn cấp, bảo trì, thông tin mạng

### Các Endpoint API

#### Xác thực (`/api/auth`)
- `POST /api/auth/login` - Đăng nhập với ID/mật khẩu
- `POST /api/auth/refresh` - Làm mới access token
- `POST /api/auth/logout` - Đăng xuất và thu hồi refresh token

#### Quản lý User (`/api/users`)
- `GET /api/users` - Liệt kê tất cả user (yêu cầu vai trò member/admin)
- `GET /api/users/{id}` - Lấy user theo ID
- `POST /api/users` - Tạo user mới
- `PUT /api/users/{id}` - Cập nhật user (chỉ name/email/phone)
- `DELETE /api/users/{id}` - Xóa user

#### GraphQL (`/graphql`)
- `allTelemetry` - Lấy tất cả bản ghi telemetry xe
- `telemetryById(deviceId)` - Lấy một bản ghi telemetry
- `telemetryPage(offset, limit)` - Truy vấn telemetry có phân trang (tối đa 200 mỗi trang)

#### WebSocket (`/ws`)
- STOMP qua SockJS cho giao tiếp thời gian thực
- Endpoints:
  - `/app/chat.sendPublic` - Broadcast tới tất cả user
  - `/app/chat.sendPrivate` - Nhắn tin trực tiếp tới user cụ thể
  - `/app/chat.join` - Thông báo sự hiện diện của user
- Subscriptions:
  - `/topic/public` - Tin nhắn chat công khai
  - `/user/queue/messages` - Tin nhắn riêng cho user đã xác thực

### Cấu Hình Bảo Mật
- Xác thực stateless dựa trên JWT
- Mã hóa mật khẩu BCrypt với tự động nâng cấp từ plaintext
- CORS được bật cho tất cả origin với credentials
- Kiểm soát truy cập dựa trên vai trò:
  - `/api/auth/**` - Công khai
  - `/ws/**` - Công khai (WebSocket handshake)
  - `/graphql`, `/graphiql/**` - Công khai (xác thực trong resolvers)
  - `/api/users/**` - Yêu cầu vai trò member hoặc admin
  - Các endpoint khác - Chỉ user đã xác thực

### Các Service Chính
- **AuthService**: Đăng nhập, refresh token, đăng xuất với quản lý session
- **UserService**: Thao tác CRUD cho users
- **RefreshTokenService**: Quản lý vòng đời token (tạo, xác thực, thu hồi)
- **JwtService**: Tạo và xác thực token JWT

---

## Frontend (Next.js)

### Công Nghệ Sử Dụng
- **Framework**: Next.js 16.2.2 (App Router)
- **Phiên bản React**: 19.2.4
- **Ngôn ngữ**: TypeScript
- **Styling**: TailwindCSS 4
- **Thời gian thực**: @stomp/stompjs + sockjs-client
- **Lấy dữ liệu**: GraphQL (qua fetch)

### Các Trang & Tính Năng

#### Trang chủ (`/`)
- Trang mặc định của Next.js
- Chỗ trống để tùy chỉnh sau này

#### Đăng nhập (`/login`)
- Form xác thực người dùng
- Nhận ID và mật khẩu user
- Lưu access token và thông tin user trong localStorage
- Chuyển hướng tới trang chat khi thành công

#### Chat (`/chat`)
- Ứng dụng chat thời gian thực sử dụng WebSocket
- **Tính năng**:
  - Phòng chat công khai (broadcast tới tất cả user)
  - Nhắn tin riêng trực tiếp giữa các user
  - Thông báo sự hiện diện của user (tham gia/rời đi)
  - Chỉ báo trạng thái kết nối
  - Lịch sử tin nhắn với timestamps
  - Tự động cuộn tới tin nhắn mới nhất
- **Bảo mật**: Yêu cầu vai trò member hoặc admin
- **UI**: Bố cục 2 cột với sidebar để chọn cuộc hội thoại

#### Telemetry (`/telemetry`)
- Trình xem dữ liệu telemetry xe
- **Tính năng**:
  - Lấy dữ liệu qua GraphQL
  - Infinite scroll với IntersectionObserver
  - Tải có phân trang (50 bản ghi mỗi trang)
  - Chế độ xem theo tab cho các nhóm dữ liệu (6 danh mục)
  - Bảng responsive với 55+ cột
  - Hiển thị badge cho các cờ boolean
  - Định dạng timestamp
  - Skeleton loading và xử lý lỗi
- **Bảo mật**: Yêu cầu vai trò member hoặc admin

#### Users (`/users`)
- Giao diện quản lý user
- Thao tác CRUD cho tài khoản user
- Kiểm soát truy cập dựa trên vai trò

#### Forbidden (`/forbidden`)
- Trang từ chối truy cập cho user không được ủy quyền

### Cấu Hình
- **API Base URL**: Được cấu hình trong `lib/config.ts`
- **Xác thực**: Token JWT được lưu trong localStorage
- **WebSocket**: SockJS fallback để tương thích trình duyệt

---

## Tóm Tắt Tính Năng Chính

1. **Hệ Thống Xác Thực**
   - Xác thực stateless dựa trên JWT
   - Rotation refresh token để tăng bảo mật
   - Tự động nâng cấp hash mật khẩu (plaintext → BCrypt)
   - Quản lý session với tự động thu hồi khi đăng nhập mới

2. **Chat Thời Gian Thực**
   - Giao thức STOMP qua WebSocket
   - Nhắn tin broadcast công khai
   - Nhắn tin riêng trực tiếp
   - Thông báo sự hiện diện của user
   - Giám sát trạng thái kết nối

3. **Giám Sát Telemetry Xe**
   - 55+ điểm dữ liệu mỗi xe
   - API GraphQL để truy vấn hiệu quả
   - Infinite scroll cho tập dữ liệu lớn
   - Trực quan hóa dữ liệu theo nhóm
   - Chỉ báo trạng thái thời gian thực

4. **Quản Lý User**
   - Kiểm soát truy cập dựa trên vai trò (member, admin, guest)
   - Thao tác CRUD cho tài khoản user
   - Tích hợp hệ thống quyền hạn

5. **Bảo Mật**
   - CORS được bật cho các request cross-origin
   - Mã hóa mật khẩu BCrypt
   - Xác thực token JWT
   - Bảo vệ endpoint dựa trên vai trò
   - Xác thực WebSocket

---

## Chạy Ứng Dụng

### Backend
```bash
cd Backend
./mvnw spring-boot:run
```
- Chạy trên port 8081
- Database SQLite được tạo trong `database/users.db`
- GraphQL Playground có sẵn tại `/graphiql`

### Frontend
```bash
cd Frontend/app
npm run dev
```
- Chạy trên port 3000 (mặc định)
- Yêu cầu backend đang chạy trên port 8081

---

## Khởi Tạo Database
- Database SQLite được tự động tạo khi chạy lần đầu
- Dữ liệu ban đầu được thêm qua `data.sql`
- Schema được tự động cập nhật qua Hibernate DDL

---

## Ghi Chú Kiến Trúc
- **Xác thực Stateless**: Không có session phía server
- **REST + GraphQL Hybrid**: REST cho auth/users, GraphQL cho telemetry
- **Giao tiếp Thời gian Thực**: WebSocket cho chat, không cần HTTP polling
- **Phân trang**: Phân trang dựa trên offset cho telemetry (tối đa 200 bản ghi mỗi request)
- **Rotation Token**: Refresh token được rotation mỗi lần làm mới để tăng bảo mật
