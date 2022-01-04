package simple.jax.rs;

import java.lang.reflect.Method;

public interface URITable {
    Method get(String path);
}
