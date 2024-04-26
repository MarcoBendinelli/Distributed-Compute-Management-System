package server.messages.data;

import com.google.gson.Gson;
import server.beans.TaskEntity;

import java.util.Collection;

/**
 * Message used from server to clientHandler containing all the tasks data relative to the single user
 */
public class ResponseUserTasksMessage {
    private final Collection<TaskEntity> data;

    public ResponseUserTasksMessage(Collection<TaskEntity> map) {
        this.data = map;
    }
    public String getData(){
        Gson gson = new Gson();
        return gson.toJson(data);
    }
}
