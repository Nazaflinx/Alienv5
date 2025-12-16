package dev.luminous.mod.modules.impl.client;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import dev.luminous.Alien;
import dev.luminous.mod.gui.notification.NotificationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.network.ServerInfo;

import java.time.OffsetDateTime;

public class DiscordRPC extends Module {
    public static DiscordRPC INSTANCE;

    private IPCClient client;
    private long startTime;
    private boolean connected = false;

    private final StringSetting clientId = add(new StringSetting("ClientId", "1234567890123456789"));
    private final BooleanSetting showServer = add(new BooleanSetting("ShowServer", true));
    private final BooleanSetting showPlayerCount = add(new BooleanSetting("ShowPlayerCount", false));
    private final BooleanSetting showElapsedTime = add(new BooleanSetting("ShowElapsedTime", true));
    private final EnumSetting<ImageMode> imageMode = add(new EnumSetting<>("ImageMode", ImageMode.Logo));
    private final StringSetting largeImageKey = add(new StringSetting("LargeImageKey", "alien_logo"));
    private final StringSetting largeImageText = add(new StringSetting("LargeImageText", "Alien Client"));
    private final StringSetting smallImageKey = add(new StringSetting("SmallImageKey", "minecraft"));
    private final StringSetting smallImageText = add(new StringSetting("SmallImageText", "Minecraft 1.20.4"));
    private final StringSetting details = add(new StringSetting("Details", "Playing Minecraft"));
    private final StringSetting state = add(new StringSetting("State", "Using Alien Client"));

    public DiscordRPC() {
        super("DiscordRPC", "Shows Discord Rich Presence", Category.Client);
        setChinese("Discord状态");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        startTime = System.currentTimeMillis();
        connectDiscord();
    }

    @Override
    public void onDisable() {
        disconnectDiscord();
    }

    private void connectDiscord() {
        try {
            client = new IPCClient(Long.parseLong(clientId.getValue()));
            client.setListener(new IPCListener() {
                @Override
                public void onReady(IPCClient client) {
                    connected = true;
                    NotificationManager.getInstance().success("Discord RPC", "Connected to Discord");
                    updatePresence();
                }

                @Override
                public void onClose(IPCClient client, String json) {
                    connected = false;
                }
            });

            client.connect();
        } catch (NoDiscordClientException e) {
            NotificationManager.getInstance().error("Discord RPC", "Discord client not found");
            connected = false;
        } catch (Exception e) {
            NotificationManager.getInstance().error("Discord RPC", "Failed to connect: " + e.getMessage());
            connected = false;
        }
    }

    private void disconnectDiscord() {
        if (client != null && connected) {
            try {
                client.close();
                connected = false;
                NotificationManager.getInstance().info("Discord RPC", "Disconnected from Discord");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onUpdate() {
        if (!connected || client == null) return;

        updatePresence();
    }

    private void updatePresence() {
        if (!connected || client == null) return;

        try {
            RichPresence.Builder builder = new RichPresence.Builder();

            if (imageMode.getValue() == ImageMode.Logo || imageMode.getValue() == ImageMode.Both) {
                builder.setLargeImage(largeImageKey.getValue(), largeImageText.getValue());
            }

            if (imageMode.getValue() == ImageMode.Minecraft || imageMode.getValue() == ImageMode.Both) {
                builder.setSmallImage(smallImageKey.getValue(), smallImageText.getValue());
            }

            String detailsText = details.getValue();
            String stateText = state.getValue();

            if (showServer.getValue() && mc.getCurrentServerEntry() != null) {
                ServerInfo server = mc.getCurrentServerEntry();
                detailsText = "Playing on " + server.name;

                if (showPlayerCount.getValue()) {
                    stateText = server.playerCountLabel.getString();
                }
            } else if (mc.isInSingleplayer()) {
                detailsText = "Playing Singleplayer";
            }

            builder.setDetails(detailsText);
            builder.setState(stateText);

            if (showElapsedTime.getValue()) {
                builder.setStartTimestamp(OffsetDateTime.now().minusSeconds((System.currentTimeMillis() - startTime) / 1000));
            }

            client.sendRichPresence(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public enum ImageMode {
        Logo,
        Minecraft,
        Both,
        None
    }
}
