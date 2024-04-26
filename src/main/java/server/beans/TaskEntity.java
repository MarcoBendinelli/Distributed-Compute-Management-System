package server.beans;

import server.enums.StatusEnum;

/**
 * Class representing task data that would be stored in a database
 * This class will also be sent as part of a http response to the client when requesting for tasks statuses
 * Thanks to this the client will know the structure of the received data and will be able to parse it
 */
public class TaskEntity {
    private final int id;
    private final String type;
    private StatusEnum complete;

    public TaskEntity(String type, int id, StatusEnum complete) {
        this.type = type;
        this.id = id;
        this.complete = complete;
    }

    
    public void updateStatus(StatusEnum status){
        this.complete=status;
    }

    public int getId() {
        return id;
    }
}
