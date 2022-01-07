package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    public void should_invoke_method_with_path_params_for_whole_process() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/1");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

        JettyServer.handle(request, response, new Class[]{ProjectResource.class});

        assertEquals("CRM-1", writer.toString());
    }

    @Test
    public void should_get_method_form_dispatcher_table() {
        DispatcherTable dispatcherTable = new DispatcherTable(NameResource.class);

        Method resourceMethod = dispatcherTable.get("/name");
        assertNotNull(resourceMethod);
        assertEquals("name", resourceMethod.getName());
    }


    @Test
    public void should_get_method_form_dispatcher_table_by_path_params() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        Method resourceMethod = dispatcherTable.get("/projects/1");
        assertNotNull(resourceMethod);
        assertEquals("findProjectById", resourceMethod.getName());
    }


    @Test
    public void should_get_path_params_form_dispatcher_table_with_path_params() {
        DispatcherTable dispatcherTable = new DispatcherTable(ProjectResource.class);

        DispatcherMethod dispatcherMethod = dispatcherTable.getDispatcherMethod("/projects/1");
        assertNotNull(dispatcherMethod.getParams());
        assertEquals(long.class, dispatcherMethod.getParams().get("id"));
    }

    @Test
    public void should_invoke_method() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.get(Mockito.any())).thenReturn(this.getClass().getDeclaredMethod("test"));

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
    public void should_invoke_method_with_path_params() throws NoSuchMethodException, IOException {
        URITable table = Mockito.mock(URITable.class);
        Mockito.when(table.get(Mockito.any())).thenReturn(this.getClass().getDeclaredMethod("findProjectById", long.class));
        Map<String, ?> params = new HashMap<>() {{
            put("id", 1);
        }};
        Mockito.<Map<String, ?>>when(table.getPathParams(Mockito.any())).thenReturn(params);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn("/projects/1");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

        Dispatcher dispatcher = new Dispatcher(table);

        dispatcher.handle(request, response);

        assertEquals("1", writer.toString());
    }


    public String test() {
        return "Test";
    }

    public String findProjectById(@PathParam("id") long id) {
        return String.valueOf(id);
    }

    static class Dispatcher {
        private URITable table;

        public Dispatcher(URITable table) {
            this.table = table;
        }

        public void handle(HttpServletRequest request, HttpServletResponse response) {
            Method method = table.get(request.getPathInfo());
            Map<String, ?> pathParams = table.getPathParams(request.getPathInfo());

            try {
                Object o = method.getDeclaringClass().getDeclaredConstructors()[0].newInstance();
                Object result;
                if (Objects.isNull(pathParams)) {
                    result = method.invoke(o);
                } else {
                    result = method.invoke(o, pathParams.values().toArray());
                }
                response.getWriter().write(result.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static public class DispatcherMethod<T> {
        private Method method;
        private Map<String, Class<T>> params = new HashMap<>();

        public Method getMethod() {
            return method;
        }

        public Map<String, Class<T>> getParams() {
            return params;
        }

        public DispatcherMethod(Method method) {
            this.method = method;

            Arrays.stream(method.getParameters())
                  .filter(parameter -> parameter.isAnnotationPresent(PathParam.class))
                  .forEach(parameter -> {
                      String key = parameter.getAnnotation(PathParam.class).value();
                      this.params.put(key, (Class<T>) parameter.getType());
                  });
        }
    }

    static public class DispatcherPath {
        private Pattern pathPattern;
        private String rawPath;

        public DispatcherPath(String path) {
            this.rawPath = path;
            Pattern pattern = Pattern.compile("\\{\\w+\\}");

            StringBuffer stringBuffer = new StringBuffer();
            Matcher pathParamsMatcher = pattern.matcher(path);
            while (pathParamsMatcher.find()) {
                pathParamsMatcher.appendReplacement(stringBuffer, "(\\\\w+)");
            }
            pathParamsMatcher.appendTail(stringBuffer);
            this.pathPattern = Pattern.compile(stringBuffer.toString());
        }

        public boolean isMatch(String path) {
            return this.pathPattern.matcher(path).find();
        }

        public Pattern getPathPattern() {
            return pathPattern;
        }

        public String getRawPath() {
            return rawPath;
        }
    }


    static class DispatcherTable implements URITable {

        private Map<DispatcherPath, DispatcherMethod> resourceMethods = new HashMap<>();

        public DispatcherTable(Class<?> resources) {
            Path path = resources.getAnnotation(Path.class);

            Arrays.stream(resources.getDeclaredMethods()).forEach(method -> {
                if (method.isAnnotationPresent(Path.class)) {
                    String subPath = method.getAnnotation(Path.class).value();
                    resourceMethods.put(new DispatcherPath(path.value() + subPath), new DispatcherMethod(method));
                } else {
                    resourceMethods.put(new DispatcherPath(path.value()), new DispatcherMethod(method));
                }
            });
        }

        @Override
        public Method get(String path) {
            DispatcherPath dispatcherPath = resourceMethods.keySet().stream().filter(key -> key.isMatch(path))
                                                           .findFirst().orElseThrow(RuntimeException::new);

            return resourceMethods.get(dispatcherPath).getMethod();
        }

        @Override
        public Map<String, ?> getPathParams(String path) {
            DispatcherPath dispatcherPath = resourceMethods.keySet().stream().filter(key -> key.isMatch(path))
                                                           .findFirst().orElseThrow(RuntimeException::new);
            Pattern pathPattern = dispatcherPath.getPathPattern();
            Matcher matcher = pathPattern.matcher(path);
            Map<String, Class> params = resourceMethods.get(dispatcherPath).getParams();
            List<String> keys = Arrays.stream(params.keySet().toArray()).map(Object::toString).collect(Collectors.toList());
            matcher.find();
            Map<String, Object> result = new HashMap<>();
            for (int index = 1; index < matcher.groupCount() + 1; index++) {
                String key = keys.get(index - 1);
                String group = matcher.group(index);
                if (params.get(key).equals(long.class)) {
                    long value = Long.parseLong(group);
                    result.put(key, value);
                }
            }
            return result;
        }

        public DispatcherMethod getDispatcherMethod(String path) {
            DispatcherPath dispatcherPath = resourceMethods.keySet().stream().filter(key -> key.isMatch(path))
                                                           .findFirst().orElseThrow(RuntimeException::new);

            return resourceMethods.get(dispatcherPath);
        }
    }


}
