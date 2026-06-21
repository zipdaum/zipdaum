package com.ssafy.zipdaum.global.exception;

import com.ssafy.zipdaum.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e
  ) {
    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponse response = new ErrorResponse(errorCode);
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler({
      MissingServletRequestParameterException.class,
      MethodArgumentTypeMismatchException.class
  })
  protected ResponseEntity<ErrorResponse> handleRequestParameterException(Exception e) {
    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponse response = new ErrorResponse(errorCode);
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(BindException.class)
  protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponse response = new ErrorResponse(errorCode);
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  protected ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
      HandlerMethodValidationException e
  ) {
    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponse response = new ErrorResponse(errorCode);
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(BusinessException.class)
  protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e){
    ErrorCode errorCode = e.getErrorCode();

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
