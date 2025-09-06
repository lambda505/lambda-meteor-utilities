package com.lambda505.meteorutils;

import com.lambda505.meteorutils.hud.OnlineFriendsHUD;
import com.lambda505.meteorutils.hud.PlayersInRangeHUD;
import com.lambda505.meteorutils.modules.ChatCoordLogger;
import com.lambda505.meteorutils.modules.PrivateMessageArchiver;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LambdaUtilities extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Lambda Utilities");
    public static final HudGroup HUD_GROUP = new HudGroup("Lambda Utilities");

    // Create a custom category with an icon (using a compass for utilities/navigation theme)
    public static final Category CATEGORY = new Category("Lambda Utilities", Items.MUSIC_DISC_MALL.getDefaultStack());

    @Override
    public void onInitialize() {
        LOG.info("Initializing Lambda Utilities");

        // Register modules
        Modules.get().add(new ChatCoordLogger());
        Modules.get().add(new PrivateMessageArchiver());

        // Register the HUD elements
        Hud.get().register(OnlineFriendsHUD.INFO);
        Hud.get().register(PlayersInRangeHUD.INFO);

        LOG.info("Lambda Utilities initialized successfully");
    }

    @Override
    public void onRegisterCategories() {
        // Register the custom category
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.lambda505.meteorutils";
    }
}
