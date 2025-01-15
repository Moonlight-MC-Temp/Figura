package org.figuramc.figura.lua.api;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.ducks.GuiMessageAccessor;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.entity.EntityAPI;
import org.figuramc.figura.lua.api.world.ItemStackAPI;
import org.figuramc.figura.lua.api.world.WorldAPI;
import org.figuramc.figura.lua.docs.LuaFieldDoc;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.mixin.LivingEntityAccessor;
import org.figuramc.figura.mixin.gui.ChatComponentAccessor;
import org.figuramc.figura.mixin.gui.ChatScreenAccessor;
import org.figuramc.figura.model.rendering.texture.FiguraTexture;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.LuaUtils;
import org.figuramc.figura.utils.TextUtils;
import org.luaj.vm2.LuaError;

import java.util.*;

@LuaWhitelist
@LuaTypeDoc(
        name = "HostAPI",
        value = "host"
)
public class HostAPI {

    private final Avatar owner;
    private final boolean isHost;
    private final Minecraft minecraft;

    @LuaWhitelist
    @LuaFieldDoc("host.unlock_cursor")
    public boolean unlockCursor = false;
    public Integer chatColor;

    public HostAPI(Avatar owner) {
        this.owner = owner;
        this.minecraft = Minecraft.getInstance();
        this.isHost = owner.isHost;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_host")
    public boolean isHost() {
        return isHost;
    }

    @LuaWhitelist
    @LuaMethodDoc(value = "host.is_cursor_unlocked")
    public boolean isCursorUnlocked() {
        return unlockCursor;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Boolean.class,
                    argumentNames = "boolean"
            ),
            value = "host.set_unlock_cursor")
    public HostAPI setUnlockCursor(boolean bool) {
        unlockCursor = bool;
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "timesData"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, Integer.class, Integer.class},
                            argumentNames = {"fadeInTime", "stayTime", "fadeOutTime"}
                    )
            },
            aliases = "titleTimes",
            value = "host.set_title_times"
    )
    public HostAPI setTitleTimes(Object x, Double y, Double z) {
        if (!isHost()) return this;
        FiguraVec3 times = LuaUtils.parseVec3("setTitleTimes", x, y, z);
        this.minecraft.gui.setTimes((int) times.x, (int) times.y, (int) times.z);
        return this;
    }

    @LuaWhitelist
    public HostAPI titleTimes(Object x, Double y, Double z) {
        return setTitleTimes(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.clear_title")
    public HostAPI clearTitle() {
        if (isHost())
            this.minecraft.gui.clear();
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "title",
            value = "host.set_title"
    )
    public HostAPI setTitle(@LuaNotNil String text) {
        if (isHost())
            this.minecraft.gui.setTitle(TextUtils.tryParseJson(text));
        return this;
    }

    @LuaWhitelist
    public HostAPI title(@LuaNotNil String text) {
        return setTitle(text);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "subtitle",
            value = "host.set_subtitle"
    )
    public HostAPI setSubtitle(@LuaNotNil String text) {
        if (isHost())
            this.minecraft.gui.setSubtitle(TextUtils.tryParseJson(text));
        return this;
    }

    @LuaWhitelist
    public HostAPI subtitle(@LuaNotNil String text) {
        return setSubtitle(text);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "text"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, boolean.class},
                            argumentNames = {"text", "animated"}
                    )
            },
            aliases = "actionbar",
            value = "host.set_actionbar"
    )
    public HostAPI setActionbar(@LuaNotNil String text, boolean animated) {
        if (isHost())
            this.minecraft.gui.setOverlayMessage(TextUtils.tryParseJson(text), animated);
        return this;
    }

    @LuaWhitelist
    public HostAPI actionbar(@LuaNotNil String text, boolean animated) {
        return setActionbar(text, animated);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "message"
            ),
            value = "host.send_chat_message"
    )
    public HostAPI sendChatMessage(@LuaNotNil String message) {
        if (!isHost() || !Configs.CHAT_MESSAGES.value) return this;
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null) connection.sendChat(message);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "command"
            ),
            value = "host.send_chat_command"
    )
    public HostAPI sendChatCommand(@LuaNotNil String command) {
        if (!isHost() || !Configs.CHAT_MESSAGES.value) return this;
        ClientPacketListener connection = this.minecraft.getConnection();
        if (connection != null) connection.sendCommand(command.startsWith("/") ? command.substring(1) : command);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "message"
            ),
            value = "host.append_chat_history"
    )
    public HostAPI appendChatHistory(@LuaNotNil String message) {
        if (isHost())
            this.minecraft.gui.getChat().addRecentChat(message);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = Integer.class,
                    argumentNames = "index"
            ),
            value = "host.get_chat_message"
    )
    public Map<String, Object> getChatMessage(int index) {
        if (!isHost())
            return null;

        index--;
        List<GuiMessage> messages = ((ChatComponentAccessor) this.minecraft.gui.getChat()).getAllMessages();
        if (index < 0 || index >= messages.size())
            return null;

        GuiMessage message = messages.get(index);
        Map<String, Object> map = new HashMap<>();

        map.put("addedTime", message.addedTime());
        map.put("message", message.content().getString());
        map.put("json", message.content());
        map.put("backgroundColor", ((GuiMessageAccessor) (Object) message).figura$getColor());

        return map;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = Integer.class,
                            argumentNames = "index"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, String.class},
                            argumentNames = {"index", "newMessage"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Integer.class, String.class, FiguraVec3.class},
                            argumentNames = {"index", "newMessage", "backgroundColor"}
                    )
            },
            value = "host.set_chat_message")
    public HostAPI setChatMessage(int index, String newMessage, FiguraVec3 backgroundColor) {
        if (!isHost()) return this;

        index--;
        List<GuiMessage> messages = ((ChatComponentAccessor) this.minecraft.gui.getChat()).getAllMessages();
        if (index < 0 || index >= messages.size())
            return this;

        if (newMessage == null)
            messages.remove(index);
        else {
            GuiMessage old = messages.get(index);
            GuiMessage neww = new GuiMessage(this.minecraft.gui.getGuiTicks(), TextUtils.tryParseJson(newMessage), null, GuiMessageTag.chatModified(old.content().getString()));
            messages.set(index, neww);
            ((GuiMessageAccessor) (Object) neww).figura$setColor(backgroundColor != null ? ColorUtils.rgbToInt(backgroundColor) : ((GuiMessageAccessor) (Object) old).figura$getColor());
        }

        this.minecraft.gui.getChat().rescaleChat();
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload,
                    @LuaMethodOverload(
                            argumentTypes = Boolean.class,
                            argumentNames = "offhand"
                    )
            },
            value = "host.swing_arm"
    )
    public HostAPI swingArm(boolean offhand) {
        if (isHost() && this.minecraft.player != null)
            this.minecraft.player.swing(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "slot"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = Integer.class,
                            argumentNames = "slot"
                    )
            },
            value = "host.get_slot"
    )
    public ItemStackAPI getSlot(@LuaNotNil Object slot) {
        if (!isHost()) return null;
        Entity e = this.owner.luaRuntime.getUser();
        if (e == null || !e.isAlive())
            return ItemStackAPI.verify(ItemStack.EMPTY);
        SlotAccess slotAccess = e.getSlot(LuaUtils.parseSlot(slot, null));
        return ItemStackAPI.verify(slotAccess.get());
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(argumentTypes = String.class, argumentNames = "slot"),
                    @LuaMethodOverload(argumentTypes = Integer.class, argumentNames = "slot"),
                    @LuaMethodOverload(argumentTypes = {String.class, String.class}, argumentNames = {"slot", "item"}),
                    @LuaMethodOverload(argumentTypes = {Integer.class, ItemStackAPI.class}, argumentNames = {"slot", "item"})
            },
            value = "host.set_slot"
    )
    public HostAPI setSlot(@LuaNotNil Object slot, Object item) {
        if (!isHost() || (slot == null && item == null) || this.minecraft.gameMode == null || this.minecraft.player == null || !this.minecraft.gameMode.getPlayerMode().isCreative())
            return this;

        Inventory inventory = this.minecraft.player.getInventory();

        int index = LuaUtils.parseSlot(slot, inventory);
        ItemStack stack = LuaUtils.parseItemStack("setSlot", item);

        inventory.setItem(index, stack);
        this.minecraft.gameMode.handleCreativeModeItemAdd(stack, index + 36);

        return this;
    }

    @LuaWhitelist
    public HostAPI setBadge(int index, boolean value, boolean pride) {
        if (!isHost()) return this;
        if (!FiguraMod.debugModeEnabled())
            throw new LuaError("Congrats, you found this debug easter egg!");

        Pair<BitSet, BitSet> badges = AvatarManager.getBadges(owner.owner);
        if (badges == null)
            return this;

        BitSet set = pride ? badges.getFirst() : badges.getSecond();
        set.set(index, value);
        return this;
    }

    @LuaWhitelist
    public HostAPI badge(int index, boolean value, boolean pride) {
        return setBadge(index, value, pride);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_chat_color")
    public Integer getChatColor() {
        return isHost() ? this.chatColor : null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = FiguraVec3.class,
                            argumentNames = "color"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {Double.class, Double.class, Double.class},
                            argumentNames = {"r", "g", "b"}
                    )
            },
            aliases = "chatColor",
            value = "host.set_chat_color"
    )
    public HostAPI setChatColor(Object x, Double y, Double z) {
        if (isHost()) this.chatColor = x == null ? null : ColorUtils.rgbToInt(LuaUtils.parseVec3("setChatColor", x, y, z));
        return this;
    }

    @LuaWhitelist
    public HostAPI chatColor(Object x, Double y, Double z) {
        return setChatColor(x, y, z);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_chat_text")
    public String getChatText() {
        if (isHost() && this.minecraft.screen instanceof ChatScreen chat)
            return ((ChatScreenAccessor) chat).getInput().getValue();

        return null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "chatText",
            value = "host.set_chat_text"
    )
    public HostAPI setChatText(@LuaNotNil String text) {
        if (isHost() && Configs.CHAT_MESSAGES.value && this.minecraft.screen instanceof ChatScreen chat)
            ((ChatScreenAccessor) chat).getInput().setValue(text);
        return this;
    }

    @LuaWhitelist
    public HostAPI chatText(@LuaNotNil String text) {
        return setChatText(text);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_screen")
    public String getScreen() {
        if (!isHost() || this.minecraft.screen == null)
            return null;
        return this.minecraft.screen.getClass().getName();
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_screen_slot_count")
    public Integer getScreenSlotCount() {
        if (isHost() && this.minecraft.screen instanceof AbstractContainerScreen<?> screen)
            return screen.getMenu().slots.size();
        return null;
    }

    @LuaWhitelist
    @LuaMethodDoc(overloads = {
            @LuaMethodOverload(argumentTypes = String.class, argumentNames = "slot"),
            @LuaMethodOverload(argumentTypes = Integer.class, argumentNames = "slot")
    }, value = "host.get_screen_slot")
    public ItemStackAPI getScreenSlot(@LuaNotNil Object slot) {
        if (!isHost() || !(this.minecraft.screen instanceof AbstractContainerScreen<?> screen))
            return null;

        NonNullList<Slot> slots = screen.getMenu().slots;
        int index = LuaUtils.parseSlot(slot, null);
        if (index < 0 || index >= slots.size())
            return null;
        return ItemStackAPI.verify(slots.get(index).getItem());
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_chat_open")
    public boolean isChatOpen() {
        return isHost() && this.minecraft.screen instanceof ChatScreen;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_container_open")
    public boolean isContainerOpen() {
        return isHost() && this.minecraft.screen instanceof AbstractContainerScreen;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "name"
            ),
            value = "host.screenshot")
    public FiguraTexture screenshot(@LuaNotNil String name) {
        if (!isHost())
            return null;

        NativeImage img = Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget());
        return owner.luaRuntime.texture.register(name, img, true);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_avatar_uploaded")
    public boolean isAvatarUploaded() {
        return isHost() && AvatarManager.localUploaded;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_status_effects")
    public List<Map<String, Object>> getStatusEffects() {
        List<Map<String, Object>> list = new ArrayList<>();

        LocalPlayer player = this.minecraft.player;
        if (!isHost() || player == null)
            return list;

        for (MobEffectInstance effect : player.getActiveEffects()) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", effect.getEffect().getDescriptionId());
            map.put("amplifier", effect.getAmplifier());
            map.put("duration", effect.getDuration());
            map.put("visible", effect.isVisible());

            list.add(map);
        }

        return list;
    }
    @LuaWhitelist
    @LuaMethodDoc(
            value = "host.update_statistics",
            aliases = "updateStats"
    )
    public void updateStatistics() {
        this.minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS)); // in theory, this cant nullptr; if it does, ill fix later
    }
    @LuaWhitelist
    public void updateStats() {updateStatistics();}

    @LuaWhitelist
    @LuaMethodDoc(
            value = "host.get_general_statistics",
            aliases = "getGeneralStats"
    )
    public Map<String, Double> getGeneralStatistics() {
        Map<String, Double> map = new HashMap<>();
        LocalPlayer player = this.minecraft.player;
        if (player == null || !isHost()) return map;
        for (Stat<?> stat : Stats.CUSTOM) {
            String name = stat.getName().replace("minecraft.custom:minecraft.",""); // turn registry to translation key
            String formatted = stat.format(player.getStats().getValue(stat)).replaceAll("[^\\d.]", ""); // remove things that aren't digits or periods
            double finalValue = Double.parseDouble(formatted);
            map.put(name, finalValue);
        }
        return map;
    }
    @LuaWhitelist
    public Map<String, Double> getGeneralStats() { return getGeneralStatistics(); }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "host.get_item_statistic",
            aliases = "getItemStat",
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = ItemStackAPI.class,
                            argumentNames = "item"
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class},
                            argumentNames = "id"
                    )
            }
    )
    public Map<String, Integer> getItemStatistic(Object id) {
        Map<String, Integer> map = new HashMap<>();
        LocalPlayer player = this.minecraft.player;
        if (player == null || !isHost()) return map;
        String itemKey;
        ItemStackAPI stack;
        if (id instanceof String) {
            stack = WorldAPI.newItem(id.toString(), 1, null); // this handles errors for me and im lazy
            itemKey = id.toString();
        } else {
            stack = (ItemStackAPI) id;
            itemKey = BuiltInRegistries.ITEM.getKey(stack.itemStack.getItem()).toString();
        }

        int timesMined = 0; // init to 0 for if it's not a block
        int timesBroken;
        int timesCrafted;
        int timesUsed;
        int timesPickedUp;
        int timesDropped;
        if (stack.itemStack.getItem() instanceof BlockItem) {
            Block block = stack.getBlockstate().blockState.getBlock();
            Stat<Block> stat = Stats.BLOCK_MINED.get(block);
            timesMined = player.getStats().getValue(stat);
        }

        Stat<Item> brokenStat = Stats.ITEM_BROKEN.get(stack.itemStack.getItem());
        timesBroken = player.getStats().getValue(brokenStat);

        Stat<Item> craftedStat = Stats.ITEM_CRAFTED.get(stack.itemStack.getItem());
        timesCrafted = player.getStats().getValue(craftedStat);

        Stat<Item> usedStat = Stats.ITEM_USED.get(stack.itemStack.getItem());
        timesUsed = player.getStats().getValue(usedStat);

        Stat<Item> pickedUpStat = Stats.ITEM_PICKED_UP.get(stack.itemStack.getItem());
        timesPickedUp = player.getStats().getValue(pickedUpStat);

        Stat<Item> droppedStat = Stats.ITEM_DROPPED.get(stack.itemStack.getItem());
        timesDropped = player.getStats().getValue(droppedStat);


        map.put("mined", timesMined);
        map.put("crafted", timesCrafted);
        map.put("used", timesUsed);
        map.put("picked_up", timesPickedUp);
        map.put("broken", timesBroken);
        map.put("dropped", timesDropped);
        return map;
    }
    @LuaWhitelist
    public Map<String, Integer> getItemStat(Object id) { return getItemStatistic(id); }

    @LuaWhitelist
    @LuaMethodDoc(
            value = "host.get_entity_statistic",
            aliases = "get_entity_stat",
            overloads = @LuaMethodOverload(
                            argumentTypes = String.class,
                            argumentNames = "type"
                    )
            )
    public Map<String, Integer> getEntityStatistic(String type) {
        Map<String, Integer> map = new HashMap<>();
        LocalPlayer player = this.minecraft.player;
        if (player == null || !isHost()) return map;
        Optional<EntityType<?>> entity = EntityType.byString(type);
        if (entity.isPresent()) {
            Stat<EntityType<?>> killedStat = Stats.ENTITY_KILLED.get(entity.get());
            Stat<EntityType<?>> killedByStat = Stats.ENTITY_KILLED_BY.get(entity.get());

            int timesKilled = player.getStats().getValue(killedStat);
            int timesKilledBy = player.getStats().getValue(killedByStat);

            map.put("killed", timesKilled);
            map.put("killed_by", timesKilledBy);
        }
        else {
            throw new LuaError("Invalid entity type " + type);
        }
        return map;
    }
    @LuaWhitelist
    public Map<String, Integer> getEntityStat(String type) { return getEntityStatistic(type); }

    @LuaWhitelist
    @LuaMethodDoc("host.get_clipboard")
    public String getClipboard() {
        return isHost() ? this.minecraft.keyboardHandler.getClipboard() : null;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "text"
            ),
            aliases = "clipboard",
            value = "host.set_clipboard")
    public HostAPI setClipboard(@LuaNotNil String text) {
        if (isHost()) this.minecraft.keyboardHandler.setClipboard(text);
        return this;
    }

    @LuaWhitelist
    public HostAPI clipboard(@LuaNotNil String text) {
        return setClipboard(text);
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_attack_charge")
    public float getAttackCharge() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return player.getAttackStrengthScale(0f);
        return 0f;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_jumping")
    public boolean isJumping() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return ((LivingEntityAccessor) player).isJumping();
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_flying")
    public boolean isFlying() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return player.getAbilities().flying;
        return false;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_reach_distance")
    public double getReachDistance() {
        return this.minecraft.gameMode == null ? 0 : this.minecraft.gameMode.getPickRange();
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_air")
    public int getAir() {
        LocalPlayer player = this.minecraft.player;
        if (isHost() && player != null)
            return player.getAirSupply();
        return 0;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_pick_block")
    public Object[] getPickBlock() {
        return isHost() ? LuaUtils.parseBlockHitResult(minecraft.hitResult) : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.get_pick_entity")
    public EntityAPI<?> getPickEntity() {
        return isHost() && minecraft.crosshairPickEntity != null ? EntityAPI.wrap(minecraft.crosshairPickEntity) : null;
    }

    @LuaWhitelist
    @LuaMethodDoc("host.is_chat_verified")
    public boolean isChatVerified() {
        if (!isHost()) return false;
        ClientPacketListener connection = this.minecraft.getConnection();
        PlayerInfo playerInfo = connection != null ? connection.getPlayerInfo(owner.owner) : null;
        return playerInfo != null && playerInfo.hasVerifiableChat();
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(argumentTypes = String.class, argumentNames = "string"),
            },
            value = "host.write_to_log"
    )
    public void writeToLog(@LuaNotNil String string) {
        if (!isHost()) return;
        FiguraMod.LOGGER.info("[FIGURA/LUA] -- " + string);
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(argumentTypes = String.class, argumentNames = "string"),
            },
            value = "host.warn_to_log"
    )
    public void warnToLog(@LuaNotNil String string) {
        if (!isHost()) return;
        FiguraMod.LOGGER.warn("[FIGURA/LUA] -- " + string);
    }

    public Object __index(String arg) {
        if ("unlockCursor".equals(arg))
            return unlockCursor;
        return null;
    }

    @LuaWhitelist
    public void __newindex(@LuaNotNil String key, Object value) {
        if ("unlockCursor".equals(key))
            unlockCursor = (Boolean) value;
        else throw new LuaError("Cannot assign value on key \"" + key + "\"");
    }

    @Override
    public String toString() {
        return "HostAPI";
    }
}
