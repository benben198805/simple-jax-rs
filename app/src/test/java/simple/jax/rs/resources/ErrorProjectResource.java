package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/error-projects")
public class ErrorProjectResource {

    @GET
    @Path("members")
    public MemberResource getMember() {
        return new MemberResource();
    }
}
