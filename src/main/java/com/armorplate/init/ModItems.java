package com.armorplate.init;

import com.armorplate.ArmorPlateMod;
import com.armorplate.item.ArmorPlateItem;
import com.armorplate.item.PlateTier;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ArmorPlateMod.MODID);

    // 修复：确保使用正确的构造函数
    public static final RegistryObject<Item> STEEL_PLATE = ITEMS.register("steel_plate",
            () -> new ArmorPlateItem(PlateTier.STEEL,
                    new Item.Properties()
                            .stacksTo(1)
                            .durability(PlateTier.STEEL.getMaxDurability())));

    public static final RegistryObject<Item> CERAMIC_PLATE = ITEMS.register("ceramic_plate",
            () -> new ArmorPlateItem(PlateTier.CERAMIC,
                    new Item.Properties()
                            .stacksTo(1)
                            .durability(PlateTier.CERAMIC.getMaxDurability())));

    public static final RegistryObject<Item> COMPOSITE_PLATE = ITEMS.register("composite_plate",
            () -> new ArmorPlateItem(PlateTier.COMPOSITE,
                    new Item.Properties()
                            .stacksTo(1)
                            .durability(PlateTier.COMPOSITE.getMaxDurability())));
}