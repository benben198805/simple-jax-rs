package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/sellers")
public class SellersResource {
    @GET
    @Produces(MediaType.WILDCARD)
    public String getAsWildCard() {
        return "get as WILDCARD";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getAsJson() {
        return "get as APPLICATION_JSON";
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML})
    public String getAsXml() {
        return "get as XML";
    }
}
