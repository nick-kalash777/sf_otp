package services;

import dao.DatabaseConnection;
import services.notifications.EmailNotificationService;
import services.notifications.SMSNotificationService;
import services.notifications.TelegramNotificationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OTPHandler {
    private static int codeLength = 6;
    private static int codeTTL = 5;
    private static int maxAttempts = 3;
    private static String defaultPhoneNumber = "+79118883377";
    private static String defaultEmail = "test@gmail.com";

    public static void start() throws SQLException {
        loadOTPConfig();
        expirationTimerStart();

    }

    public static void generateOTP(String operationID, Long userID) throws SQLException, IOException {
        OTP otp = new OTP(operationID, userID, codeLength, codeTTL);
        insertOTPIntoDB(otp);
        sendOTPToUser(otp);
        saveOTPToFile(otp);
    }

    private static void saveOTPToFile(OTP otp) throws IOException {
        Files.write(Paths.get("OTPCodes.txt"),
                String.format("Code:%s,User:%s,Operation:%s,Expiration:%s\n",
                        otp.getCode(), otp.getUserID(), otp.getOperationID(), otp.getExpiresAt()).getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    private static void sendOTPToUser(OTP otp) {
        TelegramNotificationService telegramNotificationService = new TelegramNotificationService();
        SMSNotificationService smsNotificationService = new SMSNotificationService();
        EmailNotificationService emailNotificationService = new EmailNotificationService();
        emailNotificationService.sendCode(defaultEmail, otp.getCode());
        smsNotificationService.sendCode(defaultPhoneNumber, otp.getCode());
        telegramNotificationService.sendCode(otp.getCode());
    }

    public static void validateOTP(String code, Long userID, String operationID) throws SQLException {
        String updateSql =
                "UPDATE otp_codes " +
                        "SET status = 'USED'::otp_status, used_at = NOW() " +
                        "WHERE code = ? AND user_id = ? and operation_id = ? AND status = 'ACTIVE'::otp_status";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {

            ps.setString(1, code);
            ps.setLong(2, userID);
            ps.setString(3, operationID);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated == 0) {
                throw new SQLException("Incorrect or already used OTP!");
            }
        }
    }

    private static void insertOTPIntoDB(OTP otp) throws SQLException {
        String sql = "INSERT INTO otp_codes (user_id, operation_id, code, status, created_at, expires_at) VALUES (?, ?, ?, ?::otp_status, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Set the parameters in the PreparedStatement
            pstmt.setLong(1, otp.getUserID());
            pstmt.setString(2, otp.getOperationID());
            pstmt.setString(3, otp.getCode());
            pstmt.setString(4, otp.getStatus().name());
            pstmt.setTimestamp(5, otp.getCreatedAt());
            pstmt.setTimestamp(6, otp.getExpiresAt());

            // Execute the update
            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("OTP added successfully!");
            }

        }
    }

    public static void changeOTPConfig(int newLength, int newTTL, int newMaxAttempts) throws SQLException {
        codeLength = newLength;
        codeTTL = newTTL;
        maxAttempts = newMaxAttempts;
        String sql =
                "UPDATE otp_config " +
                        "SET code_length = ?, code_ttl = ?, max_attempts = ?, updated_at = NOW() ";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);) {
            ps.setInt(1, codeLength);
            ps.setInt(2, codeTTL);
            ps.setLong(3, maxAttempts);

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated == 0) { throw new SQLException("No config detected."); }
        }
    }

    private static void loadOTPConfig() throws SQLException {
        String sql = "SELECT code_length, code_ttl, max_attempts " +
                "FROM otp_config " +
                "WHERE id = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("NO CONFIG DETECTED!");
            }
            codeLength = rs.getInt("code_length");
            codeTTL = rs.getInt("code_ttl");
            maxAttempts = rs.getInt("max_attempts");
        }
    }

    private static void expirationTimerStart() throws SQLException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runnable task = OTPHandler::expireOTPs;

        // Schedule the task to run every 1 minute, with initial delay of 0
        scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.MINUTES);
    }

    private static void expireOTPs() {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED'::otp_status " +
                "WHERE expires_at <= now() and status = 'ACTIVE'::otp_status";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int rowsUpdated = ps.executeUpdate();
            System.out.println(rowsUpdated + " OTPs expired.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}
