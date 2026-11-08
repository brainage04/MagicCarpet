package io.github.brainage04.magic_carpet.client;

import io.github.brainage04.magic_carpet.MagicCarpet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Whether the server the client is connected to has MagicCarpet.
 *
 * <p>Neither loader stops a client with MagicCarpet from joining a server without it: Fabric's
 * registry sync does nothing against a vanilla server and NeoForge accepts a vanilla server when no
 * mod needs a channel. The client then keeps its own item ids, and a creative-mode packet that
 * carries a carpet has an id the server does not know, which fails to decode and drops the
 * connection.
 *
 * <p>The signal is the server's item tags: every server sends all its item tags to the client while
 * it joins, before the player can open an inventory, and a server with MagicCarpet always has the
 * mod's own {@code magic_carpet:magic_carpets} tag from the mod's data. In singleplayer the client
 * shares the integrated server's registries, tags included.
 */
public final class ServerSupport {
    /** Every MagicCarpet item; shipped in the mod's data, so only servers with the mod have it. */
    public static final TagKey<Item> MAGIC_CARPETS =
            TagKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "magic_carpets"));

    private ServerSupport() {}

    public static boolean serverHasMagicCarpet() {
        // Without a connection there is no server to send an item to.
        if (Minecraft.getInstance().getConnection() == null) return true;
        return BuiltInRegistries.ITEM.get(MAGIC_CARPETS).isPresent();
    }

    public static boolean isMagicCarpetItem(ItemStack stack) {
        return !stack.isEmpty()
                && BuiltInRegistries.ITEM
                        .getKey(stack.getItem())
                        .getNamespace()
                        .equals(MagicCarpet.MOD_ID);
    }

    /** A MagicCarpet item the connected server cannot decode. */
    public static boolean isUnknownToServer(ItemStack stack) {
        return isMagicCarpetItem(stack) && !serverHasMagicCarpet();
    }
}
