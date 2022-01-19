package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import simple.jax.rs.dto.ExecutableMethod;
import simple.jax.rs.resources.GroupResource;
import simple.jax.rs.resources.MemberResource;
import simple.jax.rs.resources.NameResource;
import simple.jax.rs.resources.ProjectResource;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HandleTest {
    @Test
    public void should() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/name");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

        JettyServer.handle(request, response, new Class[]{NameResource.class});

        assertEquals("name", writer.toString());
    }


    @Test
    public void should_2() {
        DispatcherTable dispatcherTable = new DispatcherTable(NameResource.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/name");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);
        assertNotNull(executableMethod);
        assertEquals("name", executableMethod.getMethod().getName());
    }

    @Test
    public void should_3() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class)))
               .thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("test"), new HashMap<>()));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/name");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

        Dispatcher dispatcher = new Dispatcher(table);

        dispatcher.handle(request, response);

        assertEquals("Test", writer.toString());
    }

    @Test
    public void should_get_method_with_path_param() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/1");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);
        assertNotNull(executableMethod);
        assertEquals("findProjectById", executableMethod.getMethod().getName());
        assertEquals(1l, executableMethod.getParams().get("id"));
    }

    @Test
    public void should_throw_exception_when_not_found_method() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);


        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects-abc/1");

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod(request));

        assertTrue(exception.getMessage().contains("not found match method"));
    }

    @Test
    public void should_throw_exception_when_can_not_cast_params() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/abc");

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod(request));

        assertTrue(exception.getMessage().contains("can not cast params"));
    }

    @Test
    public void should_run_method_with_path_param() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class)))
               .thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("findProjectById",
                       long.class), new HashMap<>() {{
                   put("id", 1l);
               }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/1");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

        Dispatcher dispatcher = new Dispatcher(table);

        dispatcher.handle(request, response);

        assertEquals("CRM-1", writer.toString());
    }

    @Test
    public void should_get_method_with_multiple_path_param() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/1/items/ieu927");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);
        assertNotNull(executableMethod);
        assertEquals("findProjectByIdAndItemName", executableMethod.getMethod().getName());
        assertEquals(1l, executableMethod.getParams().get("id"));
        assertEquals("ieu927", executableMethod.getParams().get("itemName"));
    }

    @Test
    public void should_run_method_with_multiple_path_param() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class)))
               .thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("findProjectByIdAndItemName",
                       long.class, String.class), new LinkedHashMap<>() {{
                   put("id", 1l);
                   put("itemName", "ieu927");
               }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/1/items/ieu927");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

        Dispatcher dispatcher = new Dispatcher(table);

        dispatcher.handle(request, response);

        assertEquals("CRM-1(ieu927)", writer.toString());
    }

    @Test
    public void should_get_method_with_query_params() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects");
        Mockito.when(request.getQueryString()).thenReturn("start=1&size=10");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);
        assertNotNull(executableMethod);
        assertEquals("all", executableMethod.getMethod().getName());
        assertEquals(1, executableMethod.getParams().get("start"));
        assertEquals(10, executableMethod.getParams().get("size"));
    }

    @Test
    public void should_throw_exception_when_query_params_not_found() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects");
        Mockito.when(request.getQueryString()).thenReturn("start=1");

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod(request));

        assertTrue(exception.getMessage().contains("not found query params: size"));
    }

    @Test
    public void should_throw_exception_when_query_params_is_empty() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects");
        Mockito.when(request.getQueryString()).thenReturn("start=&size=2");

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod(request));

        assertTrue(exception.getMessage().contains("query params can not be empty"));
    }

    @Test
    public void should_get_method_with_list_query_params() {
        DispatcherTable dispatcherTable = new DispatcherTable(GroupResource.class);


        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/groups");
        Mockito.when(request.getQueryString()).thenReturn("status=active&status=init");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);
        assertNotNull(executableMethod);
        assertEquals("all", executableMethod.getMethod().getName());
        assertEquals(List.of("active", "init"), executableMethod.getParams().get("status"));
    }

    @Test
    public void should_run_method_with_query_param() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class)))
               .thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("all",
                       int.class, int.class), new LinkedHashMap<>() {{
                   put("start", 1);
                   put("size", 10);
               }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects");
        Mockito.when(request.getQueryString()).thenReturn("start=1&size=10");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

        Dispatcher dispatcher = new Dispatcher(table);

        dispatcher.handle(request, response);

        assertEquals("CRM-1(10)", writer.toString());
    }

    @Test
    public void should_run_method_with_list_query_param() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class)))
               .thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("all",
                       List.class), new LinkedHashMap<>() {{
                   put("status", List.of("active", "init"));
               }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/groups");
        Mockito.when(request.getQueryString()).thenReturn("status=active&status=init");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

        Dispatcher dispatcher = new Dispatcher(table);

        dispatcher.handle(request, response);

        assertEquals("CRM-active|init", writer.toString());
    }

    @Test
    public void should_get_method_with_sub_resource() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{MemberResource.class, ProjectResource.class});

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/members/9");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);
        assertNotNull(executableMethod);
        assertEquals("findMemberById", executableMethod.getMethod().getName());
        assertEquals(9l, executableMethod.getParams().get("id"));
    }

    @Test
    public void should_throw_exception_when_not_find_sub_resource_by_locator() {
        Exception exception = assertThrows(RuntimeException.class,
                () -> new DispatcherTable(new Class[]{MemberResource.class}));

        assertTrue(exception.getMessage().contains("not find sub-resource by locators: "));
    }

    @Test
    public void should_run_sub_resource_method() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any(HttpServletRequest.class)))
               .thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("findMemberById",
                       long.class), new LinkedHashMap<>() {{
                   put("id", 9l);
               }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/members/9");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

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
