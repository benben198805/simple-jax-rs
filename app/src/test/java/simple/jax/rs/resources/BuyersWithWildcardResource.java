package simple.jax.rs.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

@Path("/buyers/wildcard")
public class BuyersWithWildcardResource {
    @POST
    @Consumes(MediaType.WILDCARD)
    public void consumeAsWildcard() {
    }
}
