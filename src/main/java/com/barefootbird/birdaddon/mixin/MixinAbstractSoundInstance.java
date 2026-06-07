package com.barefootbird.birdaddon.mixin;

import com.barefootbird.birdaddon.features.impl.m4.Sounds;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AbstractSoundInstance.class)
public class MixinAbstractSoundInstance {
    @Inject(method = "getVolume", at = @At("RETURN"), cancellable = true)
    private void onGetVolume(CallbackInfoReturnable<Float> cir) {
        AbstractSoundInstance sound = (AbstractSoundInstance) (Object) this;
        String id = sound.getIdentifier().toString();
        if (Sounds.shouldBlockSound(id)) {
            cir.setReturnValue(0f);
        }
    }
}