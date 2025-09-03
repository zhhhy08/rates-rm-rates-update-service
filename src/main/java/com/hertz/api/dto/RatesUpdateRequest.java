package com.hertz.api.dto;

public class RatesUpdateRequest {
    private String requestString;

    public String getRequestString() {
        return requestString;
    }

    public void setRequestString(String requestString) {
        this.requestString = requestString;
    }

    @Override
    public String toString() {
        return "RatesUpdateRequest{" +
                "requestString='" + requestString + '\'' +
                '}';
    }
}
