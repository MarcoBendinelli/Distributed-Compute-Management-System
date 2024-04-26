package server.actors;


import akka.actor.AbstractActor;
import akka.actor.Props;
import server.enums.StatusEnum;
import server.messages.status.CompleteMessage;
import server.messages.save.SaveImageMessage;
import server.messages.save.SaveTXTMessage;
import server.exceptions.ActorFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Actor used to handle the process of saving tasks results
 */
public class SavingActor extends AbstractActor {

    private final static Random r = new Random();
    private final static int successRate = 20;
    private static final int maxRetries = 10;
    private static final int sleepTime = 500;


    @Override
    public Receive createReceive() {//different messages to avoid a switch case later
        return receiveBuilder()
                .match(SaveImageMessage.class, this::saveCompression)
                .match(SaveTXTMessage.class, this::saveTxt)
                .build();
    }

    /**
     * Save the text file in the specified directory in the message
     * It also simulates some failures and increment the message offset ONLY for the real ones in order
     * to avoid being stuck reprocessing one forever (since client input is not really checked)
     * @param saveMessage message containing the server computation results
     */
    private void saveTxt(SaveTXTMessage saveMessage) throws ActorFailureException {
        //Simulate execution of the heavy task
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            getContext().getParent().tell(saveMessage, sender());
            e.printStackTrace();
            throw new ActorFailureException("failure of Thread.sleep");
        }
        if(saveMessage.getOffset()>maxRetries){
            sender().tell(new CompleteMessage(saveMessage.getSender(), saveMessage.getId(),StatusEnum.FAILED), getSelf());
            return;
        }
        if (r.nextInt(101) <= successRate) {
            //if simulation successfull try and save for real
            String completePath = saveMessage.getResultDirectory() + saveMessage.getId() + ".txt";
            File myFile = new File(completePath);
            try {
                Files.write(Path.of(completePath), saveMessage.getPayload());
                sender().tell(new CompleteMessage(saveMessage.getSender(), saveMessage.getId(),StatusEnum.DONE), getSelf());
            } catch (IOException e) {
                SaveTXTMessage increasedMessage = saveMessage.increaseOffset();
                context().parent().tell(increasedMessage, sender());
                throw new ActorFailureException("Failure while saving txt");
            }
        } else {
            //no offset increase here since failure is simulated and can't fail forever
            context().parent().tell(saveMessage, sender());
            throw new ActorFailureException("Simulated failure while saving txt");
        }
    }

    /**
     * Save the img file in the specified directory in the message
     * It also simulates some failures and increment the message offset ONLY for the real ones in order
     * to avoid being stuck reprocessing one forever (since client input is not really checked)
     *
     * @param saveMessage message containing the server computation results
     */
    private void saveCompression(SaveImageMessage saveMessage) throws ActorFailureException {
        if(saveMessage.getOffset()>maxRetries){
            sender().tell(new CompleteMessage(saveMessage.getSender(), saveMessage.getId(), StatusEnum.FAILED), getSelf());
            return;
        }
        if (r.nextInt(101) <= successRate) {
            try {
                String completePath = saveMessage.getResultDirectory() + saveMessage.getId() + ".jpeg";
                File file = new File(completePath);
                saveMessage.getPayload().write(file);
                sender().tell(new CompleteMessage(saveMessage.getSender(), saveMessage.getId(),StatusEnum.DONE), getSelf());
            } catch (Exception e) {
                SaveImageMessage increasedMessage = saveMessage.increaseOffset();
                context().parent().tell(increasedMessage, sender());
                throw new ActorFailureException("Failure while saving image");
            }
        }
        else {
            //no offset increase here since failure is simulated and can't fail forever
            context().parent().tell(saveMessage, sender());
            throw new ActorFailureException("Simulated failure while saving image");
        }
    }

    public static Props props() {
        return Props.create(SavingActor.class);
    }
}

