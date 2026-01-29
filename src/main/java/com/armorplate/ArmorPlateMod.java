package com.armorplate;

import com.armorplate.config.PlateConfig;
import com.armorplate.init.ModItems;
import com.armorplate.screen.ModMenuTypes;
import com.armorplate.screen.PlateScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(ArmorPlateMod.MODID)
public class ArmorPlateMod {
    public static final String MODID = "armorplate";

    // 网络通道
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public ArmorPlateMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PlateConfig.SPEC);

        // 注册物品
        ModItems.ITEMS.register(modEventBus);

        // 注册菜单
        ModMenuTypes.MENUS.register(modEventBus);

        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(new com.armorplate.event.PlateEvents());
        MinecraftForge.EVENT_BUS.register(new com.armorplate.event.AttributeModifiers());

        // 注册客户端设置事件
        modEventBus.addListener(this::clientSetup);
        registerPackets();
    }

    private void registerPackets() {
        int packetId = 0;
        CHANNEL.registerMessage(packetId++,
                com.armorplate.network.OpenPlateGUIPacket.class,
                com.armorplate.network.OpenPlateGUIPacket::encode,
                com.armorplate.network.OpenPlateGUIPacket::decode,
                com.armorplate.network.OpenPlateGUIPacket::handle
        );
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.PLATE_MENU.get(), PlateScreen::new);
        });
    }
}
