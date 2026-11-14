package io.github.brainage04.magic_carpet.mixin;

import io.github.brainage04.magic_carpet.client.MagicCarpetFlightSound;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Starts the carpet's wind for its rider where vanilla starts its riding loops. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "startRiding", at = @At("RETURN"))
    private void magic_carpet$playFlightSound(
            Entity entity,
            boolean force,
            boolean sendEventAndTriggers,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && entity instanceof MagicCarpetEntity carpet) {
            Minecraft.getInstance()
                    .getSoundManager()
                    .play(new MagicCarpetFlightSound((LocalPlayer) (Object) this, carpet));
        }
    }
}
