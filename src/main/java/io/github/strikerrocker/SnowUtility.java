package io.github.strikerrocker;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SnowUtility {
    public static int computeSnowLevel(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isOf(Blocks.SNOW))
            return state.get(SnowBlock.LAYERS);
        else if (state.isOf(Blocks.SNOW_BLOCK))
            return 10;
        else if (state.isSolidBlock(world, pos))
            return SnowBlock.MAX_LAYERS;
        else
            return 0;
    }
}
