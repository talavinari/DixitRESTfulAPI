package rest;

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
import java.util.List;

@Path("/service")
public class DixitConfigurationService {
    public static final String ROOM_NAME_COLUMN = "name";
    public static final String PLAYER_NAME_COLUMN = "name";
    public static final String GET_PLAYERS_IN_ROOM_QUERY = "select p." + PLAYER_NAME_COLUMN +
                                                            " from players_to_rooms ptr, players p where ptr.player_id = p.id " +
                                                            " AND room_id = ?";
    public static final String GET_ROOM_ID_QUERY = "select id from rooms where " + ROOM_NAME_COLUMN + " = ?";

    @POST
    @Consumes("application/json")
    @Path("try/")
    public void joinRoom(DTO example) {
        String s = example.name;
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

    @POST
    @Consumes("application/json")
    @Path("join/")
    public void joinRoom(JoinRequestDTO joinRequestDTO) {
        String insertSql = "insert into users_to_rooms values (?, ?)";
        try {
            int roomId = getRoomIdByName(joinRequestDTO.roomName);
            PreparedStatement preparedStatement = getConnection().prepareStatement(insertSql);
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
    @Produces({ MediaType.APPLICATION_XML})
    public ResponseList getPlayersInRoom(@PathParam("roomName") String roomName) {
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

        ResponseList responseList = new ResponseList();
        responseList.setList(players);
        return responseList;
    }

    @GET
    @Path("/rooms")
    @Produces({ MediaType.APPLICATION_XML})
    public ResponseList getRooms() {

        List<String> allRooms = new ArrayList<String>();
        try {
            Connection con = getConnection();
            ResultSet rs = con.createStatement().executeQuery("select " + ROOM_NAME_COLUMN + " from rooms");
            while (rs.next()) {
                 allRooms.add(rs.getString(ROOM_NAME_COLUMN));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }

        ResponseList responseList = new ResponseList();
        responseList.setList(allRooms);
        return responseList;
//        return Response.status(200).entity(allRooms).build();
    }

    private Connection getConnection() throws NamingException, SQLException {
        Context initCtx = new InitialContext();
        Context envCtx = (Context) initCtx.lookup("java:comp/env");
        DataSource ds = (DataSource) envCtx.lookup("jdbc/test");
        return ds.getConnection();
    }
}