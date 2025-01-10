package com.dalvi.models;

public class AuthCode {
    private String code;
    private long expiration;

    public AuthCode(String code, long expiration) {
        this.code = code;
        this.expiration = expiration;
    }

    public String getCode() {
        return code;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}