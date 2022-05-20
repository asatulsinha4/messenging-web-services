package com.springapplication.messenging.messengingwebservices.dto.test;

import java.io.Serializable;

public class BaseDTO implements Serializable {

    private String status;
    private String reason;
    private Integer statusCode;

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }
    public Integer getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    
}
