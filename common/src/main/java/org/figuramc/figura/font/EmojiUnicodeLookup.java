package org.figuramc.figura.font;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EmojiUnicodeLookup {
    private final Map<String, String> unicodeLookup = new HashMap<>(); // <EmojiName, Unicode>
    private final Map<String, String[]> reverseUnicodeLookup = new HashMap<>(); // <Unicode, EmojiNames[]>
    private final Map<Integer, EmojiMetadata> metadataLookup = new HashMap<>(); // <Codepoint, EmojiMetadata>
    private final Map<String, String> shortcutLookup = new HashMap<>(); // <Shortcut, Unicode>
    private final Map<String, String[]> reverseShortcutLookup = new HashMap<>(); // <Unicode, Shortcut[]>

    public void putAliases(String[] aliases, String unicode) {
        for (String alias : aliases) {
            unicodeLookup.put(alias, unicode);
        }
        reverseUnicodeLookup.put(unicode, aliases);
    }

    public void putShortcuts(String[] shortcuts, String unicode) {
        for (String alias : shortcuts) {
            shortcutLookup.put(alias, unicode);
        }
        reverseShortcutLookup.put(unicode, shortcuts);
    }

    public void putMetadata(int codepoint, EmojiMetadata metadata) {
        metadataLookup.put(codepoint, metadata);
    }

    public Collection<String> getNames() {
        return unicodeLookup.keySet();
    }

    public @Nullable String[] getNames(String unicode) {
        return reverseUnicodeLookup.getOrDefault(unicode, null);
    }

    public Collection<String> getShortcuts() {
        return shortcutLookup.keySet();
    }

    public @Nullable String[] getShortcuts(String unicode) {
        return reverseShortcutLookup.getOrDefault(unicode, null);
    }

    public @Nullable EmojiMetadata getMetadata(int codepoint) {
        return metadataLookup.getOrDefault(codepoint, null);
    }

    public @Nullable String getUnicode(String emojiAlias) {
        return unicodeLookup.getOrDefault(emojiAlias, null);
    }

    public @Nullable String getUnicodeForShortcut(String shortcut) {
        return shortcutLookup.getOrDefault(shortcut, null);
    }

    public Collection<EmojiMetadata> metadataValues() {
        return metadataLookup.values();
    }

    public Collection<String> unicodeValues() {
        return reverseUnicodeLookup.keySet();
    }
}
