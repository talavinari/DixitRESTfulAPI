package rest;

import org.apache.commons.lang3.StringUtils;

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

    public static final int TOTAL_CARDS_NUM = 80;
    public static final int SAVE_CARD_HISTORY_THRESHOLD = 10;
    public static final String DIXIT_TABLE = " DXT_MAIN ";

    public static final String GET_ROOMS_QUERY = "select distinct " + ROOM_NAME_COLUMN + " from " + DIXIT_TABLE;
    public static final String GET_PLAYERS_IN_ROOM_QUERY = "SELECT " + PLAYER_NAME_COLUMN  + ", "
                            + PLAYER_INDEX_COLUMN +
                            " FROM " + DIXIT_TABLE + " WHERE " +
                            ROOM_NAME_COLUMN + " like ?";

    public static final String GET_ALL_CARDS_IN_ROOM = "SELECT " + CARD_COLUMN +
            " FROM " + DIXIT_TABLE + " WHERE " +
            ROOM_NAME_COLUMN + " like  ?";

    public static final String GET_CARDS_FOR_PLAYER = "SELECT " + CARD_COLUMN +
            " FROM " + DIXIT_TABLE + " WHERE " +
            PLAYER_NAME_COLUMN + " like  ?";

    public static final String UPDATE_CARDS_CONCAT = "update " + DIXIT_TABLE + " set " + CARD_COLUMN +
            " = concat(" + CARD_COLUMN + ", ?)" +
            " where " + ROOM_NAME_COLUMN + " like ? and " + PLAYER_NAME_COLUMN + " like ?";

    public static final String UPDATE_CARDS = "update " + DIXIT_TABLE + " set " + CARD_COLUMN + " =  ?" +
            " where " + ROOM_NAME_COLUMN + " like ? and " + PLAYER_NAME_COLUMN + " like ?";


    public static final String AFTER_FIRST_JOIN_ROOM_QUERY = "insert into " + DIXIT_TABLE + " (select ?, ?,  " +
            "max(player_index) + 1, ''  from " + DIXIT_TABLE + " where " + ROOM_NAME_COLUMN + " like ?)";

    public static final String FIRST_JOIN_ROOM_QUERY = "insert into " + DIXIT_TABLE +
            "values (?, ?,  1, '')";

    public static final String REMOVE_PLAYER_FROM_ROOM = "delete from " + DIXIT_TABLE + " " +
            "where " + PLAYER_NAME_COLUMN + " like ?" +
            " and  " + ROOM_NAME_COLUMN + " like ?";

    public static final String FIND_DUPLICATE_ROOM_QUERY = "select count(*) from " + DIXIT_TABLE +
            " where " + ROOM_NAME_COLUMN + " like ?";

    public static final String FIND_DUPLICATE_PLAYER_IN_ROOM_QUERY = "select count(*) " +
            " from " + DIXIT_TABLE +
            " where " + ROOM_NAME_COLUMN + " like ? " +
            " and " + PLAYER_NAME_COLUMN + " like ?";

    public static final String DUPLICATE_ROOM_NAME = "Duplicate room name";
    public static final String DUPLICATE_PLAYER_NAME = "Duplicate player name";

    private static final int DIXIT_NUMBER_OF_CARDS_IN_HAND = 6;
    private Connection connection;
    public static Random rnd = new Random();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sendAssociation/")
    public String sendAssociation(AssociationNotifyDTO associationNotifyDTO) {
        try {
            sortCardsInDB(associationNotifyDTO.winningCard, associationNotifyDTO.basicInfo);
            PublishUtils.publishAssociation(associationNotifyDTO);
            List<String> randomCards = getRandomCards
                    (new CardPickedNotifyDTO(associationNotifyDTO.basicInfo, 1));
            JsonObject jsonMessage = Json.createObjectBuilder()
                    .add("newCard", randomCards.get(0)).build();
            return jsonMessage.toString();
        } catch (Exception e) {
            return createJSONErrorMessage(e.getMessage());
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("sendPickedCard/")
    public String sendPickedCard(AssociationNotifyDTO dto) {
        try {
            sortCardsInDB(dto.winningCard, dto.basicInfo);
            PublishUtils.publishPickedCard(dto);
            return "";
        } catch (Exception e) {
            return createJSONErrorMessage(e.getMessage());
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("vote/")
    public String vote(VoteDTO vote ) {
        try {
            PublishUtils.publishVote(vote);
            List<String> randomCards = getRandomCards(new CardPickedNotifyDTO(vote.basicInfo, 1));
            JsonObject jsonMessage = Json.createObjectBuilder()
                    .add("newCard", randomCards.get(0)).build();
            return jsonMessage.toString();
        } catch (Exception e) {
            return createJSONErrorMessage(e.getMessage());
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("addRoom/")
    public String addRoom(BasicRequestDTO dto) {
        try {

            checkIfRoomNameExists(dto.roomName);
            PreparedStatement preparedStatement = getConnection().prepareStatement(FIRST_JOIN_ROOM_QUERY);
            preparedStatement.setString(1, dto.roomName);
            preparedStatement.setString(2, dto.nickName);
            preparedStatement.execute();
            preparedStatement.close();
            List<String> cards = getRandomCards(new CardPickedNotifyDTO(dto, 6));
            String cardsString = StringUtils.join(cards, ",");
            return Json.createObjectBuilder()
                    .add("cards", cardsString).build().toString();

        } catch (Exception e) {
            return createJSONErrorMessage(e.getMessage());
        }
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
            preparedStatement.close();
        } catch (Exception ignored) {

        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("join/")
    public String joinRoom(BasicRequestDTO dto) {
        try {
            checkIfPlayerNameExistsInRoom(dto);
            PreparedStatement preparedStatement = getConnection().prepareStatement(AFTER_FIRST_JOIN_ROOM_QUERY);
            preparedStatement.setString(1, dto.roomName);
            preparedStatement.setString(2, dto.nickName);
            preparedStatement.setString(3, dto.roomName);
            preparedStatement.execute();
            preparedStatement.close();
            PublishUtils.publishUserJoined(dto, getPlayerIndex(dto));
            List<String> cards = getRandomCards(new CardPickedNotifyDTO(dto, 6));
            return createJoinRoomReturnMessage(dto, cards);
        } catch (Exception e) {
            return createJSONErrorMessage(e.getMessage());
        }
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
            rs.close();

            // TODO change produce to json and handle it in android
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }

        return allRooms;
    }

    private Connection getConnection() throws NamingException, SQLException {
        if (connection == null) {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            DataSource ds = (DataSource) envCtx.lookup("jdbc/test");
            connection = ds.getConnection();
        }

        return connection;
    }


    private String parseUrl(String url) {
        return url.replace('+', ' ');
    }

    private List<String> getRandomCards(CardPickedNotifyDTO cardRequestDTO) throws Exception {
        List<String> cards = getRandomCardsList(cardRequestDTO);
        updateCardsToDB(cards,cardRequestDTO.basicInfo);
        return cards;
    }

    private List<String> getRandomCardsList(CardPickedNotifyDTO cardRequestDTO) throws SQLException, NamingException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(GET_ALL_CARDS_IN_ROOM);
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
        preparedStatement.close();

        List<String> newCards = new ArrayList<String>();

        for (int i = 0; i < cardRequestDTO.cardNumberRequest; i++) {
            boolean found = false;
            while (!found) {
                int randomCard = rnd.nextInt(TOTAL_CARDS_NUM);
                if (!existingNumbers.contains(randomCard)) {
                    found = true;
                    newCards.add(String.valueOf(randomCard));
                    existingNumbers.add(randomCard);
                }
            }
        }
        return newCards;
    }

    private void updateCardsToDB(List<String> newCards, BasicRequestDTO basicRequestDTO) {
        String cardsInDB = StringUtils.join(newCards, ',');
        String sql;
        if (newCards.size() == 1){
            sql = UPDATE_CARDS_CONCAT;
            cardsInDB =  ", " + cardsInDB;
        }
        else{
            sql = UPDATE_CARDS;
        }
        try {
            handleDB(basicRequestDTO, cardsInDB, sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    private void handleDB(BasicRequestDTO basicRequestDTO, String cardsInDB, String sql) throws SQLException, NamingException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
        preparedStatement.setString(1, cardsInDB);
        preparedStatement.setString(2, basicRequestDTO.roomName);
        preparedStatement.setString(3, basicRequestDTO.nickName);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }


    private void sortCardsInDB(String winningCard, BasicRequestDTO dto) throws Exception {
        PreparedStatement preparedStatement = getConnection().prepareStatement(GET_CARDS_FOR_PLAYER);
        preparedStatement.setString(1, dto.nickName);
        ResultSet rs = preparedStatement.executeQuery();
        List<String> cards = new ArrayList<String>();
        cards.add("dummy");
        while (rs.next()) {
            String string = rs.getString(CARD_COLUMN);
            String[] split = string.split(",");
            for (String s : split) {
                if (!s.trim().equals("")) {
                    cards.add(s.trim());
                }
            }
        }

        preparedStatement.close();

        if (cards.size() > SAVE_CARD_HISTORY_THRESHOLD){
            cards.remove(cards.size() - (1 + DIXIT_NUMBER_OF_CARDS_IN_HAND));
        }

        Collections.swap(cards, cards.indexOf(winningCard) , 0);
        cards.remove("dummy");
        updateCardsToDB(cards, dto);
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

    private JsonArray buildPlayersArray(BasicRequestDTO dto) {
        List<Player> players = getPlayersInRoomWithIndex(dto.roomName);

        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (Player player : players){
            jsonArrayBuilder.add(Json.createObjectBuilder()
                    .add("name", player.name)
                    .add("index", player.index));
        }

        JsonArray arrayOfPlayers = jsonArrayBuilder.build();
        Json.createObjectBuilder().add("players", arrayOfPlayers);
        return arrayOfPlayers;
    }

    private String createJoinRoomReturnMessage(BasicRequestDTO dto, List<String> cards) throws Exception{
        JsonArray arrayOfPlayers = buildPlayersArray(dto);

        String cardsString = StringUtils.join(cards, ",");

        JsonObject jsonMessage = Json.createObjectBuilder()
                .add("players", arrayOfPlayers)
                .add("cards", cardsString).build();
        return jsonMessage.toString();
    }


    private List<Player> getPlayersInRoomWithIndex(String roomName) {
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

            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }

        return players;
    }


    private String createJSONErrorMessage(String error) {
        return Json.createObjectBuilder().
                add("error",  error).build().toString(
        );
    }

    private void checkIfRoomNameExists(String roomName) throws SQLException, NamingException, DuplicateNameException {
        PreparedStatement preparedStatement = getConnection().prepareStatement(FIND_DUPLICATE_ROOM_QUERY);
        preparedStatement.setString(1, roomName);
        ResultSet rs = preparedStatement.executeQuery();
        int count = 0;
        while (rs.next()) {
            count = rs.getInt(1);
        }

        preparedStatement.close();

        if (count >= 1){
            throw new DuplicateNameException(DUPLICATE_ROOM_NAME);
        }
    }

    private void checkIfPlayerNameExistsInRoom(BasicRequestDTO dto) throws SQLException, NamingException, DuplicateNameException {
        PreparedStatement preparedStatement = getConnection().
                prepareStatement(FIND_DUPLICATE_PLAYER_IN_ROOM_QUERY);
        preparedStatement.setString(1, dto.roomName);
        preparedStatement.setString(2, dto.nickName);

        ResultSet rs = preparedStatement.executeQuery();
        int count = 0;
        while (rs.next()) {
            count = rs.getInt(1);
        }

        preparedStatement.close();
        if (count == 1){
            throw new DuplicateNameException(DUPLICATE_PLAYER_NAME);
        }
    }


}