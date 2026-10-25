# 🍳 Recipe & Nutrition App

Ứng dụng quản lý công thức nấu ăn và giá trị dinh dưỡng, xây dựng theo kiến trúc Client - Server kết nối qua RESTful API.

---

## 📁 Cấu trúc dự án

```text
mobile_recipes/
├── backend/    # REST API viết bằng Spring Boot 3 & Java 17
├── frontend/   # Ứng dụng Android Native (Java, Gradle)
└── database/   # Script khởi tạo CSDL & dữ liệu mẫu MySQL
```

---

## 🛠 Công nghệ sử dụng

- **Frontend**: Android Native (Java 11+), Volley REST Client, Material Components, ViewPager2.
- **Backend**: Spring Boot 3.3.5 (Java 17), Spring Data JPA, Spring Security (BCrypt hashing).
- **Database**: MySQL 8.x.

---

## 📡 Danh sách REST API

Hệ thống cung cấp các endpoint chính tại `http://localhost:8080/api`:

| Nhóm | Method | Endpoint | Mô tả |
|---|---|---|---|
| **Xác thực** | `POST` | `/api/auth/register` | Đăng ký tài khoản mới |
| | `POST` | `/api/auth/login` | Đăng nhập và lấy thông tin phiên |
| **Công thức** | `GET` | `/api/recipes` | Lấy danh sách món ăn (hỗ trợ `?search=` và `?userId=`) |
| | `GET` | `/api/recipes/{id}` | Chi tiết món ăn, nguyên liệu, các bước & dinh dưỡng |
| | `POST` | `/api/recipes` | Thêm công thức món ăn mới |
| | `PUT` | `/api/recipes/{id}` | Cập nhật thông tin công thức món ăn |
| **Yêu thích** | `GET` | `/api/favorites?userId={id}` | Lấy danh sách món ăn yêu thích của user |
| | `POST` | `/api/favorites/{recipeId}?userId={id}` | Thêm món vào danh sách yêu thích |
| | `DELETE` | `/api/favorites/{recipeId}?userId={id}` | Xóa món khỏi danh sách yêu thích |
| **Người dùng** | `GET` | `/api/users/me?userId={id}` | Xem thông tin hồ sơ cá nhân |
| | `PUT` | `/api/users/me?userId={id}` | Cập nhật thông tin tài khoản |

---

## 🚀 Hướng dẫn chạy nhanh (Quick Start)

### 1. Cơ sở dữ liệu (MySQL)
Chạy script tại thư mục `database/` trong MySQL:
```bash
mysql -u root -p < database/recipe_nutrition1.sql
mysql -u root -p recipe_nutrition < database/recipes_seed_vi.sql
```

### 2. Khởi chạy Backend (Spring Boot)
1. Cấu hình mật khẩu DB qua biến môi trường hoặc tạo file `backend/src/main/resources/application-local.properties`:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   ```
2. Khởi chạy server:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   *API server sẽ chạy tại `http://localhost:8080`.*

### 3. Khởi chạy Frontend (Android)
1. Mở thư mục `frontend` bằng **Android Studio**.
2. Để Android Studio sync Gradle tự động.
3. Chạy ứng dụng trên Emulator: Mặc định kết nối tới `10.0.2.2:8080` (alias trỏ về localhost máy host).
   *(Nếu chạy trên điện thoại thật: Nhập IP mạng LAN của máy tính tại nút cấu hình địa chỉ máy chủ trên app)*.
