package com.armorplate.screen;

import com.armorplate.ArmorPlateMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ArmorPlateMod.MODID);

    public static final RegistryObject<MenuType<PlateMenu>> PLATE_MENU = MENUS.register("plate_menu",
            () -> IForgeMenuType.create((containerId, inventory, data) -> {
                net.minecraft.world.item.ItemStack chestplate = data.readItem();
                int slotIndex = data.readInt();
                return new PlateMenu(containerId, inventory,
                        new net.minecraft.world.SimpleContainer(1), chestplate, slotIndex);
            }));
}