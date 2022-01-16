package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/users")
public class UserResource {

    @GET
    public String users() {
        return "John";
    }
}
