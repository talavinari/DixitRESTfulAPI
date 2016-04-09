package rest;

public class DixitException extends Exception {

    public int errorCode;

    public DixitException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
