package com.peosu.fn.utils;

import com.google.gson.*;
import com.peosu.fn.ds.PathType;

import java.lang.reflect.Type;

public class JsonUtil {
    public static Gson getGson(){
        return new GsonBuilder()
                .registerTypeHierarchyAdapter(Number.class, new NumberTypeAdapter())
                .setPrettyPrinting().create();
    }

    private static class NumberTypeAdapter implements JsonDeserializer<Number>, JsonSerializer<Number> {
        @Override
        public Number deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String numberStr = json.getAsString();
            try {
                return PathType.INTEGER.getPattern().matcher(numberStr).matches()
                 ? Integer.parseInt(numberStr) : Double.parseDouble(numberStr) ;
            } catch (NumberFormatException e) {
                return Double.parseDouble(numberStr);
            }
        }

        @Override
        public JsonElement serialize(Number src, Type typeOfSrc, JsonSerializationContext context) {
            if (src instanceof Double || src instanceof Float) {
                return new JsonPrimitive(src);
            } else if (src instanceof Integer || src instanceof Long || src instanceof Short || src instanceof Byte) {
                return new JsonPrimitive(src.longValue());
            } else {
                return new JsonPrimitive(src);
            }
        }

}}
