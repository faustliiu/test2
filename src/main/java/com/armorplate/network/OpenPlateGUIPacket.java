package com.armorplate.network;

import com.armorplate.ArmorPlateMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class OpenPlateGUIPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ItemStack chestplate;

    public OpenPlateGUIPacket(ItemStack chestplate) {
        this.chestplate = chestplate.copy();
    }

    public static void encode(OpenPlateGUIPacket packet, FriendlyByteBuf buffer) {
        buffer.writeItem(packet.chestplate);
    }

    public static OpenPlateGUIPacket decode(FriendlyByteBuf buffer) {
        return new OpenPlateGUIPacket(buffer.readItem());
    }

    public static void handle(OpenPlateGUIPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                LOGGER.info("服务器收到打开GUI请求，玩家: {}", player.getName().getString());
                // 使用玩家当前手中的物品，而不是网络包中的物品
                ItemStack currentHeldItem = player.getMainHandItem();
                if (!currentHeldItem.isEmpty() && currentHeldItem.getItem() instanceof net.minecraft.world.item.ArmorItem armorItem) {
                    if (armorItem.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.CHEST) {
                        LOGGER.info("打开GUI，胸甲: {}", currentHeldItem.getDisplayName().getString());
                        com.armorplate.event.PlateKeyHandlerServer.openPlateGUI(player, currentHeldItem.copy());
                    } else {
                        LOGGER.info("当前手持的不是胸甲");
                    }
                } else {
                    LOGGER.info("当前手中没有物品或不是护甲");
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}