package io.github.strikerrocker;

import io.github.strikerrocker.mixin.ThreadedAnvilChunkStorageAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;

public class TickHandler implements ServerTickEvents.StartWorldTick {

    @Override
    public void onStartTick(ServerWorld world) {
        if (world != null) {
            if (world.isRaining()) {
                try {
                    ThreadedAnvilChunkStorageAccessor threadedAnvilChunkStorageAccessor = (ThreadedAnvilChunkStorageAccessor) world.getChunkManager().threadedAnvilChunkStorage;
                    Iterable<ChunkHolder> chunkSet = threadedAnvilChunkStorageAccessor.getEntryIterator();
                    for (ChunkHolder holder : chunkSet) {
                        Chunk chunk = holder.getCurrentChunk();
                        if (chunk == null || !world.getChunkManager().isChunkLoaded(chunk.getPos().x, chunk.getPos().z)) {
                            continue;
                        }
                        //If it can rain here, there is a 1/16 chance of trying to add snow
                        if (world.random.nextInt(StitchedSnow.config.chanceToAccumulateSnow) == 0) {
                            //Get rain height at random position in chunk, splits the random val j2 to use for both parts of position
                            BlockPos pos1 = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, world.getRandomPosInChunk(chunk.getPos().getStartX(), 0, chunk.getPos().getStartZ(), 15));
                            //Check if block at position is a snow layer block
                            if (world.getBlockState(pos1).getBlock() instanceof SnowBlock) {
                                //Check if valid Y, correct light, and correct temp for snow formation

                                if (pos1.getY() >= 0 && pos1.getY() < 256 && world.getLightLevel(LightType.BLOCK, pos1) < 10 && !world.getBiome(pos1).value().doesNotSnow(pos1)) {
                                    //Calculate mean surrounding block height
                                    int height = world.getBlockState(pos1).get(SnowBlock.LAYERS);
                                    if (height == 8) return;
                                    float surroundings = 0;
                                    List<BlockPos> posList = new ArrayList<>();
                                    posList.add(pos1.north());
                                    posList.add(pos1.east());
                                    posList.add(pos1.south());
                                    posList.add(pos1.west());
                                    //Check for blocks on the side
                                    for (BlockPos blockPos : posList) {
                                        BlockState state = world.getBlockState(blockPos);
                                        if (state.getBlock() instanceof SnowBlock) {
                                            surroundings += state.get(SnowBlock.LAYERS);
                                        } else if (state.isSolidBlock(chunk, blockPos)) {
                                            surroundings += StitchedSnow.config.snowAccumulationLimit;
                                        }
                                    }
                                    surroundings /= 4;
                                    //Done calculating surroundings
                                    if (surroundings >= height) {
                                        float weight = (surroundings - height) / 2 + 0.05f;
                                        if (world.random.nextFloat() <= weight) {
                                            //Add Snow layer!
                                            world.setBlockState(pos1, Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, height + 1));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("COULD NOT ACCESS LOADED CHUNKS!");
                    System.out.println(ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }
}
