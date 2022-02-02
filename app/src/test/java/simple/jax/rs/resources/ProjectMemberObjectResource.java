package simple.jax.rs.resources;

import jakarta.ws.rs.Path;

@Path("/projects")
public class ProjectMemberObjectResource {

    @Path("members")
    public Object getMember() {
        return new MemberResource();
    }
}
