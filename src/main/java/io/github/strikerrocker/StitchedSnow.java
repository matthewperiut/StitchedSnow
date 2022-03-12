package io.github.strikerrocker;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class StitchedSnow implements ModInitializer {

    public static ConfigModInfo config;

    static {
        AutoConfig.register(ConfigModInfo.class, Toml4jConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ConfigModInfo.class).getConfig();
    }

    public TickHandler tickHandler;

    @Override
    public void onInitialize() {
        tickHandler = new TickHandler();
        ServerTickEvents.START_WORLD_TICK.register(tickHandler);
    }
}