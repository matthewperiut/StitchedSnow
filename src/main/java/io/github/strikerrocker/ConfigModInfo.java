package io.github.strikerrocker;


import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;


@Config(name = "stitchedsnow")
public class ConfigModInfo implements ConfigData {
    public int snowAccumulationLimit = 8;

    public int chanceToAccumulateSnow = 16;

    public int accumulationsPerChunkPerTick = 1;
}
