package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import simple.jax.rs.dto.ExecutableMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherTable implements URITable {
    private final Map<String, Method> resourceMethods = new HashMap<>();

    public DispatcherTable(Class<?> resources) {
        Path path = resources.getAnnotation(Path.class);

        Arrays.stream(resources.getDeclaredMethods()).forEach(method -> {
            String methodPath = composeMethodPath(method, path);
            resourceMethods.put(methodPath, method);
        });
    }

    @Override
    public ExecutableMethod getExecutableMethod(HttpServletRequest request) {
        String path = request.getPathInfo();

        String methodPath = this.getMethodPatternPath(path);

        Method method = resourceMethods.get(methodPath);

        Map<String, Object> pathParams = ParameterHandler.getParams(methodPath, method, request);

        return new ExecutableMethod(method, pathParams);
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
                              .stream()
                              .sorted(Comparator.comparingInt(String::length).reversed())
                              .filter(key -> {
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
            pathParamsMatcher.appendReplacement(stringBuilder, "\\\\w+");
        }
        pathParamsMatcher.appendTail(stringBuilder);
        return stringBuilder + "$";
    }
}
