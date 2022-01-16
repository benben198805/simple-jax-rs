package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import simple.jax.rs.dto.ExecutableMethod;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod("/name");
        assertNotNull(executableMethod);
        assertEquals("name", executableMethod.getMethod().getName());
    }

    @Test
    public void should_3() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any()))
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

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod("/projects/1");
        assertNotNull(executableMethod);
        assertEquals("findProjectById", executableMethod.getMethod().getName());
        assertEquals(1l, executableMethod.getParams().get("id"));
    }

    @Test
    public void should_throw_exception_when_not_found_method() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod("/projects-abc/1"));

        assertTrue(exception.getMessage().contains("not found match method"));
    }

    @Test
    public void should_throw_exception_when_can_not_cast_params() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod("/projects/abc"));

        assertTrue(exception.getMessage().contains("can not cast params"));
    }

    @Test
    public void should_run_method_with_path_param() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any()))
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

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod("/projects/1/items/ieu927");
        assertNotNull(executableMethod);
        assertEquals("findProjectByIdAndItemName", executableMethod.getMethod().getName());
        assertEquals(1l, executableMethod.getParams().get("id"));
        assertEquals("ieu927", executableMethod.getParams().get("itemName"));
    }

    @Test
    public void should_run_method_with_multiple_path_param() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any()))
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

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod("/projects?start=1&size=10");
        assertNotNull(executableMethod);
        assertEquals("all", executableMethod.getMethod().getName());
        assertEquals(1, executableMethod.getParams().get("start"));
        assertEquals(10, executableMethod.getParams().get("size"));
    }

    @Test
    public void should_throw_exception_when_query_params_not_found() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod("/projects?start=1"));

        assertTrue(exception.getMessage().contains("not found query params: size"));
    }

    @Test
    public void should_throw_exception_when_query_params_is_empty() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        Exception exception = assertThrows(RuntimeException.class,
                () -> dispatcherTable.getExecutableMethod("/projects?start=&size=2"));

        assertTrue(exception.getMessage().contains("query params can not be empty"));
    }

    @Test
    public void should_get_method_with_list_query_params() {
        DispatcherTable dispatcherTable = new DispatcherTable(GroupResource.class);

        ExecutableMethod executableMethod = dispatcherTable.getExecutableMethod("/groups?status=active&status=init");
        assertNotNull(executableMethod);
        assertEquals("all", executableMethod.getMethod().getName());
        assertEquals(List.of("active", "init"), executableMethod.getParams().get("status"));
    }

    @Test
    public void should_run_method_with_query_param() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.getExecutableMethod(Mockito.any()))
               .thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("all",
                       int.class, int.class), new LinkedHashMap<>() {{
                   put("start", 1);
                   put("size", 10);
               }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects?start=1&size=10");

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
        Mockito.when(table.getExecutableMethod(Mockito.any()))
               .thenReturn(new ExecutableMethod(this.getClass().getDeclaredMethod("all",
                       List.class), new LinkedHashMap<>() {{
                   put("status", List.of("active", "init"));
               }}));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/groups?status=active&status=init");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

        Dispatcher dispatcher = new Dispatcher(table);

        dispatcher.handle(request, response);

        assertEquals("CRM-active|init", writer.toString());
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

    static class Dispatcher {
        private URITable table;

        public Dispatcher(URITable table) {
            this.table = table;
        }

        public void handle(HttpServletRequest request, HttpServletResponse response) {
            ExecutableMethod executableMethod =
                    table.getExecutableMethod(request.getPathInfo() + "?" + request.getQueryString());
            Method method = executableMethod.getMethod();
            Map<String, Object> params = executableMethod.getParams();

            try {
                Object o = method.getDeclaringClass().getDeclaredConstructors()[0].newInstance();
                Object result = method.invoke(o, params.values().toArray());
                response.getWriter().write(result.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    static class DispatcherTable implements URITable {
        private final Map<String, Method> resourceMethods = new HashMap<>();

        public DispatcherTable(Class<?> resources) {
            Path path = resources.getAnnotation(Path.class);

            Arrays.stream(resources.getDeclaredMethods()).forEach(method -> {
                String methodPath = composeMethodPath(method, path);
                resourceMethods.put(methodPath, method);
            });
        }

        @Override
        public ExecutableMethod getExecutableMethod(String path) {
            String methodPath = this.getMethodPatternPath(path);

            Method method = resourceMethods.get(methodPath);

            Map<String, Object> pathParams = this.getParams(methodPath, method, path);

            return new ExecutableMethod(method, pathParams);
        }

        private Map<String, Object> getParams(String methodPatternPath, Method method, String path) {
            HashMap<String, Object> params = new LinkedHashMap<>();
            List<Parameter> pathParamList = List.of(method.getParameters());

            pathParamList.forEach(parameter -> {
                if (parameter.isAnnotationPresent(PathParam.class)) {
                    String key = parameter.getAnnotation(PathParam.class).value();
                    String patternStr = methodPatternPath.replace("{" + key + "}", "(\\w+)")
                                                         .replaceAll("\\{\\w+\\}", "\\\\w+");

                    Pattern pattern = Pattern.compile(patternStr);
                    Matcher matcher = pattern.matcher(path);

                    matcher.find();
                    if (matcher.groupCount() > 0) {
                        Object value = parseParameterValue(matcher.group(1), parameter);
                        params.put(key, value);
                    }
                } else if (parameter.isAnnotationPresent(QueryParam.class)) {
                    String queryKey = parameter.getAnnotation(QueryParam.class).value();
                    String queryStr = path.substring(path.indexOf("?") + 1);
                    List<String> queryParamStrWithKeys = Arrays.stream(queryStr.split("&"))
                                                               .filter(it -> it.contains(queryKey + "="))
                                                               .collect(Collectors.toList());

                    if (queryParamStrWithKeys.isEmpty()) {
                        throw new RuntimeException("not found query params: " + queryKey);
                    }


                    if (parameter.getType().equals(List.class)) {
                        List<String> values = queryParamStrWithKeys.stream()
                                                                   .map(queryParamStrWithKey -> {
                                                                       String[] splitQuery =
                                                                               queryParamStrWithKey.split("=");
                                                                       return splitQuery[1];
                                                                   })
                                                                   .filter(it -> !it.isEmpty())
                                                                   .collect(Collectors.toList());
                        params.put(queryKey, values);
                    } else {
                        String[] splitQuery = queryParamStrWithKeys.get(0).split("=");

                        if (splitQuery.length != 2) {
                            throw new RuntimeException("query params can not be empty");
                        }

                        params.put(queryKey, parseParameterValue(splitQuery[1], parameter));
                    }
                }
            });

            return params;
        }

        private Object parseParameterValue(String value, Parameter parameter) {
            try {
                if (long.class.equals(parameter.getType())) {
                    return Long.parseLong(value);
                }
                if (int.class.equals(parameter.getType())) {
                    return Integer.parseInt(value);
                }
                return value;
            } catch (RuntimeException e) {
                throw new RuntimeException("can not cast params");
            }
        }

        private String composeMethodPath(Method method, Path path) {
            String classPath = path.value();
            if (method.isAnnotationPresent(Path.class)) {
                String subPath = method.getAnnotation(Path.class).value();
                String additionalSlash = subPath.startsWith("/") ? "" : "/";
                return classPath + additionalSlash + subPath;
            } else {
                return classPath;
            }
        }

        private String getMethodPatternPath(String path) {
            String pathWithoutQueryParams = path.indexOf("?") > 0 ? path.substring(0, path.indexOf("?")) : path;

            return resourceMethods.keySet()
                                  .stream()
                                  .sorted(Comparator.comparingInt(String::length).reversed())
                                  .filter(key -> {
                                      Pattern pathParamsKey = Pattern.compile("\\{\\w+\\}");
                                      String methodPathPattern = getMethodPathPattern(key,
                                              pathParamsKey);
                                      Pattern pattern = Pattern.compile(methodPathPattern);
                                      Matcher matcher =
                                              pattern.matcher(pathWithoutQueryParams);
                                      return matcher.find();
                                  }).findFirst().orElseThrow(() -> new RuntimeException("not found match method"));
        }

        private String getMethodPathPattern(String key, Pattern pathParamsKey) {
            StringBuilder stringBuilder = new StringBuilder();
            Matcher pathParamsMatcher = pathParamsKey.matcher(key);
            while (pathParamsMatcher.find()) {
                pathParamsMatcher.appendReplacement(stringBuilder, "\\\\w+");
            }
            pathParamsMatcher.appendTail(stringBuilder);
            return stringBuilder + "$";
        }
    }
}
