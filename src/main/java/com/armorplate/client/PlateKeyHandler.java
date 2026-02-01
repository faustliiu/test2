package com.armorplate.client;

import com.armorplate.ArmorPlateMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ArmorPlateMod.MODID, value = Dist.CLIENT)
public class PlateKeyHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final KeyMapping OPEN_PLATE_GUI = new KeyMapping(
            "key.armorplate.open_plate_gui",
            GLFW.GLFW_KEY_R,
            "category.armorplate.plates"
    );

    @SubscribeEvent
    public static void onRegisterKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PLATE_GUI);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (OPEN_PLATE_GUI.consumeClick()) {
            LOGGER.info("R键被按下");
            
            ItemStack heldItem = mc.player.getMainHandItem();

            if (heldItem.getItem() instanceof ArmorItem armorItem) {
                if (armorItem.getEquipmentSlot() == EquipmentSlot.CHEST) {
                    LOGGER.info("手持胸甲: {}", heldItem.getDisplayName().getString());
                    sendOpenGUIRequest(heldItem);
                } else {
                    LOGGER.info("不是胸甲，是: {}", armorItem.getEquipmentSlot());
                }
            } else {
                LOGGER.info("手持物品不是护甲: {}",
                        heldItem.isEmpty() ? "空手" : heldItem.getItem().getName(heldItem).getString());
            }
        }
    }

    private static void sendOpenGUIRequest(ItemStack chestplate) {
        LOGGER.info("发送打开GUI请求");
        //发送网络数据包到服务器
        ArmorPlateMod.CHANNEL.sendToServer(new com.armorplate.network.OpenPlateGUIPacket(chestplate));
    }
}