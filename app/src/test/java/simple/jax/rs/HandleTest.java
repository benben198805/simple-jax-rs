package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
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
    public void should_3() throws NoSuchMethodException, IOException {
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


    public String test() {
        return "Test";
    }

    static class Dispatcher {
        private URITable table;

        public Dispatcher(URITable table) {
            this.table = table;
        }

        public void handle(HttpServletRequest request, HttpServletResponse response) {
            Method method = table.get(request.getPathInfo());

            try {
                Object o = method.getDeclaringClass().getDeclaredConstructors()[0].newInstance();
                Object result = method.invoke(o);
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

        private String getMethodPathPattern(String key, Pattern pathParamsKey) {
            StringBuffer stringBuffer = new StringBuffer();
            Matcher pathParamsMatcher = pathParamsKey.matcher(key);
            while (pathParamsMatcher.find()) {
                pathParamsMatcher.appendReplacement(stringBuffer, "\\\\w");
            }
            pathParamsMatcher.appendTail(stringBuffer);
            return stringBuffer.toString();
        }
    }


}
