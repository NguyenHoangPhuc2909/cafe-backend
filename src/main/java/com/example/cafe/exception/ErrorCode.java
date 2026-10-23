package com.example.cafe.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  // Chung
  UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định"),
  FIELD_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng nhập đầy đủ thông tin bắt buộc"),

  // Tài khoản và đăng nhập
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
  USERNAME_EXISTED(HttpStatus.BAD_REQUEST, "Tên đăng nhập đã tồn tại"),
  EMAIL_EXISTED(HttpStatus.BAD_REQUEST, "Email đã được sử dụng"),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không chính xác"),

  // Danh mục và sản phẩm
  CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"),
  PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"),
  PRODUCT_INACTIVE(HttpStatus.CONFLICT, "Món đang ngừng bán"),
  PRODUCT_CONFIGURATION_INVALID(HttpStatus.CONFLICT, "Cấu hình loại món hoặc công thức không hợp lệ"),

  // Đơn hàng
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"),
  INVALID_ORDER_QUANTITY(HttpStatus.BAD_REQUEST, "Tổng số lượng món không hợp lệ"),
  INVALID_ORDER_TOTAL(HttpStatus.BAD_REQUEST, "Tổng tiền đơn hàng vượt giới hạn cho phép"),

  // Tồn kho
  OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "Sản phẩm đã hết hàng hoặc không đủ số lượng"),
  NOT_ENOUGH_INGREDIENT(HttpStatus.BAD_REQUEST, "Không đủ nguyên liệu để pha chế"),
  INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy nguyên liệu"),

  // Bàn
  TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bàn"),
  TABLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tên bàn hoặc mã QR đã tồn tại"),
  TABLE_INACTIVE(HttpStatus.CONFLICT, "Bàn đang ngừng sử dụng"),

  // Phiên phục vụ và token khách
  TABLE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Bàn chưa có phiên phục vụ đang mở"),
  TABLE_SESSION_ALREADY_OPEN(HttpStatus.CONFLICT, "Bàn đã có phiên phục vụ đang mở"),
  TABLE_SESSION_CLOSED(HttpStatus.CONFLICT, "Phiên phục vụ đã đóng"),
  INVALID_TABLE_SESSION_TOKEN(HttpStatus.UNAUTHORIZED, "Token phiên không hợp lệ hoặc đã hết hạn");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }
}
