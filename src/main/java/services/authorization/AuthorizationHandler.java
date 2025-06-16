package services.authorization;

import services.authorization.errors.RegistrationInputException;
import services.authorization.errors.IncorrectPasswordException;
import dao.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class AuthorizationHandler {

    //логин пользователя по логину/паролю
    public String login(String login, String password) throws IncorrectPasswordException, SQLException {// время жизни (в минутах) токена авторизации
        //достаем из БД хэшированный пароль, проверяем его с введенным
        String hashedPassword = getUserHashedPW(login);
        if (!BCrypt.checkpw(password, hashedPassword)) throw new IncorrectPasswordException("Wrong Password!");

        //грузим юзера из БД
        User loggedUser = loadUser(login);

        //устанавливаем конец жизни токена
        return JWTHandler.createUserToken(loggedUser);
    }

    //загрузка юзера из БД
    private User loadUser(String login) throws SQLException {
        String sql =
                "SELECT id, login, is_admin " +
                        "FROM users " +
                        "WHERE login = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, login);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("User not found!");
                }
                long id = rs.getInt   ("id");
                String user_login = rs.getString("login");
                Boolean isAdmin = rs.getBoolean("is_admin");

                return new User(id, user_login, isAdmin);
            }
        }
    }

    //загрузка пароля из БД
    private String getUserHashedPW(String login) throws IncorrectPasswordException, SQLException {
        String sql = "SELECT password_hash FROM users WHERE login = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new IncorrectPasswordException("User not found!");

                return resultSet.getString("password_hash");
            }
        }
    }

    //регистрация пользователя
    public void register(String login, String password, Boolean isAdmin) throws RegistrationInputException, SQLException {
        //проверки на валидность введенного пароля/доступность логина
        checkPasswordValidity(password);
        checkLoginValidity(login);
        if (isAdmin) checkAdminExistence();

        //хэшируем пароль и добавляем юзера в БД
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        insertUserIntoDB(login, hashedPassword, isAdmin);
    }

    //добавление юзера в БД
    private void insertUserIntoDB(String login, String password, Boolean isAdmin) throws SQLException {
        String sql = "INSERT INTO users (login, password_hash, is_admin) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Set the parameters in the PreparedStatement
            pstmt.setString(1, login);   // Set username
            pstmt.setString(2, password);   // Set password (hashed password in real cases)
            pstmt.setBoolean(3, isAdmin);      // Set isAdmin

            // Execute the update
            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Registration successful!");
            }

        }
    }

    private void checkAdminExistence() throws RegistrationInputException, SQLException {
        String sql = "SELECT login FROM users WHERE is_admin = true";

        try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                throw new RegistrationInputException("Admin already exists!");
            }
        }
    }

    //проверка пароля на валидность
    private void checkPasswordValidity(String password) throws RegistrationInputException {
        if (password.length() < 3 || password.length() > 25)
            throw new RegistrationInputException("Пароль должен быть > 3 и < 25 символов.");
        // другие проверки
        // ...
    }

    private void checkLoginValidity(String login) throws RegistrationInputException, SQLException {
        if (login.length() < 1)
            throw new RegistrationInputException("Логин не указан.");

        String sql = "SELECT * FROM users WHERE login = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, login);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new RegistrationInputException("Username is already in use.");
                }
            }
        }
    }
}
