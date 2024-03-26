package io.github.strikerrocker;


import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;


public class StitchedSnow implements ModInitializer {
    public static final ConfigModInfo config;

    static {
        AutoConfig.register(ConfigModInfo.class, Toml4jConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ConfigModInfo.class).getConfig();
    }

    @Override
    public void onInitialize() {
    }
}
