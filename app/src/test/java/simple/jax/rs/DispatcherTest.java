package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import simple.jax.rs.dto.ExecutableMethod;
import simple.jax.rs.resources.NameResource;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DispatcherTest {

    private StringWriter writer;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() throws IOException {
        writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    public void should_run_method_by_path_with_jetty_server() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/name");

        JettyServer.handle(request, response, new Class[]{NameResource.class});

        assertEquals("name", writer.toString());
    }

    @Test
    public void should_run_method_by_path() throws NoSuchMethodException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class)))
               .thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("test"), new HashMap<>()));
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/name");

        Dispatcher dispatcher = new Dispatcher(table);
        dispatcher.handle(request, response);

        assertEquals("Test", writer.toString());
    }

    @Test
    public void should_run_method_with_path_param() throws NoSuchMethodException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class))).thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("findProjectById", long.class), new HashMap<>() {{
            put("id", 1l);
        }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/1");

        Dispatcher dispatcher = new Dispatcher(table);
        dispatcher.handle(request, response);

        assertEquals("CRM-1", writer.toString());
    }

    @Test
    public void should_run_method_with_multiple_path_param() throws NoSuchMethodException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class))).thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("findProjectByIdAndItemName", long.class, String.class), new LinkedHashMap<>() {{
            put("id", 1l);
            put("itemName", "ieu927");
        }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/1/items/ieu927");

        Dispatcher dispatcher = new Dispatcher(table);
        dispatcher.handle(request, response);

        assertEquals("CRM-1(ieu927)", writer.toString());
    }

    @Test
    public void should_run_method_with_query_param() throws NoSuchMethodException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class))).thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("all", int.class, int.class), new LinkedHashMap<>() {{
            put("start", 1);
            put("size", 10);
        }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects");
        Mockito.when(request.getQueryString()).thenReturn("start=1&size=10");

        Dispatcher dispatcher = new Dispatcher(table);
        dispatcher.handle(request, response);

        assertEquals("CRM-1(10)", writer.toString());
    }

    @Test
    public void should_run_method_with_list_query_param() throws NoSuchMethodException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class)))
               .thenReturn(new ExecutableMethod(this.getClass()
                                                    .getDeclaredMethod("all", List.class), new LinkedHashMap<>() {{
                   put("status", List.of("active", "init"));
               }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/groups");
        Mockito.when(request.getQueryString()).thenReturn("status=active&status=init");

        Dispatcher dispatcher = new Dispatcher(table);
        dispatcher.handle(request, response);

        assertEquals("CRM-active|init", writer.toString());
    }

    @Test
    public void should_run_sub_resource_method() throws NoSuchMethodException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class)))
               .thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("findMemberById", long.class),
                       new LinkedHashMap<>() {{
                   put("id", 9l);
               }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/members/9");

        Dispatcher dispatcher = new Dispatcher(table);
        dispatcher.handle(request, response);

        assertEquals("MEMBER-9", writer.toString());
    }

    public String test() {
        return "Test";
    }

    public String findProjectById(@PathParam("id") long id) {
        return "CRM-" + id;
    }

    public String findProjectByIdAndItemName(@PathParam("id") long id, @PathParam("itemName") String itemName) {
        return "CRM-" + id + "(" + itemName + ")";
    }

    public String all(@QueryParam("start") int start, @QueryParam("size") int size) {
        return "CRM-" + start + "(" + size + ")";
    }

    public String all(@QueryParam("status") List<String> statusList) {
        return "CRM-" + statusList.stream().collect(Collectors.joining("|"));
    }

    public String findMemberById(@PathParam("id") long id) {
        return "MEMBER-" + id;
    }

}
