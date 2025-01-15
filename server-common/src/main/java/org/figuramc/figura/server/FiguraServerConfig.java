package org.figuramc.figura.server;

import com.google.gson.annotations.SerializedName;

public final class FiguraServerConfig {
    @SerializedName("pingsRateLimit")
    private int pingsRateLimit = 32;
    @SerializedName("pingsSizeLimit")
    private int pingsSizeLimit = 1024;

    @SerializedName("avatarSizeLimit")
    private int avatarSizeLimit = 102400;
    @SerializedName("avatarCountLimit")
    private int avatarsCountLimit = 1;

    public int pingsRateLimit() {
        return pingsRateLimit;
    }

    public int pingsSizeLimit() {
        return pingsSizeLimit;
    }

    public int avatarSizeLimit() {
        return avatarSizeLimit;
    }

    public int avatarsCountLimit() {
        return avatarsCountLimit;
    }
}
