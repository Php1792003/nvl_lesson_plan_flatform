package com.school.lessonplan.nq52.signature.dto;

public class RemoteSignInitiateResponse {
    private boolean success;
    private String transactionId;
    private int expiresInSeconds = 120;
    private String message;
    private String provider;
    private String suggestedOtp; // For testing / demo environment
    private String gatewayRawResponse;
    private Integer gatewayStatusCode;

    public RemoteSignInitiateResponse() {}

    public String getGatewayRawResponse() { return gatewayRawResponse; }
    public void setGatewayRawResponse(String gatewayRawResponse) { this.gatewayRawResponse = gatewayRawResponse; }

    public Integer getGatewayStatusCode() { return gatewayStatusCode; }
    public void setGatewayStatusCode(Integer gatewayStatusCode) { this.gatewayStatusCode = gatewayStatusCode; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public int getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(int expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getSuggestedOtp() { return suggestedOtp; }
    public void setSuggestedOtp(String suggestedOtp) { this.suggestedOtp = suggestedOtp; }
}

