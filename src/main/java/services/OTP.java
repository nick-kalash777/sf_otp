package services;

import java.sql.Timestamp;
import java.util.Random;

public class OTP {
    private final String code;
    private final String operationID;
    private final Long userID;

    enum Status {
        ACTIVE, EXPIRED, USED
    }
    Status status;
    private final Timestamp createdAt;
    private final Timestamp expiresAt;
    private Timestamp usedAt;

    public OTP(String operationID, Long userID, int length, int lifespan) {
        this.code = generateOTPString(length);
        this.operationID = operationID;
        this.userID = userID;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.expiresAt = new Timestamp(System.currentTimeMillis() + lifespan*60*1000);
        status = Status.ACTIVE;
    }

    private String generateOTPString(int length) {
        StringBuilder otp = new StringBuilder();
        Random rand = new Random();

        for (int i = 0; i < length; i++) {
            otp.append(rand.nextInt(10)); // digits 0-9
        }

        return otp.toString();
    }

    public String getCode() {
        return code;
    }

    public String getOperationID() {
        return operationID;
    }

    public Long getUserID() {
        return userID;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public Timestamp getUsedAt() {
        return usedAt;
    }

    public Status getStatus() {
        return status;
    }
}
