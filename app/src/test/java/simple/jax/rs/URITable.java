package simple.jax.rs;

import simple.jax.rs.dto.ExecutableMethod;

public interface URITable {
    ExecutableMethod getExecutableMethod(String path);
}
