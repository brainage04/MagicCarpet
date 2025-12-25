package io.github.brainage04.magic_carpet.mixin;

import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(at = @At("HEAD"), method = "isInvulnerableTo", cancellable = true)
    public void magiccarpet$isInvulnerableTo(ServerLevel world, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (source.is(DamageTypeTags.IS_FALL) && ((EntityAccessor) this).getVehicle() instanceof MagicCarpetEntity) {
            cir.setReturnValue(true);
        }
    }
}
