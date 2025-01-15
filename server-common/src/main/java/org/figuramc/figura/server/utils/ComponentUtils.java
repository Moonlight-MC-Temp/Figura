package org.figuramc.figura.server.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ComponentUtils {
    public static JsonObject textObject(String literal) {
        JsonObject object = new JsonObject();
        object.addProperty("text", literal);
        return object;
    }

    public static Builder text(String literal) {
        return new Builder(textObject(literal));
    }

    public static JsonObject color(JsonObject component, String color) {
        component.addProperty("color", color);
        return component;
    }

    public static JsonObject style(JsonObject component, String property, boolean value) {
        component.addProperty(property, value);
        return component;
    }

    public static JsonObject clickEvent(JsonObject component, String eventType, String value) {
        JsonObject event = new JsonObject();
        event.addProperty("action", eventType);
        event.addProperty("value", value);
        component.add("clickEvent", event);
        return component;
    }

    public static JsonObject tooltip(JsonObject component, JsonObject otherComponent) {
        JsonObject event = new JsonObject();
        event.addProperty("action","show_text");
        event.add("value", otherComponent);
        component.add("hoverEvent", event);
        return component;
    }

    public static JsonObject add(JsonObject component, JsonObject other) {
        JsonArray children;
        if (component.has("extra")) {
            children = component.getAsJsonArray("extra");
        }
        else {
            children = new JsonArray();
            component.add("extra", children);
        }
        children.add(other);
        return component;
    }

    public static class Builder {
        private final JsonObject text;

        public Builder(JsonObject component) {
            this.text = component;
        }

        public Builder color(String color) {
            ComponentUtils.color(text, color);
            return this;
        }

        public Builder style(String property, boolean value) {
            ComponentUtils.style(text, property, value);
            return this;
        }

        public Builder clickEvent(String type, String value) {
            ComponentUtils.clickEvent(text, type, value);
            return this;
        }

        public Builder tooltip(Builder other) {
            return tooltip(other.build());
        }

        public Builder tooltip(JsonObject other) {
            ComponentUtils.tooltip(text, other);
            return this;
        }

        public Builder add(Builder other) {
            return add(other.build());
        }

        public Builder add(JsonObject other) {
            ComponentUtils.add(text, other);
            return this;
        }

        public JsonObject build() {
            return text;
        }
    }
}
