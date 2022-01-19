package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import simple.jax.rs.dto.ExecutableMethod;

import java.lang.reflect.Method;
import java.util.Map;

public class Dispatcher {
    private URITable table;

    public Dispatcher(URITable table) {
        this.table = table;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) {
        ExecutableMethod executableMethod = table.getExecutableMethod(request);
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
