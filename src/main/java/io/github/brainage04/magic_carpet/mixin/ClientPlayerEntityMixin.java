package io.github.brainage04.magic_carpet.mixin;

import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {
    @Shadow
    public ClientInput input;

    @Inject(at = @At("TAIL"), method = "rideTick()V")
    private void ridingInputs(CallbackInfo i) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        Entity v = player.getVehicle();

        if (v == null) return;
        if (!(v instanceof MagicCarpetEntity vehicle)) return;

        vehicle.setInputs(input);
    }
}
