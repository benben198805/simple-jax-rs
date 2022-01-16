package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import simple.jax.rs.dto.test.Group;

import java.util.ArrayList;
import java.util.List;

@Path("/groups")
public
class GroupResource {
    @GET
    public List<Group> all(@QueryParam("status") List<String> statusList) {
        ArrayList<Group> groups = new ArrayList<>();
        for (String status : statusList) {
            groups.add(new Group("GROUP-" + status));
        }
        return groups;
    }
}
