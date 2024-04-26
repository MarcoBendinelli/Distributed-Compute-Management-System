package server.messages.save;

import org.apache.commons.fileupload.FileItem;

/**
 * Message used from image compression worker to saver actor containing the processed data to save in the directory
 */
public class SaveImageMessage {
    private final FileItem payload;
    private final String resultDirectory;
    private final String Sender;
    private final Integer id;
    private final int offset;

    public SaveImageMessage(FileItem payload, String resultDirectory, String sender, Integer id) {
        this.payload = payload;
        this.resultDirectory = resultDirectory;
        Sender = sender;
        this.id = id;
        this.offset=0;
    }

    private SaveImageMessage(SaveImageMessage old){
        this.payload = old.getPayload();
        this.resultDirectory = old.getResultDirectory();
        Sender = old.getSender();
        this.id = old.getId();
        this.offset= old.getOffset()+1;
    }

    public FileItem getPayload() {
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
    public SaveImageMessage increaseOffset(){
        return new SaveImageMessage(this);
    }

    public int getOffset() {
        return offset;
    }


}
