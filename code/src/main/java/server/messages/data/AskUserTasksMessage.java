package server.messages.data;

/**
 * Message used from client Handler to server when a request for all tasks data is received
 */
public class AskUserTasksMessage {
    private final String username;

    public AskUserTasksMessage(String username) {
        this.username=username;
    }

    public String getUsername() {
        return username;
    }
}
