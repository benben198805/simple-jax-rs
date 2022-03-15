package simple.jax.rs.resources;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/buyers/none")
public class BuyersWithoutConsumeResource {
    @POST
    public void consumeAsNone() {
    }
}
