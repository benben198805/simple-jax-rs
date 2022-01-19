package simple.jax.rs.resources;

import jakarta.ws.rs.Path;

@Path("/projects")
public class ProjectMemberClassResource {

    @Path("members")
    public Class<MemberResource> getMember() {
        return MemberResource.class;
    }
}
