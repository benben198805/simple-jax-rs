package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import simple.jax.rs.dto.test.Order;

import java.util.List;

public class OrdersResource {

    @GET
    public List<Order> all() {
        return null;
    }

    @GET
    @Path("{id}")
    public Order findById(@PathParam("id") long id) {
        return null;
    }
}
