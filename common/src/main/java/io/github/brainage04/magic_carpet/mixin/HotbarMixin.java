package io.github.brainage04.magic_carpet.mixin;

import io.github.brainage04.magic_carpet.client.ServerSupport;
import java.util.List;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A hotbar saved where MagicCarpet exists can still hold its carpets. On a server without
 * MagicCarpet those slots load empty, both in the Saved Hotbars tab and when a hotbar is restored
 * with its key, so restoring one never sends the server an item it cannot decode.
 */
@Mixin(Hotbar.class)
public abstract class HotbarMixin {
    @Inject(method = "load", at = @At("RETURN"), cancellable = true)
    private void magic_carpet$dropItemsTheServerLacks(
            HolderLookup.Provider registries, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (ServerSupport.serverHasMagicCarpet()) return;
        cir.setReturnValue(
                cir.getReturnValue().stream()
                        .map(
                                stack ->
                                        ServerSupport.isMagicCarpetItem(stack)
                                                ? ItemStack.EMPTY
                                                : stack)
                        .toList());
    }
}
