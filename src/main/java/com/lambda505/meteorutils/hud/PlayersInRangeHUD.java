package com.lambda505.meteorutils.hud;

import com.lambda505.meteorutils.LambdaUtilities;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayersInRangeHUD extends HudElement {
    public static final HudElementInfo<PlayersInRangeHUD> INFO = new HudElementInfo<>(
        LambdaUtilities.HUD_GROUP,
        "players-in-range",
        "Displays players in render distance.",
        PlayersInRangeHUD::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background behind the player list.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color of the background.")
        .defaultValue(new SettingColor(0, 0, 0, 64))
        .build()
    );

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Maximum distance to show players.")
        .defaultValue(64.0)
        .min(1.0)
        .max(256.0)
        .sliderMax(128.0)
        .build()
    );

    private final Setting<Boolean> showDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("show-distance")
        .description("Shows distance next to player names.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> titleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("title-color")
        .description("Color of the title text.")
        .defaultValue(new SettingColor(255, 255, 255)) // White
        .build()
    );

    private final Setting<SettingColor> friendColor = sgGeneral.add(new ColorSetting.Builder()
        .name("friend-color")
        .description("Color of friend names.")
        .defaultValue(new SettingColor(0, 255, 0)) // Green
        .build()
    );

    private final Setting<SettingColor> playerColor = sgGeneral.add(new ColorSetting.Builder()
        .name("player-color")
        .description("Color of regular player names.")
        .defaultValue(new SettingColor(255, 255, 255)) // White
        .build()
    );

    public PlayersInRangeHUD() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (mc.world == null || mc.player == null) {
            renderOffline(renderer);
            return;
        }

        List<PlayerInfo> playersInRange = getPlayersInRange();
        renderPlayersList(renderer, playersInRange);
    }

    private void renderOffline(HudRenderer renderer) {
        String title = "Players in Range";
        double width = renderer.textWidth(title, true);
        double height = renderer.textHeight(true);

        setSize(width, height);

        if (background.get()) {
            renderer.quad(x, y, width, height, backgroundColor.get());
        }

        renderer.text(title, x, y, titleColor.get(), true);
    }

    private void renderPlayersList(HudRenderer renderer, List<PlayerInfo> playersInRange) {
        String title = "Players in Range (" + playersInRange.size() + ")";
        double titleWidth = renderer.textWidth(title, true);
        double lineHeight = renderer.textHeight(true);

        // Calculate dimensions
        double maxWidth = titleWidth;
        for (PlayerInfo playerInfo : playersInRange) {
            String displayText = getDisplayText(playerInfo);
            double playerWidth = renderer.textWidth(displayText, false);
            if (playerWidth > maxWidth) maxWidth = playerWidth;
        }

        double totalHeight = lineHeight;
        if (!playersInRange.isEmpty()) {
            totalHeight += playersInRange.size() * lineHeight;
        }

        setSize(maxWidth, totalHeight);

        if (background.get()) {
            renderer.quad(x, y, maxWidth, totalHeight, backgroundColor.get());
        }

        // Render title in white
        renderer.text(title, x, y, titleColor.get(), true);

        // Render player names
        double currentY = y + lineHeight;
        for (PlayerInfo playerInfo : playersInRange) {
            String displayText = getDisplayText(playerInfo);
            SettingColor color = playerInfo.isFriend ? friendColor.get() : playerColor.get();
            renderer.text(displayText, x, currentY, color, false);
            currentY += lineHeight;
        }
    }

    private String getDisplayText(PlayerInfo playerInfo) {
        if (showDistance.get()) {
            return String.format("%s (%.1fm)", playerInfo.name, playerInfo.distance);
        }
        return playerInfo.name;
    }

    private List<PlayerInfo> getPlayersInRange() {
        List<PlayerInfo> playersInRange = new ArrayList<>();

        if (mc.world == null || mc.player == null) return playersInRange;

        String ourPlayerName = mc.player.getName().getString();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue; // Skip ourselves

            double distance = mc.player.distanceTo(player);
            if (distance <= maxDistance.get()) {
                String playerName = player.getName().getString();
                boolean isFriend = Friends.get().isFriend(player);

                playersInRange.add(new PlayerInfo(playerName, distance, isFriend));
            }
        }

        // Sort by distance (closest first)
        playersInRange.sort((a, b) -> Double.compare(a.distance, b.distance));

        return playersInRange;
    }

    private static class PlayerInfo {
        public final String name;
        public final double distance;
        public final boolean isFriend;

        public PlayerInfo(String name, double distance, boolean isFriend) {
            this.name = name;
            this.distance = distance;
            this.isFriend = isFriend;
        }
    }
}
