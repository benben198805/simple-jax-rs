package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.jetty.http.HttpMethod;
import simple.jax.rs.dto.ExecutableMethod;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DispatcherTable implements URITable {
    private static final HttpMethod[] HTTP_METHODS = HttpMethod.values();
    public static final String MEDIA_TYPE_HEADER = "Accept";
    private final Map<String, List<Method>> resourceMethods = new HashMap<>();

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
            Map.Entry<String, List<Method>> parentMethodResource = resourceMethods
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().stream().anyMatch(it -> filterSubResourceLocator(resource, it)))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("not find sub-resource by locators: " + resource));

            String parentPath = parentMethodResource.getKey();
            resourceMethods.remove(parentPath);

            initDispatcherTableByResource(resource, parentPath);
        }
    }

    private boolean filterSubResourceLocator(Class<?> resource, Method method) {
        boolean isSameReturn = isSameReturn(resource, method);

        boolean noHttpMethodAnnotation = Arrays.stream(method.getAnnotations())
                                               .noneMatch(annotation ->
                                                       Arrays.asList(DispatcherTable.HTTP_METHODS)
                                                             .contains(HttpMethod.fromString(annotation.annotationType().getSimpleName())));
        return isSameReturn && noHttpMethodAnnotation;
    }

    private boolean isSameReturn(Class<?> resource, Method method) {
        if (Objects.equals(resource, method.getReturnType())) {
            return true;
        } else if (Objects.equals(Class.class, method.getReturnType())) {
            return Arrays.asList(((ParameterizedType) (method.getGenericReturnType()))
                    .getActualTypeArguments()).contains(resource);
        } else if (Objects.equals(Object.class, method.getReturnType())) {
            try {
                Object o = method.getDeclaringClass().getDeclaredConstructors()[0].newInstance();
                Object returnObject = method.invoke(o);
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

                  List<Method> methodList = resourceMethods.getOrDefault(methodPath, new ArrayList<>());
                  methodList.add(method);

                  resourceMethods.put(methodPath, methodList);
              });
    }

    private boolean subResourceOrMethodWithHttp(Method method) {
        return Objects.nonNull(method.getAnnotation(Path.class)) || getHttpMethod(method).isPresent();
    }

    @Override
    public ExecutableMethod getExecutableMethod(HttpServletRequest request) {
        List<String> methodPaths = this.getMethodPatternPath(request);

        String methodPath = findAvailableMethodPath(methodPaths);

        Method method = getMethodByMethodPath(request, methodPath);

        Map<String, Object> pathParams = ParameterHandler.getParams(methodPath, method, request);

        return new ExecutableMethod(method, pathParams);
    }

    private Method getMethodByMethodPath(HttpServletRequest request, String methodPath) {
        List<String> acceptMediaTypes = Arrays.stream(Optional.ofNullable(request.getHeader(MEDIA_TYPE_HEADER))
                                                              .orElse(MediaType.WILDCARD)
                                                              .split(",")).collect(Collectors.toList());

        return resourceMethods.get(methodPath)
                              .stream()
                              .filter(method -> isAvailableAcceptMediaType(acceptMediaTypes, method))
                              .findAny().orElseThrow(NotAcceptableException::new);
    }

    private boolean isAvailableAcceptMediaType(List<String> acceptMediaTypes, Method method) {
        List<String> methodSupportMediaTypes =
                Optional.ofNullable(method.getAnnotation(Produces.class))
                        .map(it -> Arrays.stream(it.value()).collect(Collectors.toList()))
                        .orElse(List.of(MediaType.WILDCARD));
        return methodSupportMediaTypes.contains("*/*") ||
                methodSupportMediaTypes.stream().anyMatch(acceptMediaTypes::contains);
    }

    private String findAvailableMethodPath(List<String> methodPaths) {
        return methodPaths.stream()
                          .filter(methodPathKey -> !resourceMethods.get(methodPathKey).isEmpty())
                          .findAny()
                          .orElseThrow(() -> new RuntimeException("not found match method"));
    }

    private String composeMethodPath(Method method, String classPath) {
        if (method.isAnnotationPresent(Path.class)) {
            String subPath = method.getAnnotation(Path.class).value();
            String additionalSlash = subPath.startsWith("/") || classPath.endsWith("/") ? "" : "/";

            String prefix = "";
            if (getHttpMethod(method).isPresent()) {
                prefix = getHttpMethod(method).orElse(null) + ":";
            }
            return prefix + classPath + additionalSlash + subPath;
        } else {
            return getHttpMethod(method).orElse(null) + ":" + classPath;
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
                       .map(it -> String.join("-", it.value()))
                       .orElse(MediaType.WILDCARD);
    }

    private List<String> getMethodPatternPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        String httpMethod = Objects.isNull(request.getMethod()) ? GET.class.getSimpleName() : request.getMethod();

        return resourceMethods.keySet()
                              .stream()
                              .sorted(Comparator.comparingInt(String::length).reversed())
                              .filter(key -> {
                                  Pattern pathParamsKey = Pattern.compile("\\{\\w+\\}");
                                  String methodPathPattern = getMethodPathPattern(key, pathParamsKey);

                                  Pattern pattern = Pattern.compile(methodPathPattern);
                                  Matcher matcher = pattern.matcher(httpMethod + ":" + path);
                                  return matcher.find();
                              }).collect(Collectors.toList());
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
