package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import simple.jax.rs.dto.test.Member;

public class MemberResource {
    public MemberResource() {
    }

    @GET
    @Path("{id}")
    public Member findMemberById(@PathParam("id") long id) {
        return new Member("MEMBER-" + id);
    }
}
