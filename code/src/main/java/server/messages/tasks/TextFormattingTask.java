package server.messages.tasks;

import org.apache.commons.fileupload.FileItem;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Message used from Handler->Server->Router->TextWorker containing the data to process
 */
public class TextFormattingTask {

    private final String sender;
    private int id;
    private final FileItem payload;
    private final String resultDirectory;

    public TextFormattingTask(Map<String,FileItem> parts) {
        this.payload = parts.get("payload");
        this.resultDirectory = parts.get("directory").getString();
        this.sender = parts.get("sender").getString();
    }
    public FileItem getPayload() {
        return payload;
    }

    public String getResultDirectory() {
        return resultDirectory;
    }

    public String getSender() {
        return sender;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
