package com.africapoa.fn.ds;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.africapoa.fn.utils.JsonUtil;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.africapoa.fn.utils.Log.log;



/**
 * A utility class for querying and manipulating JSON-like data structures.
 * Provides methods to parse, filter, and extract data from JSON strings, files,
 * input streams, or POJOs (Plain Old Java Objects). Supports JSON path expressions
 * for querying nested structures.
 * <p>
 * Warning: Use only for thoroughly tested cases as this implementation, while convenient,
 * may be unstable and subject to breaking changes.
 */
//ONLY USE for thoroughly tested cases, though this is convenient, it is still unstable and may break
@SuppressWarnings("unused")
public class JsonQ {
    private static final Gson gson = JsonUtil.getGson();
    private final Object root;
    private final PathEvaluator pathEvaluator=PathEvaluator.getInstance();
    private final BoolEvaluator BOOL = new BoolEvaluator();
    private static final Pattern VALUED_TRUE=PathType.VALUED_TRUE.getPattern();
    private static final Pattern VALUED_FALSE=PathType.VALUED_FALSE.getPattern();

    /**
     * Constructs a JsonQ instance with the given input as the root object.
     *
     * @param input The root object to query (can be a Map, List, or primitive)
     */
    private JsonQ(Object input) {
        root = input;
    }

    /**
     * Creates a JsonQ instance from a JSON string.
     *
     * @param json The JSON string to parse
     * @return A new JsonQ instance with the parsed JSON structure
     */
    public static JsonQ fromJson(String json) {
        return new JsonQ(val(json));
    }

    /**
     * Deserializes a JSON string into an object of the specified class.
     *
     * @param <T>   The type of the target object
     * @param json  The JSON string to deserialize
     * @param clazz The class of the target object
     * @return An instance of the specified class populated with the JSON data
     */
    public static <T> T fromJson(String json,Class<T> clazz) {
        return gson.fromJson(json,clazz);
    }

    /**
     * Creates a JsonQ instance from an InputStream containing JSON data.
     *
     * @param jsonStream The InputStream with JSON data
     * @return A new JsonQ instance with the parsed JSON structure
     */
    public static JsonQ fromIO(InputStream jsonStream) {
        return new JsonQ(val(stringFromIO(jsonStream)));
    }

    public static JsonQ fromURL(String urlString) {
        try( HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            return JsonQ.fromIO(response.body());
        } catch (IOException | InterruptedException e) {
            log(e);
            Thread.currentThread().interrupt();
        }
        return new JsonQ("");
    }


    /**
     * Creates a JsonQ instance from a File containing JSON data.
     *
     * @param jsonFile The File with JSON data
     * @return A new JsonQ instance with the parsed JSON structure
     */
    public static JsonQ fromIO(File jsonFile) {
        if(!jsonFile.exists()) return new JsonQ("");
        return new JsonQ(val(stringFromIO(jsonFile)));
    }

    /**
     * Creates a JsonQ instance from a Plain Old Java Object (POJO).
     *
     * @param object The POJO to wrap
     * @return A new JsonQ instance with the POJO as the root
     */
    public static JsonQ fromPOJO(Object object) {
        return new JsonQ(isPrimitive(object) ? object : getObjectRoot(object));
    }

    /**
     * Reads a string from an InputStream or File.
     *
     * @param input The InputStream or File to read from
     * @return The string content, or an empty string if reading fails
     */
    private static String stringFromIO(Object input) {
        if (!(input instanceof File || input instanceof InputStream)) {
            return "";
        }
        try (BufferedReader reader = input instanceof File
                ? new BufferedReader(new FileReader((File) input))
                : new BufferedReader(new InputStreamReader((InputStream) input, StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString();
        } catch (IOException e) {log(e);return "";}
    }

    public boolean toFile(File file) {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(this.toString());
            return true;}
        catch (IOException e) { log(e); return false;}
    }



    /**
     * Parses a JSON string into a Java object (Map, List, or primitive).
     *
     * @param jsonRoot The JSON string to parse
     * @return The parsed object (Map for objects, List for arrays, or primitive value)
     */
    private static Object val(String jsonRoot) {
        JsonElement element = JsonParser.parseString(jsonRoot);
        Type objectType = new TypeToken<Map<String, Object>>() {
        }.getType();
        Type arrayType = new TypeToken<List<Object>>() {
        }.getType();

        return element.isJsonObject() ? gson.fromJson(element, objectType)
                : element.isJsonArray() ? gson.fromJson(element, arrayType)
                : element.isJsonPrimitive() ? valPrimitive(element.getAsJsonPrimitive())
                : null;
    }

    /**
     * Converts a JsonPrimitive to a Java primitive type.
     *
     * @param <T>      The expected primitive type
     * @param primitive The JsonPrimitive to convert
     * @return The converted value (Boolean, Number, String, or null)
     */
    @SuppressWarnings("unchecked")
    private static <T> T valPrimitive(JsonPrimitive primitive) {
        return primitive.isBoolean() ? (T) Boolean.valueOf(primitive.getAsBoolean())
                : primitive.isNumber() ? (T) primitive.getAsNumber()
                : primitive.isString() ? (T) primitive.getAsString()
                : null;
    }

    /**
     * Retrieves a list of transformed values from a JSON path.
     *
     * @param <T>     The type of the transformed values
     * @param jsonPath The JSON path to query
     * @param changer A function to transform each found object
     * @return A list of transformed values
     */
    private <T> List<T> getListImpl(String jsonPath, JFunction<Object, T> changer) {
        List<T> list = new ArrayList<>();
        flatForEach(find(jsonPath), (key, obj) -> list.add(changer.apply(obj)));
        return list;
    }

    /**
     * Retrieves a list of transformed Map values from a JSON path.
     *
     * @param <T>     The type of the transformed values
     * @param jsonPath The JSON path to query
     * @param changer A function to transform each Map object
     * @return A list of transformed values
     */
    public <T> List<T> getList(String jsonPath, JFunction<Map<?, ?>, T> changer) {
        List<T> list = new ArrayList<>();
        flatForEach(find(jsonPath), (key, obj) -> {
            if (obj instanceof Map<?, ?>)
                list.add(changer.apply((Map<?, ?>) obj));
        });
        return list;
    }

    /**
     * Selects specific columns from a collection of objects.
     *
     * @param columns The names of the columns to select
     * @return A new JsonQ instance with the selected data
     */
    public JsonQ select(String ...columns) {
        List<Object> results=new ArrayList<>();
        collectionForEach(root,(k,v)->{
            if(v instanceof Map<?,?>){
                //noinspection unchecked
                Map<String,Object>data=(Map<String,Object>)v;
                Map<String,Object> selection=new HashMap<>();
                for(String key:columns){
                       data.get(key);
                       selection.put(key,data.get(key));
                }
                results.add(selection);
            }
        });
        return fromResults(results);
    }
    /**
     * Extracts a column of integers from the JSON data.
     *
     * @param columnName The name of the column to extract
     * @return A list of integers from the specified column
     */
    public List<Integer> intColumn(String columnName) {
        String path= String.format("[(@.%s~'\\d+\\.?\\d*')].%s",columnName,columnName);
        return fromResults(getListImpl(path, a -> a instanceof Number ? ((Number) a).intValue() : null)).val();
    }
    /**
     * Extracts a column of integers from the JSON data.
     *
     * @param path The path of the column to extract
     * @return A list of integers from the specified column
     */
    public List<Integer> integers(String path) {
        return fromResults(getListImpl(path, a -> a instanceof Number ? ((Number) a).intValue() : null)).val();
    }


    /**
     * Extracts a column of dates from the JSON data.
     *
     * @param columnName The name of the column to extract
     * @return A list of Date objects from the specified column
     */
    public List<Date> dateColumn(String columnName) {
        String path= String.format("[(@.%s~'\\d{2,4}-\\d{2}-\\d{2,4}')].%s",columnName,columnName);
        return getDates(path);
    }
    /**
     * Retrieves a list of Date objects from a JSON path.
     *
     * @param path The JSON path to query
     * @return A list of parsed Date objects
     */
    public List<Date> getDates(String path) {
        List<Date> dates = new ArrayList<>();
        for(String d:getStrings(path)){
            Date date=getDate(d);
            if(date!=null)dates.add(date);
        }
        return dates;
    }

    /**
     * Parses a string into a Date object using supported formats.
     *
     * @param date The date string to parse
     * @return The parsed Date object, or null if parsing fails
     */
    private Date getDate(String date){
        try {
            if(date.matches("\\d{4}-\\d{2}-\\d{2}"))
                return DATE_FORMATS.get(0).parse(date);
            else if (date.matches("\\d{2}-\\d{2}-\\d{4}"))
                return DATE_FORMATS.get(1).parse(date);
        }
        catch (ParseException e) {log(e);}
        return null;
    }

    /**
     * Extracts a column of strings from the JSON data.
     *
     * @param columnName The name of the column to extract
     * @return A list of strings from the specified column
     */
    public List<String> stringColumn(String columnName) {
        return getStrings("[*]."+columnName);
    }

    /**
     * Retrieves all strings from the root object.
     *
     * @return A list of all string values
     */
    public List<String> getStrings() {return getStrings("[*]");}

    /**
     * Retrieves a list of strings from a formatted JSON path.
     *
     * @param jsonPath The JSON path template
     * @return A list of string values
     */
    public List<String> getStrings(String jsonPath) {
        return getListImpl(jsonPath, a -> (a instanceof String) ? (String) a : gson.toJson(a));
    }

    /**
     * Retrieves a list of strings from a formatted JSON path.
     *
     * @param jsonPath The JSON path template
     * @param args     Arguments to format the path
     * @return A list of string values
     */
    public List<String> getStrings(String jsonPath, Object... args) {
        return getStrings(String.format(jsonPath, args));
    }

    /**
     * Returns the root object as a JSON string.
     *
     * @return The JSON string representation of the root
     */
    public String str() {return str("");}

    /**
     * Retrieves a string value from a formatted JSON path.
     *
     * @param jsonPath The JSON path template
     * @param args     Arguments to format the path
     * @return The string value, or an empty string if not found
     */
    public String str(String jsonPath, Object... args) {
        return str(String.format(jsonPath, args));
    }

    public String str(String jsonPath) {
        Object obj = first(jsonPath, o -> o);
        return obj instanceof String ? (String) obj : obj == null ? "" : gson.toJson(obj);
    }

    /**
     * Queries the JSON data with a formatted path.
     *
     * @param path The JSON path template
     * @param args Arguments to format the path
     * @return A new JsonQ instance with the query results
     */
    public JsonQ get(String path, Object... args) {
        return get(String.format(path, args));
    }

    /**
     * Filters the JSON data based on a condition.
     *
     * @param condition The condition string (e.g., "@.age > 18")
     * @param values    Values to replace placeholders in the condition
     * @return A new JsonQ instance with the filtered results
     */
    public JsonQ where(String condition, Object ... values){
        condition=condition.replaceAll("\\band\\b","&&")
                .replaceAll("\\bor\\b","||")
                .replaceAll("\\$?(\\w+)","@.$1")
                .replaceAll("=+","==");

        for(Object v :values){
            if(!isPrimitive(v)) continue;
            String value = v instanceof String? String.format("'%s'",v): String.valueOf(v);
            condition=condition.replaceFirst("\\?",escapeRGX(value));
        }
        condition=String.format("(%s)",condition);
        List<Object> results=filter(condition,root,new ArrayList<>());
        return fromResults(results);
    }
    /**
     * Escapes special characters in a string for use in regular expressions.
     *
     * @param input The string to escape
     * @return The escaped string
     */

    private String escapeRGX(String input){
       return input.replace("\\","\\\\");
    }

    /**
     * Creates a JsonQ instance from a list of results, cleaning null values.
     *
     * @param results The list of results
     * @return A new JsonQ instance with the cleaned results
     */
    @SuppressWarnings("Java8CollectionRemoveIf")
    private JsonQ fromResults(List<Object> results){
        Iterator<Object> it=results.listIterator();
        while(it.hasNext()){
            Object o=it.next();
            if(o==null)it.remove();
        }
        return fromPOJO(results.size()==1?results.getFirst():results);

    }

    public JsonQ get(String path) {
        return fromResults(find(path));
    }

    /**
     * Returns the root object, cast to the desired type.
     *
     * @param <T> The expected type
     * @return The root object
     */
    public <T> T val() {
        //noinspection unchecked
        return isEmpty()?null:(T) root;}


    public Integer asInt(String jsonPath) {
        Object x=get(jsonPath).root;
        return x instanceof Number? ((Number)x).intValue():null;
    }

    public int asInt() {
        return asInt(".");
    }

    /**
     * Checks if the JSON data is empty.
     *
     * @return True if the data is empty, false otherwise
     */
    public boolean isEmpty() {
        return root == null || (root instanceof Map && ((Map<?, ?>) root).isEmpty()) ||
                (root instanceof Collection && ((Collection<?>) root).isEmpty()) ||
                (root instanceof String && ((String) root).isEmpty());
    }

    /**
     * Checks if the JSON data has content.
     *
     * @return True if the data is not empty, false otherwise
     */
    public boolean hasStuff(){ return !isEmpty(); }
    /**
     * Checks if the JSON data is not empty.
     *
     * @return True if the data is not empty, false otherwise
     */
    public boolean notEmpty(){ return !isEmpty(); }

    /**
     * Returns a string representation of the JSON data.
     *
     * @return The JSON string representation
     */
    @Override
    public String toString() {
        return root instanceof String ? (String) root : gson.toJson(root);
    }

    /**
     * Retrieves the first value from a JSON path and applies a transformation.
     *
     * @param <T>      The type of the transformed value
     * @param jsonPath The JSON path to query
     * @param changer  A function to transform the found object
     * @return The transformed value, or null if not found
     */
    public <T> T first(String jsonPath, JFunction<Object, T> changer) {
        JsonQ jq = get(jsonPath);
        if (!jq.isEmpty()) {
            return changer.apply(jq.root);
        }
        return null;
    }

    /**
     * Adds or updates a value at a JSON path.
     *
     * @param jsonPath The JSON path to modify
     * @param value    The value to set
     */
    public void put(String jsonPath, Object value) {
        int x = jsonPath.lastIndexOf(".");
        String prop = jsonPath.substring(x < 0 ? 0 : x + 1);
        put(jsonPath, true, prop, value);
    }
    /**
     * Adds a value to the root list (if it is a list).
     *
     * @param value The value to add
     */
    public void add( Object value) {
        put("", false, "", value);
    }

    /**
     * Adds or updates multiple values at a JSON path.
     *
     * @param jsonPath The JSON path to modify
     * @param override Whether to override existing values
     * @param values   Pairs of keys and values to set
     */
    public void put(String jsonPath, boolean override, Object... values) {
        int x = jsonPath.lastIndexOf(".");
        String prop = jsonPath.substring(x < 0 ? 0 : x + 1);
        Object res = find(jsonPath.substring(0, Math.max(x, 0)));
        flatForEach(res, (k, v) -> put(override, v, values));
    }

        public void putNoNull(String jsonPath, Object value) {
                if(value==null) return;
                put(jsonPath, value);
            }

    /**
     * @noinspection unchecked
     */
    private void put(boolean override, Object container, Object... values) {
        boolean isMap = container instanceof Map<?, ?>;
        boolean isList = container instanceof List<?>;

        for (int i = 0; i < values.length; i += 2) {
            Object key = values[i];
            Object val = values[i + 1];
            val = val instanceof JsonQ ? ((JsonQ) val).val() : val;
            if (isMap && !(values[i] instanceof String)) {
                throw new IllegalArgumentException("attempting to set values to a JSON object without a key");
            }
            if (isList && !(values[i] instanceof Integer || values[i].toString().matches("\\$|"))) {
                throw new IllegalArgumentException("attempting to set values to a JSON array without a valid integer index");
            }
            if (isMap) {
                Map<String, Object> map = (Map<String, Object>) container;
                if (!map.containsKey((String) key) || override)
                    (map).put((String) key, val);
            }
            if (isList) {
                List<Object> list = (List<Object>) container;
                boolean isValidKey = key instanceof Integer && (int) key >= 0 && (int) key < list.size();

                if (isValidKey && override) list.add((int) key, val);
                else list.add(val);
            }
        }
    }

    private PathHandler getPathHandler(String fullPath, String path){
        return switch (pathEvaluator.getMatching(path)) {
            case REGULAR_PATH -> this::handleNormalPath;
            case GLOBED_PATH -> this::globedPath;
            case PATH_EXPRESSION -> this::filter;
            case WILDCARD -> this::findMatchingPath;
            case ARRAY -> this::handleArrayMatch;
            default -> {
                log("Path not found %s. When processing this part %s", fullPath, path);
                yield null;
            }
        };
    }

    private List<Object> find(String jsonPath) {
        List<Object> results = new ArrayList<>();
        if (jsonPath.matches("\\.|")) return Collections.singletonList(root);
        if (isPrimitive(root)) return results;

        List<String> paths = pathEvaluator.evaluatePath(jsonPath);
        results.add(root);

        List<Object> temp = new ArrayList<>();
        for (String path : paths) {
            temp.clear();
            PathHandler func=getPathHandler(jsonPath,path);
            if(results.isEmpty()||func==null){return Collections.emptyList();}

            collectionForEach(results,(key,obj)-> func.handle(path,obj,temp));
            results.clear();
            results.addAll(temp);
        }
        results.clear();
        for (Object o : temp) {
            if (o == null) continue;
            results.add(o);
        }
        return results;
    }

    private void collectionForEach(Object input, Taker<Object> consumer) {
        if (input instanceof Collection<?>) {
            flatForEach(input, consumer);
        } else consumer.take("", input);
    }

    private void handleArrayMatch(String path, Object object, List<Object> results) {
        Matcher parts = PathType.ARRAY.getPattern().matcher(path);
        if (!parts.find()) return;
        if (parts.group(3) != null) {
            collectionForEach(object, (k, v) -> results.add(v));
        } else if (parts.group(1) != null) {
            filter(parts.group(1), object, results);
        } else if (parts.group(2) != null) {
            collectionForEach(object, (k, v) -> results.add(v));
            sliceList(results, parts.group(2));
        } else if (parts.group(4) != null) {
            results.add(valueAtKey(parts.group(6), object));
        }
    }

    private static <T> void sliceList(List<T> list, @Nullable String sliceNotation) {
        if (sliceNotation == null || list.isEmpty()) return;

        List<T> result = new ArrayList<>();
        String[] slices = sliceNotation.split(",");

        for (String slice : slices) {
            String[] parts = slice.split(":",-1);
            boolean singleIndex = parts.length == 1 && !parts[0].isEmpty();
            int len= list.size();
            int start = parts.length > 0 && !parts[0].isEmpty() ? Integer.parseInt(parts[0]) : 0;
            int end = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : list.size();
            start = Math.max( start < 0 ? len + start : start , 0);
            end = singleIndex ? start + 1 : Math.min( end < 0 ? len + end : end ,len);
            for (int i = start; i < end; i++) {
                result.add(list.get(i));
            }
        }
        list.clear();
        list.addAll(result);
    }

    private static boolean isPrimitive(Object o) {
        return o instanceof Number || o instanceof String || o instanceof Boolean;
    }


    private List<Object> filter(String expression, Object object,List<Object> results) {
        collectionForEach(object, (key, obj) -> {
            obj = getObjectRoot(obj);
            boolean evaluation = false;
            if (obj instanceof Map<?, ?> json) {
                Matcher m = PathType.JSON_VARIABLE.getPattern().matcher(expression);
                String exp = expression;
                while (m.find()) {
                    String variable = m.group(1);
                    String val = prepVariableForExpression(json.get(variable));
                    if (val == null) return;
                    exp = exp.replace("@." + variable, val);
                }
                evaluation = BOOL.evaluate(exp.replaceAll("[]\\[]", ""));
            } else if (isPrimitive(obj) && !key.isEmpty()) {
                String val = prepVariableForExpression(obj);
                if (val == null) return;
                evaluation = BOOL.evaluate(expression.replace("@." + key, val));
            }
            if (evaluation) results.add(obj);
        });
        return results;
    }

    private static Object getObjectRoot(Object object) {
        return object == null ? null
                : object instanceof Map<?, ?> || object instanceof List<?> ? object
                : object instanceof JsonQ ? ((JsonQ) object).root
                : val(gson.toJson(object));
    }

    private String prepVariableForExpression(Object val) {
        if (!(val instanceof String v)) {
            return isPrimitive(val) ? String.valueOf(val) : null;
        }
        return VALUED_TRUE.matcher(v).matches() ? "true"
                : VALUED_FALSE.matcher(v).matches() ? "false"
                : String.format("'%s'", v);
    }

    private void handleNormalPath(String path, Object object,List<Object> results) {
        if (path == null || path.isEmpty()) {
            results.add(object);return;
        }

        Object current = object;
        for (String p : path.split("\\."))
            current = valueAtKey(p, current);
        results.add(current == object ? null : current);
    }

    private void globedPath(String path, Object jsonThing,List<Object> results){
        flatForEach(jsonThing,(k,v)->{
            if(k.matches(path.replace("*","\\w*"))){
                results.add(v);
            }
        });
    }

    private void findMatchingPath(String path, Object root, List<Object> results) {
        Deque<Object> stack = new ArrayDeque<>();
        Set<Object> seen = new HashSet<>();
        stack.push(root);
        path = path.replaceAll("^[^\\w*]+", "");
        while (!stack.isEmpty()) {
            Object current = stack.pop();

            if(!path.contains("*"))
                handleNormalPath(path, current,results);
            else
                globedPath(path.replace("*","\\w*"),current,results);

            flatForEach(current, (key, obj) -> {
                if (obj == null || seen.contains(obj)) return;
                stack.push(obj);
                seen.add(obj);
            });
        }
    }

    private Object valueAtKey(String key, Object jsonThing) {
        return jsonThing instanceof Map<?, ?> ? ((Map<?, ?>) jsonThing).get(key)
                : jsonThing instanceof List && PathType.INTEGER.getPattern().matcher(key).matches() ? ((List<?>) jsonThing).get(Integer.parseInt(key))
                : null;
    }

    public void forEach(Taker<JsonQ> taker) {
        flatForEach(root, (k, v) -> {
            if (v != root) {
                taker.take(k, fromPOJO(v));
            }
        });
    }

    private void flatForEach(Object input, Taker<Object> consumer) {
        if (input instanceof Map<?, ?> data) {
            for (Map.Entry<?, ?> entry : data.entrySet()) {
                Object prop = entry.getValue();
                if (prop != null) consumer.take(entry.getKey().toString(), prop);
            }
        } else if (input instanceof List<?> data) {
            for (int i = 0, len = data.size(); i < len; i++) {
                Object obj = data.get(i);
                if (obj != null) consumer.take(String.valueOf(i), obj);
            }
        } else if (input != null) consumer.take("", input);
    }

    private interface PathHandler { void handle(String path, Object jsonThing, List<Object> results);}
    public interface Taker<T> { void take(String key, T t);}
    public interface JFunction<S, T> { T apply(S s);}

    private static final List<SimpleDateFormat> DATE_FORMATS=Arrays. asList(
            new SimpleDateFormat("yyyy-MM-dd",Locale.ENGLISH ),
            new SimpleDateFormat("dd-MM-yyyy" ,Locale.ENGLISH)
    );


}