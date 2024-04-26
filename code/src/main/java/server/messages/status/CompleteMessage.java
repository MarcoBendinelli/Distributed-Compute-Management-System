package server.messages.status;

import server.enums.StatusEnum;

/**
 * Message used from saverActor to server in order to inform that a specific task has finished it's processing cycle
 * The task status can be DONE if completed or FAILED if saving failed too many times
 * (due to an impossible directory or permanently corrupted file)
 */
public class CompleteMessage {
    private final String sender;
    private final int id;
    private final StatusEnum status;

    public CompleteMessage(String sender, int id,StatusEnum status) {
        this.sender = sender;
        this.id = id;
        this.status=status;
    }

    public String getSender() {
        return sender;
    }

    public int getId() {
        return id;
    }

    public StatusEnum getStatus() {
        return status;
    }
}
