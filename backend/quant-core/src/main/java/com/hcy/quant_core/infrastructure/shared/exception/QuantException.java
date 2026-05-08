package com.hcy.quant_core.infrastructure.shared.exception;

public class QuantException extends RuntimeException {
	public QuantException(String message) {
		super(message);
	}

	public QuantException(String message, Throwable cause) {
		super(message, cause);
	}
}
