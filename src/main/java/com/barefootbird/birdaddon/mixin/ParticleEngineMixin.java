package com.barefootbird.birdaddon.mixin;

import com.barefootbird.birdaddon.features.impl.m4.RenderOptimizer;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin<T extends Entity> {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void onAdd(ParticleOptions particleOptions, double d, double e, double f, double g, double h, double i, CallbackInfoReturnable<Particle> cir) {
        if (RenderOptimizer.shouldHideParticle(particleOptions)) cir.setReturnValue(null);
    }
}
