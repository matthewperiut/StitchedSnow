package io.github.strikerrocker.mixin;


import io.github.strikerrocker.StitchedSnow;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {
    protected ServerWorldMixin(
            MutableWorldProperties properties,
            RegistryKey<World> registryRef,
            DynamicRegistryManager registryManager,
            RegistryEntry<DimensionType> dimensionEntry,
            boolean isClient,
            boolean debugWorld,
            long seed,
            int maxChainedNeighborUpdates
    ) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
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
    private void tickChunk(WorldChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (!this.isRaining())
            return; // Don't do snow calculation if we're not raining

        for (int i = 0; i < StitchedSnow.config.accumulationsPerChunkPerTick; i++) {
            accumulateSnowLayers(chunk);
        }
    }

    @Redirect(
            method = "tickIceAndSnow",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/Biome;canSetSnow(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)Z",
                    ordinal = 0
            )
    )
    private boolean canSetSnow(Biome instance, WorldView worldview, BlockPos blockPos) {
        // Cancel vanilla snow algorithm all together, but still allow the precipitation tick to happen
        return false;
    }

    @Unique
    private void accumulateSnowLayers(WorldChunk chunk) {
        // If it can rain here, there is a 1/16 chance of trying to add snow
        if (this.random.nextInt(StitchedSnow.config.chanceToAccumulateSnow) == 0) {
            //            System.out.println(StitchedSnow.config.chanceToAccumulateSnow);
            ChunkPos chunkPos = chunk.getPos();
            int chunkX = chunkPos.getStartX();
            int chunkZ = chunkPos.getStartZ();

            // Get rain height at random position in chunk, splits the random val j2 to use for both parts of position
            BlockPos pos = getTopPosition(Heightmap.Type.MOTION_BLOCKING, getRandomPosInChunk(chunkX, 0, chunkZ, 15));

            Biome biome = this.getBiome(pos).value();

            if (!biome.canSetSnow(this, pos))
                return; // Return if we can't set snow

            // Calculate mean surrounding block height
            BlockState state = getBlockState(pos);
            int height;

            if (state.isOf(Blocks.SNOW))
                height = state.get(SnowBlock.LAYERS);
            else if (state.isOf(Blocks.AIR))
                height = 0;
            else
                return;

            int pHeight = height + 8 * (getBlockState(pos.down()).isOf(Blocks.SNOW_BLOCK) ? 1 : 0);

            if (height == StitchedSnow.config.snowAccumulationLimit)
                return;

            if (height == SnowBlock.MAX_LAYERS) {
                setBlockState(pos, Blocks.SNOW_BLOCK.getDefaultState());
                pos = pos.up();
                state = getBlockState(pos);
                height = 0;
            }

            int localSnowLevel = 0;

            // Check for blocks on the side (in star shape)
            localSnowLevel += computeSnowLevel(pos.north());
            localSnowLevel += computeSnowLevel(pos.north().east());
            localSnowLevel += computeSnowLevel(pos.north().west());
            localSnowLevel += computeSnowLevel(pos.south());
            localSnowLevel += computeSnowLevel(pos.south().east());
            localSnowLevel += computeSnowLevel(pos.south().west());
            localSnowLevel += computeSnowLevel(pos.east());
            localSnowLevel += computeSnowLevel(pos.west());

            // finely tuned formula for weight of surroundings
            float surroundings = ((float) localSnowLevel - 2) / 8 - (pHeight) * 0.045f;

            // Done calculating surroundings

            // finely tuned weight formula
            float weight = ((surroundings - height)) + 0.1f / (pHeight * pHeight * pHeight);

            if (weight >= this.random.nextFloat()) {
                // Add Snow layer!
                setBlockState(pos, Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, Math.min(height + 1, SnowBlock.MAX_LAYERS)));
            }
        }
    }

    @Unique
    private int computeSnowLevel(BlockPos pos) {
        BlockState state = getBlockState(pos);
        if (state.isOf(Blocks.SNOW))
            return state.get(SnowBlock.LAYERS);
        else if (state.isOf(Blocks.SNOW_BLOCK))
            return 10;
        else if (state.isSolidBlock(this, pos))
            return SnowBlock.MAX_LAYERS;
        else
            return 0;
    }
}
