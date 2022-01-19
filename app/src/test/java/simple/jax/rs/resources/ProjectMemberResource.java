package simple.jax.rs.resources;

import jakarta.ws.rs.Path;

@Path("/projects/members")
public class ProjectMemberResource {

    @Path("")
    public MemberResource getMember() {
        return new MemberResource();
    }
}
