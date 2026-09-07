package com.example.cafe.exception;

import com.example.cafe.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  // 1. Xử lý các lỗi nghiệp vụ do chúng ta chủ động ném ra (VD: Không tìm thấy sản phẩm)
  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiResponse<Void>> handleAppException(AppException e) {
    // Lấy thông tin ErrorCode từ exception bị ném ra
    ErrorCode errorCode = e.getErrorCode();

    // Đóng gói vào ApiResponse chuẩn
    ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
        .success(false)
        .message(errorCode.getMessage())
        .build();

    // Trả về kèm theo mã HTTP Status (VD: 404 Not Found, 400 Bad Request...)
    return ResponseEntity.status(errorCode.getStatus()).body(apiResponse);
  }

  // 2. Xử lý những lỗi hệ thống chung chung (VD: Lỗi null pointer,...) chưa lường trước được
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception e) {
    // Gán thành lỗi hệ thống không xác định
    ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;

    ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
        .success(false)
        .message(errorCode.getMessage()) // "Lỗi hệ thống không xác định"
        .build();

    return ResponseEntity.status(errorCode.getStatus()).body(apiResponse);
  }
}
