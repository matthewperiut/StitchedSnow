package io.github.strikerrocker.mixin;


import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(Biome.class)
public abstract class BiomeMixin {
    @Shadow
    public abstract boolean doesNotSnow(BlockPos pos);

    /**
     * @author solonovamax
     * @reason Overwritten to support multiple snow layers
     */
    @Overwrite
    public boolean canSetSnow(WorldView world, BlockPos pos) {
        // I don't think we changed this at all? Why is this method overwritten?
        if (!this.doesNotSnow(pos)) {
            if (pos.getY() >= world.getBottomY() && pos.getY() < world.getTopY() && world.getLightLevel(LightType.BLOCK, pos) < 10) {
                BlockState blockState = world.getBlockState(pos);
                return (blockState.isAir() || blockState.isOf(Blocks.SNOW)) && Blocks.SNOW.getDefaultState().canPlaceAt(world, pos);
            }
        }

        return false;
    }
}
