package io.github.brainage04.magic_carpet.mixin;

import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(at = @At("HEAD"), method = "isInvulnerableTo", cancellable = true)
    public void magiccarpet$isInvulnerableTo(ServerWorld world, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (source.isIn(DamageTypeTags.IS_FALL) && ((EntityAccessor) this).getVehicle() instanceof MagicCarpetEntity) {
            cir.setReturnValue(true);
        }
    }
}
