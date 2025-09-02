package com.lambda505.meteorutils;

import com.lambda505.meteorutils.hud.OnlineFriendsHUD;
import com.lambda505.meteorutils.hud.PlayersInRangeHUD;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LambdaUtilities extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Lambda Utilities");
    public static final HudGroup HUD_GROUP = new HudGroup("Lambda Utilities");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Lambda Utilities");

        // Register the HUD elements
        Hud.get().register(OnlineFriendsHUD.INFO);
        Hud.get().register(PlayersInRangeHUD.INFO);

        LOG.info("Lambda Utilities initialized successfully");
    }

    @Override
    public void onRegisterCategories() {
        // No custom categories needed for this addon
    }

    @Override
    public String getPackage() {
        return "com.lambda505.meteorutils";
    }
}
