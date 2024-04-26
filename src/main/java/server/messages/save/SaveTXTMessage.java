package server.messages.save;

import org.apache.commons.fileupload.FileItem;

/**
 * Message used from text and sum workers to saver actor containing the processed data to save in the directory
 */
public class SaveTXTMessage {
    private final byte[] payload;
    private final String resultDirectory;
    private final String Sender;
    private final Integer id;
    private final int offset;

    public SaveTXTMessage(byte[] payload, String resultDirectory, String sender, Integer id) {
        this.payload = payload;
        this.resultDirectory = resultDirectory;
        Sender = sender;
        this.id = id;
        this.offset=0;
    }

    private SaveTXTMessage(SaveTXTMessage old) {
        this.payload = old.getPayload();
        this.resultDirectory = old.getResultDirectory();
        Sender = old.getSender();
        this.id = old.getId();
        this.offset = old.getOffset()+1;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getResultDirectory() {
        return resultDirectory;
    }

    public String getSender() {
        return Sender;
    }

    public Integer getId() {
        return id;
    }

    //public void increaseOffset(){offset++;}

    public SaveTXTMessage increaseOffset(){
        return new SaveTXTMessage(this);
    }

    public int getOffset() {
        return offset;
    }
}
