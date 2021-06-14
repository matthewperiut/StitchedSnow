package io.github.strikerrocker;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;

import java.lang.reflect.Method;

public class StitchedSnow implements ModInitializer {

    public static ConfigModInfo config;

    static {
        AutoConfig.register(ConfigModInfo.class, Toml4jConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ConfigModInfo.class).getConfig();
    }

    public TickHandler tickHandler;

    @Override
    public void onInitialize() {
        Method getChunkHolder;
        try {
            getChunkHolder = (ThreadedAnvilChunkStorage.class).getDeclaredMethod("f");
            tickHandler = new TickHandler(getChunkHolder);
            ServerTickEvents.START_WORLD_TICK.register(tickHandler);
        } catch (NoSuchMethodException ex1) {
            try {
                getChunkHolder = (ThreadedAnvilChunkStorage.class).getDeclaredMethod("method_17264");
                tickHandler = new TickHandler(getChunkHolder);
                ServerTickEvents.START_WORLD_TICK.register(tickHandler);
            } catch (NoSuchMethodException ex2) {
                try {
                    getChunkHolder = (ThreadedAnvilChunkStorage.class).getDeclaredMethod("entryIterator");
                    tickHandler = new TickHandler(getChunkHolder);
                    ServerTickEvents.START_WORLD_TICK.register(tickHandler);
                } catch (NoSuchMethodException ex3) {
                    System.err.println("Stitched Snow failed to apply hooks! Stitched Snow is not loaded");
                }
            }
        }
    }
}