package com.hcy.quant_core.infrastructure.web.response;

public record JobStatusResponse<T>(String status, T result) {

	public static <T> JobStatusResponse<T> processing() {
		return new JobStatusResponse<>("PROCESSING", null);
	}

	public static <T> JobStatusResponse<T> completed(T result) {
		return new JobStatusResponse<>("COMPLETED", result);
	}
}
