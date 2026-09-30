package com.example.cafe.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định"),
  CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"),
  PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"),
  FIELD_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng nhập đầy đủ thông tin bắt buộc"),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"),
  USERNAME_EXISTED(HttpStatus.BAD_REQUEST, "Tên đăng nhập đã tồn tại"),
  EMAIL_EXISTED(HttpStatus.BAD_REQUEST, "Email đã được sử dụng"),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không chính xác"),
  OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "Sản phẩm đã hết hàng hoặc không đủ số lượng"),
  NOT_ENOUGH_INGREDIENT(HttpStatus.BAD_REQUEST, "Không đủ nguyên liệu để pha chế");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }
}
