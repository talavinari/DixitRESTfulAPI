package rest;

public class DuplicateNameException extends DixitException {
    public DuplicateNameException(String message) {
        super(message, 1);
    }
}
