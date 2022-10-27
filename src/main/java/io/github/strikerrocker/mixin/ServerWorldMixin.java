package io.github.strikerrocker.mixin;


import io.github.strikerrocker.StitchedSnow;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.Heightmap;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;


@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {
    protected ServerWorldMixin(MutableWorldProperties properties,
                               RegistryKey<World> registryRef,
                               RegistryEntry<DimensionType> dimension,
                               Supplier<Profiler> profiler, boolean isClient, boolean debugWorld,
                               long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, dimension, profiler, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }
    
    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE_STRING",
                    target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
                    args = "ldc=iceandsnow",
                    shift = At.Shift.AFTER
            )
    )
    public void tickChunk(WorldChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (!this.isRaining())
            return; // Don't do snow calculation if we're not raining
        
        for (int i = 0; i < StitchedSnow.config.accumulationsPerChunkPerTick; i++) {
            accumulateSnowLayers(chunk);
        }
    }
    
    @Redirect(method = "tickChunk",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;" +
                                "Lnet/minecraft/block/BlockState;)Z",
                       ordinal = 1))
    public boolean setBlockState(ServerWorld instance, BlockPos blockPos, BlockState blockState) {
        // Don't perform setBlockState for snow
        return false;
    }
    
    private void accumulateSnowLayers(WorldChunk chunk) {
        // If it can rain here, there is a 1/16 chance of trying to add snow
        if (random.nextInt(StitchedSnow.config.chanceToAccumulateSnow) == 0) {
//            System.out.println(StitchedSnow.config.chanceToAccumulateSnow);
            ChunkPos chunkPos = chunk.getPos();
            int chunkX = chunkPos.getStartX();
            int chunkZ = chunkPos.getStartZ();
            
            // Get rain height at random position in chunk, splits the random val j2 to use for both parts of position
            BlockPos pos = getTopPosition(Heightmap.Type.MOTION_BLOCKING,
                                          getRandomPosInChunk(chunkX, 0, chunkZ, 15));
            
            Biome biome = this.getBiome(pos).value();
            
            if (!biome.canSetSnow(this, pos)) { // Return if we can't set snow
                return;
            }
            
            // Calculate mean surrounding block height
            BlockState state = getBlockState(pos);
            int height;
            
            if (state.isOf(Blocks.SNOW)) {
                height = state.get(SnowBlock.LAYERS);
            } else if (state.isOf(Blocks.AIR)) {
                height = 0;
            } else {
                return;
            }
            
            int pHeight = height + 8 * (getBlockState(pos.down()).isOf(Blocks.SNOW_BLOCK) ? 1 : 0);
            
            if (height == StitchedSnow.config.snowAccumulationLimit)
                return;
            
            if (height == SnowBlock.MAX_LAYERS) {
                setBlockState(pos, Blocks.SNOW_BLOCK.getDefaultState());
                pos = pos.up();
                state = getBlockState(pos);
                height = 0;
            }
            
            int surroundingsSnowLevel = 0;
            
            // Check for blocks on the side (in star shape)
            surroundingsSnowLevel += computeSnowLevel(pos.north());
            surroundingsSnowLevel += computeSnowLevel(pos.north().east());
            surroundingsSnowLevel += computeSnowLevel(pos.north().west());
            surroundingsSnowLevel += computeSnowLevel(pos.south());
            surroundingsSnowLevel += computeSnowLevel(pos.south().east());
            surroundingsSnowLevel += computeSnowLevel(pos.south().west());
            surroundingsSnowLevel += computeSnowLevel(pos.east());
            surroundingsSnowLevel += computeSnowLevel(pos.west());
            
            // finely tuned formula for weight of surroundings
            float surroundings = ((float) surroundingsSnowLevel - 2) / 8 - (pHeight) * 0.045f;
            
            // Done calculating surroundings
            
            // finely tuned weight formula
            float weight = ((surroundings - height)) + 0.1f / (pHeight * pHeight * pHeight);
            
            if (weight >= random.nextFloat()) {
                // Add Snow layer!
                setBlockState(pos, Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, Math.min(height + 1, SnowBlock.MAX_LAYERS)));
            }
        }
    }
    
    private int computeSnowLevel(BlockPos pos) {
        BlockState state = getBlockState(pos);
        if (state.isOf(Blocks.SNOW)) {
            return state.get(SnowBlock.LAYERS);
        } else if (state.isOf(Blocks.SNOW_BLOCK)) {
            return 10;
        } else if (state.isSolidBlock(this, pos)) {
            return SnowBlock.MAX_LAYERS;
        }
        return 0;
    }
}
