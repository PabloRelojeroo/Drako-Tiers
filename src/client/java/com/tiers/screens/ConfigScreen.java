package com.tiers.screens;

import com.tiers.TiersClient;
import com.tiers.misc.ColorControl;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ConfigScreen extends Screen {

    private final Identifier MCTIERS_COM_IMAGE =
            new Identifier("minecraft", "textures/mctiers_com_logo.png");
    private final Identifier DRAKENSE_TIERS_IMAGE =
            new Identifier("minecraft", "textures/drakense_tiers_logo.png");

    private ButtonWidget toggleModWidget;
    private ButtonWidget toggleShowIcons;
    private ButtonWidget toggleSeparatorMode;
    private ButtonWidget cycleDisplayMode;
    private ButtonWidget mcTiersCOMPosition;
    private ButtonWidget drakenseTiersPosition;
    private ButtonWidget clearPlayerCache;

    private int centerX;
    private int listY;
    private int separator;
    private int firstListX;
    private int secondListX;
    private int distance;

    private static int updateButtons;

    public ConfigScreen() {
        super(Text.literal("DRKTiers Config"));
    }

    @Override
    protected void init() {
        centerX = width / 2;
        listY = (int) (height / 2.8);
        separator = height / 23;
        firstListX = (int) (centerX - width / 6);
        secondListX = (int) (centerX + width / 6);
        distance = (int) (height / 7.5);

        toggleModWidget = ButtonWidget.builder(
                Text.literal(TiersClient.toggleMod ? "✔" : " ")
                        .setStyle(Style.EMPTY.withColor(ColorControl.getColor("green"))),
                button -> {
                    TiersClient.toggleMod();
                    button.setMessage(Text.literal(TiersClient.toggleMod ? "✔" : " ")
                            .setStyle(Style.EMPTY.withColor(ColorControl.getColor("green"))));
                }
        ).dimensions(width / 2 - 51, distance - 4, 16, 16).build();
        toggleModWidget.setTooltip(Tooltip.of(Text.of("✔ - Mod is enabled")));

        toggleShowIcons = ButtonWidget.builder(
                Text.literal(TiersClient.showIcons ? "✔" : " ")
                        .setStyle(Style.EMPTY.withColor(ColorControl.getColor("green"))),
                button -> {
                    TiersClient.toggleShowIcons();
                    button.setMessage(Text.literal(TiersClient.showIcons ? "✔" : " ")
                            .setStyle(Style.EMPTY.withColor(ColorControl.getColor("green"))));
                }
        ).dimensions(width / 2 - 40, distance + separator - 4, 16, 16).build();
        toggleShowIcons.setTooltip(Tooltip.of(Text.of("✔ - Icons will be showed next to tier")));

        toggleSeparatorMode = ButtonWidget.builder(
                Text.literal(TiersClient.isSeparatorAdaptive ? "✔" : " ")
                        .setStyle(Style.EMPTY.withColor(ColorControl.getColor("green"))),
                button -> {
                    TiersClient.toggleSeparatorAdaptive();
                    button.setMessage(Text.literal(TiersClient.isSeparatorAdaptive ? "✔" : " ")
                            .setStyle(Style.EMPTY.withColor(ColorControl.getColor("green"))));
                }
        ).dimensions(width / 2 - 69, distance + 2 * separator - 4, 16, 16).build();
        toggleSeparatorMode.setTooltip(
                Tooltip.of(Text.of("✔ - The separator will be the same color as the tier instead of gray"))
        );

        cycleDisplayMode = ButtonWidget.builder(
                Text.of(TiersClient.displayMode.getIcon()),
                button -> {
                    TiersClient.cycleDisplayMode();
                    button.setMessage(Text.of(TiersClient.displayMode.getIcon()));
                }
        ).dimensions(width / 2 - 46, distance + 3 * separator - 4, 16, 16).build();
        cycleDisplayMode.setTooltip(Tooltip.of(Text.of(
                "● - Selected tier: only the selected tier will be displayed\n" +
                "↑ - Highest: only the highest tier will be displayed\n" +
                "↓ - Adaptive Highest: the highest tier will be displayed if selected does not exist"
        )));

        mcTiersCOMPosition = ButtonWidget.builder(
                Text.of(TiersClient.mcTiersCOMPosition.getIcon()),
                button -> {
                    TiersClient.cycleMCTiersCOMPosition();
                    updateButtons = 0;
                    button.setMessage(Text.of(TiersClient.mcTiersCOMPosition.getIcon()));
                }
        ).dimensions(firstListX - 28, listY + 2 * separator - 4, 16, 16).build();

        drakenseTiersPosition = ButtonWidget.builder(
                Text.of(TiersClient.drakenseTiersPosition.getIcon()),
                button -> {
                    TiersClient.cycleDrakenseTiersPosition();
                    updateButtons = 1;
                    button.setMessage(Text.of(TiersClient.drakenseTiersPosition.getIcon()));
                }
        ).dimensions(secondListX - 28, listY + 2 * separator - 4, 16, 16).build();

        mcTiersCOMPosition.setTooltip(Tooltip.of(Text.of(
                "→ - The tier will be displayed on the right of the nametag\n" +
                "← - The tier will be displayed on the left of the nametag\n" +
                "● - Off"
        )));
        drakenseTiersPosition.setTooltip(Tooltip.of(Text.of(
                "→ - The tier will be displayed on the right of the nametag\n" +
                "← - The tier will be displayed on the left of the nametag\n" +
                "● - Off"
        )));

        clearPlayerCache = ButtonWidget.builder(
                Text.of("\uD83D\uDDD1"),
                button -> TiersClient.clearCache()
        ).dimensions(width - 20, height - 20, 16, 16).build();
        clearPlayerCache.setTooltip(Tooltip.of(Text.of("Clear player cache")));

        addDrawableChild(toggleModWidget);
        addDrawableChild(toggleShowIcons);
        addDrawableChild(toggleSeparatorMode);
        addDrawableChild(cycleDisplayMode);
        addDrawableChild(mcTiersCOMPosition);
        addDrawableChild(drakenseTiersPosition);
        addDrawableChild(clearPlayerCache);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("DRKTiers config"),
                width / 2,
                10,
                ColorControl.getColor("text")
        );

        drawCategoryList(context, MCTIERS_COM_IMAGE, firstListX, listY);
        drawCategoryList(context, DRAKENSE_TIERS_IMAGE, secondListX, listY);

        context.drawTextWithShadow(this.textRenderer,
                Text.of("Enable DRKTiers"), centerX - 30, distance,
                ColorControl.getColor("text"));
        context.drawTextWithShadow(this.textRenderer,
                Text.of("Enable Icons"), centerX - 19, distance + separator,
                ColorControl.getColor("text"));
        context.drawTextWithShadow(this.textRenderer,
                Text.of("Dynamic Separator Color"), centerX - 48, distance + 2 * separator,
                ColorControl.getColor("text"));
        context.drawTextWithShadow(this.textRenderer,
                Text.of("Displayed Tier"), centerX - 25, distance + 3 * separator,
                ColorControl.getColor("text"));

        context.drawTextWithShadow(this.textRenderer,
                Text.of("Position"), firstListX - 7, listY + 2 * separator,
                ColorControl.getColor("text"));
        context.drawTextWithShadow(this.textRenderer,
                Text.of("Position"), secondListX - 7, listY + 2 * separator,
                ColorControl.getColor("text"));

        checkUpdates();
    }

    private void drawCategoryList(DrawContext context, Identifier image, int x, int y) {
        if (image == MCTIERS_COM_IMAGE) {
            context.drawTexture(image, x - 56, y + 5, 0, 0, 112, 21, 112, 21);
        } else {
            context.drawTexture(image, x - 13, y, 0, 0, 26, 26, 26, 26);
        }
    }

    private void checkUpdates() {
        toggleModWidget.setPosition(width / 2 - 51, distance - 4);
        toggleShowIcons.setPosition(width / 2 - 40, distance + separator - 4);
        toggleSeparatorMode.setPosition(width / 2 - 69, distance + 2 * separator - 4);
        cycleDisplayMode.setPosition(width / 2 - 46, distance + 3 * separator - 4);

        mcTiersCOMPosition.setPosition(firstListX - 28, listY + 2 * separator - 4);
        drakenseTiersPosition.setPosition(secondListX - 28, listY + 2 * separator - 4);

        if (updateButtons == 0) {
            if (TiersClient.drakenseTiersPosition == TiersClient.mcTiersCOMPosition
                    && TiersClient.mcTiersCOMPosition != TiersClient.DisplayStatus.OFF) {
                TiersClient.drakenseTiersPosition = TiersClient.DisplayStatus.OFF;
                drakenseTiersPosition.setMessage(
                        Text.of(TiersClient.drakenseTiersPosition.getIcon())
                );
            }
            updateButtons = -1;
        } else if (updateButtons == 1) {
            if (TiersClient.mcTiersCOMPosition == TiersClient.drakenseTiersPosition
                    && TiersClient.drakenseTiersPosition != TiersClient.DisplayStatus.OFF) {
                TiersClient.mcTiersCOMPosition = TiersClient.DisplayStatus.OFF;
                mcTiersCOMPosition.setMessage(
                        Text.of(TiersClient.mcTiersCOMPosition.getIcon())
                );
            }
            updateButtons = -1;
        }
    }

    public static Screen getConfigScreen(Screen ignoredScreen) {
        return new ConfigScreen();
    }
}
