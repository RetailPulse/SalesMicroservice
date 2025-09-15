package com.retailpulse.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
  private final String errorCode;

  public BusinessException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
  
  public String getCode() {
    return errorCode;
  }
}
