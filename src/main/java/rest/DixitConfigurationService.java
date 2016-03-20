package rest;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/service")
public class DixitConfigurationService {
    public static final String ROOM_NAME_COLUMN = "name";
    public static final String APIKey= "AIzaSyDnv6KNfOy08cZiBKVOn6yYPBo5qWaYTJY";
    //public static final String APIKey= "AIzaSyBqGEWSkT0G9cvzDunEQv8UU13ylkZj0so";

    public static final String ROOM_ID_COLUMN = "id";
    public static final String GET_ROOMS_QUERY = "select " + ROOM_NAME_COLUMN  + " from rooms";
    public static final String PLAYER_NAME_COLUMN = "player_name";
    public static final String GET_PLAYERS_IN_ROOM_QUERY = "SELECT " + PLAYER_NAME_COLUMN +
                                                            " FROM players_to_rooms WHERE " +
                                                            " room_id = ?";
    public static final String GET_ROOM_ID_QUERY = "select id from rooms where " + ROOM_NAME_COLUMN + " = ?";
    public static final String JOIN_USER_QUERY = "insert into players_to_rooms (select ?, ?,  " +
                                                 "max(player_index) + 1  from players_to_rooms where room_id = 1)";
    public static final String NEW_ROOM_QUERY = "insert into rooms (name) values (?)";

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("sendToken/")
    public void notifyToken(String token) {
        String s = token;
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("{roomName}/sendAssociation/")
    public void sendAssociation(String association , @PathParam("roomName") String roomName) {
        try {
            //sendToTopic(roomName, association);
            sendSimple(roomName,association);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("addRoom/")
    public void addRoom(String roomName) {
        try {
            PreparedStatement preparedStatement = getConnection().prepareStatement(NEW_ROOM_QUERY);
            preparedStatement.setString(1, roomName);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("join/")
    public void joinRoom(JoinRequestDTO joinRequestDTO) {
        try {
            int roomId = getRoomIdByName(joinRequestDTO.roomName);
            PreparedStatement preparedStatement = getConnection().prepareStatement(JOIN_USER_QUERY);
            preparedStatement.setInt(1, roomId);
            preparedStatement.setString(2, joinRequestDTO.nickName);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @GET
    @Path("/players/{roomName}")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<String> getPlayersInRoom(@PathParam("roomName") String roomName) {
        List<String> players = new ArrayList<String>();
        try {
            Connection con = getConnection();
            int roomId = getRoomIdByName(roomName);
            PreparedStatement preparedStatement = con.prepareStatement(GET_PLAYERS_IN_ROOM_QUERY);
            preparedStatement.setInt(1, roomId);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                players.add(rs.getString(PLAYER_NAME_COLUMN));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }

        return players;
    }

    @GET
    @Path("/rooms")
    @Produces({ MediaType.APPLICATION_JSON})
    public List<String> getRooms() {

        List<String> allRooms = new ArrayList<String>();
        try {
            Connection con = getConnection();
            ResultSet rs = con.createStatement().executeQuery(GET_ROOMS_QUERY);
            while (rs.next()) {
                 allRooms.add(rs.getString(ROOM_NAME_COLUMN));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }

        return allRooms;
//        return Response.status(200).entity(allRooms).build();
    }

    private Connection getConnection() throws NamingException, SQLException {
        Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:comp/env");
        DataSource ds = (DataSource) envCtx.lookup("jdbc/test");
        return ds.getConnection();
    }

    private int getRoomIdByName(String roomName) throws SQLException , NamingException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(GET_ROOM_ID_QUERY);
        preparedStatement.setString(1, roomName);
        ResultSet rs = preparedStatement.executeQuery();
        int roomId = -1;
        while (rs.next()) {
            roomId = rs.getInt("id");
        }

        return roomId;
    }

    private void sendSimple(String topicName, String association){
        HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead

        String json = "{ " +
                "\"to\": \"/topics/" + topicName + "\", " +
                " \"data\": { " +
                "\"message\": \"This is a GCM Topic Message!" + association + "\" " +
                "}}";

        try {
            HttpPost request = new HttpPost("https://gcm-http.googleapis.com/gcm/send");
            StringEntity params =new StringEntity(json);
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", "key="+APIKey);
            request.setEntity(params);
            httpClient.execute(request);

            // handle response here...
        }catch (Exception ex) {
            // handle exception here
        } finally {
            httpClient.getConnectionManager().shutdown(); //Deprecated
        }
    }
}