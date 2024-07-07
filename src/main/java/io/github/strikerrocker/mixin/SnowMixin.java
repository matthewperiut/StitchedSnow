package io.github.strikerrocker.mixin;

import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.state.property.Properties.PERSISTENT;

@Mixin(SnowBlock.class)
public class SnowMixin extends Block {

    @Shadow @Final public static IntProperty LAYERS;

    public SnowMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    void addInitialPropertyPersistent(AbstractBlock.Settings settings, CallbackInfo ci) {
        setDefaultState(stateManager.getDefaultState().with(LAYERS, 1).with(PERSISTENT, false));
    }

    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    public void placementPersistent(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(cir.getReturnValue().with(PERSISTENT, true));
    }

    @Inject(method = "appendProperties", at = @At("RETURN"))
    protected void appendPersistent(StateManager.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(PERSISTENT);
    }

    @Inject(method = "randomTick", at = @At("TAIL"))
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
    if (!world.isRaining()) {
        if (!state.get(PERSISTENT)) {
            int layers = state.get(LAYERS);
            if (layers > 1) {
                    world.setBlockState(pos, state.with(LAYERS, layers - 1));
                }
            }
        }
    }
}
