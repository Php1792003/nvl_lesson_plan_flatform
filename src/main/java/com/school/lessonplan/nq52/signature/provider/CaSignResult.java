package com.school.lessonplan.nq52.signature.provider;

import java.time.Instant;

public class CaSignResult {
    private boolean success;
    private String certSubject;
    private String certSerial;
    private String certIssuer;
    private Instant certValidFrom;
    private Instant certValidTo;
    private String signatureValue;
    private String timestampToken;
    private String errorMessage;
    private String warningMessage;
    private String rawResponse;
    private String certStatus;
    private String serviceName;
    private String deviceInfo;

    public CaSignResult() {}

    public static CaSignResult success(String certSubject, String certSerial, String certIssuer,
                                       Instant certValidFrom, Instant certValidTo,
                                       String signatureValue, String timestampToken) {
        CaSignResult res = new CaSignResult();
        res.success = true;
        res.certSubject = certSubject;
        res.certSerial = certSerial;
        res.certIssuer = certIssuer;
        res.certValidFrom = certValidFrom;
        res.certValidTo = certValidTo;
        res.signatureValue = signatureValue;
        res.timestampToken = timestampToken;
        return res;
    }

    public static CaSignResult error(String errorMessage) {
        CaSignResult res = new CaSignResult();
        res.success = false;
        res.errorMessage = errorMessage;
        return res;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getCertSubject() { return certSubject; }
    public void setCertSubject(String certSubject) { this.certSubject = certSubject; }

    public String getCertSerial() { return certSerial; }
    public void setCertSerial(String certSerial) { this.certSerial = certSerial; }

    public String getCertIssuer() { return certIssuer; }
    public void setCertIssuer(String certIssuer) { this.certIssuer = certIssuer; }

    public Instant getCertValidFrom() { return certValidFrom; }
    public void setCertValidFrom(Instant certValidFrom) { this.certValidFrom = certValidFrom; }

    public Instant getCertValidTo() { return certValidTo; }
    public void setCertValidTo(Instant certValidTo) { this.certValidTo = certValidTo; }

    public String getSignatureValue() { return signatureValue; }
    public void setSignatureValue(String signatureValue) { this.signatureValue = signatureValue; }

    public String getTimestampToken() { return timestampToken; }
    public void setTimestampToken(String timestampToken) { this.timestampToken = timestampToken; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }

    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }

    public String getCertStatus() { return certStatus; }
    public void setCertStatus(String certStatus) { this.certStatus = certStatus; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
}

