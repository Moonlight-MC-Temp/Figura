package org.figuramc.figura.server.json;

import com.google.gson.*;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.Utils;

import java.lang.reflect.Type;

public class HashSerializer implements JsonSerializer<Hash>, JsonDeserializer<Hash> {
    @Override
    public Hash deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement instanceof JsonPrimitive primitive && primitive.isString()) {
            return Utils.parseHash(primitive.getAsString());
        }
        throw new JsonParseException("Hash has to be in string representation");
    }

    @Override
    public JsonElement serialize(Hash hash, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(hash.toString());
    }
}
