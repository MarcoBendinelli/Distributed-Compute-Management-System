package server.exceptions;

/**
 * Exception thrown from actors when failing
 */
public class ActorFailureException extends Exception{
    public ActorFailureException(String message) {
        super(message);
    }
}
