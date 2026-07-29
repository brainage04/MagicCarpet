package io.github.brainage04.magic_carpet.item;

import net.minecraft.world.item.Item;
import java.util.function.Supplier;

public final class ModItems {
    private static Supplier<? extends Item> basic;
    private static Supplier<? extends Item> advanced;
    private static Supplier<? extends Item> legendary;
    public static void register(Supplier<? extends Item> basic, Supplier<? extends Item> advanced, Supplier<? extends Item> legendary) { ModItems.basic = basic; ModItems.advanced = advanced; ModItems.legendary = legendary; }
    public static Item basic() { return basic.get(); }
    public static Item advanced() { return advanced.get(); }
    public static Item legendary() { return legendary.get(); }
    public static void initialize() { }
}
