# Quản Lý Chi Tiêu

> **Lưu ý**: Ứng dụng sử dụng Firebase làm backend nên không trích xuất database. Bạn có thể tự tạo tài khoản hoặc đăng nhập bằng Google để test trực tiếp ứng dụng.

## ⚠️ Cảnh Báo
Trước khi chạy ứng dụng, bạn cần tạo 2 file cấu hình:
1. `quanlychitieu-admin/quanlychitieu-586c9-firebase-adminsdk.json` - File cấu hình Firebase Admin SDK
2. `quanlychitieu-app/app/google-services.json` - File cấu hình Firebase cho ứng dụng mobile

Vui lòng liên hệ admin để được cung cấp các file cấu hình này.

## Cách Chạy Ứng Dụng

### 1. Khởi động Web Server (API & Admin Dashboard)
1. Thêm file `quanlychitieu-586c9-firebase-adminsdk.json` vào thư mục `quanlychitieu-admin`
2. Cài đặt dependencies:
   ```bash
   cd quanlychitieu-admin
   npm install
   ```
3. Khởi động server:
   ```bash
   npm run dev
   ```
4. Kiểm tra server đang chạy ở port 3000. Nếu muốn thay đổi port, cần cấu hình lại.

### 2. Chạy Ứng Dụng Mobile
Có 2 cách để chạy ứng dụng mobile:

#### Cách 1: Chạy Local
1. Thêm file `google-services.json` vào thư mục `quanlychitieu-app/app`
2. Kiểm tra cấu hình port trong file `data/api/ApiClient.java` để đảm bảo khớp với port của server
3. Build và chạy ứng dụng

#### Cách 2: Cài đặt trực tiếp
- Sử dụng file APK có sẵn để cài đặt trực tiếp trên thiết bị

## Demo Ứng Dụng

### Mobile App
![Mobile App Demo 1](img_demo/176ccde86febdcb585fa7.jpg)
![Mobile App Demo 2](img_demo/718c283b8a38396660293.jpg)
![Mobile App Demo 3](img_demo/7b882f068d053e5b67145.jpg)
![Mobile App Demo 4](img_demo/7bc1f5725771e42fbd604.jpg)
![Mobile App Demo 5](img_demo/80789ecd3cce8f90d6df2.jpg)
![Mobile App Demo 6](img_demo/af3eae860c85bfdbe6941.jpg)
![Mobile App Demo 7](img_demo/e0df5f4afd494e1717589.jpg)
![Mobile App Demo 8](img_demo/ea05819f239c90c2c98d8.jpg)
![Mobile App Demo 9](img_demo/ee9ad4107613c54d9c026.jpg)

