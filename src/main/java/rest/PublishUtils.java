package rest;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jettison.json.JSONException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class PublishUtils {
    public static final String GOOGLE_API_NOTIFICATION_URL = "https://gcm-http.googleapis.com/gcm/send";
    public static final String APIKey = "AIzaSyDnv6KNfOy08cZiBKVOn6yYPBo5qWaYTJY";
    public static final String AuthenticationKey = "key=" + APIKey;

    public static final String PLAYER_NAME = "playerName";
    public static final String WINNING_CARD = "winningCard";
    public static final String VOTED_CARD = "card";
    public static final String ASSOCIATION = "association";
    public static final String MESSAGE_TYPE = "messageType";
    public static final String INDEX = "index";


    public static void publishVote(VoteDTO voteObject) throws JSONException {
        JsonObjectBuilder data =  Json.createObjectBuilder()
                .add(MESSAGE_TYPE, MessageType.Vote.getDescription())
                .add(PLAYER_NAME, voteObject.basicInfo.nickName)
                .add(VOTED_CARD, voteObject.card);
        publishMessage(voteObject.basicInfo.roomName, data);
    }

    public static void publishRoomDestroy(BasicRequestDTO dto) throws JSONException{
        JsonObjectBuilder data =  Json.createObjectBuilder()
                .add(MESSAGE_TYPE, MessageType.DestroyRoom.getDescription())
                .add(PLAYER_NAME, dto.nickName);
        publishMessage(dto.roomName, data);
    }
    public static void publishUserJoined(BasicRequestDTO dto, String index) throws JSONException {
        JsonObjectBuilder data =  Json.createObjectBuilder()
                .add(MESSAGE_TYPE, MessageType.JoinedToRoom.getDescription())
                .add(PLAYER_NAME, dto.nickName)
                .add(INDEX, index);
        publishMessage(dto.roomName, data);
    }

    public static void publishPickedCard(AssociationNotifyDTO dto) throws JSONException, UnsupportedEncodingException {
        handleGenericAssociationNotification(dto, MessageType.PickedCard);
    }

    public static  void publishAssociation(AssociationNotifyDTO dto) throws JSONException, UnsupportedEncodingException {
        handleGenericAssociationNotification(dto, MessageType.Association);
    }

    private static void handleGenericAssociationNotification(AssociationNotifyDTO dto, MessageType messageType) throws JSONException, UnsupportedEncodingException {
        JsonObjectBuilder data =  Json.createObjectBuilder()
                .add(MESSAGE_TYPE, messageType.getDescription())
                .add(PLAYER_NAME, dto.basicInfo.nickName)
                .add(WINNING_CARD, dto.winningCard)
                .add(ASSOCIATION, URLEncoder.encode(dto.association,"UTF8"));
        publishMessage(dto.basicInfo.roomName, data);
    }

    private static void publishMessage(String topicName, JsonObjectBuilder jsonObjectBuilder) throws JSONException {
        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            JsonObject value = Json.createObjectBuilder()
                    .add("to", "/topics/"+ topicName)
                    .add("data", jsonObjectBuilder).build();

            HttpPost request = new HttpPost(GOOGLE_API_NOTIFICATION_URL);
            StringEntity params = new StringEntity(value.toString());
            request.addHeader("content-type", MediaType.APPLICATION_JSON);
            request.addHeader("Authorization", AuthenticationKey);
            request.setEntity(params);
            httpClient.execute(request);

        } catch (Exception ignored) {
        } finally {
            httpClient.getConnectionManager().shutdown(); //Deprecated
        }
    }
}
