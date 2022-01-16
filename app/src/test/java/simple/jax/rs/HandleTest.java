package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import simple.jax.rs.dto.ExecutableMethod;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public String test() {
        return "Test";
    }

    public String findProjectById(@PathParam("id") long id) {
        return "CRM-" + id;
    }

    static class Dispatcher {
        private URITable table;

        public Dispatcher(URITable table) {
            this.table = table;
        }

        public void handle(HttpServletRequest request, HttpServletResponse response) {
            ExecutableMethod executableMethod = table.getExecutableMethod(request.getPathInfo());
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

            Map<String, Object> pathParams = this.getPathParams(methodPath, method, path);

            return new ExecutableMethod(method, pathParams);
        }

        private Map<String, Object> getPathParams(String methodPatternPath, Method method, String path) {
            HashMap<String, Object> pathParams = new HashMap<>();
            List<Parameter> pathParamList = Arrays.stream(method.getParameters())
                                                  .filter(it -> it.isAnnotationPresent(PathParam.class))
                                                  .collect(Collectors.toList());

            pathParamList.forEach(parameter -> {
                String key = parameter.getAnnotation(PathParam.class).value();
                String patternStr = methodPatternPath.replace("{" + key + "}", "(\\w+)");

                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(path);

                matcher.find();
                if (matcher.groupCount() > 0) {
                    Object value = parseValue(matcher.group(1), parameter);
                    pathParams.put(key, value);
                }
            });

            return pathParams;
        }

        private Object parseValue(String value, Parameter parameter) {
            if (long.class.equals(parameter.getType())) {
                return Long.parseLong(value);
            }
            return value;
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
            return resourceMethods.keySet()
                                  .stream().filter(key -> {
                        Pattern pathParamsKey = Pattern.compile("\\{\\w+\\}");
                        String methodPathPattern = getMethodPathPattern(key, pathParamsKey);

                        Pattern pattern = Pattern.compile(methodPathPattern);
                        Matcher matcher = pattern.matcher(path);
                        return matcher.find();
                    }).findFirst().orElseThrow(() -> new RuntimeException("not found match method"));
        }

        private String getMethodPathPattern(String key, Pattern pathParamsKey) {
            StringBuilder stringBuilder = new StringBuilder();
            Matcher pathParamsMatcher = pathParamsKey.matcher(key);
            while (pathParamsMatcher.find()) {
                pathParamsMatcher.appendReplacement(stringBuilder, "\\\\w");
            }
            pathParamsMatcher.appendTail(stringBuilder);
            return stringBuilder.toString();
        }
    }
}
