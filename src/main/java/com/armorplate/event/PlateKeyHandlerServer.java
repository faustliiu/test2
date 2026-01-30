package com.armorplate.event;

import com.armorplate.screen.PlateMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.world.item.ItemStack;

public class PlateKeyHandlerServer {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void openPlateGUI(ServerPlayer player, ItemStack chestplate) {
        LOGGER.info("PlateKeyHandlerServer: 为玩家 {} 打开GUI", player.getName().getString());

        // 使用 -1 作为槽位索引，表示手持
        int slotIndex = -1;

        NetworkHooks.openScreen(player, new net.minecraft.world.MenuProvider() {
            @Override
            public net.minecraft.network.chat.Component getDisplayName() {
                return chestplate.getHoverName();
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int containerId,
                    Inventory playerInventory,
                    net.minecraft.world.entity.player.Player player
            ) {
                LOGGER.info("创建PlateMenu，容器ID: {}", containerId);
                return new PlateMenu(containerId, playerInventory,
                        new SimpleContainer(1), chestplate, slotIndex);
            }
        }, buf -> {
            buf.writeItem(chestplate);
            buf.writeInt(slotIndex);
        });
    }
}