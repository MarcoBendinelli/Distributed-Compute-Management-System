package server.actors;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import akka.routing.BalancingPool;
import server.actors.workers.CompressionWorkerActor;
import server.actors.workers.NumberWorkerActor;
import server.actors.workers.TextWorkerActor;
import server.beans.TaskEntity;
import server.enums.StatusEnum;
import server.messages.data.AskUserTasksMessage;
import server.messages.status.CompleteMessage;
import server.messages.tasks.ImageCompressionTask;
import server.messages.data.ResponseUserTasksMessage;
import server.messages.tasks.NumberSumTask;
import server.exceptions.ActorFailureException;
import server.messages.tasks.TextFormattingTask;

import java.time.Duration;
import java.util.*;

/**
 * This is the main server actor
 * It receives tasks messages from the client handler and forwards them to routers actors that will send
 * them to the workers using a balancing strategy
 * It also stores data about which tasks have been completed in a map simulating a database
 *
 */
public class ServerActor extends AbstractActor {

    //key one username, key 2 taskID, value = class representing a task
    private final Map<String, Map<Integer, TaskEntity>> db_tasks = new HashMap<>();
    //id incremented for each new task and unequivocally associated with one (simulating a database)
    private int taskID_db = 0;
    //number of worker actors for each task type
    final int nOfWorkers = 2;
    //list of routers used to handle the distribution of tasks to the workers (each router handles one type of task)
    private final List<ActorRef> routers = new ArrayList<>();
    //when a worker child actor fails this defined strategy is used to handle it
    private static final SupervisorStrategy strategy =
            new OneForOneStrategy(
                    5,
                    Duration.ofMinutes(1),
                    DeciderBuilder.match(ActorFailureException.class, e-> SupervisorStrategy.resume()).build());


    /**
     * One router is instantiated for each type of worker in order to avoid bottleneck on long task like image compression
     * Routers are also the parent of each worker actor and supervise them
     */
    public ServerActor() {
        //create routers handling n workers with a specific strategy in case of failure and
        routers.add(getContext().actorOf(new BalancingPool(nOfWorkers).withSupervisorStrategy(strategy).props(CompressionWorkerActor.props()), "ImageCompressionRouter"));
        routers.add(getContext().actorOf(new BalancingPool(nOfWorkers).withSupervisorStrategy(strategy).props(TextWorkerActor.props()), "TextFormattingRouter"));
        routers.add(getContext().actorOf(new BalancingPool(nOfWorkers).withSupervisorStrategy(strategy).props(NumberWorkerActor.props()), "NumberSumRouter"));

    }

    //define the messages the actor will handle and how
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ImageCompressionTask.class, this::onImageCompressionTask)
                .match(TextFormattingTask.class, this::onTextFormattingTask)
                .match(NumberSumTask.class, this::onNumberSumTask)
                .match(CompleteMessage.class,this::onCompleteMessage)
                .match(AskUserTasksMessage.class,this::onAskTasksMessage)
                .build();
    }

    /**
     * Method used when the client needs data about its tasks
     * It sends a response message containing the list of taskEntity
     * @param message message containing the username of who request the data
     */
    private void onAskTasksMessage(AskUserTasksMessage message) {
        Map<Integer,TaskEntity> userTasks = db_tasks.get(message.getUsername());
        Collection<TaskEntity> listOfUserTasks = null;
        if(userTasks!=null){
            listOfUserTasks = userTasks.values();
        }
        ResponseUserTasksMessage response = new ResponseUserTasksMessage(listOfUserTasks);
        sender().tell(response,self());
    }

    /**
     * This method is called when a complete message is received from a savingActor
     * this happens when a task has been both successfully processed and saved in the requested directory
     * Or it has permanently failed due to too many unsuccessful saving attempts
     * The task entity corresponding to the task is updated as done/failed
     *
     * @param completeMessage message from the saving actor
     */
    private void onCompleteMessage(CompleteMessage completeMessage) {
        Map<Integer, TaskEntity> userTasks = db_tasks.get(completeMessage.getSender());
        //update as completed
        userTasks.get(completeMessage.getId()).updateStatus(completeMessage.getStatus());
        System.out.println(completeMessage.getStatus()+": "+ completeMessage.getSender()+" "+completeMessage.getId());
    }

    /**
     * This method is called after a task for image compressing is requested by the client
     * The message is sent to the router handling that type of task that will forward it to one of its child workers
     * @param message task containing the data needed for the task plus the sender username and the task unique id
     */
    private void onImageCompressionTask(ImageCompressionTask message) {
        //if first time message add it to the list

            message.setId(taskID_db);
            Map<Integer, TaskEntity> userTasks = db_tasks.get(message.getSender());
            TaskEntity taskEntity = new TaskEntity("Compression", taskID_db, StatusEnum.PROCESSING);
            saveInDB(userTasks, taskEntity, message.getSender());

        taskID_db++;
        routers.get(0).tell(message,self());
    }

    /**
     * This method is called after a task for text formatting is requested by the client
     * The message is sent to the router handling that type of task that will forward it to one of its child workers
     * @param message task containing the data needed for the task plus the sender username and the task unique id
     */
    private void onTextFormattingTask (TextFormattingTask message) {
        message.setId(taskID_db);
        Map<Integer, TaskEntity> userTasks = db_tasks.get(message.getSender());
        TaskEntity taskEntity = new TaskEntity("Format", taskID_db, StatusEnum.PROCESSING);
        saveInDB(userTasks, taskEntity, message.getSender());
        taskID_db++;
        routers.get(1).tell(message,self());
    }


    /**
     * This method is called after a task for summing two numbers is requested by the client
     * The message is sent to the router handling that type of task that will forward it to one of its child workers
     * @param message task containing the data needed for the task plus the sender username and the task unique id
     */
    private void onNumberSumTask (NumberSumTask message) {
        message.setId(taskID_db);
        Map<Integer, TaskEntity> userTasks = db_tasks.get(message.getSender());
        TaskEntity taskEntity = new TaskEntity("Sum", taskID_db, StatusEnum.PROCESSING);
        saveInDB(userTasks, taskEntity, message.getSender());
        taskID_db++;
        routers.get(2).tell(message,self());
    }

    /**
     * This method is called to save a new task entity in the "database"
     * this happens every time a new task is requested by one client
     * @param userTasks tasks associated with that user (could be null if first task ever)
     * @param taskEntity Entity containing the new task data
     * @param sender username of the user requesting the task
     */
    private void saveInDB(Map<Integer, TaskEntity> userTasks, TaskEntity taskEntity, String sender) {
        Map<Integer,TaskEntity> taskData = new HashMap<>();
        taskData.put(taskEntity.getId(),taskEntity);
        if(userTasks!=null){
            //add new pending task to the user
            userTasks.put(taskEntity.getId(),taskEntity);
        }else {
            //add new user with this task as pending
            db_tasks.put(sender, taskData);
        }
    }


    /**
     * Classic props method to keep method to create the actor as close as possible to the actor class
     * @return Props for serverActor
     */
    public static Props props() {
        return Props.create(ServerActor.class);
    }
}
