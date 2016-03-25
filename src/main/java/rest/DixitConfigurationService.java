package rest;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
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
    public static final String PLAYER_INDEX_COLUMN = "player_index";

    public static final int TOTAL_CARDS_NUM = 256;
    public static final String DIXIT_TABLE = " DXT_MAIN ";

    //public static final String APIKey= "AIzaSyBqGEWSkT0G9cvzDunEQv8UU13ylkZj0so";

    public static final String GET_ROOMS_QUERY = "select distinct " + ROOM_NAME_COLUMN + " from " + DIXIT_TABLE;
    public static final String GET_PLAYERS_IN_ROOM_QUERY = "SELECT " + PLAYER_NAME_COLUMN  + ", "
                            + PLAYER_INDEX_COLUMN +
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sendAssociation/")
    public String sendAssociation(AssociationNotifyDTO associationNotifyDTO) {
        try {
            PublishUtils.publishAssociation(associationNotifyDTO);
            List<String> randomCards = getRandomCards
                    (new CardRequestDTO(associationNotifyDTO.basicInfo, 1));
            return randomCards.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("vote/")
    public String vote(VoteDTO associationNotifyDTO) {
        try {
            PublishUtils.publishVote(associationNotifyDTO);
            List<String> randomCards = getRandomCards(new CardRequestDTO(associationNotifyDTO.basicInfo, 1));
            return randomCards.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("removePlayer/")
    public void removePlayer(BasicRequestDTO requestDTO) {
        try {
            PreparedStatement preparedStatement = getConnection().prepareStatement(REMOVE_PLAYER_FROM_ROOM);
            preparedStatement.setString(1, requestDTO.nickName);
            preparedStatement.setString(2, requestDTO.roomName);
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
    public String joinRoom(BasicRequestDTO dto) {
        try {
            PreparedStatement preparedStatement = getConnection().prepareStatement(AFTER_FIRST_JOIN_ROOM_QUERY);
            preparedStatement.setString(1, dto.roomName);
            preparedStatement.setString(2, dto.nickName);
            preparedStatement.setString(3, dto.roomName);
            preparedStatement.execute();
            PublishUtils.publishUserJoined(dto, getPlayerIndex(dto));
            List<String> cards = getRandomCards(new CardRequestDTO(dto, 6));
            return createJoinRoomReturnMessage(dto, cards);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getPlayerIndex(BasicRequestDTO dto) {
        List<Player> players = getPlayersInRoomWithIndex(dto.roomName);

        for (Player player : players){
            if (player.name.equals(dto.nickName)){
                return player.index;
            }
        }

        return null;
    }

    private String createJoinRoomReturnMessage(BasicRequestDTO dto, List<String> cards) throws Exception{
        List<Player> players = getPlayersInRoomWithIndex(dto.roomName);
        String cardsString = StringUtils.join(cards, ",");

        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (Player player : players){
            jsonArrayBuilder.add(Json.createObjectBuilder()
                                .add("name", player.name)
                                .add("index", player.index));
        }

        JsonArray arrayOfPlayers = jsonArrayBuilder.build();
        Json.createObjectBuilder().add("players", arrayOfPlayers);

        JsonObject jsonMessage = Json.createObjectBuilder()
                .add("players", arrayOfPlayers)
                .add("cards", cardsString).build();
        return jsonMessage.toString();
    }


    @GET
    @Path("players/{roomName}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<String> getPlayersInRoom(@PathParam("roomName") String roomName) {
        List<Player> Players = getPlayersInRoomWithIndex(roomName);
        List<String> names = new ArrayList<String>();

        for (Player p : Players){
            names.add(p.name);
        }

        return names;
    }

    private List<Player> getPlayersInRoomWithIndex(@PathParam("roomName") String roomName) {
        roomName = parseUrl(roomName);
        List<Player> players = new ArrayList<Player>();
        try {
            Connection con = getConnection();
            PreparedStatement preparedStatement = con.prepareStatement(GET_PLAYERS_IN_ROOM_QUERY);
            preparedStatement.setString(1, roomName);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                players.add(new Player(rs.getString(PLAYER_NAME_COLUMN),
                                      rs.getString(PLAYER_INDEX_COLUMN)));
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


    private String parseUrl(String url) {
        return url.replace('+', ' ');
    }

    private List<String> getRandomCards(CardRequestDTO cardRequestDTO) throws Exception {
        List<String> cards = getRandomCardsList(cardRequestDTO);
        updateCardToDB(cards,cardRequestDTO.basicInfo);
        return cards;
    }

    private List<String> getRandomCardsList(CardRequestDTO cardRequestDTO) throws SQLException, NamingException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(GET_CARD_FOR_PLAYER);
        preparedStatement.setString(1, cardRequestDTO.basicInfo.roomName);
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

        List<String> newCards = new ArrayList<String>();

        for (int i = 0; i < cardRequestDTO.cardNumberRequest; i++) {
            boolean found = false;
            while (!found) {
                int randomCard = rnd.nextInt(TOTAL_CARDS_NUM);
                if (!existingNumbers.contains(randomCard)) {
                    found = true;
                }
                newCards.add(String.valueOf(randomCard));
            }
        }
        return newCards;
    }

    private void updateCardToDB(List<String> newCards, BasicRequestDTO basicRequestDTO) {
        String cardsInDB = StringUtils.join(newCards, ',');

        try {
            PreparedStatement preparedStatement = getConnection().prepareStatement(UPDATE_CARDS);
            preparedStatement.setString(1, cardsInDB);
            preparedStatement.setString(2, basicRequestDTO.roomName);
            preparedStatement.setString(3, basicRequestDTO.nickName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
}