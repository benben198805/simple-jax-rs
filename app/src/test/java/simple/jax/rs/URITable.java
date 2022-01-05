package simple.jax.rs;

import java.lang.reflect.Method;
import java.util.Map;

public interface URITable {
    Method get(String path);
    Map<String, ?> getPathParams(String path);
}
