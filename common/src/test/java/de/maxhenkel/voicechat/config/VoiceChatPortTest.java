package de.maxhenkel.voicechat.config;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.configbuilder.entry.ConfigEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class VoiceChatPortTest {

    @TempDir
    Path directory;

    private ConfigEntry<String> load() {
        return ConfigBuilder.builder(VoiceChatPort::createEntry)
                .path(directory.resolve("voicechat-server.properties"))
                .saveSyncAfterBuild(true).build();
    }

    private String savedPort() throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(directory.resolve("voicechat-server.properties"))) {
            properties.load(reader);
        }
        return properties.getProperty("port");
    }

    @Test
    void newConfigStaysBlankAndFollowsServerPortAcrossRestarts() throws IOException {
        ConfigEntry<String> entry = load();
        assertEquals("", entry.get());
        assertEquals(25665, VoiceChatPort.resolve(entry.get(), 25565));
        entry.saveSync();
        assertEquals("", savedPort());
        assertEquals(30100, VoiceChatPort.resolve(load().get(), 30000));
        assertEquals("", savedPort());
    }

    @Test
    void existingNumericOverrideSurvivesSavesAndServerPortChanges() throws IOException {
        Files.writeString(directory.resolve("voicechat-server.properties"), "port=24454\n");
        ConfigEntry<String> entry = load();
        assertEquals(24454, VoiceChatPort.resolve(entry.get(), 25565));
        assertEquals(24454, VoiceChatPort.resolve(load().get(), 30000));
        assertEquals("24454", savedPort());
        entry.set("31000").saveSync();
        assertEquals(31000, VoiceChatPort.resolve(load().get(), 65535));
        assertEquals("31000", savedPort());
    }

    @Test
    void clearingOverrideEnablesAutomaticPortAgain() throws IOException {
        load().set("31000").saveSync();
        load().set("").saveSync();
        assertEquals(30100, VoiceChatPort.resolve(load().get(), 30000));
        assertEquals("", savedPort());
    }

    @Test
    void acceptsWhitespaceAndPreservesLegacySpecialValues() {
        assertEquals(25665, VoiceChatPort.resolve("  ", 25565));
        assertEquals(31000, VoiceChatPort.resolve(" 31000 ", 25565));
        assertEquals(25565, VoiceChatPort.resolve("-1", 25565));
        assertEquals(0, VoiceChatPort.resolve("0", 25565));
    }

    @Test
    void rejectsInvalidOverridesWithoutReplacingThemInConfig() throws IOException {
        for (String value : new String[]{"invalid", "-2", "65536", "999999999999"}) {
            load().set(value).saveSync();
            ConfigEntry<String> entry = load();
            assertThrows(IllegalArgumentException.class, () -> VoiceChatPort.resolve(entry.get(), 25565));
            assertEquals(value, savedPort());
        }
    }

    @Test
    void checksAutomaticPortBounds() {
        assertEquals(65535, VoiceChatPort.resolve("", 65435));
        assertThrows(IllegalArgumentException.class, () -> VoiceChatPort.resolve("", 65436));
        assertThrows(IllegalArgumentException.class, () -> VoiceChatPort.resolve("", 0));
        assertThrows(IllegalArgumentException.class, () -> VoiceChatPort.resolve("", Integer.MAX_VALUE));
        assertEquals(1, VoiceChatPort.resolve("1", 65535));
        assertEquals(65535, VoiceChatPort.resolve("65535", 25565));
    }
}
