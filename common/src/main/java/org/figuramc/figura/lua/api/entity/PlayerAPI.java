package org.figuramc.figura.lua.api.entity;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.NbtToLua;
import org.figuramc.figura.lua.ReadOnlyLuaTable;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.EntityUtils;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@LuaWhitelist
@LuaTypeDoc(
        name = "PlayerAPI",
        value = "player"
)
public class PlayerAPI extends LivingEntityAPI<Player> {

    private PlayerInfo playerInfo;

    public PlayerAPI(Player entity) {
        super(entity);
    }

    private boolean checkPlayerInfo() {
        if (playerInfo != null)
            return true;

        PlayerInfo info = EntityUtils.getPlayerInfo(entity.getUUID());
        if (info == null)
            return false;

        playerInfo = info;
        return true;
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_food")
    public int getFood() {
        checkEntity();
        return entity.getFoodData().getFoodLevel();
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_saturation")
    public float getSaturation() {
        checkEntity();
        return entity.getFoodData().getSaturationLevel();
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_exhaustion")
    public float getExhaustion() {
        checkEntity();
        return entity.getFoodData().getExhaustionLevel();
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_experience_progress")
    public float getExperienceProgress() {
        checkEntity();
        return entity.experienceProgress;
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_xp_for_next_level")
    public int getExperienceForNextLevel(){
        checkEntity();
        return entity.getXpNeededForNextLevel();
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_experience_level")
    public int getExperienceLevel() {
        checkEntity();
        return entity.experienceLevel;
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_model_type")
    public String getModelType() {
        checkEntity();
        return (checkPlayerInfo() ? playerInfo.getModelName() : DefaultPlayerSkin.getSkinModelName(entity.getUUID())).toUpperCase(Locale.US);
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_gamemode")
    public String getGamemode() {
        checkEntity();
        if (!checkPlayerInfo())
            return null;

        GameType gamemode = playerInfo.getGameMode();
        return gamemode == null ? null : gamemode.getName().toUpperCase(Locale.US);
    }

    @LuaWhitelist
    @LuaMethodDoc("player.has_cape")
    public boolean hasCape() {
        checkEntity();
        return checkPlayerInfo() && playerInfo.isCapeLoaded();
    }

    @LuaWhitelist
    @LuaMethodDoc("player.has_skin")
    public boolean hasSkin() {
        checkEntity();
        return checkPlayerInfo() && playerInfo.isSkinLoaded();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "part"
            ),
            value = "player.is_skin_layer_visible"
    )
    public boolean isSkinLayerVisible(@LuaNotNil String part) {
        checkEntity();
        try {
            if (part.equalsIgnoreCase("left_pants") || part.equalsIgnoreCase("right_pants"))
                part += "_leg";
            return entity.isModelPartShown(PlayerModelPart.valueOf(part.toUpperCase(Locale.US)));
        } catch (Exception ignored) {
            throw new LuaError("Invalid player model part: " + part);
        }
    }

    @LuaWhitelist
    @LuaMethodDoc("player.is_fishing")
    public boolean isFishing() {
        checkEntity();
        return entity.fishing != null;
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_charged_attack_delay")
    public float getChargedAttackDelay() {
        checkEntity();
        return entity.getCurrentItemAttackStrengthDelay();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Boolean.class,
                            argumentNames = "right"
                    )
            },
            value = "player.get_shoulder_entity")
    public LuaTable getShoulderEntity(boolean right) {
        checkEntity();
        return new ReadOnlyLuaTable(NbtToLua.convert(right ? entity.getShoulderEntityRight() : entity.getShoulderEntityLeft()));
    }

    @LuaWhitelist
    @LuaMethodDoc("player.get_team_info")
    public Map<String, Object> getTeamInfo() {
        checkEntity();
        if (!checkPlayerInfo())
            return null;

        PlayerTeam team = playerInfo.getTeam();
        if (team == null)
            return null;

        Map<String, Object> map = new HashMap<>();

        map.put("name", team.getName());
        map.put("display_name", team.getDisplayName().getString());
        map.put("color", team.getColor().getName());
        map.put("prefix", team.getPlayerPrefix().getString());
        map.put("suffix", team.getPlayerSuffix().getString());
        map.put("friendly_fire", team.isAllowFriendlyFire());
        map.put("see_friendly_invisibles", team.canSeeFriendlyInvisibles());
        map.put("nametag_visibility", team.getNameTagVisibility().name);
        map.put("death_message_visibility", team.getDeathMessageVisibility().name);
        map.put("collision_rule", team.getCollisionRule().name);

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {ItemStackAPI.class, Float.class},
                            argumentNames = {"stack", "delta"}
                    ),
            },
            value = "player.get_cooldown_percent"
    )
    public float getCooldownPercent(@LuaNotNil ItemStackAPI stack, Float delta) {
        checkEntity();
        if (delta == null) delta = 0f;
        return this.entity.getCooldowns().getCooldownPercent(stack.itemStack.getItem(), delta);
    }

    @Override
    public String toString() {
        checkEntity();
        return entity.getName().getString() + " (Player)";
    }
}
