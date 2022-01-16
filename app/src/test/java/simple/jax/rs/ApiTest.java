package simple.jax.rs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import simple.jax.rs.dto.Project;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ApiTest {
    private JettyServer server;
    private HttpClient httpClient;

    @BeforeEach
    public void beforeEach() throws Exception {
        httpClient = new HttpClient();
        httpClient.setFollowRedirects(true);
        httpClient.start();
    }

    private void startServer(Class... resources) throws Exception {
        server = new JettyServer(resources);
        server.start();
    }

    @AfterEach
    public void afterEach() throws Exception {
        server.stop();
        httpClient.stop();
    }

    @Test
    @Disabled
    public void should() throws Exception {
        startServer(NameResource.class);

        ContentResponse response = httpClient.GET("http://localhost:8080/name");
        assertEquals("name", response.getContentAsString());
    }

    @Test
    @Disabled
    public void should_get_users() throws Exception {
        startServer(UserResource.class);

        ContentResponse response = httpClient.GET("http://localhost:8080/users");
        assertEquals("John", response.getContentAsString());
    }

    @Test
    @Disabled
    public void should_get_project_by_id() throws Exception {
        startServer(ProjectResource.class);

        ContentResponse response = httpClient.GET("http://localhost:8080/projects/1");
        assertEquals("CRM-1", response.getContentAsString());
    }
}

@Path("/name")
class NameResource {

    @GET
    public String name() {
        return "name";
    }
}

@Path("/users")
class UserResource {

    @GET
    public String users() {
        return "John";
    }
}

@Path("/projects")
class ProjectResource {

    @GET
    @Path("{id}")
    public Project findProjectById(@PathParam("id") long id) {
        return new Project("CRM-" + id);
    }
}


@Path("/users")
class Users {

    @GET
    public List<User> all(@QueryParam("start") int start, @QueryParam("size") int size) {
        ArrayList<User> users = new ArrayList<>();
        users.add(new User());
        return users;
    }

    @GET
    @Path("{id}")
    public User findById(@PathParam("id") long id) {
        return null;
    }

    @Path("{id}/orders")
    public Orders getOrders() {
        return new Orders();
    }
}

// /users/1/orders

class Orders {

    @GET
    public List<Order> all() {
        return null;
    }

    @GET
    @Path("{id}")
    public Order findById(@PathParam("id") long id) {
        return null;
    }
}

@Path("/sellers")
class Sellers {

    @GET
    public List<User> all(@QueryParam("start") int start, @QueryParam("size") int size) {
        return null;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.WILDCARD)
    public User findByIdAny(@PathParam("id") long id) {
        return null;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public User findById(@PathParam("id") long id) {
        return null;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_XML)
    public User findByIdXML(@PathParam("id") long id) {
        return null;
    }


    @Path("{id}/orders")
    public Orders getOrders() {
        return new Orders();
    }
}

class User {

}

class Order {

}
