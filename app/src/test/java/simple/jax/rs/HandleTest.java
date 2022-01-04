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
import java.util.HashMap;
import java.util.Map;

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

        Method resourceMethod = dispatcherTable.get("/name");
        assertNotNull(resourceMethod);
        assertEquals("name", resourceMethod.getName());
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
            resourceMethods.put(path.value(), resources.getDeclaredMethods()[0]);
        }

        @Override
        public Method get(String path) {
            return resourceMethods.get(path);
        }
    }


}
