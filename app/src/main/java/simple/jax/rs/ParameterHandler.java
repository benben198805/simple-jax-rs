package simple.jax.rs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParameterHandler {

    public static Map<String, Object> getParams(String methodPatternPath, Method method,
                                                HttpServletRequest request) {
        String path = request.getPathInfo();
        HashMap<String, Object> params = new LinkedHashMap<>();
        List<Parameter> pathParamList = List.of(method.getParameters());

        pathParamList.forEach(parameter -> {
            if (parameter.isAnnotationPresent(PathParam.class)) {
                Map<String, Object> pathParamMap = processPathParam(methodPatternPath, path, parameter);
                if (Objects.nonNull(pathParamMap)) {
                    params.putAll(pathParamMap);
                }
            } else if (parameter.isAnnotationPresent(QueryParam.class)) {
                params.putAll(processQueryParam(request, parameter));
            }
        });

        return params;
    }

    private static Map<String, Object> processQueryParam(HttpServletRequest request,
                                                         Parameter parameter) {
        Map<String, Object> params = new LinkedHashMap<>();
        String queryKey = parameter.getAnnotation(QueryParam.class).value();

        List<String> queryParamStrWithKeys = Arrays.stream(request.getQueryString().split("&"))
                                                   .filter(it -> it.contains(queryKey + "="))
                                                   .collect(Collectors.toList());

        if (queryParamStrWithKeys.isEmpty()) {
            throw new RuntimeException("not found query params: " + queryKey);
        }

        if (parameter.getType().equals(List.class)) {
            List<String> values = queryParamStrWithKeys.stream()
                                                       .map(queryParamStrWithKey -> {
                                                           String[] splitQuery =
                                                                   queryParamStrWithKey.split("=");
                                                           return splitQuery[1];
                                                       })
                                                       .filter(it -> !it.isEmpty())
                                                       .collect(Collectors.toList());
            params.put(queryKey, values);
        } else {
            String[] splitQuery = queryParamStrWithKeys.get(0).split("=");

            if (splitQuery.length != 2) {
                throw new RuntimeException("query params can not be empty");
            }

            params.put(queryKey, parseParameterValue(splitQuery[1], parameter));
        }
        return params;
    }

    private static Map<String, Object> processPathParam(String methodPatternPath, String path,
                                                        Parameter parameter) {
        String key = parameter.getAnnotation(PathParam.class).value();
        String patternStr = methodPatternPath.replace("{" + key + "}", "(\\w+)")
                                             .replaceAll("\\{\\w+\\}", "\\\\w+");

        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(path);

        matcher.find();
        if (matcher.groupCount() > 0) {
            Object value = parseParameterValue(matcher.group(1), parameter);
            return new LinkedHashMap<>() {{
                put(key, value);
            }};
        }
        return null;
    }

    static private Object parseParameterValue(String value, Parameter parameter) {
        try {
            if (long.class.equals(parameter.getType())) {
                return Long.parseLong(value);
            }
            if (int.class.equals(parameter.getType())) {
                return Integer.parseInt(value);
            }
            return value;
        } catch (RuntimeException e) {
            throw new RuntimeException("can not cast params");
        }
    }
}
