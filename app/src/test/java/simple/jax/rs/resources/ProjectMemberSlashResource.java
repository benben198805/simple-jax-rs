package simple.jax.rs.resources;

import jakarta.ws.rs.Path;

@Path("/projects/members")
public class ProjectMemberSlashResource {

    @Path("/")
    public MemberResource getMember() {
        return new MemberResource();
    }
}
