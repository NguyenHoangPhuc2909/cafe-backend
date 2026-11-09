package com.example.cafe.common.response;

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
  INVALID_PRODUCT_SIZE(HttpStatus.BAD_REQUEST, "Món không hỗ trợ size đã chọn. Vui lòng kiểm tra lại thực đơn"),
  INVALID_DRINK_CUSTOMIZATION(HttpStatus.BAD_REQUEST, "Món không hỗ trợ tùy chọn đá hoặc độ ngọt"),
  INVALID_ORDER_TOTAL(HttpStatus.BAD_REQUEST, "Tổng tiền đơn hàng vượt giới hạn cho phép"),
  INVALID_ORDER_QUERY(HttpStatus.BAD_REQUEST, "Bộ lọc hoặc thông tin phân trang đơn hàng không hợp lệ"),
  ORDER_NOT_COMPLETABLE(HttpStatus.CONFLICT, "Chỉ được hoàn thành đơn đã thanh toán và đang chế biến"),

  // Tồn kho
  OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "Sản phẩm đã hết hàng hoặc không đủ số lượng"),
  NOT_ENOUGH_INGREDIENT(HttpStatus.BAD_REQUEST, "Không đủ nguyên liệu để pha chế"),
  INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy nguyên liệu"),
  INVENTORY_DATA_INVALID(HttpStatus.CONFLICT, "Dữ liệu tồn kho hoặc giữ hàng không hợp lệ"),

  // Bàn
  TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bàn"),
  TABLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tên bàn hoặc mã QR đã tồn tại"),
  TABLE_INACTIVE(HttpStatus.CONFLICT, "Bàn đang ngừng sử dụng"),

  // Thanh toán
  INVALID_CASH_ENTRY(HttpStatus.BAD_REQUEST, "Thông tin tiền mặt không hợp lệ. Nhập số tiền nguyên và nội dung đầy đủ"),
  CASH_DAY_CHANGED(HttpStatus.CONFLICT, "Đã sang ngày mới theo giờ Việt Nam. Tải lại quỹ trước khi ghi nhận"),
  CASH_DATA_CHANGED(HttpStatus.CONFLICT, "Quỹ tiền mặt đã có giao dịch mới. Tải lại và kiểm tra số thực đếm trước khi lưu"),
  CASH_REQUEST_CONFLICT(HttpStatus.CONFLICT, "Mã yêu cầu tiền mặt đã được dùng với nội dung khác"),
  CASH_NOTE_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng ghi chú nguyên nhân chênh lệch tiền mặt"),
  CASH_ACTIVE_ORDERS(HttpStatus.CONFLICT, "Vui lòng xử lý hết đơn chờ thanh toán và đang pha chế trước khi lưu đối chiếu tiền mặt"),
  PAYMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền xác nhận thanh toán"),
  ORDER_NOT_PAYABLE(HttpStatus.CONFLICT, "Đơn hàng không đủ điều kiện thanh toán"),
  PAYMENT_EXPIRED(HttpStatus.CONFLICT, "Đơn hàng đã quá hạn thanh toán"),
  PAYMENT_CONFLICT(HttpStatus.CONFLICT, "Đơn đã có thông tin thanh toán khác"),
  PAYMENT_DATA_INVALID(HttpStatus.CONFLICT, "Dữ liệu thanh toán hoặc giữ hàng không hợp lệ"),

  // Phiên phục vụ và token khách
  TABLE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Bàn chưa có phiên phục vụ đang mở"),
  TABLE_SESSION_ALREADY_OPEN(HttpStatus.CONFLICT, "Bàn đã có phiên phục vụ đang mở"),
  TABLE_SESSION_CLOSED(HttpStatus.CONFLICT, "Phiên phục vụ đã đóng"),
  TABLE_SESSION_HAS_ACTIVE_ORDERS(HttpStatus.CONFLICT, "Chưa thể kết thúc phiên: còn đơn chờ thanh toán hoặc đang chuẩn bị"),
  INVALID_TABLE_SESSION_TOKEN(HttpStatus.UNAUTHORIZED, "Token phiên không hợp lệ hoặc đã hết hạn"),

  // Tồn kho
  INVALID_STOCKTAKE(HttpStatus.BAD_REQUEST, "Phiếu kiểm kho không hợp lệ. Kiểm tra số thực tế và ghi chú"),
  STOCKTAKE_ACTIVE_ORDERS(HttpStatus.CONFLICT, "Vui lòng xử lý hết đơn chờ thanh toán và đang pha chế trước khi kiểm kho"),
  STOCKTAKE_STALE(HttpStatus.CONFLICT, "Tồn kho đã thay đổi. Tải lại số đối chiếu và kiểm tra lại số thực tế trước khi lưu"),
  STOCKTAKE_CONFLICT(HttpStatus.CONFLICT, "Mã phiếu kiểm kho đã được sử dụng với nội dung khác"),
  STOCKTAKE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu kiểm kho"),
  INVALID_STOCK_HISTORY_QUERY(HttpStatus.BAD_REQUEST, "Bộ lọc lịch sử nhập kho không hợp lệ"),
  PACKAGED_QUANTITY_NOT_INTEGER(HttpStatus.BAD_REQUEST, "Số lượng hàng đóng gói phải là số nguyên"),
  STOCK_RECEIPT_CONFLICT(HttpStatus.CONFLICT, "Mã yêu cầu nhập kho đã được sử dụng với nội dung khác"),
  INVALID_STOCK_RECEIPT(HttpStatus.BAD_REQUEST, "Thông tin nhập kho không hợp lệ. Chọn đúng một nguyên liệu hoặc hàng đóng gói");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }
}
