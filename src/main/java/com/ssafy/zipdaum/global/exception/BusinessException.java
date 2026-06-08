package com.ssafy.zipdaum.global.exception;

import com.ssafy.zipdaum.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{
  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode){
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
