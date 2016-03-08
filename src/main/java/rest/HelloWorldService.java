package rest;


import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Path("/hello")
public class HelloWorldService {

    @GET
    @Path("/{param}")
    public Response getMsg(@PathParam("param") String msg) {
        Context initCtx = null;
        String value = "Empty";
        try {
            initCtx = new InitialContext();
            Context envCtx = null;
            envCtx = (Context) initCtx.lookup("java:comp/env");
            DataSource ds = null;
            ds = (DataSource)
                    envCtx.lookup("jdbc/test");
            ResultSet rs = ds.getConnection().createStatement().executeQuery("select message from tal");
            while (rs.next()) {
                value = rs.getString("MESSAGE");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }

        String output = "Tal say : " + value;
        return Response.status(200).entity(output).build();
    }
}