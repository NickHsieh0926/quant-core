package com.hcy.quant_core.infrastructure.web;

import com.hcy.quant_core.infrastructure.shared.exception.QuantException;
import com.hcy.quant_core.infrastructure.shared.util.DebugTrace;
import com.hcy.quant_core.infrastructure.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final DebugTrace TRACE = new DebugTrace(LOGGER, LOGGER.isDebugEnabled());

	@ExceptionHandler(QuantException.class)
	public ResponseEntity<ApiResponse<Void>> handleQuantException(QuantException e) {
		LOGGER.error("系統發生錯誤: {} ", e.getMessage());
		return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
	}
}
