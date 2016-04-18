package rest;

public enum MessageType {
    JoinedToRoom("JOIN"), Vote("VOTE"), Association("ASSOCIATION"),
    PickedCard("PICKED"), DestroyRoom("Destroy");

    private String description;

    MessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
