package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import simple.jax.rs.dto.test.User;

import java.util.List;

@Path("/sellers")
public class SellersResource {

    @GET
    public List<User> all(@QueryParam("start") int start, @QueryParam("size") int size) {
        return null;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.WILDCARD)
    public User findByIdAny(@PathParam("id") long id) {
        return null;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public User findById(@PathParam("id") long id) {
        return null;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_XML)
    public User findByIdXML(@PathParam("id") long id) {
        return null;
    }


    @Path("{id}/orders")
    public OrdersResource getOrders() {
        return new OrdersResource();
    }
}
