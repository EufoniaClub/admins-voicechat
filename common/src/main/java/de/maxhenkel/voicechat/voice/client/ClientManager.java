package de.maxhenkel.voicechat.voice.client;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.intercompatibility.ClientCompatibilityManager;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import de.maxhenkel.voicechat.net.NetManager;
import de.maxhenkel.voicechat.net.RequestSecretPacket;
import de.maxhenkel.voicechat.net.SecretPacket;
import io.netty.channel.local.LocalAddress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

public class ClientManager {

    @Nullable
    private ClientVoicechat client;
    private final ClientPlayerStateManager playerStateManager;
    private final PTTKeyHandler pttKeyHandler;
    private final RenderEvents renderEvents;
    private final KeyEvents keyEvents;
    private final Minecraft minecraft;

    private ClientManager() {
        playerStateManager = new ClientPlayerStateManager();
        pttKeyHandler = new PTTKeyHandler();
        renderEvents = new RenderEvents();
        keyEvents = new KeyEvents();
        minecraft = Minecraft.getInstance();

        ClientCompatibilityManager.INSTANCE.onJoinWorld(this::onJoinWorld);
        ClientCompatibilityManager.INSTANCE.onDisconnect(this::onDisconnect);

        ClientCompatibilityManager.INSTANCE.onVoiceChatConnected(connection -> {
            if (client != null) {
                client.onVoiceChatConnected(connection);
            }
        });
        ClientCompatibilityManager.INSTANCE.onVoiceChatDisconnected(() -> {
            if (client != null) {
                client.onVoiceChatDisconnected();
            }
        });

        CommonCompatibilityManager.INSTANCE.getNetManager().secretChannel.setClientListener((client, handler, packet) -> {
            authenticate(handler.getLocalGameProfile().getId(), packet);
        });
    }

    private void authenticate(UUID playerUUID, SecretPacket secretPacket) {
        if (client == null) {
            Voicechat.LOGGER.error("Received secret without a client being present");
            return;
        }
        Voicechat.LOGGER.info("Received secret");
        if (client.getConnection() != null) {
            ClientCompatibilityManager.INSTANCE.emitVoiceChatDisconnectedEvent();
        }
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            try {
                SocketAddress socketAddress = ClientCompatibilityManager.INSTANCE.getSocketAddress(connection.getConnection());
                if (socketAddress instanceof InetSocketAddress address) {
                    client.connect(new InitializationData(address.getHostString(), playerUUID, secretPacket));
                } else if (socketAddress instanceof LocalAddress) {
                    client.connect(new InitializationData("127.0.0.1", playerUUID, secretPacket));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void onJoinWorld() {
        Voicechat.LOGGER.info("Sending secret request to the server");
        NetManager.sendToServer(new RequestSecretPacket(Voicechat.COMPATIBILITY_VERSION));
        client = new ClientVoicechat();
    }

    private void onDisconnect() {
        if (client != null) {
            client.close();
            client = null;
        }
        ClientCompatibilityManager.INSTANCE.emitVoiceChatDisconnectedEvent();
    }

    @Nullable
    public static ClientVoicechat getClient() {
        return instance().client;
    }

    public static ClientPlayerStateManager getPlayerStateManager() {
        return instance().playerStateManager;
    }

    public static PTTKeyHandler getPttKeyHandler() {
        return instance().pttKeyHandler;
    }

    public static RenderEvents getRenderEvents() {
        return instance().renderEvents;
    }

    public KeyEvents getKeyEvents() {
        return keyEvents;
    }

    private static ClientManager instance;

    public static ClientManager instance() {
        if (instance == null) {
            instance = new ClientManager();
        }
        return instance;
    }

}