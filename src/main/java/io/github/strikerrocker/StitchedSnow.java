package io.github.strikerrocker;


import io.github.lucaargolo.seasons.FabricSeasons;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;


public class StitchedSnow implements ModInitializer {
    public static final ConfigModInfo config;
    private static boolean IS_FABRIC_SEASONS_LOADED = false;

    static {
        AutoConfig.register(ConfigModInfo.class, Toml4jConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ConfigModInfo.class).getConfig();
    }

    public static void setFabricSeasonsMeltable(BlockPos pos) {
        if (IS_FABRIC_SEASONS_LOADED)
            FabricSeasons.setMeltable(pos);
    }

    @Override
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    public void onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("seasons"))
            StitchedSnow.IS_FABRIC_SEASONS_LOADED = true;
    }
}
