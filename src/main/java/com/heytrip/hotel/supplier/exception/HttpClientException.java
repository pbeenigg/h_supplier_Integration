package com.heytrip.hotel.supplier.exception;


/**
 * 调用供应商 API异常
 */
public class HttpClientException extends RuntimeException  {

    public HttpClientException(String message) {
        super(message);
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
    }
    public HttpClientException(Throwable cause) {
        super(cause);
    }
    public HttpClientException(String code, String message, Throwable cause) {
        super(message, cause);
    }
}
