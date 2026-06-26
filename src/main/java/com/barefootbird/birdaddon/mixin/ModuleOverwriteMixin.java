package com.barefootbird.birdaddon.mixin;

import com.odtheking.odin.config.ModuleConfig;
import com.odtheking.odin.features.Module;
import com.odtheking.odin.features.ModuleManager;
import com.odtheking.odin.features.impl.boss.SpiritBear;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

@Mixin(value = ModuleManager.class, remap = false)
public class ModuleOverwriteMixin {
    @ModifyVariable(method = "registerModules", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Module[] filterModules(Module[] modules, ModuleConfig config) {
        return Arrays.stream(modules)
                .filter(module -> !(module instanceof SpiritBear))
                .toArray(Module[]::new);
    }
}
