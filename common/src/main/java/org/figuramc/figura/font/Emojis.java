package org.figuramc.figura.font;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.utils.FiguraResourceListener;
import org.figuramc.figura.utils.TextUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Emojis {

    private static final Map<String, EmojiContainer> EMOJIS = new HashMap<>();
    private static final Map<String, EmojiContainer> SHORTCUT_LOOKUP = new HashMap<>();

    public static final char DELIMITER = ':';
    public static final char ESCAPE = '\\';

    // listener to load emojis from the resource pack
    public static final FiguraResourceListener RESOURCE_LISTENER = FiguraResourceListener.createResourceListener("emojis", manager -> {
        EMOJIS.clear();

        for (Map.Entry<ResourceLocation, Resource> emojis : manager.listResources("emojis", location -> location.getNamespace().equals(FiguraMod.MOD_ID) && location.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation location = emojis.getKey();
            String[] split = location.getPath().split("/", 2);

            if (split.length <= 1)
                continue;

            String name = split[1].substring(0, split[1].length() - 5);

            // open the resource as json
            try (InputStream stream = emojis.getValue().open()) {
                // add emoji
                JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                EmojiContainer container = new EmojiContainer(name, json);
                EMOJIS.put(name, container);
                container.getLookup().getShortcuts().forEach(shortcut -> SHORTCUT_LOOKUP.put(shortcut, container));

            } catch (Exception e) {
                FiguraMod.LOGGER.error("Failed to load {} emojis", name, e);
            }
        }

        // check for duplicates
        HashMap<String, List<String>> duplicates = new HashMap<>();
        HashMap<String, String> map = new HashMap<>();
        for (EmojiContainer curContainer : EMOJIS.values()) {
            // For each emoji name in the current container
            for (String curName : curContainer.getLookup().getNames()) {
                // Check if there was already a value in the map
                String prevValue = map.put(curName, curContainer.name);

                // If there was, save the container it was found in for logging
                if (prevValue != null) {
                    List<String> list;
                    if (!duplicates.containsKey(curName)) {
                        list = new ArrayList<>();
                        list.add(curContainer.name);
                        duplicates.put(curName, list);
                    } else {
                        list = duplicates.get(curName);
                    }

                    list.add(prevValue);
                }
            }
        }

        // Print out each duplicate emoji and which containers it was found in.
        for (String curName : duplicates.keySet()) {
            FiguraMod.LOGGER.warn("Duplicate emoji \"{}\" found in containers: {}", curName, String.join(", ", duplicates.get(curName)));
        }
    });

    public static Collection<String> getCategoryNames() {
        return EMOJIS.keySet();
    }

    public static EmojiContainer getCategory(String key) {
        return EMOJIS.get(key);
    }

    public static boolean hasCategory(String key) {
        return EMOJIS.containsKey(key);
    }

    public static MutableComponent applyEmojis(Component text) {
        Component newText = TextUtils.parseLegacyFormatting(text);
        MutableComponent ret = Component.empty();
        newText.visit((style, string) -> {
            ret.append(convertEmoji(string, style));
            return Optional.empty();
        }, Style.EMPTY);
        return ret;
    }

    private static MutableComponent convertEmoji(String string, Style style) {

        // string lists, every odd index is an emoji
        List<String> strings = new ArrayList<>();

        // temp variables
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        boolean inside = false;

        // iterate over every char
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            // escape sequence
            if (c == ESCAPE) {
                escaped = !escaped;
                // append only if the next char is not the delimiter
                if (i + 1 == string.length() || string.charAt(i + 1) != DELIMITER)
                    current.append(c);
                // delimiter, only if not escaped
            } else if (c == DELIMITER && !escaped) {
                // toggle inside status
                inside = !inside;
                // and also append to the list
                strings.add(current.toString());
                current = new StringBuilder();
                // otherwise just add to the queue
            } else {
                escaped = false;
                current.append(c);

                // space character breaks current emoji parsing
                if (c == ' ' && inside) {
                    inside = false;
                    // removed last appended emoji, as were undoing the parsing of the emoji
                    String removed = strings.remove(strings.size() - 1);
                    // replace current with the removed string, adding back the delimiter and appending the current parsed text
                    current = new StringBuilder(removed).append(DELIMITER).append(current);
                }
            }
        }

        // if the queue is not flushed
        if (current.length() > 0 || inside) {
            String toAdd = current.toString();
            // if we left inside, we want to keep the text within the last index
            // also keep the delimiter since its end was not found
            if (inside) {
                int index = strings.size() - 1;
                String s = strings.get(index) + DELIMITER + toAdd;
                strings.set(index, s);
                // otherwise just add what is remaining
            } else {
                strings.add(toAdd);
            }
        }

        MutableComponent result = Component.empty().withStyle(style);

        // now we parse the list
        for (int i = 0; i < strings.size(); i++) {
            String s = strings.get(i);

            // even: append text
            if (i % 2 == 0) {
                // Find all emoji shortcuts in string
                List<String> shortcuts = SHORTCUT_LOOKUP.keySet().stream().filter(s::contains).toList();

                // Replace all shortcuts
                if (shortcuts.size() > 0) {
                    while (s.length() > 0) {
                        boolean anyFound = false;
                        for (String shortcut : shortcuts) {
                            if (s.startsWith(shortcut)) {
                                s = s.substring(shortcut.length());
                                result.append(SHORTCUT_LOOKUP.get(shortcut).getShortcutComponent(shortcut, style));
                                anyFound = true;
                                break;
                            }
                        }
                        if (!anyFound) {
                            result.append(String.valueOf(s.charAt(0)));
                            s = s.substring(1);
                        }
                    }
                }
                // Otherwise append as normal
                else {
                    result.append(s);
                }
            }
            // odd: format and append emoji
            else {
                appendEmoji(result, s, style);
            }
        }

        return result;
    }

    private static void appendEmoji(MutableComponent result, String alias, Style style) {
        MutableComponent emoji = Emojis.getEmoji(alias, style);
        
        if (emoji != null) {
            result.append(emoji);
        } else {
            result.append(DELIMITER + alias + DELIMITER);
        }
    }

    public static MutableComponent getEmoji(String emojiAlias, Style style) {
        for (EmojiContainer container : EMOJIS.values()) {
            MutableComponent emoji = container.getEmojiComponent(emojiAlias, style);
            if (emoji != null) {
                return emoji;
            }
        }
        return null;
    }

    public static MutableComponent getEmoji(String emojiAlias, Style style, MutableComponent hover) {
        for (EmojiContainer container : EMOJIS.values()) {
            MutableComponent emoji = container.getEmojiComponent(emojiAlias, hover, style);
            if (emoji != null) {
                return emoji;
            }
        }
        return null;
    }

    public static EmojiContainer getCategoryByFont(ResourceLocation location) {
        for (EmojiContainer container : EMOJIS.values()) {
            if (location.equals(container.getFont())) {
                return container;
            }
        }
        return null;
    }

    public static Component removeBlacklistedEmojis(Component text) {
        for (EmojiContainer container : EMOJIS.values())
            text = container.blacklist(text);
        return text;
    }

    public static List<String> getMatchingEmojis(String query) {
        if (query.length() == 0 || query.charAt(0) != DELIMITER)
            return List.of();

        String name = query.substring(1);
        List<String> emojis = new ArrayList<>();

        for (EmojiContainer container : EMOJIS.values()) {
            for (String s : container.getLookup().getNames()) {
                if (s.startsWith(name))
                    emojis.add(DELIMITER + s + DELIMITER);
            }
        }

        return emojis;
    }

    public static void tickAnimations() {
        for (EmojiContainer container : EMOJIS.values()) {
            container.tickAnimations();
        }
    }
}
