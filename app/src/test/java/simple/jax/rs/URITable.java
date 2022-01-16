package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import simple.jax.rs.dto.ExecutableMethod;

public interface URITable {
    ExecutableMethod getExecutableMethod(HttpServletRequest request);
}
