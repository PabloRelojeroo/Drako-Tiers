package com.tiers.profiles;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tiers.profiles.types.MCTiersCOMProfile;
import com.tiers.TiersClient;
import com.tiers.profiles.types.DrakenseTiersProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PlayerProfile {
    public static final Logger LOGGER = LoggerFactory.getLogger(PlayerProfile.class);
    public Status status = Status.SEARCHING;

    public String name;
    public String uuid;
    public boolean isPremium = true;

    public MCTiersCOMProfile mcTiersCOMProfile;
    public DrakenseTiersProfile drakenseTiersProfile;

    public Text originalNameText;
    public boolean imageSaved = false;
    private int numberOfRequests = 0;
    public int numberOfImageRequests = 0;


    public PlayerProfile(String name) {
        this.name = name;
        originalNameText = Text.of(name);
    }

    public void buildRequest(String name) {
        if (numberOfRequests == 12 || status != Status.SEARCHING) {
            status = Status.TIMEOUTED;
            return;
        }

        numberOfRequests++;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
                    .header("User-Agent", "DRKTiers")
                    .GET()
                    .build();

            HttpClient.newHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        int statusCode = response.statusCode();

                        if (statusCode != 200 && statusCode != 404) {
                            long delay = getRetryDelay(numberOfRequests);
                            CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                                    .execute(() -> buildRequest(name));
                            return;
                        }
                        if (statusCode == 404) {
                            this.name = name;
                            this.uuid = generateOfflineUUID(name);
                            this.isPremium = false;
                            savePlayerImage();
                            fetchProfiles();
                            return;
                        }
                        parseUUID(response.body());
                    })
                    .exceptionally(exception -> {
                        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS)
                                .execute(() -> buildRequest(name));
                        return null;
                    });
        } catch (IllegalArgumentException ignored) {
            status = Status.NOT_EXISTING;
        }
    }

    private long getRetryDelay(int attempts) {
        if (attempts == 1) return 50;
        if (attempts == 2) return 100;
        if (attempts == 3) return 200;
        if (attempts == 4) return 400;
        if (attempts == 5) return 700;
        if (attempts == 6) return 1100;
        return 1500;
    }


    private String generateOfflineUUID(String username) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
            
            hash[6] = (byte) ((hash[6] & 0x0f) | 0x30);
            hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            LOGGER.info(username + " is not premium. Generated offline UUID: " + sb.toString());
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(username.hashCode());
        }
    }

    private void parseUUID(String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        if (jsonObject.has("name") && jsonObject.has("id")) {
            name = jsonObject.get("name").getAsString();
            uuid = jsonObject.get("id").getAsString();
            isPremium = true;
        } else {
            status = Status.NOT_EXISTING;
            return;
        }

        savePlayerImage();
        fetchProfiles();
    }

    private void savePlayerImage() {
        if (numberOfImageRequests == 5)
            return;
        numberOfImageRequests++;
        
        String imageUrl;
        if (isPremium) {
            imageUrl = "https://mc-heads.net/body/" + uuid;
        } else {
            imageUrl = "https://mc-heads.net/body/" + name;
        }
        
        String savePath = FabricLoader.getInstance().getConfigDir() + "/drktiers-cache/" + uuid + ".png";
        
        try {
            Files.createDirectories(Paths.get(FabricLoader.getInstance().getConfigDir() + "/drktiers-cache/"));
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .header("User-Agent", "DRKTiers")
                    .GET()
                    .build();
            
            HttpClient.newHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(response.body());
                                ImageIO.write(ImageIO.read(bis), "png", new File(savePath));
                                imageSaved = true;
                            } catch (IOException e) {
                                retryImageDownload();
                            }
                        } else {
                            retryImageDownload();
                        }
                    })
                    .exceptionally(e -> {
                        retryImageDownload();
                        return null;
                    });
        } catch (IOException ignored) {
            retryImageDownload();
        }
    }
    
    private void retryImageDownload() {
        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS)
                .execute(this::savePlayerImage);
    }

    private void fetchProfiles() {
        String identifier = (uuid != null && !uuid.isEmpty()) ? uuid : name;
        mcTiersCOMProfile = new MCTiersCOMProfile(identifier);
        drakenseTiersProfile = new DrakenseTiersProfile(identifier);

        status = Status.READY;
    }

    public void resetDrawnStatus() {
        if (mcTiersCOMProfile != null) {
            mcTiersCOMProfile.drawn = false;
            for (GameMode mode : mcTiersCOMProfile.gameModes)
                mode.drawn = false;
        }
        if (drakenseTiersProfile != null) {
            drakenseTiersProfile.drawn = false;
            for (GameMode mode : drakenseTiersProfile.gameModes)
                mode.drawn = false;
        }
    }
}
