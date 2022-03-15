package simple.jax.rs.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

@Path("/buyers")
public class BuyersResource {
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public void consumeAsXml(XmlBuyer buyer) {

    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void consumeAsForm(FormBuyer buyer) {

    }

    public class FormBuyer {
        public String name;
    }

    public class XmlBuyer {
        public String name;
    }
}
