package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import simple.jax.rs.dto.test.Student;

@Path("/students")
public class StudentResource {

    @GET
    public String find() {
        return "get John";
    }

    @POST
    public Student create(Object request) {
        System.out.println(request);
        return new Student();
    }
}
