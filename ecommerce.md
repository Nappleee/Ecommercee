Tôi đang xây dựng Spring Boot microservices gồm auth-service (MVC), api-gateway (WebFlux) và order-service (MVC).

Auth-service tự tạo JWT HS256, không dùng Keycloak. Access token có payload như sau:

{
  "iss": "ecommerce-auth-service",
  "sub": "nguyenhobac9995",
  "roles": ["USER"],
  "token_type": "access",
  "iat": 1786067594,
  "exp": 1786068494
}

JWT secret trong Docker Compose là:

JWT_SECRET: Zm9vLWJhci1iYXNlNjQtc2VjcmV0LWZvci1sb2NhbC1kZXYtMzIx
JWT_ISSUER: ecommerce-auth-service

Secret này là Base64 và auth-service đang dùng nó để ký JWT. Tôi muốn tất cả service xác minh cùng token này.

Hiện JwtConfig ở api-gateway đọc claim `roles` và chuyển `USER` thành authority `ROLE_USER`. Nhưng order-service bị 403 khi endpoint dùng:

@PreAuthorize("hasAuthority('USER')")

Yêu cầu của bạn:

1. Viết cấu hình JWT hoàn chỉnh, đúng chuẩn cho:
   - auth-service: JwtEncoder + JwtDecoder HS256
   - api-gateway WebFlux: ReactiveJwtDecoder và ReactiveJwtAuthenticationConverterAdapter
   - order-service MVC: JwtDecoder và JwtAuthenticationConverter

2. Cả 3 service phải:
   - Base64 decode `JWT_SECRET` trước khi tạo HmacSHA256 key.
   - Kiểm tra issuer là `ecommerce-auth-service`.
   - Đọc claim `roles: ["USER"]`.
   - Chuyển role thành `ROLE_USER`, `ROLE_ADMIN`.

3. Sửa các `@PreAuthorize` trong OrderController theo convention:
   - USER tạo/cập nhật/xóa order của mình.
   - ADMIN có thể xem và quản lý toàn bộ order.
   - Dùng `hasRole('USER')` hoặc `hasAnyRole('ADMIN', 'USER')`, không dùng `hasAuthority('USER')`.

4. Cho đầy đủ các import cần thiết, các class config, phần SecurityConfig cần gắn converter, và application.yaml/Docker Compose tối thiểu.

5. Lưu ý rõ những cấu hình Keycloak cần xóa khỏi api-gateway, vì hệ thống này dùng JWT do auth-service tự ký chứ không dùng Keycloak.

Giải thích ngắn gọn nguyên nhân gây lỗi 403 và vì sao `roles: ["USER"]` phải được converter thành `ROLE_USER`.