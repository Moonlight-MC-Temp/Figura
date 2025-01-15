package org.figuramc.figura.gui.screens;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.local.LocalAvatarFetcher;
import org.figuramc.figura.avatar.local.LocalAvatarLoader;
import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.backend2.NetworkStuff;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.gui.widgets.*;
import org.figuramc.figura.gui.widgets.lists.AvatarList;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.IOUtils;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.nio.file.Path;
import java.util.List;

public class WardrobeScreen extends AbstractPanelScreen {
    private static final Component DEBUG_MOTD_FALLBACK = Component.literal("No motd could be loaded.\n\n")
            .append("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n")
                    .withStyle(ChatFormatting.GRAY)
            .append(Component.literal("(This is some text you can hover)\n")
                    .withStyle(Style.EMPTY.withColor(0xFFF311A0).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("hi chat")))))
            .append(Component.literal("(This is some text you can click on)\n")
                    .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/FiguraMC/Figura"))))
            .append(Component.literal("(This is only visible in debug mode)")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

    private Label panic;

    private Button upload, delete, back;
    private ContextMenu uploadContext, deleteContext;

    public WardrobeScreen(Screen parentScreen) {
        super(parentScreen, FiguraText.of("gui.panels.title.wardrobe"));
    }

    private AvatarInfoWidget infoWidget;
    private BackendMotdWidget motdWidget;

    @Override
    protected void init() {
        super.init();

        // screen
        Minecraft minecraft = Minecraft.getInstance();
        int middle = width / 2;
        int panels = getPanels();

        int modelBgSize = Math.min(width - panels * 2 - 16, height - 96);
        panels = Math.max((width - modelBgSize) / 2 - 8, panels);

        // -- left -- // 

        AvatarList avatarList = new AvatarList(4, 28, panels, height - 32, this);
        addRenderableWidget(avatarList);

        // -- middle -- // 

        // model
        int entitySize = 11 * modelBgSize / 29;
        int entityX = middle - modelBgSize / 2;
        int entityY = this.height / 2 - modelBgSize / 2;

        EntityPreview entity = new EntityPreview(entityX, entityY, modelBgSize, modelBgSize, entitySize, -15f, 30f, minecraft.player, this);
        addRenderableWidget(entity);

        int buttX = entity.getX() + entity.getWidth() / 2;
        int buttY = entity.getY() + entity.getHeight() + 4;

        // upload
        addRenderableWidget(upload = new Button(buttX - 48, buttY, 24, 24, 0, 0, 24, new FiguraIdentifier("textures/gui/upload.png"), 72, 24, FiguraText.of("gui.wardrobe.upload.tooltip"), button -> {
            if (FSB.instance().connected()) showUploadContext();
            else uploadAvatar(NetworkStuff.Destination.FSB_OR_BACKEND);
        }));
        upload.setActive(false);
        generateUploadContext();

        // reload
        addRenderableWidget(new Button(buttX - 12, buttY, 24, 24, 0, 0, 24, new FiguraIdentifier("textures/gui/reload.png"), 72, 24, FiguraText.of("gui.wardrobe.reload.tooltip"), button -> {
            AvatarManager.clearAvatars(FiguraMod.getLocalPlayerUUID());
            try {
                LocalAvatarLoader.loadAvatar(null, null);
            } catch (Exception ignored) {}
            AvatarManager.localUploaded = true;
            AvatarList.selectedEntry = null;
            NetworkStuff.auth();
        }));

        // delete
        addRenderableWidget(delete = new Button(buttX + 24, buttY, 24, 24, 0, 0, 24, new FiguraIdentifier("textures/gui/delete.png"), 72, 24, FiguraText.of("gui.wardrobe.delete.tooltip"), button -> {
            if (FSB.instance().connected()) showDeleteContext();
            else deleteAvatar(NetworkStuff.Destination.FSB_OR_BACKEND);
        }));
        delete.setActive(false);
        generateDeleteContext();

        StatusWidget statusWidget = new StatusWidget(entity.getX() + entity.getWidth() - 64, 0, 64);
        statusWidget.setY(entity.getY() - statusWidget.getHeight() - 4);
        addRenderableOnly(statusWidget);

        addRenderableOnly(new LoadingErrorWidget(statusWidget.getX() - 18, statusWidget.getY(), 14));

        // -- bottom -- // 

        // version
        MutableComponent versionText = FiguraText.of().append(" " + FiguraMod.VERSION.noBuildString()).withStyle(ChatFormatting.ITALIC);
        int versionStatus = NetworkStuff.latestVersion != null ? NetworkStuff.latestVersion.compareTo(FiguraMod.VERSION) : 0;
        boolean oldVersion = versionStatus > 0;
        if (oldVersion) {
            versionText
                    .append(" ")
                    .append(Component.literal("=")
                            .withStyle(Style.EMPTY
                                    .withFont(UIHelper.UI_FONT)
                                    .withItalic(false)
                                    .applyLegacyFormat(ChatFormatting.WHITE)
                            ))
                    .withStyle(Style.EMPTY
                            .applyFormat(ChatFormatting.AQUA)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    FiguraText.of("gui.new_version.tooltip", Component.literal(NetworkStuff.latestVersion.toString()).withStyle(ChatFormatting.GREEN))
                            ))
                            .withClickEvent(new TextUtils.FiguraClickEvent(UIHelper.openURL(
                                NetworkStuff.latestVersion.pre==null ? 
                                    (FiguraMod.Links.Modrinth.url + "/versions") : 
                                    (FiguraMod.Links.Github.url + "/releases")
                                ))
                            )
                    );
        } else if (versionStatus < 0) {
            versionText.withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    FiguraText.of("gui.old_version.tooltip", Component.literal(NetworkStuff.latestVersion.toString()).withStyle(ChatFormatting.LIGHT_PURPLE))
            )));
        }

        Label version = new Label(versionText, middle, this.height - 4, TextUtils.Alignment.CENTER);
        addRenderableWidget(version);
        if (!oldVersion) version.setAlpha(0x33);
        version.setY(version.getRawY() - version.getHeight());

        int rightSide = Math.min(panels, 134);

        // back
        back = new Button(width - rightSide - 4, height - 24, rightSide, 20, FiguraText.of("gui.done"), null, bx -> onClose());
        addRenderableWidget(back);

        // -- right side -- //

        rightSide = panels / 2 + 52;

        // avatar settings
        Button avatarSettings;
        addRenderableWidget(avatarSettings = new Button(
                this.width - rightSide, 28, 24, 24,
                0, 0, 24,
                new FiguraIdentifier("textures/gui/avatar_settings.png"),
                72, 24,
                FiguraText.of("gui.avatar_settings.tooltip").append("\n").append(FiguraText.of("gui.not_available_yet").withStyle(ChatFormatting.RED)),
                bx -> {}
        ));
        avatarSettings.setActive(false);

        // sounds
        Button sounds = new Button(this.width - rightSide + 36, 28, 24, 24, 0, 0, 24, new FiguraIdentifier("textures/gui/sound.png"), 72, 24, FiguraText.of("gui.wardrobe.sound.tooltip"),
                button -> Minecraft.getInstance().setScreen(new SoundScreen(this))
        );
        addRenderableWidget(sounds);

        // keybinds
        Button keybinds = new Button(this.width - rightSide + 72, 28, 24, 24, 0, 0, 24, new FiguraIdentifier("textures/gui/keybind.png"), 72, 24, FiguraText.of("gui.wardrobe.keybind.tooltip"),
                button -> Minecraft.getInstance().setScreen(new KeybindScreen(this))
        );
        addRenderableWidget(keybinds);

        // avatar metadata
        addRenderableOnly(infoWidget = new AvatarInfoWidget(this.width - panels - 4, 56, panels, back.getY() - 60));

        // backend MOTD
        if (motdWidget != null) {
            removeWidget(motdWidget);
            motdWidget = null;
        }
        updateMotdWidget();

        // panic warning - always added last, on top
        addRenderableWidget(panic = new Label(
                FiguraText.of("gui.panic", Configs.PANIC_BUTTON.keyBind.getTranslatedKeyMessage()).withStyle(ChatFormatting.YELLOW),
                middle, version.getRawY(), TextUtils.Alignment.CENTER, 0)
        );
        panic.setY(panic.getRawY() - panic.getHeight());
        panic.setVisible(false);
    }

    private void uploadAvatar(NetworkStuff.Destination destination) {
        Avatar avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID());
        try {
            LocalAvatarLoader.loadAvatar(null, null);
        } catch (Exception ignored) {}
        NetworkStuff.uploadAvatar(avatar, destination);
        AvatarList.selectedEntry = null;
    }

    private void showUploadContext() {
        if (!FSB.instance().connected()) return;
        Window window = minecraft.getWindow();
        double mouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        uploadContext.setX((int) mouseX);
        uploadContext.setY((int) mouseY);
        uploadContext.setVisible(true);
        UIHelper.setContext(uploadContext);
    }

    private void deleteAvatar(NetworkStuff.Destination destination) {
        NetworkStuff.deleteAvatar(null, destination);
    }

    private void showDeleteContext() {
        if (!FSB.instance().connected()) return;
        Window window = minecraft.getWindow();
        double mouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        deleteContext.setX((int) mouseX);
        deleteContext.setY((int) mouseY);
        deleteContext.setVisible(true);
        UIHelper.setContext(deleteContext);
    }

    private void generateUploadContext() {
        uploadContext = new ContextMenu(upload);
        uploadContext.addAction(FiguraText.of("gui.wardrobe.upload_backend"), FiguraText.of("gui.wardrobe.upload_backend.tooltip"), button -> {
            uploadAvatar(NetworkStuff.Destination.BACKEND);
        });

        uploadContext.addAction(FiguraText.of("gui.wardrobe.upload_fsb"), FiguraText.of("gui.wardrobe.upload_fsb.tooltip"), button -> {
            uploadAvatar(NetworkStuff.Destination.FSB);
        });

        uploadContext.addAction(FiguraText.of("gui.wardrobe.upload_both"), FiguraText.of("gui.wardrobe.upload_both.tooltip"), button -> {
            uploadAvatar(NetworkStuff.Destination.BOTH);
        });
    }

    private void generateDeleteContext() {
        deleteContext = new ContextMenu(delete);
        deleteContext.addAction(FiguraText.of("gui.wardrobe.delete_backend"), FiguraText.of("gui.wardrobe.delete_backend.tooltip"), button -> {
            deleteAvatar(NetworkStuff.Destination.BACKEND);
        });

        deleteContext.addAction(FiguraText.of("gui.wardrobe.delete_fsb"), FiguraText.of("gui.wardrobe.delete_fsb.tooltip"), button -> {
            deleteAvatar(NetworkStuff.Destination.FSB);
        });

        deleteContext.addAction(FiguraText.of("gui.wardrobe.delete_both"), FiguraText.of("gui.wardrobe.delete_both.tooltip"), button -> {
            deleteAvatar(NetworkStuff.Destination.BOTH);
        });
    }

    private int getPanels() {
        return Math.min(width / 3, 256) - 8;
    }

    private void updateMotdWidget() {
        int panels = getPanels();

        int infoBottom = infoWidget.getY() + infoWidget.getHeight();

        int width = panels - 8;
        int height = back.getY() - infoBottom - 16;
        int x = this.width - panels;
        int y = infoBottom + 8;

        infoWidget.tick();
        if (motdWidget == null) {
            Component motd = NetworkStuff.motd == null ? DEBUG_MOTD_FALLBACK : NetworkStuff.motd;
            if (!FiguraMod.debugModeEnabled() && motd == DEBUG_MOTD_FALLBACK) {
                return;
            }
            motdWidget = addRenderableWidget(new BackendMotdWidget(x, y, width, height, motd, font));
        }  else {
            motdWidget.setPosition(x, y);
            motdWidget.setWidth(width);
            motdWidget.setHeight(height);
            Component motd = NetworkStuff.motd == null ? DEBUG_MOTD_FALLBACK : NetworkStuff.motd;
            if (!FiguraMod.debugModeEnabled() && motd == DEBUG_MOTD_FALLBACK) {
                return;
            }
            motdWidget.setMessage(motd);
        }

        motdWidget.visible = motdWidget.getHeight() > 48;
    }

    @Override
    public void tick() {
        // children tick
        super.tick();

        // panic visible
        panic.setVisible(AvatarManager.panic);

        // backend buttons
        Avatar avatar;
        upload.setActive(NetworkStuff.canUpload() && !AvatarManager.localUploaded && (avatar = AvatarManager.getAvatarForPlayer(FiguraMod.getLocalPlayerUUID())) != null && avatar.nbt != null && avatar.loaded);
        delete.setActive((FSB.instance().connected() || NetworkStuff.isConnected()) && AvatarManager.localUploaded);

        updateMotdWidget();
    }

    @Override
    public void removed() {
        super.removed();
        LocalAvatarFetcher.save();
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        super.onFilesDrop(paths);

        StringBuilder packs = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0)
                packs.append("\n");
            packs.append(IOUtils.getFileNameOrEmpty(paths.get(i)));
        }

        this.minecraft.setScreen(new FiguraConfirmScreen(confirmed -> {
            if (confirmed) {
                try {
                    LocalAvatarFetcher.loadExternal(paths);
                    FiguraToast.sendToast(FiguraText.of("toast.wardrobe_copy.success", paths.size()));
                } catch (Exception e) {
                    FiguraToast.sendToast(FiguraText.of("toast.wardrobe_copy.error"), FiguraToast.ToastType.ERROR);
                    FiguraMod.LOGGER.error("Failed to copy files", e);
                }
            }
            this.minecraft.setScreen(this);
        }, FiguraText.of("gui.wardrobe.drop_files"), packs.toString(), this));
    }
}
