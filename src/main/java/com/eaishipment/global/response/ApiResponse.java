package com.eaishipment.global.response;

public class ApiResponse<T> {
    private final String resultCode;
    private final String message;
    private final T data;

    private ApiResponse(String resultCode, String message, T data) {
        this.resultCode = resultCode;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("S", message, data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>("E", message, null);
    }

    public String getResultCode() {
        return resultCode;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
