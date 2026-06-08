package com.barefootbird.birdaddon.mixin;

import com.barefootbird.birdaddon.features.impl.m4.RenderOptimizer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void hideBlock(
            BlockState blockState, CallbackInfoReturnable<RenderShape> cir
    ) {
        if(RenderOptimizer.shouldHideBlock(blockState)) cir.setReturnValue(RenderShape.INVISIBLE);
    }
}