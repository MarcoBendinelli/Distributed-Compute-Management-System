package server.actors.workers;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import server.actors.SavingActor;
import server.messages.tasks.ImageCompressionTask;
import server.messages.save.SaveImageMessage;
import server.exceptions.ActorFailureException;

import java.time.Duration;
import java.util.Random;

public class CompressionWorkerActor extends AbstractActor {

    private final ActorRef saver = getContext().actorOf(SavingActor.props(),"saveActor");
    private final static Random r = new Random();
    //success rate of tasks handled in percentage
    private final static int successRate =20;
    //time needed to complete each task
    private static final int sleepTime = 1000;//in ms

    private static final SupervisorStrategy strategy =
            new OneForOneStrategy(
                    5,//max number of retries
                    Duration.ofMinutes(1),//within number of minutes
                    DeciderBuilder.match(ActorFailureException.class, e-> {
                        //System.out.println(e.getMessage());
                        return SupervisorStrategy.resume();
                    })
                            .build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ImageCompressionTask.class, this::onImageCompressionTask)
                .match(SaveImageMessage.class,this::onSaveMessage)
                .build();
    }

    private void onSaveMessage(SaveImageMessage message) {
        saver.tell(message,sender());
    }

    /**
     * Handling of image compression
     * The worker handles the tasks given to him and sends an adequate response:
     * If the task failed the parent(ROUTER) is informed by resending to him the same task to route again and
     * an exception is thrown to be handled by the parent
     * If successfully a save message is sent to the saver actor to handle the saving part of the task
     * @param message task to handle
     * @throws ActorFailureException Throw when the task is failed
     */
    private void onImageCompressionTask(ImageCompressionTask message) throws ActorFailureException {
        //Simulate execution of the heavy task
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            getContext().getParent().tell(message, sender());
            e.printStackTrace();
            throw new ActorFailureException("failure of Thread.sleep");
        }
        //Randomly chose if the task was successful or not
        if(r.nextInt(101) <= successRate) {
            //if successful delegate saving to save actor
            saver.tell(new SaveImageMessage(message.getPayload(),
                    message.getResultDirectory(), message.getSender(),message.getId())
                    ,sender());
        }else {
            //if task "failed" push it as a new message in line to the router actor to not lose it and thow exception
            getContext().getParent().tell(message, sender());
            throw new ActorFailureException("compression failure simulated");
        }
    }

    public static Props props() {
        return Props.create(CompressionWorkerActor.class);
    }
}