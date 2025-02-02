package io.github.strikerrocker.mixin;


import io.github.strikerrocker.StitchedSnow;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
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

            BlockState stateBelow = getBlockState(pos.down());
            boolean solidSnowBelow = stateBelow.isOf(Blocks.SNOW_BLOCK) ||
                                     (stateBelow.isOf(Blocks.SNOW) && state.get(SnowBlock.LAYERS) == SnowBlock.MAX_LAYERS);
            int pHeight = height + 8 * (solidSnowBelow ? 1 : 0);

            if (height > StitchedSnow.config.snowAccumulationLimit)
                return;

            if (height == SnowBlock.MAX_LAYERS) {
                pos = pos.up();
                state = getBlockState(pos);
                height = 0;
            }

            int localSnowLevel = 0;

            // Check for blocks on the side (in star shape)
            localSnowLevel += computeSnowLevel(pos.north(), Direction.NORTH.getOpposite());
            localSnowLevel += computeSnowLevel(pos.north().up(), null);
            localSnowLevel += computeSnowLevel(pos.north().east(), null);
            localSnowLevel += computeSnowLevel(pos.north().west(), null);
            localSnowLevel += computeSnowLevel(pos.south(), Direction.SOUTH.getOpposite());
            localSnowLevel += computeSnowLevel(pos.south().up(), null);
            localSnowLevel += computeSnowLevel(pos.south().east(), null);
            localSnowLevel += computeSnowLevel(pos.south().west(), null);
            localSnowLevel += computeSnowLevel(pos.east(), Direction.EAST.getOpposite());
            localSnowLevel += computeSnowLevel(pos.east().up(), null);
            localSnowLevel += computeSnowLevel(pos.west(), Direction.WEST.getOpposite());
            localSnowLevel += computeSnowLevel(pos.west().up(), null);
            localSnowLevel -= getBlockState(pos.north().down()).isOf(Blocks.POWDER_SNOW) ? 10 : 0;
            localSnowLevel -= getBlockState(pos.south().down()).isOf(Blocks.POWDER_SNOW) ? 10 : 0;
            localSnowLevel -= getBlockState(pos.east().down()).isOf(Blocks.POWDER_SNOW) ? 10 : 0;
            localSnowLevel -= getBlockState(pos.west().down()).isOf(Blocks.POWDER_SNOW) ? 10 : 0;

            // finely tuned formula for weight of surroundings
            float surroundings = ((float) localSnowLevel - 2) / 8 - (pHeight) * 0.045f;

            // Done calculating surroundings

            // finely tuned weight formula
            float weight = ((surroundings - height)) + 0.1f / (pHeight * pHeight * pHeight);

            if (weight >= this.random.nextFloat()) {
                // Add Snow layer!
                BlockState updatedState = Blocks.SNOW.getDefaultState()
                                                     .with(SnowBlock.LAYERS, Math.min(height + 1, SnowBlock.MAX_LAYERS));
                Block.pushEntitiesUpBeforeBlockChange(state, updatedState, this, pos);
                StitchedSnow.setFabricSeasonsMeltable(pos);
                setBlockState(pos, updatedState);
            }
        }
    }

    @Unique
    private int computeSnowLevel(BlockPos pos, @Nullable Direction direction) {
        BlockState state = getBlockState(pos);
        boolean maxLayersOrSolid = state.isOf(Blocks.SNOW_BLOCK) ||
                                   state.isOf(Blocks.POWDER_SNOW) ||
                                   state.isSolidBlock(this, pos) ||
                                   state.isOf(Blocks.SNOW) && state.get(SnowBlock.LAYERS) == SnowBlock.MAX_LAYERS;
        if (maxLayersOrSolid)
            return 10;
        else if (state.isOf(Blocks.SNOW))
            return state.get(SnowBlock.LAYERS);
        else if (direction != null) {
            VoxelShape shape = state.getCollisionShape(this, pos);
            if (Block.isFaceFullSquare(shape, direction))
                return 10;
            else
                return 0;
        } else return 0;
    }
}
