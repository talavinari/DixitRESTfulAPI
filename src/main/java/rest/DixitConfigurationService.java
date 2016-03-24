package rest;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import org.codehaus.jettison.json.JSONException;

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
import java.util.*;

@Path("/service")
public class DixitConfigurationService {
    public static final String ROOM_NAME_COLUMN = "room_name";
    public static final String CARD_COLUMN = "cards";
    public static final String PLAYER_NAME_COLUMN = "player_name";

    public static final int TOTAL_CARDS_NUM = 256;
    public static final String DIXIT_TABLE = " DXT_MAIN ";


    public static final String APIKey = "AIzaSyDnv6KNfOy08cZiBKVOn6yYPBo5qWaYTJY";
    //public static final String APIKey= "AIzaSyBqGEWSkT0G9cvzDunEQv8UU13ylkZj0so";
    public static final String GET_ROOMS_QUERY = "select distinct " + ROOM_NAME_COLUMN + " from " + DIXIT_TABLE + " group by " + ROOM_NAME_COLUMN + " having count(*) < 4";
    public static final String GET_PLAYERS_IN_ROOM_QUERY = "SELECT " + PLAYER_NAME_COLUMN +
                            " FROM " + DIXIT_TABLE + " WHERE " +
                            ROOM_NAME_COLUMN + " like ?";

    public static final String GET_CARD_FOR_PLAYER = "SELECT " + CARD_COLUMN +
            " FROM " + DIXIT_TABLE + " WHERE " +
            ROOM_NAME_COLUMN + " like  ?";
    public static final String UPDATE_CARDS = "update " + DIXIT_TABLE + " set " + CARD_COLUMN + " = ?" +
            " where " + ROOM_NAME_COLUMN + " like ? and " + PLAYER_NAME_COLUMN + " like ?";
    public static final String AFTER_FIRST_JOIN_ROOM_QUERY = "insert into " + DIXIT_TABLE + " (select ?, ?,  " +
            "max(player_index) + 1, ''  from " + DIXIT_TABLE + " where " + ROOM_NAME_COLUMN + " like ?)";

    public static final String FIRST_JOIN_ROOM_QUERY = "insert into " + DIXIT_TABLE +
            "values (?, ?,  1, '')";

    public static final String REMOVE_PLAYER_FROM_ROOM = "delete from " + DIXIT_TABLE + " " +
            "where " + PLAYER_NAME_COLUMN + " like ?" +
            " and  " + ROOM_NAME_COLUMN + " like ?";

    public static Random rnd = new Random();

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("sendToken/")
    public void notifyToken(String token) {
        String s = token;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("sendAssociation/")
    public void sendAssociation(AssociationNotifyDTO associationNotifyDTO) {
        try {
            sendSimple(associationNotifyDTO.requestBasicDTO.roomName
                    , associationNotifyDTO.association);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("addRoom/")
    public List<String> addRoom(BasicRequestDTO dto) {
        try {
            PreparedStatement preparedStatement = getConnection().prepareStatement(FIRST_JOIN_ROOM_QUERY);
            preparedStatement.setString(1, dto.roomName);
            preparedStatement.setString(2, dto.nickName);
            preparedStatement.execute();
            return getRandomCards(new CardRequestDTO(dto, 6));
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("removePlayer/")
    public void removePlayer(BasicRequestDTO jrdto) {
        try {
            PreparedStatement preparedStatement = getConnection().prepareStatement(REMOVE_PLAYER_FROM_ROOM);
            preparedStatement.setString(1, jrdto.nickName);
            preparedStatement.setString(2, jrdto.roomName);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("join/")
    public List<String> joinRoom(BasicRequestDTO dto) {
        try {
            PreparedStatement preparedStatement = getConnection().prepareStatement(AFTER_FIRST_JOIN_ROOM_QUERY);
            preparedStatement.setString(1, dto.roomName);
            preparedStatement.setString(2, dto.nickName);
            preparedStatement.setString(3, dto.roomName);
            preparedStatement.execute();
            return getRandomCards(new CardRequestDTO(dto, 6));
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @GET
    @Path("players/{roomName}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<String> getPlayersInRoom(@PathParam("roomName") String roomName) {
        roomName = parseUrl(roomName);
        List<String> players = new ArrayList<String>();
        try {
            Connection con = getConnection();
            PreparedStatement preparedStatement = con.prepareStatement(GET_PLAYERS_IN_ROOM_QUERY);
            preparedStatement.setString(1, roomName);
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
    @Produces({MediaType.APPLICATION_JSON})
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
    }

    private Connection getConnection() throws NamingException, SQLException {
        Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:comp/env");
        DataSource ds = (DataSource) envCtx.lookup("jdbc/test");
        return ds.getConnection();
    }

    private void sendSimple(String topicName, String association) {
        HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead

        String json = "{ " +
                "\"to\": \"/topics/" + topicName + "\", " +
                " \"data\": { " +
                "\"message\": \"This is a GCM Topic Message!" + association + "\" " +
                "}}";

        try {
            HttpPost request = new HttpPost("https://gcm-http.googleapis.com/gcm/send");
            StringEntity params = new StringEntity(json);
            request.addHeader("content-type", MediaType.APPLICATION_JSON);
            request.addHeader("Authorization", "key=" + APIKey);
            request.setEntity(params);
            httpClient.execute(request);

            // handle response here...
        } catch (Exception ex) {
            // handle exception here
        } finally {
            httpClient.getConnectionManager().shutdown(); //Deprecated
        }
    }

    private String parseUrl(String url) {
        return url.replace('+', ' ');
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("cards/")
    public List<String> getRandomCards(CardRequestDTO cardRequestDTO) throws SQLException, NamingException, JSONException {
        List<String> cards = new ArrayList<String>();

        PreparedStatement preparedStatement = getConnection().prepareStatement(GET_CARD_FOR_PLAYER);
        preparedStatement.setString(1, cardRequestDTO.requestBasicDTO.roomName);
        ResultSet rs = preparedStatement.executeQuery();
        Set<Integer> existingNumbers = new HashSet<Integer>();
        while (rs.next()) {
            String string = rs.getString(CARD_COLUMN);
            String[] split = string.split(",");
            for (String s : split) {
                if (!s.trim().equals("")) {
                    existingNumbers.add(Integer.valueOf(s.trim()));
                }
            }
        }

        int[] newCards = new int[cardRequestDTO.cardNumberRequest];
        for (int i = 0; i < cardRequestDTO.cardNumberRequest; i++) {
            boolean found = false;
            while (!found) {
                int randomCard = rnd.nextInt(TOTAL_CARDS_NUM);
                if (!existingNumbers.contains(randomCard)) {
                    found = true;
                }
                newCards[i] = randomCard;
                cards.add(String.valueOf(randomCard));
            }

        }

        updateCardToDB(newCards,
                cardRequestDTO.requestBasicDTO.roomName,
                cardRequestDTO.requestBasicDTO.nickName);

        return cards;
    }

    private void updateCardToDB(int[] newCards, String roomName, String userName) {
        String cardInDb = Arrays.toString(newCards);
        cardInDb = cardInDb.replace("[", "").replace("]", "");

        try {
            PreparedStatement preparedStatement = getConnection().prepareStatement(UPDATE_CARDS);
            preparedStatement.setString(1, cardInDb);
            preparedStatement.setString(2, roomName);
            preparedStatement.setString(3, userName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
}