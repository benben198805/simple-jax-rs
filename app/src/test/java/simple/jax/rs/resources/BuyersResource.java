package simple.jax.rs.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

@Path("/buyers")
public class BuyersResource {
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public String consumeAsXml() {
        return "get as APPLICATION_XML";
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void consumeAsForm() {

    }
}
