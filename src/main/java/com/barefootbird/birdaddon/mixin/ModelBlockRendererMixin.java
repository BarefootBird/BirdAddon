package com.barefootbird.birdaddon.mixin;

import com.barefootbird.birdaddon.features.impl.m4.Decoy;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BlockBehaviour.class)
public class ModelBlockRendererMixin {

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void hideBlock(
            BlockState blockState, CallbackInfoReturnable<RenderShape> cir
    ) {
        if(Decoy.shouldHideBlock(blockState)) cir.setReturnValue(null);
    }
}