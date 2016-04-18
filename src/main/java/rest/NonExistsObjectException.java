package rest;

public class NonExistsObjectException extends DixitException {
    private static final String NON_EXISTS_OBJECT_MESSAGE = "Room not exists";

    public NonExistsObjectException() {
        super(NON_EXISTS_OBJECT_MESSAGE, 3);
    }
}
