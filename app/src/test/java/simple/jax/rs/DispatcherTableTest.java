package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import simple.jax.rs.dto.ExecutableMethod;
import simple.jax.rs.resources.ErrorProjectResource;
import simple.jax.rs.resources.GroupResource;
import simple.jax.rs.resources.MemberResource;
import simple.jax.rs.resources.NameResource;
import simple.jax.rs.resources.ProjectMemberClassResource;
import simple.jax.rs.resources.ProjectMemberObjectResource;
import simple.jax.rs.resources.ProjectMemberResource;
import simple.jax.rs.resources.ProjectMemberSlashResource;
import simple.jax.rs.resources.ProjectResource;
import simple.jax.rs.resources.StudentResource;
import simple.jax.rs.resources.StudentWithoutMethodResource;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DispatcherTableTest {
    @Test
    public void should_get_method_by_path() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{NameResource.class});

        HttpServletRequest request = getHttpServletRequest("/name");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);

        assertExecutableMethod(executableMethod, "name");
    }

    @Test
    public void should_get_method_with_path_param() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{ProjectResource.class});

        HttpServletRequest request = getHttpServletRequest("/projects/1");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);

        assertExecutableMethod(executableMethod, "findProjectById", new HashMap<>() {{
            put("id", 1l);
        }});
    }

    @Test
    public void should_throw_exception_when_not_found_method() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{ProjectResource.class});


        HttpServletRequest request = getHttpServletRequest("/projects-abc/1");

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod(request));

        assertTrue(exception.getMessage().contains("not found match method"));
    }

    @Test
    public void should_throw_exception_when_can_not_cast_params() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{ProjectResource.class});

        HttpServletRequest request = getHttpServletRequest("/projects/abc");

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod(request));

        assertTrue(exception.getMessage().contains("can not cast params"));
    }


    @Test
    public void should_get_method_with_multiple_path_param() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{ProjectResource.class});

        HttpServletRequest request = getHttpServletRequest("/projects/1/items/ieu927");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);

        assertExecutableMethod(executableMethod, "findProjectByIdAndItemName", new HashMap<>() {{
            put("id", 1l);
            put("itemName", "ieu927");
        }});
    }

    @Test
    public void should_get_method_with_query_params() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{ProjectResource.class});
        HttpServletRequest request = getHttpServletRequest("/projects");
        Mockito.when(request.getQueryString()).thenReturn("start=1&size=10");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);

        assertExecutableMethod(executableMethod, "all", new HashMap<>() {{
            put("start", 1);
            put("size", 10);
        }});
    }

    @Test
    public void should_throw_exception_when_query_params_not_found() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{ProjectResource.class});

        HttpServletRequest request = getHttpServletRequest("/projects");
        Mockito.when(request.getQueryString()).thenReturn("start=1");

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod(request));

        assertTrue(exception.getMessage().contains("not found query params: size"));
    }

    @Test
    public void should_throw_exception_when_query_params_is_empty() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{ProjectResource.class});

        HttpServletRequest request = getHttpServletRequest("/projects");
        Mockito.when(request.getQueryString()).thenReturn("start=&size=2");

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod(request));

        assertTrue(exception.getMessage().contains("query params can not be empty"));
    }

    @Test
    public void should_get_method_with_list_query_params() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{GroupResource.class});

        HttpServletRequest request = getHttpServletRequest("/groups");
        Mockito.when(request.getQueryString()).thenReturn("status=active&status=init");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);

        assertExecutableMethod(executableMethod, "all", new HashMap<>() {{
            put("status", List.of("active", "init"));
        }});
    }

    @Test
    public void should_get_method_with_sub_resource() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{MemberResource.class, ProjectResource.class});

        HttpServletRequest request = getHttpServletRequest("/projects/members/9");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);

        assertExecutableMethod(executableMethod, "findMemberById", new HashMap<>() {{
            put("id", 9l);
        }});
    }

    @Test
    public void should_throw_exception_when_not_find_sub_resource_by_locator() {
        Exception exception = assertThrows(RuntimeException.class,
                () -> new DispatcherTable(new Class[]{MemberResource.class}));

        assertTrue(exception.getMessage().contains("not find sub-resource by locators: "));
    }

    @Test
    public void should_throw_exception_when_sub_resource_locator_annotated_http_method() {
        Exception exception = assertThrows(RuntimeException.class,
                () -> new DispatcherTable(new Class[]{MemberResource.class, ErrorProjectResource.class}));

        assertTrue(exception.getMessage().contains("not find sub-resource by locators: "));
    }

    @Test
    public void should_get_method_with_sub_resource_with_empty_path() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{MemberResource.class,
                ProjectMemberResource.class});

        HttpServletRequest request = getHttpServletRequest("/projects/members/9");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);

        assertExecutableMethod(executableMethod, "findMemberById", new HashMap<>() {{
            put("id", 9l);
        }});
    }

    @Test
    public void should_get_method_with_sub_resource_with_slash_path() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{MemberResource.class,
                ProjectMemberSlashResource.class});

        HttpServletRequest request = getHttpServletRequest("/projects/members/9");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);

        assertExecutableMethod(executableMethod, "findMemberById", new HashMap<>() {{
            put("id", 9l);
        }});
    }

    @Test
    public void should_get_method_with_sub_resource_with_class_return_type() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{MemberResource.class,
                ProjectMemberClassResource.class});

        HttpServletRequest request = getHttpServletRequest("/projects/members/9");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);

        assertExecutableMethod(executableMethod, "findMemberById", new HashMap<>() {{
            put("id", 9l);
        }});
    }

    @Test
    public void should_get_method_with_sub_resource_returned_object() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{MemberResource.class,
                ProjectMemberObjectResource.class});

        HttpServletRequest request = getHttpServletRequest("/projects/members/9");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);

        assertExecutableMethod(executableMethod, "findMemberById", new HashMap<>() {{
            put("id", 9l);
        }});
    }

    @Test
    public void should_get_method_with_post_http_method() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{StudentResource.class});

        HttpServletRequest request = getHttpServletRequest("/students");
        Mockito.when(request.getMethod()).thenReturn("POST");

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod(request);

        assertExecutableMethod(executableMethod, "create", new HashMap<>());
    }

    @Test
    public void should_filter_method_without_method_type() {
        DispatcherTable dispatcherTable = new DispatcherTable(new Class[]{StudentWithoutMethodResource.class});

        HttpServletRequest request = getHttpServletRequest("/students");
        Mockito.when(request.getMethod()).thenReturn("POST");

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod(request));

        assertTrue(exception.getMessage().contains("not found match method"));
    }

    private void assertExecutableMethod(ExecutableMethod executableMethod,
                                        String exceptedMethod) {
        this.assertExecutableMethod(executableMethod, exceptedMethod, new HashMap<>());
    }

    private void assertExecutableMethod(ExecutableMethod executableMethod,
                                        String exceptedMethod,
                                        HashMap<String, Object> exceptedParams) {
        assertNotNull(executableMethod);
        assertEquals(exceptedMethod, executableMethod.getMethod().getName());

        exceptedParams.forEach((key, value) -> assertEquals(value, executableMethod.getParams().get(key)));
    }

    private HttpServletRequest getHttpServletRequest(String value) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn(value);
        Mockito.when(request.getMethod()).thenReturn("GET");
        return request;
    }
}
