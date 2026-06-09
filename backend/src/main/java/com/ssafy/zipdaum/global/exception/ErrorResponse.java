package com.ssafy.zipdaum.global.exception;

import com.ssafy.zipdaum.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class ErrorResponse {
  private final boolean success = false;

  private final String code;
  private final String message;

  public ErrorResponse(ErrorCode errorCode){
    this.code = errorCode.getCode();
    this.message = errorCode.getMessage();
  }
}
