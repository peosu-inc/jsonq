package com.peosu.fn.ds;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.peosu.fn.utils.JsonUtil;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

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

import static com.peosu.fn.utils.Log.log;



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
     * Creates a JsonQ instance from a File containing JSON or YAML data.
     * Automatically detects YAML files by extension (.yml or .yaml).
     *
     * @param file The File with JSON or YAML data
     * @return A new JsonQ instance with the parsed data structure
     */
    public static JsonQ fromIO(File file) {
        if (!file.exists()) return new JsonQ("");
        String name = file.getName().toLowerCase();
        if (name.endsWith(".yml") || name.endsWith(".yaml")) {
            return fromYamlFile(file);
        }
        return new JsonQ(val(stringFromIO(file)));
    }

    /**
     * Creates a JsonQ instance from a YAML file.
     *
     * @param yamlFile The File with YAML data
     * @return A new JsonQ instance with the parsed YAML structure
     */
    private static JsonQ fromYamlFile(File yamlFile) {
        try (InputStream is = new FileInputStream(yamlFile)) {
            return fromYamlIO(is);
        } catch (IOException e) {
            log(e);
            return new JsonQ("");
        }
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

    // ===== YAML Support =====

    /**
     * Creates a JsonQ instance from a YAML string.
     *
     * @param yaml The YAML string to parse
     * @return A new JsonQ instance with the parsed YAML structure
     */
    public static JsonQ fromYaml(String yaml) {
        try {
            Yaml yamlParser = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object data = yamlParser.load(yaml);
            return new JsonQ(data);
        } catch (Exception e) {
            log(e);
            return new JsonQ("");
        }
    }

    /**
     * Creates a JsonQ instance from an InputStream containing YAML data.
     *
     * @param yamlStream The InputStream with YAML data
     * @return A new JsonQ instance with the parsed YAML structure
     */
    public static JsonQ fromYamlIO(InputStream yamlStream) {
        try {
            Yaml yamlParser = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object data = yamlParser.load(yamlStream);
            return new JsonQ(data);
        } catch (Exception e) {
            log(e);
            return new JsonQ("");
        }
    }

    /**
     * Returns a YAML string representation of the data.
     *
     * @return The YAML string representation
     */
    public String toYaml() {
        return toYaml(2);
    }

    /**
     * Returns a YAML string representation with configurable indentation.
     *
     * @param indent The number of spaces to use for indentation
     * @return The YAML string representation
     */
    public String toYaml(int indent) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(Math.max(indent, 1));
        options.setIndicatorIndent(Math.max(indent - 1, 0));
        Yaml yaml = new Yaml(options);
        return yaml.dump(root);
    }

    /**
     * Prints the data as YAML to stdout.
     */
    public void printYaml() {
        System.out.println(toYaml());
    }

    /**
     * Prints the data as YAML to stdout with configurable indentation.
     *
     * @param indent The number of spaces to use for indentation
     */
    public void printYaml(int indent) {
        System.out.println(toYaml(indent));
    }

    /**
     * Writes YAML data to a file.
     *
     * @param file The file to write to
     * @return true if successful, false otherwise
     */
    public boolean toYamlFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(toYaml());
            return true;
        } catch (IOException e) {
            log(e);
            return false;
        }
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
                       String actualKey = key.contains(" as ") ? key.split(" as ")[0].trim() : key;
                       String alias = key.contains(" as ") ? key.split(" as ")[1].trim() : key;
                       selection.put(alias, data.get(actualKey));
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
       return Matcher.quoteReplacement(input);
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
        return isEmpty()?null:(T) root;
    }

    /**
     * Returns the root object, cast to the desired type.
     * Alias for val() with clearer naming.
     *
     * @param <T> The expected type
     * @return The root object, or null if empty
     */
    public <T> T getValue() {
        return val();
    }

    /**
     * Returns an Optional containing the root object.
     *
     * @param <T> The expected type
     * @return Optional containing the value, or empty if null/empty
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptional() {
        return isEmpty() ? Optional.empty() : Optional.ofNullable((T) root);
    }

    /**
     * Returns the value at the given path, or defaultValue if not found.
     *
     * @param <T> The expected type
     * @param path The JSON path
     * @param defaultValue The default value to return if path not found
     * @return The value at the path, or defaultValue
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String path, T defaultValue) {
        JsonQ result = get(path);
        return result.isEmpty() ? defaultValue : (T) result.root;
    }

    /**
     * Returns the string value at the given path.
     * Alias for str() with clearer naming.
     *
     * @param path The JSON path
     * @return The string value, or empty string if not found
     */
    public String getString(String path) {
        return str(path);
    }

    /**
     * Returns the string value at the given path, or defaultValue if not found.
     *
     * @param path The JSON path
     * @param defaultValue The default value if path not found or empty
     * @return The string value, or defaultValue
     */
    public String getStringOrDefault(String path, String defaultValue) {
        String result = str(path);
        return result.isEmpty() ? defaultValue : result;
    }

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
     * @deprecated Use {@link #isNotEmpty()} instead
     */
    @Deprecated
    public boolean hasStuff(){ return !isEmpty(); }

    /**
     * Checks if the JSON data is not empty.
     *
     * @return True if the data is not empty, false otherwise
     * @deprecated Use {@link #isNotEmpty()} instead
     */
    @Deprecated
    public boolean notEmpty(){ return !isEmpty(); }

    /**
     * Checks if the JSON data is not empty.
     *
     * @return True if the data is not empty, false otherwise
     */
    public boolean isNotEmpty(){ return !isEmpty(); }

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

    /**
     * Puts a value at the specified path only if the value is not null.
     *
     * @param jsonPath The JSON path to modify
     * @param value The value to set (ignored if null)
     * @deprecated Use {@link #putIfPresent(String, Object)} instead
     */
    @Deprecated
    public void putNoNull(String jsonPath, Object value) {
        putIfPresent(jsonPath, value);
    }

    /**
     * Puts a value at the specified path only if the value is not null.
     *
     * @param jsonPath The JSON path to modify
     * @param value The value to set (ignored if null)
     */
    public void putIfPresent(String jsonPath, Object value) {
        if (value == null) return;
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
            if (isList && !(values[i] instanceof Integer || values[i].toString().matches("\\$.*"))) {
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

    // ===== New methods ported from Python jsonq.py =====

    /**
     * Creates a JsonQ instance from all JSON files in a folder.
     *
     * @param folder The folder containing JSON files
     * @return A new JsonQ instance with an array of all parsed JSON data
     */
    public static JsonQ fromFolder(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            return new JsonQ(new ArrayList<>());
        }
        List<Object> results = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    JsonQ jq = fromIO(file);
                    if (jq.notEmpty()) {
                        results.add(jq.root);
                    }
                }
            }
        }
        return new JsonQ(results);
    }

    /**
     * Creates a JsonQ instance from all JSON files in a folder.
     *
     * @param folderPath The path to the folder containing JSON files
     * @return A new JsonQ instance with an array of all parsed JSON data
     */
    public static JsonQ fromFolder(String folderPath) {
        return fromFolder(new File(folderPath));
    }

    /**
     * Returns the raw value at the specified path without wrapping in JsonQ.
     *
     * @param <T>  The expected type
     * @param path The JSON path to query
     * @return The raw value at the path, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T value(String path) {
        List<Object> results = find(path);
        if (results.isEmpty()) return null;
        return results.size() == 1 ? (T) results.getFirst() : (T) results;
    }

    /**
     * Returns the raw list of results from a JSON path query.
     *
     * @param path The JSON path to query
     * @return The list of matching objects
     */
    public List<Object> findRaw(String path) {
        return find(path);
    }

    /**
     * Returns a JSON string representation with configurable indentation.
     *
     * @param indent The number of spaces to use for indentation
     * @return The formatted JSON string
     */
    public String toString(int indent) {
        if (root instanceof String) return (String) root;

        // Compact (no newlines)
        if (indent <= 0) {
            Gson compactGson = new GsonBuilder().create();
            return compactGson.toJson(root);
        }

        // Pretty print with default 2-space indent
        Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
        if (indent == 2) {
            return prettyGson.toJson(root);
        }

        // Custom indent by replacing the default 2-space indent
        String json = prettyGson.toJson(root);
        String spaces = " ".repeat(indent);
        Pattern indentPattern = Pattern.compile("(?m)^(\\s{2})+");
        Matcher matcher = indentPattern.matcher(json);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement = spaces.repeat(matcher.group().length() / 2);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Prints the JSON data to stdout.
     */
    public void print() {
        System.out.println(this);
    }

    /**
     * Prints the JSON data to stdout with configurable indentation.
     *
     * @param indent The number of spaces to use for indentation
     */
    public void print(int indent) {
        System.out.println(toString(indent));
    }

    /**
     * Modifies a value at the specified path using a transformation function.
     *
     * @param path The JSON path to the value to modify
     * @param func The transformation function to apply
     */
    public void change(String path, JFunction<Object, Object> func) {
        Object current = get(path).root;
        Object newValue = func.apply(current);
        put(path, newValue);
    }

    /**
     * Adds a value to the list at the specified path.
     *
     * @param path  The JSON path to the list
     * @param value The value to add
     */
    public void add(String path, Object value) {
        List<Object> targets = find(path);
        boolean appended = false;
        for (Object target : targets) {
            Object container = getObjectRoot(target);
            if (container instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) container;
                list.add(value);
                appended = true;
            }
        }
        if (!appended && !path.isEmpty()) {
            put(path, value);
        }
    }

    /**
     * Merges a value into the node(s) at the specified path.
     *
     * @param path       The JSON path to merge into
     * @param value      The value to merge
     * @param listPolicy How to handle list merging: "extend" (default), "replace", or "append"
     */
    @SuppressWarnings("unchecked")
    public void merge(String path, Object value, String listPolicy) {
        List<Object> targets = find(path);
        boolean merged = false;

        for (Object target : targets) {
            Object container = getObjectRoot(target);
            if (container instanceof Map || container instanceof List) {
                mergeValues(container, getObjectRoot(value), listPolicy);
                merged = true;
            }
        }

        if (!merged && !path.isEmpty()) {
            // Try to merge into parent
            int lastDot = path.lastIndexOf('.');
            int lastBracket = path.lastIndexOf('[');
            int splitPos = Math.max(lastDot, lastBracket);

            if (splitPos > 0) {
                String parentPath = path.substring(0, splitPos);
                String field = lastDot > lastBracket
                        ? path.substring(lastDot + 1)
                        : path.substring(lastBracket).replaceAll("[\\[\\]\"]", "");

                List<Object> parents = find(parentPath);
                for (Object parent : parents) {
                    Object container = getObjectRoot(parent);
                    if (container instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) container;
                        if (!map.containsKey(field) || map.get(field) == null) {
                            map.put(field, value);
                        } else {
                            map.put(field, mergeValues(map.get(field), value, listPolicy));
                        }
                    }
                }
            } else {
                put(path, value);
            }
        }
    }

    /**
     * Merges a value into the node(s) at the specified path with default "extend" policy.
     *
     * @param path  The JSON path to merge into
     * @param value The value to merge
     */
    public void merge(String path, Object value) {
        merge(path, value, "extend");
    }

    @SuppressWarnings("unchecked")
    private Object mergeValues(Object target, Object incoming, String listPolicy) {
        Object targetRoot = getObjectRoot(target);
        Object incomingRoot = getObjectRoot(incoming);

        if (targetRoot instanceof Map && incomingRoot instanceof Map) {
            Map<String, Object> targetMap = (Map<String, Object>) targetRoot;
            Map<String, Object> incomingMap = (Map<String, Object>) incomingRoot;

            for (Map.Entry<String, Object> entry : incomingMap.entrySet()) {
                String key = entry.getKey();
                Object incomingVal = entry.getValue();

                if (targetMap.containsKey(key)) {
                    Object targetVal = targetMap.get(key);
                    if ((targetVal instanceof Map || targetVal instanceof List)
                            && (incomingVal instanceof Map || incomingVal instanceof List)) {
                        targetMap.put(key, mergeValues(targetVal, incomingVal, listPolicy));
                    } else {
                        targetMap.put(key, incomingVal);
                    }
                } else {
                    targetMap.put(key, incomingVal);
                }
            }
            return targetRoot;
        }

        if (targetRoot instanceof List) {
            List<Object> targetList = (List<Object>) targetRoot;
            mergeIntoList(targetList, incomingRoot, listPolicy);
            return targetRoot;
        }

        return incomingRoot;
    }

    @SuppressWarnings("unchecked")
    private void mergeIntoList(List<Object> target, Object incoming, String listPolicy) {
        if ("replace".equals(listPolicy) && incoming instanceof List) {
            target.clear();
            target.addAll((List<Object>) incoming);
            return;
        }

        if (incoming instanceof List) {
            if ("append".equals(listPolicy)) {
                target.add(incoming);
            } else { // default: extend
                target.addAll((List<Object>) incoming);
            }
        } else {
            target.add(incoming);
        }
    }

    /**
     * Applies multiple merge operations from a path-value mapping.
     *
     * @param mapping    A map of JSON paths to values to merge
     * @param listPolicy How to handle list merging
     */
    public void mergeMany(Map<String, Object> mapping, String listPolicy) {
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            String path = entry.getKey() == null ? "" : entry.getKey();
            merge(path, entry.getValue(), listPolicy);
        }
    }

    /**
     * Applies multiple merge operations with default "extend" policy.
     *
     * @param mapping A map of JSON paths to values to merge
     */
    public void mergeMany(Map<String, Object> mapping) {
        mergeMany(mapping, "extend");
    }

    /**
     * Returns all keys at the given path.
     *
     * @param path The JSON path to query
     * @return A list of keys
     */
    public List<String> keys(String path) {
        return keys(path, false);
    }

    /**
     * Returns all keys at the given path.
     *
     * @param path    The JSON path to query
     * @param trimmed If true, returns only the final key name without the full path
     * @return A list of keys
     */
    public List<String> keys(String path, boolean trimmed) {
        Map<String, Object> leafMap = leaves(path);
        if (trimmed) {
            return leafMap.keySet().stream()
                    .map(k -> k.replaceAll(".+\\.(\\w+)$", "$1"))
                    .distinct()
                    .toList();
        }
        return new ArrayList<>(leafMap.keySet());
    }

    /**
     * Returns all leaf (primitive) values as a flat map of path to value.
     *
     * @param path The JSON path to start from
     * @return A map of full paths to their primitive values
     */
    public Map<String, Object> leaves(String path) {
        return leaves(path, null);
    }

    /**
     * Returns all leaf (primitive) values as a flat map, filtered by a predicate.
     *
     * @param path      The JSON path to start from
     * @param predicate A predicate to filter leaves (path, value) -> boolean, or null for all
     * @return A map of full paths to their primitive values
     */
    public Map<String, Object> leaves(String path, BiPredicate<String, Object> predicate) {
        Map<String, Object> result = new LinkedHashMap<>();
        Object obj = get(path).root;

        if (isPrimitive(obj)) {
            if (predicate == null || predicate.test(path, obj)) {
                result.put(path, obj);
            }
            return result;
        }

        Set<String> seen = new HashSet<>();
        Deque<Map.Entry<String, Object>> stack = new ArrayDeque<>();
        stack.push(new AbstractMap.SimpleEntry<>(path, obj));

        while (!stack.isEmpty()) {
            Map.Entry<String, Object> entry = stack.pop();
            String currentPath = entry.getKey().replaceAll("^\\.", "");
            Object current = entry.getValue();

            if (seen.contains(currentPath)) continue;
            seen.add(currentPath);

            if (current instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    String newPath = currentPath.isEmpty()
                            ? e.getKey().toString()
                            : currentPath + "." + e.getKey();
                    if (e.getValue() != null) {
                        stack.push(new AbstractMap.SimpleEntry<>(newPath, e.getValue()));
                    }
                }
            } else if (current instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    String newPath = currentPath.isEmpty()
                            ? String.valueOf(i)
                            : currentPath + "." + i;
                    if (list.get(i) != null) {
                        stack.push(new AbstractMap.SimpleEntry<>(newPath, list.get(i)));
                    }
                }
            } else if (isPrimitive(current)) {
                if (predicate == null || predicate.test(currentPath, current)) {
                    result.put(currentPath, current);
                }
            }
        }
        return result;
    }

    /**
     * Fills a template with values from the current JSON data.
     * Template values starting with '$' are replaced with values from this JsonQ.
     *
     * @param template The template object (Map or List structure)
     * @return A new object with template values filled in
     */
    public Object fillTemplate(Object template) {
        JsonQ templateQ = fromPOJO(template);
        Map<String, Object> templateLeaves = templateQ.leaves("", (path, value) ->
                value instanceof String && ((String) value).startsWith("$"));

        for (Map.Entry<String, Object> entry : templateLeaves.entrySet()) {
            String templatePath = entry.getKey();
            String valuePath = ((String) entry.getValue()).substring(1); // Remove leading $

            // Try direct path first
            Object value = this.value(valuePath);
            if (value == null) {
                // Try bracket notation
                String queryPath = valuePath.replaceAll("([^.]+)", "[\"$1\"]").replace(".", "");
                value = this.value(queryPath);
            }

            // Put using dot notation path
            templateQ.put(templatePath, value);
        }
        return templateQ.root;
    }

    /**
     * Fills a template from a file with values from the current JSON data.
     *
     * @param templateFile The file containing the template JSON
     * @return A new object with template values filled in
     */
    public Object fillTemplate(File templateFile) {
        JsonQ templateQ = fromIO(templateFile);
        return fillTemplate(templateQ.root);
    }

    // ===== Path validation and aggregation methods =====

    /**
     * Checks if a path exists in the JSON data.
     *
     * @param path The JSON path to check
     * @return true if the path exists and has a value, false otherwise
     */
    public boolean exists(String path) {
        return !get(path).isEmpty();
    }

    /**
     * Returns the count of items in the root collection or at the specified path.
     *
     * @return The number of items
     */
    public int count() {
        if (root instanceof Collection) {
            return ((Collection<?>) root).size();
        } else if (root instanceof Map) {
            return ((Map<?, ?>) root).size();
        }
        return isEmpty() ? 0 : 1;
    }

    /**
     * Returns the count of items at the specified path.
     *
     * @param path The JSON path
     * @return The number of items
     */
    public int count(String path) {
        return get(path).count();
    }

    /**
     * Calculates the sum of numeric values at the specified path.
     *
     * @param path The JSON path to numeric values
     * @return The sum, or 0 if no numeric values found
     */
    public double sum(String path) {
        List<Object> values = find(path);
        return values.stream()
                .filter(v -> v instanceof Number)
                .mapToDouble(v -> ((Number) v).doubleValue())
                .sum();
    }

    /**
     * Calculates the sum of numeric values in the root collection.
     *
     * @return The sum, or 0 if no numeric values found
     */
    public double sum() {
        return sum(".");
    }

    /**
     * Calculates the average of numeric values at the specified path.
     *
     * @param path The JSON path to numeric values
     * @return The average, or 0 if no numeric values found
     */
    public double avg(String path) {
        List<Object> values = find(path);
        return values.stream()
                .filter(v -> v instanceof Number)
                .mapToDouble(v -> ((Number) v).doubleValue())
                .average()
                .orElse(0.0);
    }

    /**
     * Calculates the average of numeric values in the root collection.
     *
     * @return The average, or 0 if no numeric values found
     */
    public double avg() {
        return avg(".");
    }

    /**
     * Finds the minimum numeric value at the specified path.
     *
     * @param path The JSON path to numeric values
     * @return Optional containing the minimum, or empty if no numeric values
     */
    public Optional<Double> min(String path) {
        List<Object> values = find(path);
        return values.stream()
                .filter(v -> v instanceof Number)
                .mapToDouble(v -> ((Number) v).doubleValue())
                .min()
                .stream().boxed().findFirst();
    }

    /**
     * Finds the maximum numeric value at the specified path.
     *
     * @param path The JSON path to numeric values
     * @return Optional containing the maximum, or empty if no numeric values
     */
    public Optional<Double> max(String path) {
        List<Object> values = find(path);
        return values.stream()
                .filter(v -> v instanceof Number)
                .mapToDouble(v -> ((Number) v).doubleValue())
                .max()
                .stream().boxed().findFirst();
    }

    // ===== Sorting and limiting methods =====

    /**
     * Returns a new JsonQ with items sorted by the specified path.
     * Only works on root collections (List).
     *
     * @param path The path to sort by (e.g., "name" or "age")
     * @return A new JsonQ with sorted items
     */
    @SuppressWarnings("unchecked")
    public JsonQ orderBy(String path) {
        return orderBy(path, true);
    }

    /**
     * Returns a new JsonQ with items sorted by the specified path.
     *
     * @param path The path to sort by
     * @param ascending true for ascending order, false for descending
     * @return A new JsonQ with sorted items
     */
    @SuppressWarnings("unchecked")
    public JsonQ orderBy(String path, boolean ascending) {
        if (!(root instanceof List)) {
            return this;
        }
        List<Object> list = new ArrayList<>((List<Object>) root);
        list.sort((a, b) -> {
            Object valA = fromPOJO(a).value(path);
            Object valB = fromPOJO(b).value(path);
            int cmp = compareValues(valA, valB);
            return ascending ? cmp : -cmp;
        });
        return fromPOJO(list);
    }

    @SuppressWarnings("unchecked")
    private int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        if (a instanceof Comparable && b instanceof Comparable) {
            return ((Comparable<Object>) a).compareTo(b);
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    /**
     * Returns a new JsonQ with only the first n items.
     * Only works on root collections (List).
     *
     * @param n The maximum number of items to return
     * @return A new JsonQ with at most n items
     */
    @SuppressWarnings("unchecked")
    public JsonQ limit(int n) {
        if (!(root instanceof List)) {
            return this;
        }
        List<Object> list = (List<Object>) root;
        return fromPOJO(list.stream().limit(n).toList());
    }

    /**
     * Returns a new JsonQ with items after skipping the first n.
     * Only works on root collections (List).
     *
     * @param n The number of items to skip
     * @return A new JsonQ with items after the first n
     */
    @SuppressWarnings("unchecked")
    public JsonQ skip(int n) {
        if (!(root instanceof List)) {
            return this;
        }
        List<Object> list = (List<Object>) root;
        return fromPOJO(list.stream().skip(n).toList());
    }

    /**
     * Functional interface for predicates with two arguments.
     */
    public interface BiPredicate<T, U> {
        boolean test(T t, U u);
    }

    private interface PathHandler { void handle(String path, Object jsonThing, List<Object> results);}
    public interface Taker<T> { void take(String key, T t);}
    public interface JFunction<S, T> { T apply(S s);}

    private static final List<SimpleDateFormat> DATE_FORMATS=Arrays. asList(
            new SimpleDateFormat("yyyy-MM-dd",Locale.ENGLISH ),
            new SimpleDateFormat("dd-MM-yyyy" ,Locale.ENGLISH)
    );


}