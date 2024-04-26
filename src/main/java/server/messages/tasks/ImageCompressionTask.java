package server.messages.tasks;

import org.apache.commons.fileupload.FileItem;

import java.util.Map;

/**
 * Message used from Handler->Server->Router->ImageWorker containing the data to process
 */
public class ImageCompressionTask {
    private final String sender;
    private int id;
    private final int compressionRatio;
    private final FileItem payload;
    private final String resultDirectory;

    public ImageCompressionTask(Map<String, FileItem> parts) {
        this.sender = parts.get("sender").getString();
        this.compressionRatio = Integer.parseInt(parts.get("ratio").getString());
        this.payload = parts.get("payload");
        this.resultDirectory = parts.get("directory").getString();;
    }

    public FileItem getPayload() {
        return payload;
    }

    public int getCompressionRatio() {
        return compressionRatio;
    }

    public String getResultDirectory() {
        return resultDirectory;
    }

    public String getSender() {
        return sender;
    }

    /**
     * This is the only set function in all the messages used
     * It is needed as the main server actor is the only one who can assign unique ids to the tasks
     * It is not a problem if the message is not immutable since only the main actor server will ever modify this value and it will happen
     * only once for every task.
     *
     * @param id id to assign to the single task
     */
    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}