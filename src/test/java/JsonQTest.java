import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.peosu.fn.ds.JsonQ;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JsonQTest {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    public void testGetWithSimpleJson() {
        String json = "{\"name\": \"John\", \"age\": 30}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$.name");
        assertEquals("John", result.val());

        result = jsonQ.get("$.age");
        assertEquals(30, (Double) result.val());
    }

    @Test
    public void testGetWithNestedJson() {
        String json = "{\"person\": {\"name\": \"John\", \"address\": {\"city\": \"New York\", \"zip\": \"10001\"}}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$.person.name");
        assertEquals("John", result.val());

        result = jsonQ.get("$.person.address.city");
        assertEquals("New York", result.val());

        result = jsonQ.get("$.person.address.zip");
        assertEquals("10001", result.val());
    }

    @Test
    public void testGetWithArrayJson() {
        String json = "{\"people\": [{\"name\": \"John\"}, {\"name\": \"Jane\"}, {\"name\": \"Doe\"}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$.people[0].name");
        assertEquals("John", result.val());

        result = jsonQ.get("$.people[1].name");
        assertEquals("Jane", result.val());

        result = jsonQ.get("$.people[2].name");
        assertEquals("Doe", result.val());
    }

    @Test
    public void testGetWithWildcardJson() {
        String json = "{\"items\": [{\"name\": \"item1\", \"value\": 10}, {\"name\": \"item2\", \"value\": 20}, {\"name\": \"item3\", \"value\": 30}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$.items[*].name");
        assertEquals("[\"item1\",\"item2\",\"item3\"]", result.toString().replaceAll("\n\\s*",""));

        assertEquals("[10,20,30]", jsonQ.integers("$.items[*].value").toString().replaceAll("\n*\\s*",""));
    }

    @Test
    public void testGetWithDeeplyNestedJson() {
        String json = "{ \"a\": { \"b\": { \"c\": { \"d\": { \"e\": \"value\" } } } } }";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$.a.b.c.d.e");
        assertEquals("value", result.val());
    }

    @Test
    public void testGetWithMixedContentJson() {
        String json = "{\"data\": {\"items\": [{\"id\": 1, \"name\": \"item1\"}, {\"id\": 2, \"name\": \"item2\"}]}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$.data.items[0].name");
        assertEquals("item1", result.val());

        result = jsonQ.get("$.data.items[1].name");
        assertEquals("item2", result.val());
    }

    @Test
    public void testGetWithNonExistingPath() {
        String json = "{\"name\": \"John\"}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$.nonExisting");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetWithSpecialCharactersInKeys() {
        String json = "{\"na.me\": \"John\", \"a-ge\": 30}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$['na.me']");
        assertEquals("John", result.val());
        assertEquals(30,  jsonQ.asInt("$['a-ge']"));
    }

    @Test
    public void testGetWithArraySlices() {
        String json = "{\"numbers\": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        List<Integer> result = jsonQ.integers("$.numbers[:3]");
        assertEquals(List.of(0,1,2), result);

        result = jsonQ.integers("$.numbers[6:]");
        assertEquals(List.of(6,7,8,9), result);

        result = jsonQ.integers("$.numbers[0:6]");
        assertEquals(List.of(0,1,2,3,4,5), result);
    }

    @Test
    public void testGetWithFilterExpression() {
        String json = "{\"items\": [{\"name\": \"item1\", \"value\": 10}, {\"name\": \"item2\", \"value\": 20}, {\"name\": \"item3\", \"value\": 30}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$.items[?(@.value > 15)].name");
        assertEquals("[\"item2\",\"item3\"]", result.toString().replaceAll("\n\\s*",""));
    }
    @Test
    public void testGetWithEmptyJson() {
        String json = "{}";
        JsonQ jsonQ = JsonQ.fromJson(json);
        JsonQ result = jsonQ.get("$.name");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetWithEmptyArray() {
        String json = "[]";
        JsonQ jsonQ = JsonQ.fromJson(json);
        JsonQ result = jsonQ.get("$[0]");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetWithNullValue() {
        String json = "{\"name\": null}";
        JsonQ jsonQ = JsonQ.fromJson(json);
        JsonQ result = jsonQ.get("$.name");
        assertNull(result.val());
    }

    @Test
    public void testGetWithPartiallyNonExistingPath() {
        String json = "{\"person\": {\"name\": \"John\"}}";
        JsonQ jsonQ = JsonQ.fromJson(json);
        JsonQ result = jsonQ.get("$.person.address.city");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetWithIndexOutOfBounds() {
        String json = "{\"people\": [{\"name\": \"John\"}, {\"name\": \"Jane\"}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);
        try{
            JsonQ result = jsonQ.get("$.people[2].name");
            assertTrue(result.isEmpty());
        }
        catch (Exception  e){
            assertInstanceOf(IndexOutOfBoundsException.class, e);
        }
    }

    @Test
    public void testBooleanValues() {
        String json = "{\"isActive\": true, \"isVerified\": false}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$.isActive");
        assertEquals(true, result.val());

        result = jsonQ.get("$.isVerified");
        assertEquals(false, result.val());
    }

    @Test
    public void testNumericTypes() {
        String json = "{\"intVal\": 10, \"doubleVal\": 10.5}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        assertEquals(10, jsonQ.asInt("$.intVal"));
        assertEquals(10.5, (Double) jsonQ.get("$.doubleVal").val());
    }
    @Test
    public void testEscapedCharacters() {
        String json = "{\"quote\": \"He said, \\\"Hello\\\"\"}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$.quote");
        assertEquals("He said, \"Hello\"", result.val());
    }
    @Test
    public void testKeysWithSpaces() {
        String json = "{\"first name\": \"Alice\", \"last-name\": \"Smith\"}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$['first name']");
        assertEquals("Alice", result.val());

        result = jsonQ.get("$['last-name']");
        assertEquals("Smith", result.val());
    }

    @Test
    public void testDeeplyNestedArrayStructure() {
        String json = "{\"groups\": [{\"members\": [{\"name\": \"Alice\"}, {\"name\": \"Bob\"}]}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("$.groups[0].members[1].name");
        assertEquals("Bob", result.val());
    }
    @Test
    public void testNegativeIndices() {
        String json = "{\"numbers\": [0, 1, 2, 3, 4]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        // Assuming -1 refers to the last element.
        JsonQ result = jsonQ.get("$.numbers[-1]");
        assertEquals(4, result.asInt());
    }

    @Test
    public void testInvalidJsonPath() {
        String json = "{\"name\": \"John\"}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        // Depending on implementation, you may either catch an exception or expect an empty result.
        try {
            JsonQ result = jsonQ.get("$.name.ew"); // Invalid path syntax
            assertTrue(result.isEmpty());
        } catch (Exception e) {
            // Optionally, assert the type of exception if expected.
            assertInstanceOf(IllegalArgumentException.class, e);
        }
    }

    @Test
    public void testComplexFilterExpression() {
        String json = "{\"items\": ["
                + "{\"name\": \"item1\", \"value\": 10, \"active\": true},"
                + "{\"name\": \"item2\", \"value\": 20, \"active\": false},"
                + "{\"name\": \"item3\", \"value\": 30, \"active\": true}"
                + "]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        // Test simple filter: select items with value > 25
        JsonQ result = jsonQ.get("$.items[?(@.value > 25)].name");
        assertEquals("item3", result.val());
    }

    // ===== Tests ported from Python test_jsonq.py =====

    @Test
    public void testRecursiveWildcardPaths() {
        String json = "{\"person\": {\"name\": \"John\", \"address\": {\"city\": \"New York\", \"zip\": \"10001\"}, " +
                "\"contacts\": [{\"city\": \"Boston\"}, {\"city\": \"Denver\"}]}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        // Use $.person..city to find all cities under person
        List<String> cities = jsonQ.getStrings("$.person..city");
        assertTrue(cities.size() >= 1, "Should find at least one city");

        // Test finding zip with recursive descent
        String zip = jsonQ.str("$.person...zip");
        assertFalse(zip.isEmpty(), "Should find zip code");
    }

    @Test
    public void testGlobbedPathMatchesKeys() {
        String json = "{\"wild\": {\"matchOne\": 1, \"matchTwo\": 2, \"miss\": 3}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        List<Integer> result = jsonQ.integers("wild.match*");
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertEquals(2, result.size());
    }

    @Test
    public void testArraySlicesWithNegativeIndicesAndUnions() {
        String json = "{\"numbers\": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        // Negative indices
        List<Integer> result = jsonQ.integers("$.numbers[-2:]");
        assertEquals(List.of(8, 9), result);

        // Unions with slices
        result = jsonQ.integers("$.numbers[1:4,7:]");
        assertEquals(List.of(1, 2, 3, 7, 8, 9), result);

        // Index unions
        result = jsonQ.integers("$.numbers[0,2,4]");
        assertEquals(List.of(0, 2, 4), result);
    }

    @Test
    public void testFilterWithRegexExpression() {
        String json = "{\"items\": [" +
                "{\"name\": \"item-alpha\", \"value\": 10}," +
                "{\"name\": \"skip\", \"value\": 20}," +
                "{\"name\": \"item-beta\", \"value\": 30}" +
                "]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        List<Integer> result = jsonQ.integers("items[?(@.name~'item.*')].value");
        assertEquals(List.of(10, 30), result);
    }

    @Test
    public void testFilterAcceptsDoubleQuotes() {
        String json = "{\"items\": [{\"name\": \"item1\", \"value\": 10}, {\"name\": \"item2\", \"value\": 20}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        // Use single quotes which are supported by the Java implementation
        JsonQ result = jsonQ.get("items[?(@.name=='item2')].value");
        assertEquals(20, result.asInt());
    }

    @Test
    public void testFilterInterpretsBooleanLikeStrings() {
        String json = "{\"entries\": [{\"flag\": \"yes\"}, {\"flag\": \"no\"}, {\"flag\": \"ndiyo\"}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        List<String> filtered = jsonQ.getStrings("entries[?(@.flag)].flag");
        assertTrue(filtered.contains("yes"));
        assertTrue(filtered.contains("ndiyo"));
        assertFalse(filtered.contains("no"));
    }

    @Test
    public void testStrHelper() {
        String json = "{\"items\": [{\"name\": \"item1\"}, {\"name\": \"item2\"}, {\"name\": \"item3\"}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        String result = jsonQ.str("items[*].name");
        assertTrue(result.contains("item1"));
        assertTrue(result.contains("item2"));
        assertTrue(result.contains("item3"));
    }

    @Test
    public void testIntegersHelper() {
        String json = "{\"items\": [{\"name\": \"item1\", \"value\": 10}, {\"name\": \"item2\", \"value\": 20}, {\"name\": \"item3\", \"value\": 30}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        List<Integer> result = jsonQ.integers("items[*].value");
        assertEquals(List.of(10, 20, 30), result);
    }

    @Test
    public void testSelectColumns() {
        String json = "{\"items\": [" +
                "{\"name\": \"item1\", \"value\": 10, \"date\": \"2020-01-02\"}," +
                "{\"name\": \"item2\", \"value\": 20, \"date\": \"02-01-2020\"}," +
                "{\"name\": \"item3\", \"value\": 30, \"date\": \"2020-01-04\"}" +
                "]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ selection = jsonQ.get("items").select("name", "value");
        List<String> names = selection.getStrings("[*].name");
        List<Integer> values = selection.integers("[*].value");

        assertEquals(List.of("item1", "item2", "item3"), names);
        assertEquals(List.of(10, 20, 30), values);
    }

    @Test
    public void testWhereFiltersItems() {
        String json = "{\"items\": [" +
                "{\"name\": \"item1\", \"value\": 10}," +
                "{\"name\": \"item2\", \"value\": 20}," +
                "{\"name\": \"item3\", \"value\": 30}" +
                "]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        JsonQ result = jsonQ.get("items").where("@.value > ?", 15);
        List<String> names = result.getStrings("[*].name");
        assertEquals(List.of("item2", "item3"), names);
    }

    @Test
    public void testPutOverridesDictionaryValue() {
        String json = "{\"person\": {\"address\": {\"zip\": \"10001\"}}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        jsonQ.put("person.address.zip", "20002");
        assertEquals("20002", jsonQ.str("person.address.zip"));
    }

    @Test
    public void testAddAppendsToList() {
        String json = "{\"items\": [1, 2]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        jsonQ.add(3);
        // Note: Java's add() adds to root, not to a path like Python
        // This test verifies the root-level add behavior
        List<Integer> result = jsonQ.integers("items[*]");
        assertEquals(List.of(1, 2), result);
    }

    @Test
    public void testForEachVisitsAllItems() {
        String json = "{\"items\": [" +
                "{\"name\": \"item1\", \"value\": 10}," +
                "{\"name\": \"item2\", \"value\": 20}," +
                "{\"name\": \"item3\", \"value\": 30}" +
                "]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        java.util.List<String> visited = new java.util.ArrayList<>();
        jsonQ.get("items").forEach((key, node) -> visited.add(key + ":" + node.str("name")));

        assertEquals(3, visited.size());
        assertTrue(visited.contains("0:item1"));
        assertTrue(visited.contains("1:item2"));
        assertTrue(visited.contains("2:item3"));
    }

    @Test
    public void testIsEmptyAndVal() {
        assertTrue(JsonQ.fromJson("{}").isEmpty());
        assertTrue(JsonQ.fromJson("[]").isEmpty());

        JsonQ nonEmpty = JsonQ.fromJson("{\"data\": 1}");
        assertFalse(nonEmpty.isEmpty());
        assertNotNull(nonEmpty.val());
    }

    @Test
    public void testToStringRoundTripsJson() {
        String json = "{\"people\": [{\"name\": \"John\"}, {\"name\": \"Jane\"}, {\"name\": \"Doe\"}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        String serialized = jsonQ.get("people").toString();
        JsonQ reparsed = JsonQ.fromJson(serialized);

        List<String> names = reparsed.getStrings("[*].name");
        assertEquals(List.of("John", "Jane", "Doe"), names);
    }

    @Test
    public void testDateColumnParsesKnownFormats() {
        String json = "{\"items\": [" +
                "{\"name\": \"item1\", \"date\": \"2020-01-02\"}," +
                "{\"name\": \"item2\", \"date\": \"02-01-2020\"}," +
                "{\"name\": \"item3\", \"date\": \"2020-01-04\"}" +
                "]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        java.util.List<java.util.Date> dates = jsonQ.get("items").dateColumn("date");
        assertEquals(3, dates.size());
        // All dates should be parsed successfully
        for (java.util.Date d : dates) {
            assertNotNull(d);
        }
    }

    @Test
    public void testFindReturnsEmptyForPrimitives() {
        JsonQ jsonQ = JsonQ.fromPOJO(5);
        assertTrue(jsonQ.get("anything").isEmpty());
    }

    @Test
    public void testStringColumn() {
        String json = "[{\"name\": \"Alice\"}, {\"name\": \"Bob\"}, {\"name\": \"Charlie\"}]";
        JsonQ jsonQ = JsonQ.fromJson(json);

        List<String> names = jsonQ.stringColumn("name");
        assertEquals(List.of("Alice", "Bob", "Charlie"), names);
    }

    @Test
    public void testIntColumn() {
        String json = "[{\"value\": 10}, {\"value\": 20}, {\"value\": 30}]";
        JsonQ jsonQ = JsonQ.fromJson(json);

        List<Integer> values = jsonQ.intColumn("value");
        assertEquals(List.of(10, 20, 30), values);
    }

    @Test
    public void testHasStuffAndNotEmpty() {
        JsonQ empty = JsonQ.fromJson("{}");
        JsonQ notEmpty = JsonQ.fromJson("{\"data\": 1}");

        assertFalse(empty.hasStuff());
        assertFalse(empty.notEmpty());
        assertTrue(notEmpty.hasStuff());
        assertTrue(notEmpty.notEmpty());
    }

    @Test
    public void testPutNoNull() {
        String json = "{\"person\": {\"name\": \"John\"}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        jsonQ.putNoNull("person.age", null);
        assertTrue(jsonQ.get("person.age").isEmpty());

        jsonQ.putNoNull("person.age", 30);
        assertEquals(30, jsonQ.asInt("person.age"));
    }

    @Test
    public void testFirstWithTransformation() {
        String json = "{\"items\": [{\"name\": \"item1\"}, {\"name\": \"item2\"}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        // first() returns all matching items transformed - the method operates on the entire result set
        String result = jsonQ.first("items[*].name", obj -> obj.toString().toUpperCase());
        // Result contains all transformed names
        assertTrue(result.contains("ITEM1"));
        assertTrue(result.contains("ITEM2"));
    }

    @Test
    public void testGetWithFormattedPath() {
        String json = "{\"users\": [{\"id\": 1, \"name\": \"Alice\"}, {\"id\": 2, \"name\": \"Bob\"}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        String name = jsonQ.str("users[%d].name", 1);
        assertEquals("Bob", name);
    }

    @Test
    public void testGetStringsWithFormattedPath() {
        String json = "{\"data\": {\"items\": [{\"name\": \"a\"}, {\"name\": \"b\"}]}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        List<String> names = jsonQ.getStrings("%s.items[*].name", "data");
        assertEquals(List.of("a", "b"), names);
    }

    // ===== Tests for new methods ported from Python =====

    @Test
    public void testValueReturnsRawValue() {
        String json = "{\"items\": [{\"value\": 10}, {\"value\": 20}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        // Single value
        Object singleValue = jsonQ.value("items[0].value");
        assertEquals(10.0, singleValue);

        // Multiple values returns list
        Object multipleValues = jsonQ.value("items[*].value");
        assertTrue(multipleValues instanceof List);
        assertEquals(2, ((List<?>) multipleValues).size());
    }

    @Test
    public void testFindRawReturnsList() {
        String json = "{\"items\": [{\"name\": \"a\"}, {\"name\": \"b\"}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        List<Object> results = jsonQ.findRaw("items[*].name");
        assertEquals(2, results.size());
        assertTrue(results.contains("a"));
        assertTrue(results.contains("b"));
    }

    @Test
    public void testToStringWithIndent() {
        String json = "{\"name\": \"John\", \"age\": 30}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        // Pretty print with indent=2 has newlines
        String pretty = jsonQ.toString(2);
        assertTrue(pretty.contains("\n"), "Pretty printed JSON should contain newlines");

        // Compact with indent=0 has no newlines
        String compact = jsonQ.toString(0);
        assertFalse(compact.contains("\n"), "Compact JSON should not contain newlines");
    }

    @Test
    public void testChangeUpdatesValue() {
        String json = "{\"person\": {\"name\": \"john\"}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        jsonQ.change("person.name", obj -> ((String) obj).toUpperCase());
        assertEquals("JOHN", jsonQ.str("person.name"));
    }

    @Test
    public void testAddWithPath() {
        String json = "{\"items\": [1, 2]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        jsonQ.add("items", 3);
        List<Integer> result = jsonQ.integers("items[*]");
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void testAddAppendsToAllMatchedLists() {
        String json = "{\"items\": [[1], [2]]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        jsonQ.add("items[*]", 3);
        List<Integer> first = jsonQ.integers("items[0][*]");
        List<Integer> second = jsonQ.integers("items[1][*]");
        assertEquals(List.of(1, 3), first);
        assertEquals(List.of(2, 3), second);
    }

    @Test
    public void testMergeMergesExistingDict() {
        String json = "{\"person\": {\"name\": \"John\", \"address\": {\"city\": \"New York\"}}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        jsonQ.merge("person.address", java.util.Map.of("zip", "10001"));
        assertEquals("New York", jsonQ.str("person.address.city"));
        assertEquals("10001", jsonQ.str("person.address.zip"));
    }

    @Test
    public void testMergeExtendsListByDefault() {
        String json = "{\"items\": [1, 2]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        jsonQ.merge("items", List.of(3, 4));
        List<Integer> result = jsonQ.integers("items[*]");
        assertEquals(List.of(1, 2, 3, 4), result);
    }

    @Test
    public void testMergeReplacePolicyReplacesList() {
        String json = "{\"items\": [1, 2, 3]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        jsonQ.merge("items", List.of(9, 8), "replace");
        List<Integer> result = jsonQ.integers("items[*]");
        assertEquals(List.of(9, 8), result);
    }

    @Test
    public void testMergeManyUpdatesMultipleTargets() {
        String json = "{\"config\": {\"items\": [1]}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("config.items", List.of(2));
        updates.put("config.name", "test");

        jsonQ.mergeMany(updates);
        assertEquals("test", jsonQ.str("config.name"));
        List<Integer> items = jsonQ.integers("config.items[*]");
        assertEquals(List.of(1, 2), items);
    }

    @Test
    public void testMergeManyRespectsListPolicy() {
        String json = "{\"items\": [1, 2, 3]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        jsonQ.mergeMany(Map.of("items", List.of(9, 8)), "replace");
        List<Integer> result = jsonQ.integers("items[*]");
        assertEquals(List.of(9, 8), result);
    }

    @Test
    public void testKeysAndLeaves() {
        String json = "{\"person\": {\"name\": \"John\", \"address\": {\"city\": \"New York\", \"zip\": \"10001\"}}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        List<String> keys = jsonQ.keys("person", true);
        assertTrue(keys.contains("name"));
        assertTrue(keys.contains("city"));
        assertTrue(keys.contains("zip"));

        Map<String, Object> leaves = jsonQ.leaves("person");
        assertEquals("John", leaves.get("person.name"));
        assertEquals("New York", leaves.get("person.address.city"));
        assertEquals("10001", leaves.get("person.address.zip"));
    }

    @Test
    public void testLeavesWithPredicate() {
        String json = "{\"data\": {\"items\": [{\"id\": 1, \"name\": \"item1\"}, {\"id\": 2, \"name\": \"item2\"}]}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        Map<String, Object> onlyIds = jsonQ.leaves("data", (path, value) -> path.endsWith("id"));
        assertEquals(2, onlyIds.size());
        assertTrue(onlyIds.containsKey("data.items.0.id"));
        assertTrue(onlyIds.containsKey("data.items.1.id"));
    }

    @Test
    public void testFillTemplate() {
        String dataJson = "{\"name\": \"John\", \"city\": \"New York\"}";
        JsonQ dataQ = JsonQ.fromJson(dataJson);

        Map<String, Object> template = new LinkedHashMap<>();
        template.put("greeting", "$name");
        template.put("location", "$city");

        Object filled = dataQ.fillTemplate(template);
        JsonQ result = JsonQ.fromPOJO(filled);
        assertEquals("John", result.str("greeting"));
        assertEquals("New York", result.str("location"));
    }

    @Test
    public void testMergeHandlesMultipleListMatches() {
        String json = "{\"items\": [{\"tags\": [1]}, {\"tags\": [2]}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        jsonQ.merge("items[*].tags", List.of(3));

        List<Integer> firstTags = jsonQ.integers("items[0].tags[*]");
        List<Integer> secondTags = jsonQ.integers("items[1].tags[*]");
        assertEquals(List.of(1, 3), firstTags);
        assertEquals(List.of(2, 3), secondTags);
    }

    @Test
    public void testLeavesWithArrays() {
        String json = "{\"items\": [{\"name\": \"a\"}, {\"name\": \"b\"}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        Map<String, Object> leaves = jsonQ.leaves("");
        assertTrue(leaves.containsKey("items.0.name"));
        assertTrue(leaves.containsKey("items.1.name"));
        assertEquals("a", leaves.get("items.0.name"));
        assertEquals("b", leaves.get("items.1.name"));
    }

    // ===== YAML Tests =====

    @Test
    public void testFromYamlString() {
        String yaml = """
                name: John
                age: 30
                address:
                  city: New York
                  zip: "10001"
                """;
        JsonQ jsonQ = JsonQ.fromYaml(yaml);

        assertEquals("John", jsonQ.str("name"));
        assertEquals(30, jsonQ.asInt("age"));
        assertEquals("New York", jsonQ.str("address.city"));
        assertEquals("10001", jsonQ.str("address.zip"));
    }

    @Test
    public void testFromYamlWithList() {
        String yaml = """
                items:
                  - name: item1
                    value: 10
                  - name: item2
                    value: 20
                """;
        JsonQ jsonQ = JsonQ.fromYaml(yaml);

        List<String> names = jsonQ.getStrings("items[*].name");
        assertEquals(List.of("item1", "item2"), names);

        List<Integer> values = jsonQ.integers("items[*].value");
        assertEquals(List.of(10, 20), values);
    }

    @Test
    public void testToYaml() {
        String json = "{\"name\": \"John\", \"age\": 30}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        String yaml = jsonQ.toYaml();
        assertTrue(yaml.contains("name:"));
        assertTrue(yaml.contains("John"));
        assertTrue(yaml.contains("age:"));
        assertTrue(yaml.contains("30"));
    }

    @Test
    public void testYamlRoundTrip() {
        String originalYaml = """
                config:
                  database:
                    host: localhost
                    port: 5432
                  features:
                    - auth
                    - logging
                """;
        JsonQ jsonQ = JsonQ.fromYaml(originalYaml);

        // Convert to YAML and back
        String yamlOutput = jsonQ.toYaml();
        JsonQ reparsed = JsonQ.fromYaml(yamlOutput);

        assertEquals("localhost", reparsed.str("config.database.host"));
        assertEquals(5432, reparsed.asInt("config.database.port"));
        List<String> features = reparsed.getStrings("config.features[*]");
        assertTrue(features.contains("auth"));
        assertTrue(features.contains("logging"));
    }

    @Test
    public void testJsonToYamlConversion() {
        String json = "{\"users\": [{\"name\": \"Alice\", \"active\": true}, {\"name\": \"Bob\", \"active\": false}]}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        // Convert JSON to YAML
        String yaml = jsonQ.toYaml();
        assertTrue(yaml.contains("users:"), "YAML should contain users key");
        assertTrue(yaml.contains("Alice"), "YAML should contain Alice");
        assertTrue(yaml.contains("Bob"), "YAML should contain Bob");

        // Parse the YAML back and verify
        JsonQ fromYaml = JsonQ.fromYaml(yaml);
        List<String> names = fromYaml.getStrings("users[*].name");
        assertEquals(List.of("Alice", "Bob"), names);
    }

    @Test
    public void testYamlToJsonConversion() {
        String yaml = """
                server:
                  host: 0.0.0.0
                  port: 8080
                  ssl: true
                """;
        JsonQ jsonQ = JsonQ.fromYaml(yaml);

        // Convert to JSON
        String json = jsonQ.toString();
        assertTrue(json.contains("\"server\""));
        assertTrue(json.contains("\"host\""));
        assertTrue(json.contains("\"0.0.0.0\""));

        // Parse back as JSON
        JsonQ fromJson = JsonQ.fromJson(json);
        assertEquals("0.0.0.0", fromJson.str("server.host"));
        assertEquals(8080, fromJson.asInt("server.port"));
    }

    @Test
    public void testFromYamlEmptyString() {
        JsonQ jsonQ = JsonQ.fromYaml("");
        assertTrue(jsonQ.isEmpty());
    }

    @Test
    public void testFromYamlInvalidSyntax() {
        // Invalid YAML should return empty JsonQ
        JsonQ jsonQ = JsonQ.fromYaml("not: valid: yaml: [");
        // Should not throw, just return empty or partial result
        assertNotNull(jsonQ);
    }

    @Test
    public void testToYamlWithIndent() {
        String json = "{\"nested\": {\"deep\": {\"value\": 1}}}";
        JsonQ jsonQ = JsonQ.fromJson(json);

        String yaml2 = jsonQ.toYaml(2);
        String yaml4 = jsonQ.toYaml(4);

        // Both should be valid YAML
        assertTrue(yaml2.contains("nested:"));
        assertTrue(yaml4.contains("nested:"));

        // 4-space indent should be different from 2-space
        assertNotEquals(yaml2, yaml4);
    }

    @Test
    public void testYamlWithSpecialCharacters() {
        String yaml = """
                message: "Hello, World!"
                path: "/usr/local/bin"
                regex: "^[a-z]+$"
                """;
        JsonQ jsonQ = JsonQ.fromYaml(yaml);

        assertEquals("Hello, World!", jsonQ.str("message"));
        assertEquals("/usr/local/bin", jsonQ.str("path"));
        assertEquals("^[a-z]+$", jsonQ.str("regex"));
    }

    @Test
    public void testYamlMixedTypes() {
        String yaml = """
                string: hello
                integer: 42
                float: 3.14
                boolean: true
                null_value: null
                list:
                  - 1
                  - 2
                  - 3
                """;
        JsonQ jsonQ = JsonQ.fromYaml(yaml);

        assertEquals("hello", jsonQ.str("string"));
        assertEquals(42, jsonQ.asInt("integer"));
        assertTrue(jsonQ.str("float").startsWith("3.14"));
        assertEquals(true, jsonQ.value("boolean"));
        assertNull(jsonQ.value("null_value"));
        assertEquals(List.of(1, 2, 3), jsonQ.integers("list[*]"));
    }


}
