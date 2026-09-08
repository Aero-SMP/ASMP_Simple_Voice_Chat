package de.maxhenkel.voicechat.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;

public final class VoiceChatPort {

    private VoiceChatPort() {
    }

    public static ConfigEntry<String> createEntry(ConfigBuilder builder) {
        return builder.stringEntry("port", "",
                "The UDP port to use for voice chat communication.",
                "Leave blank to use the Minecraft server port + 100 on each server start.",
                "The calculated port is never written to this config.",
                "Set a port from 1 to 65535 to keep a fixed port instead.",
                "Set -1 to use the Minecraft server port, or 0 to select an available port.",
                "Using the Minecraft server port may conflict with the UDP server query."
        );
    }

    public static int resolve(String configuredPort, int minecraftPort) {
        String value = configuredPort.trim();
        if (value.isEmpty()) {
            if (minecraftPort < 1 || minecraftPort > 65435) {
                throw new IllegalArgumentException("Minecraft server port + 100 must be between 1 and 65535; set an explicit voice chat port");
            }
            return minecraftPort + 100;
        }

        int port;
        try {
            port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid voice chat port '" + configuredPort + "': use a blank value or an integer from -1 to 65535", e);
        }
        if (port < -1 || port > 65535) {
            throw new IllegalArgumentException("Voice chat port must be between -1 and 65535: " + port);
        }
        return port == -1 ? minecraftPort : port;
    }

}
