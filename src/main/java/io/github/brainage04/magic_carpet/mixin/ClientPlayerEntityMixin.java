package io.github.brainage04.magic_carpet.mixin;

import io.github.brainage04.magic_carpet.MagicCarpetEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Shadow
    public Input input;

    @Inject(at = @At("TAIL"), method = "tickRiding()V")
    private void ridingInputs(CallbackInfo i) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        Entity v = player.getVehicle();

        if (v == null) return;
        if (!(v instanceof MagicCarpetEntity vehicle)) return;

        vehicle.setInputs(input);
    }
}
