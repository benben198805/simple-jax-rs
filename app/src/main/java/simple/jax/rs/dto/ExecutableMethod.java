package simple.jax.rs.dto;

import java.lang.reflect.Method;
import java.util.Map;

public class ExecutableMethod {
    private Method method;
    private Map<String, Object> params;

    public ExecutableMethod(Method method, Map<String, Object> params) {
        this.method = method;
        this.params = params;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "ExecutableMethod{" +
                "method=" + method +
                ", params=" + params +
                '}';
    }
}
