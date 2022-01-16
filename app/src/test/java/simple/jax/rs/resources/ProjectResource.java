package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import simple.jax.rs.dto.test.Project;

import java.util.ArrayList;
import java.util.List;

@Path("/projects")
public class ProjectResource {

    @GET
    @Path("{id}")
    public Project findProjectById(@PathParam("id") long id) {
        return new Project("CRM-" + id);
    }

    @GET
    @Path("{id}/items/{itemName}")
    public Project findProjectByIdAndItemName(@PathParam("id") long id, @PathParam("itemName") String itemName) {
        return new Project("CRM-" + id + "(" + itemName + ")");
    }

    @GET
    public List<Project> all(@QueryParam("start") int start, @QueryParam("size") int size) {
        ArrayList<Project> projects = new ArrayList<>();
        projects.add(new Project("CRM-" + start));
        projects.add(new Project("CRM-" + size));
        return projects;
    }
}
