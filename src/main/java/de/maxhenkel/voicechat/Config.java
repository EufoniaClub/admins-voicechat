package de.maxhenkel.voicechat;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<ServerConfig, ForgeConfigSpec> specPairServer = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPairServer.getRight();
        SERVER = specPairServer.getLeft();

        Pair<ClientConfig, ForgeConfigSpec> specPairClient = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPairClient.getRight();
        CLIENT = specPairClient.getLeft();
    }

    public static class ServerConfig {

        public final ForgeConfigSpec.IntValue VOICE_CHAT_PORT;
        public final ForgeConfigSpec.DoubleValue VOICE_CHAT_DISTANCE;
        public final ForgeConfigSpec.DoubleValue VOICE_CHAT_FADE_DISTANCE;

        public ServerConfig(ForgeConfigSpec.Builder builder) {
            VOICE_CHAT_PORT = builder
                    .comment("The port of the voice chat server")
                    .defineInRange("voice_chat_port", 24454, 0, 65535);

            VOICE_CHAT_DISTANCE = builder
                    .comment("The distance to where the voice can be heard")
                    .defineInRange("voice_chat_distance", 32D, 1D, 1_000_000D);

            VOICE_CHAT_FADE_DISTANCE = builder
                    .comment("The distance to where the voice starts fading")
                    .defineInRange("voice_chat_fade_distance", 16D, 1D, 1_000_000D);
        }
    }

    public static class ClientConfig {

        public final ForgeConfigSpec.DoubleValue VOICE_CHAT_VOLUME;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            VOICE_CHAT_VOLUME = builder
                    .comment("The voice chat volume")
                    .defineInRange("voice_chat_volume", 1D, 0D, 2D);
        }
    }

}
