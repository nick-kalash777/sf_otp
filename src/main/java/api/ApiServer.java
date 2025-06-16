package api;

import services.authorization.AuthorizationHandler;
import services.authorization.JWTHandler;
import services.authorization.User;
import services.authorization.errors.IncorrectPasswordException;
import services.authorization.errors.RegistrationInputException;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import dao.DatabaseConnection;
import io.jsonwebtoken.JwtException;
import services.OTPHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.net.URI;

public class ApiServer {
    private static final int PORT = 8080;

    public static void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        // Register contexts using method references
        server.createContext("/register", ApiServer::handleRegister);
        server.createContext("/login", ApiServer::handleLogin);
        server.createContext("/get_users", ApiServer::handleGetAllUsers);
        server.createContext("/generate_otp", ApiServer::handleGenerateOTP);
        server.createContext("/validate_otp", ApiServer::handleValidateOTP);
        server.createContext("/change_otp_config", ApiServer::handleOTPConfigChange);
        server.createContext("/delete_user", ApiServer::handleDeleteUser);
        server.setExecutor(null); // default executor

        System.out.println("Server started on port " + PORT);
        server.start();
    }

    private static void handleOTPConfigChange(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
        }

        try {
            String token = checkHeader(exchange.getRequestHeaders().getFirst("Authorization"));
            User user = JWTHandler.getUser(token);
            if (!user.isAdmin()) {
                sendResponse(exchange, 403, "Unauthorized");
                return;
            }
            var params = parseQuery(exchange.getRequestURI());
            int codeLength = Integer.parseInt(params.get("code_length"));
            int codeTTL = Integer.parseInt(params.get("code_ttl"));
            int maxAttempts = Integer.parseInt(params.get("max_attempts"));

            OTPHandler.changeOTPConfig(codeLength, codeTTL, maxAttempts);

            sendResponse(exchange, 200, "OK");


        } catch (Exception e) {
            sendResponse(exchange, 400, e.getMessage());
        }
    }

    private static void handleValidateOTP(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
        }

        try {
            String token = checkHeader(exchange.getRequestHeaders().getFirst("Authorization"));
            User user = JWTHandler.getUser(token);
            var params = parseQuery(exchange.getRequestURI());
            String code = params.get("code");
            String operationID = params.get("operation_id");
            OTPHandler.validateOTP(code, user.getId(), operationID);
            sendResponse(exchange, 200, "OTP Validated");
        } catch (Exception e) {
            sendResponse(exchange, 400, e.getMessage());
        }

    }

    private static void handleGenerateOTP(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
        }

        try {
            String token = checkHeader(exchange.getRequestHeaders().getFirst("Authorization"));
            User user = JWTHandler.getUser(token);

            var params = parseQuery(exchange.getRequestURI());
            String operationID = params.get("operation_id");
            OTPHandler.generateOTP(operationID, user.getId());
            sendResponse(exchange, 200, "OK");
        } catch (Exception e) {
            sendResponse(exchange, 400, e.getMessage());
        }
    }

    private static void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        var params = parseQuery(exchange.getRequestURI());
        String username = params.get("username");
        String password = params.get("password");
        Boolean isAdmin = Boolean.valueOf(params.get("is_admin"));

        AuthorizationHandler authHandler = new AuthorizationHandler();
        try {
            authHandler.register(username, password, isAdmin);
            sendResponse(exchange, 200, "User registered.");
        } catch (RegistrationInputException e) {
            sendResponse(exchange, 400, e.getMessage());
        } catch (Exception e) {
            sendResponse(exchange, 400,
                    "UNEXPECTED ERROR" + e.getMessage());
        }
    }

    private static void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        var params = parseQuery(exchange.getRequestURI());
        String username = params.get("username");
        String password = params.get("password");

        try {
            AuthorizationHandler authHandler = new AuthorizationHandler();
            String authToken = authHandler.login(username, password);
            sendResponse(exchange, 200, authToken);
        } catch (IncorrectPasswordException e) {
            sendResponse(exchange, 400, e.getMessage());
        } catch (Exception e) {
            sendResponse(exchange, 400,
                    "UNEXPECTED ERROR" + e.getMessage());
        }
    }

    private static void handleGetAllUsers(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
        }

        try {
            String token = checkHeader(exchange.getRequestHeaders().getFirst("Authorization"));
            User user = JWTHandler.getUser(token);
            if (!user.isAdmin()) {
                sendResponse(exchange, 403, "Unauthorized");
                return;
            }
            getAllUsers(exchange);
        } catch (Exception e) {
            sendResponse(exchange, 400, e.getMessage());
        }

    }

    private static void getAllUsers(HttpExchange exchange) throws SQLException, IOException {
        String sql = "SELECT id, login FROM users WHERE is_admin = false";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            LinkedHashMap<Integer, String> users = new LinkedHashMap<>();
            while (rs.next()) {
                int id = rs.getInt("id");
                String login = rs.getString("login");
                users.put(id, login);
            }
            sendResponse(exchange, 200, String.valueOf(users));
        }
    }

    private static void handleDeleteUser(HttpExchange exchange) throws IOException {
        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
        }

        try {
            String token = checkHeader(exchange.getRequestHeaders().getFirst("Authorization"));
            User user = JWTHandler.getUser(token);
            if (!user.isAdmin()) {
                sendResponse(exchange, 403, "Unauthorized");
                return;
            }
            var params = parseQuery(exchange.getRequestURI());
            String id = params.get("id");

            if (id.equals(String.valueOf(user.getId())))
                throw new SQLException("You cannot delete yourself!");

            deleteUser(id);
            sendResponse(exchange, 200, "User deleted.");

        } catch (SQLException e) {
            sendResponse(exchange, 400, e.getMessage());
        }

    }

    private static void deleteUser(String id) throws SQLException {
        String deleteUserSQL = "DELETE FROM users WHERE id = '" + id + "'";
        String deleteOTPSQL = "DELETE FROM otp_codes WHERE user_id = '" + id + "'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement deleteUserStmt = conn.prepareStatement(deleteUserSQL);
             PreparedStatement deleteOTPStmt = conn.prepareStatement(deleteOTPSQL);) {
            int rowsAffected = deleteUserStmt.executeUpdate();
            deleteOTPStmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("User not found");
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> result = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isEmpty()) return result;

        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0 && idx < pair.length() - 1) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }
        return result;
    }

    private static String checkHeader(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new JwtException("Invalid JWT token");
        }
        return header.substring("Bearer ".length());
    }
}
