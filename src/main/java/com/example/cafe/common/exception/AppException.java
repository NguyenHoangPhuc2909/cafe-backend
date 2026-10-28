package com.example.cafe.common.exception;

import com.example.cafe.common.response.ErrorCode;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException{
  private final ErrorCode errorCode;

  public AppException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
