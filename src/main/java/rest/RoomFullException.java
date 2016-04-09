package rest;

public class RoomFullException extends DixitException{
    public static final String FULL_ROOM_ERROR_MESSAGE = "Room is already full";

    public RoomFullException () {
        super(FULL_ROOM_ERROR_MESSAGE, 2);
    }
}
