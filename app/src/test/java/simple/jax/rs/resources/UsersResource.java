package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import simple.jax.rs.dto.test.User;

import java.util.ArrayList;
import java.util.List;

@Path("/users")
public class UsersResource {

    @GET
    public List<User> all(@QueryParam("start") int start, @QueryParam("size") int size) {
        ArrayList<User> users = new ArrayList<>();
        users.add(new User());
        return users;
    }

    @GET
    @Path("{id}")
    public User findById(@PathParam("id") long id) {
        return null;
    }

    @Path("{id}/orders")
    public OrdersResource getOrders() {
        return new OrdersResource();
    }
}
