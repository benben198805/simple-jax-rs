package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.jetty.http.HttpMethod;
import simple.jax.rs.dto.ExecutableMethod;

import javax.inject.Provider;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherTable implements URITable {
    private static final HttpMethod[] HTTP_METHODS = HttpMethod.values();
    public static final String MEDIA_TYPE_HEADER = "Accept";
    private final Map<String, Method> resourceMethods = new HashMap<>();
    private final Map<String, Consumer<Provider<?>>> resourceMethodsConsumers = new HashMap<>();

    public DispatcherTable(Class[] resources) {
        Arrays.stream(resources)
              .sorted(Comparator.comparing(it -> !it.isAnnotationPresent(Path.class)))
              .forEach(this::initDispatcherTable);
    }

    private void initDispatcherTable(Class<?> resource) {
        Path classPath = resource.getAnnotation(Path.class);

        if (Objects.nonNull(classPath)) {
            initDispatcherTableByResource(resource, classPath.value());
        } else {
            Map.Entry<String, Method> parentMethodResource = resourceMethods
                    .entrySet()
                    .stream()
                    .filter(entry -> filterSubResourceLocator(resource, entry))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("not find sub-resource by locators: " + resource));

            String parentPath = parentMethodResource.getKey();
            resourceMethods.remove(parentPath);

            initDispatcherTableByResource(resource, parentPath);
        }
    }

    private boolean filterSubResourceLocator(Class<?> resource, Map.Entry<String, Method> entry) {
        boolean isSameReturn = isSameReturn(resource, entry);

        boolean noHttpMethodAnnotation = Arrays.stream(entry.getValue().getAnnotations())
                                               .noneMatch(annotation ->
                                                       Arrays.asList(DispatcherTable.HTTP_METHODS)
                                                             .contains(HttpMethod.fromString(annotation.annotationType().getSimpleName())));
        return isSameReturn && noHttpMethodAnnotation;
    }

    private boolean isSameReturn(Class<?> resource, Map.Entry<String, Method> entry) {
        if (Objects.equals(resource, entry.getValue().getReturnType())) {
            return true;
        } else if (Objects.equals(Class.class, entry.getValue().getReturnType())) {
            return Arrays.asList(((ParameterizedType) (entry.getValue().getGenericReturnType()))
                    .getActualTypeArguments()).contains(resource);
        } else if (Objects.equals(Object.class, entry.getValue().getReturnType())) {
            try {
                Object o = entry.getValue().getDeclaringClass().getDeclaredConstructors()[0].newInstance();
                Object returnObject = entry.getValue().invoke(o);
                return Objects.equals(resource, returnObject.getClass());
            } catch (Exception e) {
                System.out.println("can not invoke method");
            }
        }

        return false;
    }

    private void initDispatcherTableByResource(Class<?> resource, String parentPath) {
        Arrays.stream(resource.getDeclaredMethods())
              .filter(this::subResourceOrMethodWithHttp)
              .forEach(method -> {
                  String methodPath = composeMethodPath(method, parentPath);
                  System.out.println(methodPath);
                  System.out.println(method);
                  resourceMethods.put(methodPath, method);
              });
    }

    private boolean subResourceOrMethodWithHttp(Method method) {
        return Objects.nonNull(method.getAnnotation(Path.class)) || getHttpMethod(method).isPresent();
    }

    @Override
    public ExecutableMethod getExecutableMethod(HttpServletRequest request) {
        System.out.println(resourceMethods);
        String path = request.getPathInfo();
        String httpMethod = Objects.isNull(request.getMethod()) ? GET.class.getSimpleName() : request.getMethod();
        String mediaType = Optional.ofNullable(request.getHeader(MEDIA_TYPE_HEADER)).orElse(MediaType.WILDCARD);

        String methodPath = this.getMethodPatternPath(path, httpMethod, mediaType);

        Method method = resourceMethods.get(methodPath);

        Map<String, Object> pathParams = ParameterHandler.getParams(methodPath, method, request);

        return new ExecutableMethod(method, pathParams);
    }

    private String composeMethodPath(Method method, String classPath) {
        if (method.isAnnotationPresent(Path.class)) {
            String subPath = method.getAnnotation(Path.class).value();
            String additionalSlash = subPath.startsWith("/") || classPath.endsWith("/") ? "" : "/";

            String prefix = "";
            if (getHttpMethod(method).isPresent()) {
                prefix = getHttpMethod(method).orElse(null) + ":" + getMediaType(method) + ":";
            }
            return prefix + classPath + additionalSlash + subPath;
        } else {
            return getHttpMethod(method).orElse(null) + ":" + getMediaType(method) + ":" + classPath;
        }
    }

    private Optional<String> getHttpMethod(Method method) {
        return Arrays.stream(method.getAnnotations())
                     .map(it -> it.annotationType().getSimpleName())
                     .filter(it -> Arrays.stream(HTTP_METHODS).anyMatch(e -> e.toString().equals(it)))
                     .findFirst();
    }

    private String getMediaType(Method method) {
        return Optional.ofNullable(method.getAnnotation(Produces.class))
                       .flatMap(it -> Arrays.stream(it.value()).findAny())
                       .orElse(MediaType.WILDCARD);
    }

    private String getMethodPatternPath(String path, String httpMethod, String mediaType) {
        return resourceMethods.keySet()
                              .stream()
                              .sorted(Comparator.comparingInt(String::length).reversed())
                              .filter(key -> {
                                  Pattern pathParamsKey = Pattern.compile("\\{\\w+\\}");
                                  String methodPathPattern = getMethodPathPattern(key, pathParamsKey);

                                  Pattern pattern = Pattern.compile(methodPathPattern);
                                  Matcher matcher = pattern.matcher(httpMethod + ":" + mediaType + ":" + path);
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
        return stringBuilder.toString().replace("*/*", "[\\w\\*]+/[\\w\\*]+") + "$";
    }
}
