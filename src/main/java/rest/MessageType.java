package rest;

/**
 * Created by tal on 3/25/2016.
 */
public enum MessageType {
    JoinedToRoom("JOIN"), Vote("VOTE"), Association("ASSOCIATION");

    private String description;

    MessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
