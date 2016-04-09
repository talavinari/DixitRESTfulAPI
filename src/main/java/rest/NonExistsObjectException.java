package rest;

/**
 * Created by tal on 4/9/2016.
 */
public class NonExistsObjectException extends DixitException {
    private static final String NON_EXISTS_OBJECT_MESSAGE = "Room not exists";

    public NonExistsObjectException() {
        super(NON_EXISTS_OBJECT_MESSAGE, 3);
    }
}
