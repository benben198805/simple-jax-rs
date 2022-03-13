package simple.jax.rs.resources;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/students")
public class StudentResource {

    @GET
    public String find() {
        return "get John";
    }

    @POST
    public String create() {
        return "post John";
    }

    @DELETE
    public void delete(Object request) {
        System.out.println(request);
    }
}
