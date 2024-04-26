package server.beans;

/**
 * Class used to store user data during the login phase
 * This class will be sent as part of a http response to the client
 * Thanks to this the client will know the structure of the received data and will be able to parse it
 */
public class User {
    private String username;
    private String password;

    public User() {
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
