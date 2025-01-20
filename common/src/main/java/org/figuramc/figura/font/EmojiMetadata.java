package org.figuramc.figura.font;

import com.google.gson.JsonObject;
import org.figuramc.figura.utils.JsonUtils;

import static org.figuramc.figura.font.EmojiContainer.*;

public class EmojiMetadata {
    public final int frames;
    public final int frameTime;
    public final int width;
    public final int defaultColor;
    public final boolean canBeColored;
    private int frameTimer;
    private int curFrame;

    public EmojiMetadata(int frames, int frameTime, int width, int defaultColor) {
        this.frames = frames;
        this.frameTime = frameTime;
        this.width = width;
        this.defaultColor = defaultColor;
        this.canBeColored = true;
    }

    public EmojiMetadata(int frames, int frameTime, int width) {
        this.frames = frames;
        this.frameTime = frameTime;
        this.width = width;
        this.defaultColor = -1;
        this.canBeColored = false;
    }

    public static EmojiMetadata fromJson(JsonObject entry) {
        int frameCount = JsonUtils.getIntOrDefault(entry, JSON_KEY_FRAMES, 1);
        int frameTime = JsonUtils.getIntOrDefault(entry, JSON_KEY_FRAME_TIME, 1);
        int width = JsonUtils.getIntOrDefault(entry, JSON_KEY_WIDTH, 8);

        if (entry.has("color")) {
            String hexColor = entry.get(JSON_KEY_DEFAULT_COLOR).getAsString();
            return new EmojiMetadata(frameCount, frameTime, width, Integer.parseInt(hexColor, 16));
        }

        return new EmojiMetadata(frameCount, frameTime, width);
    }

    public void tickAnimation() {
        frameTimer++;
        if (frameTimer >= frameTime) {
            frameTimer -= frameTime;
            curFrame = (curFrame + 1) % frames;
        }
    }

    public int getCurrentFrame() {
        return curFrame;
    }
}
