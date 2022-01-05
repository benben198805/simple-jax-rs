package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    @Disabled
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

        Map<String, ?> resourceMethod = dispatcherTable.getPathParams("/projects/1");
        assertNotNull(resourceMethod);
        assertEquals("1", resourceMethod.get("id"));
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
                Object result = method.invoke(o, pathParams.values().toArray());
                response.getWriter().write(result.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    static class DispatcherTable implements URITable {

        private Map<String, Method> resourceMethods = new HashMap<>();

        public DispatcherTable(Class<?> resources) {
            Path path = resources.getAnnotation(Path.class);

            Arrays.stream(resources.getDeclaredMethods()).forEach(method -> {
                if (method.isAnnotationPresent(Path.class)) {
                    String subPath = method.getAnnotation(Path.class).value();
                    resourceMethods.put(path.value() + subPath, method);
                } else {
                    resourceMethods.put(path.value(), method);
                }
            });
        }

        @Override
        public Method get(String path) {
            String methodPath = resourceMethods.keySet().stream().filter(key -> {
                Pattern pathParamsKey = Pattern.compile("\\{\\w+\\}");
                String methodPathPattern = getMethodPathPattern(key, pathParamsKey);

                Pattern pattern = Pattern.compile(methodPathPattern);
                Matcher matcher = pattern.matcher(path);
                return matcher.find();
            }).findFirst().orElseThrow(RuntimeException::new);

            return resourceMethods.get(methodPath);
        }

        @Override
        public Map<String, ?> getPathParams(String path) {
            HashMap<String, Object> pathParams = new HashMap<>();
            List<String> keyList = new ArrayList<>();
            List<String> valueList = new ArrayList<>();

            String methodPathKey = resourceMethods.keySet().stream().filter(key -> {
                Pattern pathParamsKey = Pattern.compile("\\{\\w+\\}");
                String methodPathPattern = getMethodPathPattern(key, pathParamsKey);

                Pattern pattern = Pattern.compile(methodPathPattern);
                Matcher matcher = pattern.matcher(path);
                return matcher.find();
            }).findFirst().orElseThrow(RuntimeException::new);

            Pattern pathParamsKey = Pattern.compile("\\{(\\w+)\\}");

            StringBuffer stringBuffer = new StringBuffer();
            Matcher pathParamsMatcher = pathParamsKey.matcher(methodPathKey);

            while (pathParamsMatcher.find()) {
                keyList.add(pathParamsMatcher.group(1));
                pathParamsMatcher.appendReplacement(stringBuffer, "(\\\\w+)");
            }
            pathParamsMatcher.appendTail(stringBuffer);
            String methodPathPattern = stringBuffer.toString();

            Pattern pattern = Pattern.compile(methodPathPattern);
            Matcher matcher = pattern.matcher(path);

            while (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    valueList.add(matcher.group(1));
                }
            }

            for (int index = 0; index < keyList.size(); index++) {
                pathParams.put(keyList.get(index), valueList.get(index));
            }

            return pathParams;
        }

        private String getMethodPathPattern(String key, Pattern pathParamsKey) {
            StringBuffer stringBuffer = new StringBuffer();
            Matcher pathParamsMatcher = pathParamsKey.matcher(key);
            while (pathParamsMatcher.find()) {
                pathParamsMatcher.appendReplacement(stringBuffer, "(\\\\w+)");
            }
            pathParamsMatcher.appendTail(stringBuffer);
            return stringBuffer.toString();
        }
    }


}
