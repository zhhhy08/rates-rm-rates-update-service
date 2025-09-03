package com.hertz.api.dto;

public class RatesUpdateResponse {
    private String responseMessage;

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    @Override
    public String toString() {
        return "RatesUpdateResponse{" +
                "responseMessage='" + responseMessage + '\'' +
                '}';
    }
}
