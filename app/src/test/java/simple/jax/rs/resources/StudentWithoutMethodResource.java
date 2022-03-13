package simple.jax.rs.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import simple.jax.rs.dto.test.Student;

@Path("/students")
public class StudentWithoutMethodResource {

    @GET
    public String find() {
        return "get John";
    }

    public Student create(Object request) {
        System.out.println(request);
        return new Student();
    }
}
