package io.github.brainage04.magic_carpet.mixin;

import io.github.brainage04.magic_carpet.client.ServerSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The last line of defence: whatever puts a MagicCarpet item into a creative slot (another mod's
 * item list, a path this mod does not hide), the creative-mode packet that would carry it to a
 * server without MagicCarpet is not sent, since that server cannot decode it and drops the
 * connection. The slot is emptied again, so the client does not show an item the server never
 * received.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "handleCreativeModeItemAdd", at = @At("HEAD"), cancellable = true)
    private void magic_carpet$keepUnknownItemsFromTheServer(
            ItemStack clicked, int slot, CallbackInfo ci) {
        if (!ServerSupport.isUnknownToServer(clicked)) return;
        ci.cancel();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && slot >= 0 && slot < player.inventoryMenu.slots.size()) {
            player.inventoryMenu.getSlot(slot).set(ItemStack.EMPTY);
        }
    }

    @Inject(method = "handleCreativeModeItemDrop", at = @At("HEAD"), cancellable = true)
    private void magic_carpet$dropNothingTheServerLacks(ItemStack clicked, CallbackInfo ci) {
        if (ServerSupport.isUnknownToServer(clicked)) ci.cancel();
    }
}
