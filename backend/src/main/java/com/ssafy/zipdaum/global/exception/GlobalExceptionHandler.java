package com.ssafy.zipdaum.global.exception;

import com.ssafy.zipdaum.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e
  ) {
    log.warn("요청값 검증 실패 | 오류 개수: {}", e.getBindingResult().getErrorCount());

    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponse response = new ErrorResponse(errorCode);
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler({
      MissingServletRequestParameterException.class,
      MethodArgumentTypeMismatchException.class
  })
  protected ResponseEntity<ErrorResponse> handleRequestParameterException(Exception e) {
    log.warn("요청 파라미터 오류 | 예외: {}", e.getClass().getSimpleName());

    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponse response = new ErrorResponse(errorCode);
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(BusinessException.class)
  protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e){
    ErrorCode errorCode = e.getErrorCode();

    log.warn("BusinessException 발생 | 코드: {}, 메시지: {}", errorCode.name(), errorCode.getMessage());

    ErrorResponse response = new ErrorResponse(errorCode);
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ErrorResponse> handleException(Exception e){
    log.error("예상치 못한 시스템 에러 발생: ", e);

    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    ErrorResponse response = new ErrorResponse(errorCode);
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

}
