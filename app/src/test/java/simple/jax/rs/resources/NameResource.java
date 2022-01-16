package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/name")
public
class NameResource {

    @GET
    public String name() {
        return "name";
    }
}
