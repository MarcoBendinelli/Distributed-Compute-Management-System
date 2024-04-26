package server.messages.tasks;

/**
 * Message used from Handler->Server->Router->SumWorker containing the data to process
 */
public class NumberSumTask {
    private final String sender;
    private int id;
    private final Integer num1;
    private final Integer num2;
    private final String resultDirectory;

    public NumberSumTask(String sender, Integer num1, Integer num2, String resultDirectory) {
        this.sender = sender;
        this.num1 = num1;
        this.num2 = num2;
        this.resultDirectory = resultDirectory;
    }

    public Integer getNum1() {
        return num1;
    }

    public Integer getNum2() {
        return num2;
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
