package services.authorization;

public class User {
    private Long id;
    private String login;
    private Boolean isAdmin;

    public User(Long id, String login, Boolean isAdmin) {
        this.id = id;
        this.login = login;
        this.isAdmin = isAdmin;
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public Boolean isAdmin() { return isAdmin; }

}
