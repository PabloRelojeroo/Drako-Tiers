package com.tiers;

import com.mojang.brigadier.context.CommandContext;
import com.tiers.misc.ColorControl;
import com.tiers.misc.ColorLoader;
import com.tiers.misc.Icons;
import com.tiers.misc.PlayerProfileQueue;
import com.tiers.profiles.GameMode;
import com.tiers.profiles.PlayerProfile;
import com.tiers.profiles.Status;
import com.tiers.profiles.types.BaseProfile;
import com.tiers.screens.PlayerSearchResultScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FileUtils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TiersClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(TiersClient.class);
    protected static final ArrayList<PlayerProfile> playerProfiles = new ArrayList<>();
    protected static final HashMap<String, Text> playerTexts = new HashMap<>();

    public static boolean toggleMod = true;
    public static boolean showIcons = true;
    public static boolean isSeparatorAdaptive = true;
    public static ModesTierDisplay displayMode = ModesTierDisplay.ADAPTIVE_HIGHEST;

    public static DisplayStatus mcTiersCOMPosition = DisplayStatus.LEFT;
    public static Modes activeMCTiersCOMMode = Modes.MCTIERSCOM_VANILLA;

    public static DisplayStatus drakenseTiersPosition = DisplayStatus.RIGHT;
    public static Modes activeDrakenseTiersMode = Modes.DRAKENSE_VANILLA;

    private static KeyBinding cycleRightKey;
    private static KeyBinding cycleLeftKey;
    private static KeyBinding openNearestPlayerKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfig();
        clearCache();
        CommandRegister.registerCommands();
        FabricLoader.getInstance().getModContainer("drktiers").ifPresent(tiers -> 
            ResourceManagerHelper.registerBuiltinResourcePack(new Identifier("drktiers", "modern"), tiers, ResourcePackActivationType.ALWAYS_ENABLED));
        FabricLoader.getInstance().getModContainer("drktiers").ifPresent(tiers -> 
            ResourceManagerHelper.registerBuiltinResourcePack(new Identifier("drktiers", "classic"), tiers, ResourcePackActivationType.NORMAL));
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new ColorLoader());
        cycleRightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Cycle Right Gamemodes", GLFW.GLFW_KEY_I, "DRKTiers"));
        cycleLeftKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Cycle Left Gamemodes", GLFW.GLFW_KEY_U, "DRKTiers"));
        openNearestPlayerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open Nearest Player Profile", GLFW.GLFW_KEY_O, "DRKTiers"));
        ClientTickEvents.END_CLIENT_TICK.register(TiersClient::checkKeys);
        LOGGER.info("DRKTiers initialized");
    }

    public static Text getFullName(String originalName, Text originalNameText) {
        PlayerProfile profile = addGetPlayer(originalName, false);
        if (profile.status == Status.READY) {
            if (profile.originalNameText == null || profile.originalNameText != originalNameText)
                updatePlayerNametag(originalNameText, profile);
        }

        if (playerTexts.containsKey(originalName)) return playerTexts.get(originalName);

        return originalNameText;
    }

    public static void updateAllTags() {
        for (PlayerProfile profile : playerProfiles) {
            if (profile.status == Status.READY && profile.originalNameText != null)
                updatePlayerNametag(profile.originalNameText, profile);
        }
    }

    public static void updatePlayerNametag(Text originalNameText, PlayerProfile profile) {
        Text rightText = Text.literal("");
        Text leftText = Text.literal("");

        if (mcTiersCOMPosition == DisplayStatus.RIGHT) {
            rightText = updateProfileNameTagRight(profile.mcTiersCOMProfile, activeMCTiersCOMMode);
        } else if (mcTiersCOMPosition == DisplayStatus.LEFT) {
            leftText = updateProfileNameTagLeft(profile.mcTiersCOMProfile, activeMCTiersCOMMode);
        }
        if (drakenseTiersPosition == DisplayStatus.RIGHT) {
            rightText = updateProfileNameTagRight(profile.drakenseTiersProfile, activeDrakenseTiersMode);
        } else if (drakenseTiersPosition == DisplayStatus.LEFT) {
            leftText = updateProfileNameTagLeft(profile.drakenseTiersProfile, activeDrakenseTiersMode);
        }

        playerTexts.put(profile.name, Text.literal("")
                .append(leftText)
                .append(originalNameText)
                .append(rightText));

        profile.originalNameText = originalNameText;
    }

    private static Text updateProfileNameTagRight(BaseProfile profile, Modes activeMode) {
        MutableText returnValue = Text.literal("");
        if (profile == null || profile.status != Status.READY) return returnValue;
        
        GameMode shown = profile.getGameMode(activeMode);
        if (shown == null || shown.status == Status.SEARCHING) return returnValue;
        if (shown.status == Status.NOT_EXISTING && displayMode == ModesTierDisplay.SELECTED) return returnValue;
        
        if (displayMode == ModesTierDisplay.ADAPTIVE_HIGHEST && shown.status == Status.NOT_EXISTING && profile.highest != null)
            shown = profile.highest;
        if (displayMode == ModesTierDisplay.HIGHEST && profile.highest != null && profile.highest.getTierPoints(false) > shown.getTierPoints(false))
            shown = profile.highest;
        if (shown == null || shown.status != Status.READY) return returnValue;
        
        MutableText separator = Text.literal(" | ").setStyle(isSeparatorAdaptive ? shown.displayedTier.getStyle() : Style.EMPTY.withColor(ColorControl.getColor("static_separator")));
        returnValue.append(Text.literal("").append(separator).append(shown.displayedTier));
        if (showIcons)
            returnValue.append(Text.literal(" ").append(shown.name.getIconTag()));
        
        return returnValue;
    }

    private static Text updateProfileNameTagLeft(BaseProfile profile, Modes activeMode) {
        MutableText returnValue = Text.literal("");
        if (profile == null || profile.status != Status.READY) return returnValue;
        
        GameMode shown = profile.getGameMode(activeMode);
        if (shown == null || shown.status == Status.SEARCHING) return returnValue;
        if (shown.status == Status.NOT_EXISTING && displayMode == ModesTierDisplay.SELECTED) return returnValue;
        
        if (displayMode == ModesTierDisplay.ADAPTIVE_HIGHEST && shown.status == Status.NOT_EXISTING && profile.highest != null)
            shown = profile.highest;
        if (displayMode == ModesTierDisplay.HIGHEST && profile.highest != null && profile.highest.getTierPoints(false) > shown.getTierPoints(false))
            shown = profile.highest;
        if (shown == null || shown.status != Status.READY) return returnValue;
        
        MutableText separator = Text.literal(" | ").setStyle(isSeparatorAdaptive ? shown.displayedTier.getStyle() : Style.EMPTY.withColor(ColorControl.getColor("static_separator")));
        if (showIcons)
            returnValue = Text.literal("").append(shown.name.getIconTag()).append(" ");
        returnValue.append(Text.literal("").append(shown.displayedTier).append(separator));
        
        return returnValue;
    }

    public static void restyleAllTexts() {
        for (PlayerProfile profile : playerProfiles) {
            if (profile.status == Status.READY) {
                if (profile.mcTiersCOMProfile != null && profile.mcTiersCOMProfile.status == Status.READY)
                    profile.mcTiersCOMProfile.parseInfo(profile.mcTiersCOMProfile.originalJson);
                if (profile.drakenseTiersProfile != null && profile.drakenseTiersProfile.status == Status.READY)
                    profile.drakenseTiersProfile.parseInfo(profile.drakenseTiersProfile.originalJson);
            }
        }
    }

    public static void checkKeys(MinecraftClient client) {
        if (cycleRightKey.wasPressed()) {
            if (client.player == null) return;
            if (mcTiersCOMPosition.toString().equalsIgnoreCase("RIGHT")) {
                client.player.sendMessage(Text.literal("Right (MCTiersCOM) is now displaying ")
                        .setStyle(Style.EMPTY.withColor(ColorControl.getColor("text"))).append(cycleMCTiersCOMMode()), true);
                return;
            }
            if (drakenseTiersPosition.toString().equalsIgnoreCase("RIGHT")) {
                client.player.sendMessage(Text.literal("Right (DrakenseTiers) is now displaying ")
                        .setStyle(Style.EMPTY.withColor(ColorControl.getColor("text"))).append(cycleDrakenseTiersMode()), true);
                return;
            }
            client.player.sendMessage(Text.literal("There's nothing on the right display").setStyle(Style.EMPTY.withColor(ColorControl.getColor("red"))), true);
        }
        if (cycleLeftKey.wasPressed()) {
            if (client.player == null) return;
            if (mcTiersCOMPosition.toString().equalsIgnoreCase("LEFT")) {
                client.player.sendMessage(Text.literal("Left (MCTiersCOM) is now displaying ")
                        .setStyle(Style.EMPTY.withColor(ColorControl.getColor("text"))).append(cycleMCTiersCOMMode()), true);
                return;
            }
            if (drakenseTiersPosition.toString().equalsIgnoreCase("LEFT")) {
                client.player.sendMessage(Text.literal("Left (DrakenseTiers) is now displaying ")
                        .setStyle(Style.EMPTY.withColor(ColorControl.getColor("text"))).append(cycleDrakenseTiersMode()), true);
                return;
            }
            client.player.sendMessage(Text.literal("There's nothing on the left display").setStyle(Style.EMPTY.withColor(ColorControl.getColor("red"))), true);
        }
        if (openNearestPlayerKey.wasPressed()) {
            if (client.player == null || client.world == null) return;
            net.minecraft.entity.player.PlayerEntity nearestPlayer = null;
            double nearestDistance = Double.MAX_VALUE;
            for (net.minecraft.entity.player.PlayerEntity player : client.world.getPlayers()) {
                if (player == client.player) continue;
                double distance = client.player.squaredDistanceTo(player);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
            if (nearestPlayer != null) {
                searchPlayer(nearestPlayer.getGameProfile().getName());
            } else {
                client.player.sendMessage(Text.literal("No players nearby").setStyle(Style.EMPTY.withColor(ColorControl.getColor("red"))), true);
            }
        }
    }

    public static void sendMessageToPlayer(String chat_message, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null)
            client.player.sendMessage((Text.literal(chat_message).setStyle(Style.EMPTY.withColor(color))), false);
    }

    protected static int toggleMod(CommandContext<FabricClientCommandSource> ignoredFabricClientCommandSourceCommandContext) {
        toggleMod = !toggleMod;
        ConfigManager.saveConfig();
        sendMessageToPlayer("DRKTiers is now " + (toggleMod ? "enabled" : "disabled"), (toggleMod ? ColorControl.getColor("green") : ColorControl.getColor("red")));
        return 1;
    }

    public static void toggleMod() {
        toggleMod = !toggleMod;
        ConfigManager.saveConfig();
    }

    public static PlayerProfile addGetPlayer(String name, boolean priority) {
        for (PlayerProfile profile : playerProfiles) {
            if (profile.name.equalsIgnoreCase(name)) {
                if (priority)
                    PlayerProfileQueue.changeToFirstInQueue(profile);
                return profile;
            }
        }
        for (PlayerProfile profile : playerProfiles) {
            if (profile.name.equalsIgnoreCase(name)) {
                if (priority)
                    PlayerProfileQueue.changeToFirstInQueue(profile);
                return profile;
            }
        }
        PlayerProfile newProfile = new PlayerProfile(name);

        if (priority)
            PlayerProfileQueue.putFirstInQueue(newProfile);
        else
            PlayerProfileQueue.enqueue(newProfile);

        playerProfiles.add(newProfile);
        return newProfile;
    }

    private static void openPlayerSearchResultScreen(PlayerProfile profile) {
        MinecraftClient.getInstance().setScreen(new PlayerSearchResultScreen(profile));
    }

    protected static int searchPlayer(String name) {
        CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS)
                .execute(() -> MinecraftClient.getInstance().execute(() -> openPlayerSearchResultScreen(addGetPlayer(name, true))));
        return 1;
    }

    public static void clearCache() {
        playerProfiles.clear();
        playerTexts.clear();
        PlayerProfileQueue.clearQueue();
        try {
            FileUtils.deleteDirectory(new File(FabricLoader.getInstance().getConfigDir() + "/drktiers-cache"));
        } catch (IOException e) {
            LOGGER.warn("Error deleting cache folder: {}", e.getMessage());
        }
    }

    public static void toggleSeparatorAdaptive() {
        isSeparatorAdaptive = !isSeparatorAdaptive;
        updateAllTags();
        ConfigManager.saveConfig();
    }

    public static void toggleShowIcons() {
        showIcons = !showIcons;
        updateAllTags();
        ConfigManager.saveConfig();
    }

    public static Text cycleMCTiersCOMMode() {
        activeMCTiersCOMMode = cycleEnum(activeMCTiersCOMMode, Modes.getMCTiersCOMValues());
        updateAllTags();
        ConfigManager.saveConfig();
        return activeMCTiersCOMMode.label;
    }

    public static Text cycleDrakenseTiersMode() {
        activeDrakenseTiersMode = cycleEnum(activeDrakenseTiersMode, Modes.getDrakenseTiersValues());
        updateAllTags();
        ConfigManager.saveConfig();
        return activeDrakenseTiersMode.label;
    }

    public static void cycleMCTiersCOMPosition() {
        mcTiersCOMPosition = cycleEnum(mcTiersCOMPosition, DisplayStatus.values());
        updateAllTags();
        ConfigManager.saveConfig();
    }

    public static void cycleDrakenseTiersPosition() {
        drakenseTiersPosition = cycleEnum(drakenseTiersPosition, DisplayStatus.values());
        updateAllTags();
        ConfigManager.saveConfig();
    }

    public static void cycleDisplayMode() {
        displayMode = cycleEnum(displayMode, ModesTierDisplay.values());
        updateAllTags();
        ConfigManager.saveConfig();
    }

    private static <T extends Enum<T>> T cycleEnum(T current, T[] values) {
        int currentIndex = -1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex == -1) return values[0];
        return values[(currentIndex + 1) % values.length];
    }

    public enum Modes {
        MCTIERSCOM_VANILLA(Icons.MCTIERSCOM_VANILLA, Icons.MCTIERSCOM_VANILLA_TAG, "mctierscom_vanilla", "Vanilla"),
        MCTIERSCOM_UHC(Icons.MCTIERSCOM_UHC, Icons.MCTIERSCOM_UHC_TAG, "mctierscom_uhc", "UHC"),
        MCTIERSCOM_POT(Icons.MCTIERSCOM_POT, Icons.MCTIERSCOM_POT_TAG, "mctierscom_pot", "Pot"),
        MCTIERSCOM_NETHPOT(Icons.MCTIERSCOM_NETHPOT, Icons.MCTIERSCOM_NETHPOT_TAG, "mctierscom_nethpot", "NethPot"),
        MCTIERSCOM_SMP(Icons.MCTIERSCOM_SMP, Icons.MCTIERSCOM_SMP_TAG, "mctierscom_smp", "Smp"),
        MCTIERSCOM_SWORD(Icons.MCTIERSCOM_SWORD, Icons.MCTIERSCOM_SWORD_TAG, "mctierscom_sword", "Sword"),
        MCTIERSCOM_AXE(Icons.MCTIERSCOM_AXE, Icons.MCTIERSCOM_AXE_TAG, "mctierscom_axe", "Axe"),
        MCTIERSCOM_MACE(Icons.MCTIERSCOM_MACE, Icons.MCTIERSCOM_MACE_TAG, "mctierscom_mace", "Mace"),

        DRAKENSE_VANILLA(Icons.DRAKENSE_VANILLA, Icons.DRAKENSE_VANILLA_TAG, "drakense_vanilla", "Vanilla"),
        DRAKENSE_UHC(Icons.DRAKENSE_UHC, Icons.DRAKENSE_UHC_TAG, "drakense_uhc", "UHC"),
        DRAKENSE_POT(Icons.DRAKENSE_POT, Icons.DRAKENSE_POT_TAG, "drakense_pot", "Pot"),
        DRAKENSE_NETHPOT(Icons.DRAKENSE_NETHPOT, Icons.DRAKENSE_NETHPOT_TAG, "drakense_nethpot", "NethPot"),
        DRAKENSE_SMP(Icons.DRAKENSE_SMP, Icons.DRAKENSE_SMP_TAG, "drakense_smp", "Smp"),
        DRAKENSE_SWORD(Icons.DRAKENSE_SWORD, Icons.DRAKENSE_SWORD_TAG, "drakense_sword", "Sword"),
        DRAKENSE_AXE(Icons.DRAKENSE_AXE, Icons.DRAKENSE_AXE_TAG, "drakense_axe", "Axe"),
        DRAKENSE_CRYSTAL(Icons.DRAKENSE_CRYSTAL, Icons.DRAKENSE_CRYSTAL_TAG, "drakense_crystal", "Crystal"),
        DRAKENSE_MACE(Icons.DRAKENSE_MACE, Icons.DRAKENSE_MACE_TAG, "drakense_mace", "Mace PvP");

        private final Text icon;
        private final Text iconTag;
        private final String color;
        private final String stringLabel;
        public Text label;

        Modes(Text icon, Text iconTag, String color, String label) {
            this.icon = icon;
            this.iconTag = iconTag;
            this.color = color;
            this.stringLabel = label;
            this.label = Text.literal(label).setStyle(Style.EMPTY.withColor(ColorControl.getColor(color)));
        }

        public static void updateColors() {
            for (Modes mode : values())
                mode.label = Text.literal(mode.stringLabel).setStyle(Style.EMPTY.withColor(ColorControl.getColor(mode.color)));
        }

        public Text getIcon() {
            return icon;
        }

        public Text getIconTag() {
            return iconTag;
        }

        public Text getLabel() {
            return label;
        }

        public static Modes[] getMCTiersCOMValues() {
            Modes[] modesArray = new Modes[7];
            ArrayList<Modes> modes = new ArrayList<>();
            for (Modes mode : Modes.values()) {
                if (mode.toString().contains("MCTIERSCOM"))
                    modes.add(mode);
            }
            return modes.toArray(modesArray);
        }

        public static Modes[] getDrakenseTiersValues() {
            Modes[] modesArray = new Modes[9];
            ArrayList<Modes> modes = new ArrayList<>();
            for (Modes mode : Modes.values()) {
                if (mode.toString().contains("DRAKENSE"))
                    modes.add(mode);
            }
            return modes.toArray(modesArray);
        }
    }

    public enum ModesTierDisplay {
        HIGHEST,
        SELECTED,
        ADAPTIVE_HIGHEST;

        public String getIcon() {
            if (this.toString().equalsIgnoreCase("HIGHEST"))
                return "↑";
            else if (this.toString().equalsIgnoreCase("SELECTED"))
                return "●";
            return "↓";
        }
    }

    public enum DisplayStatus {
        RIGHT,
        LEFT,
        OFF;

        public String getIcon() {
            if (this.toString().equalsIgnoreCase("RIGHT"))
                return "→";
            else if (this.toString().equalsIgnoreCase("LEFT"))
                return "←";
            return "●";
        }
    }
}
