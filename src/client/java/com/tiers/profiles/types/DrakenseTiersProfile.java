package com.tiers.profiles.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tiers.TiersClient;
import com.tiers.misc.ColorControl;
import com.tiers.profiles.GameMode;
import com.tiers.profiles.Status;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class DrakenseTiersProfile extends BaseProfile {
    public String combatRole;
    public Text displayedCombatRole;
    public Text combatRoleTooltip;

    public DrakenseTiersProfile(String uuid) {
        super(uuid, "https://tiers.pablorelojerio.online/api/profile/");
    }

    @Override
    public void parseInfo(String json) {
        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

            if (jsonObject.has("error")) {
                status = Status.NOT_EXISTING;
                return;
            }

            if (jsonObject.has("name") && jsonObject.has("region") && jsonObject.has("points")) {
                if (!jsonObject.get("region").isJsonNull())
                    region = jsonObject.get("region").getAsString();
                else region = "Unknown";
                points = jsonObject.get("points").getAsInt();
                
                if (jsonObject.has("combat_role") && !jsonObject.get("combat_role").isJsonNull()) {
                    combatRole = jsonObject.get("combat_role").getAsString();
                } else {
                    combatRole = calculateCombatRole(points);
                }
                
                if (jsonObject.has("overall") && !jsonObject.get("overall").isJsonNull()) {
                    overallPosition = jsonObject.get("overall").getAsInt();
                } else {
                    overallPosition = 0;
                }
                
                combatMaster = combatRole.equalsIgnoreCase("Leyenda") || combatRole.equalsIgnoreCase("Comandante");

                displayedRegion = getRegionText();
                regionTooltip = getRegionTooltip();
                displayedOverall = getOverallText();
                overallTooltip = getOverallTooltip();
                displayedCombatRole = getCombatRoleText();
                combatRoleTooltip = getCombatRoleTooltip();

                if (jsonObject.has("rankings") && !jsonObject.get("rankings").isJsonNull()) {
                    parseRankings(jsonObject.getAsJsonObject("rankings"));
                }

                status = Status.READY;
                originalJson = json;
            } else {
                status = Status.NOT_EXISTING;
            }
        } catch (Exception e) {
            status = Status.NOT_EXISTING;
        }
    }

    private void parseRankings(JsonObject jsonObject) {
        gameModes.clear();
        
        for (String modalityKey : jsonObject.keySet()) {
            TiersClient.Modes mode = getModalityEnum(modalityKey);
            if (mode != null) {
                GameMode gameMode = new GameMode(mode, modalityKey);
                JsonObject rankingData = jsonObject.getAsJsonObject(modalityKey);
                gameMode.parseTiers(rankingData);
                gameModes.add(gameMode);
            }
        }
        
        highest = getHighestMode();
    }

    private TiersClient.Modes getModalityEnum(String modality) {
        String mod = modality.toLowerCase();
        if (mod.equals("crystalpvp") || mod.equals("novanilla")) return TiersClient.Modes.DRAKENSE_VANILLA;
        if (mod.equals("uhc")) return TiersClient.Modes.DRAKENSE_UHC;
        if (mod.equals("pot")) return TiersClient.Modes.DRAKENSE_POT;
        if (mod.equals("nethop") || mod.equals("netherite_op") || mod.equals("nethpot")) return TiersClient.Modes.DRAKENSE_NETHPOT;
        if (mod.equals("smp")) return TiersClient.Modes.DRAKENSE_SMP;
        if (mod.equals("sword")) return TiersClient.Modes.DRAKENSE_SWORD;
        if (mod.equals("axe")) return TiersClient.Modes.DRAKENSE_AXE;
        if (mod.equals("crystal")) return TiersClient.Modes.DRAKENSE_CRYSTAL;
        if (mod.equals("macepvp") || mod.equals("mace")) return TiersClient.Modes.DRAKENSE_MACE;
        return null;
    }

    private GameMode getHighestMode() {
        GameMode highest = null;
        int highestPoints = 0;
        for (GameMode gameMode : gameModes) {
            if (gameMode.status == Status.READY && gameMode.getTierPoints(false) > highestPoints) {
                highest = gameMode;
                highestPoints = gameMode.getTierPoints(false);
            }
        }
        return highest;
    }

    private String calculateCombatRole(int totalPoints) {
        if (totalPoints >= 430) return "Leyenda";
        if (totalPoints >= 360) return "Comandante";
        if (totalPoints >= 300) return "Elite";
        if (totalPoints >= 220) return "Veterano";
        if (totalPoints >= 140) return "Especialista";
        if (totalPoints >= 60) return "Combatiente";
        return "Recluta";
    }

    private Text getCombatRoleText() {
        int color = getCombatRoleColor(combatRole);
        return Text.literal(combatRole).setStyle(Style.EMPTY.withColor(color));
    }

    private Text getCombatRoleTooltip() {
        String tooltip = "Combat Role: " + combatRole + "\n\nPoints: " + points;
        return Text.literal(tooltip).setStyle(Style.EMPTY.withColor(getCombatRoleColor(combatRole)));
    }

    private int getCombatRoleColor(String role) {
        String r = role.toLowerCase();
        if (r.equals("leyenda")) return ColorControl.getColor("ht1");
        if (r.equals("comandante")) return ColorControl.getColor("lt1");
        if (r.equals("elite")) return ColorControl.getColor("ht2");
        if (r.equals("veterano")) return ColorControl.getColor("lt2");
        if (r.equals("especialista")) return ColorControl.getColor("ht3");
        if (r.equals("combatiente")) return ColorControl.getColor("lt3");
        return ColorControl.getColor("lt4");
    }

    private Text getRegionText() {
        if (region.equalsIgnoreCase("EU")) return Text.literal(region).setStyle(Style.EMPTY.withColor(ColorControl.getColor("eu")));
        else if (region.equalsIgnoreCase("NA")) return Text.literal(region).setStyle(Style.EMPTY.withColor(ColorControl.getColor("na")));
        else if (region.equalsIgnoreCase("AS")) return Text.literal(region).setStyle(Style.EMPTY.withColor(ColorControl.getColor("as")));
        else if (region.equalsIgnoreCase("AU")) return Text.literal(region).setStyle(Style.EMPTY.withColor(ColorControl.getColor("au")));
        else if (region.equalsIgnoreCase("SA")) return Text.literal(region).setStyle(Style.EMPTY.withColor(ColorControl.getColor("sa")));
        else if (region.equalsIgnoreCase("ME")) return Text.literal(region).setStyle(Style.EMPTY.withColor(ColorControl.getColor("me")));
        else if (region.equalsIgnoreCase("LATAM")) return Text.literal(region).setStyle(Style.EMPTY.withColor(ColorControl.getColor("sa")));
        return Text.literal("Unknown").setStyle(Style.EMPTY.withColor(ColorControl.getColor("unknown")));
    }

    private Text getRegionTooltip() {
        if (region.equalsIgnoreCase("EU")) return Text.literal("Europe").setStyle(Style.EMPTY.withColor(ColorControl.getColor("eu")));
        else if (region.equalsIgnoreCase("NA")) return Text.literal("North America").setStyle(Style.EMPTY.withColor(ColorControl.getColor("na")));
        else if (region.equalsIgnoreCase("AS")) return Text.literal("Asia").setStyle(Style.EMPTY.withColor(ColorControl.getColor("as")));
        else if (region.equalsIgnoreCase("AU")) return Text.literal("Australia").setStyle(Style.EMPTY.withColor(ColorControl.getColor("au")));
        else if (region.equalsIgnoreCase("SA")) return Text.literal("South America").setStyle(Style.EMPTY.withColor(ColorControl.getColor("sa")));
        else if (region.equalsIgnoreCase("ME")) return Text.literal("Middle East").setStyle(Style.EMPTY.withColor(ColorControl.getColor("me")));
        else if (region.equalsIgnoreCase("LATAM")) return Text.literal("Latin America").setStyle(Style.EMPTY.withColor(ColorControl.getColor("sa")));
        return Text.literal("Unknown").setStyle(Style.EMPTY.withColor(ColorControl.getColor("unknown")));
    }

    private Text getOverallText() {
        if (overallPosition == 0) return Text.literal("-").setStyle(Style.EMPTY.withColor(ColorControl.getColor("unknown")));
        if (combatMaster) return Text.literal("#" + overallPosition).setStyle(Style.EMPTY.withColor(ColorControl.getColor("master")));
        else if (points >= 100) return Text.literal("#" + overallPosition).setStyle(Style.EMPTY.withColor(ColorControl.getColor("ace")));
        else if (points >= 50) return Text.literal("#" + overallPosition).setStyle(Style.EMPTY.withColor(ColorControl.getColor("specialist")));
        else if (points >= 20) return Text.literal("#" + overallPosition).setStyle(Style.EMPTY.withColor(ColorControl.getColor("cadet")));
        else if (points >= 10) return Text.literal("#" + overallPosition).setStyle(Style.EMPTY.withColor(ColorControl.getColor("novice")));
        return Text.literal("#" + overallPosition).setStyle(Style.EMPTY.withColor(ColorControl.getColor("rookie")));
    }

    private Text getOverallTooltip() {
        String overallTooltip = "Combat Role: " + combatRole + "\n\nPoints: " + points;
        return Text.literal(overallTooltip).setStyle(displayedOverall.getStyle());
    }
}
