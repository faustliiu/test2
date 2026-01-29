package com.armorplate.screen;

import com.armorplate.event.AttributeModifiers;
import com.armorplate.event.PlateEvents;
import com.armorplate.item.ArmorPlateItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlateMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LogManager.getLogger();

    private final SimpleContainer plateContainer;
    private final ItemStack chestplate;
    private final int chestplateSlot;
    private boolean hasBeenSaved = false;

    public PlateMenu(int containerId, Inventory playerInventory, SimpleContainer plateContainer,
                     ItemStack chestplate, int chestplateSlot) {
        super(ModMenuTypes.PLATE_MENU.get(), containerId);
        LOGGER.info("创建PlateMenu: chestplate={}, chestplateSlot={}",
                chestplate.getDisplayName().getString(), chestplateSlot);

        this.plateContainer = plateContainer;
        this.chestplate = chestplate;
        this.chestplateSlot = chestplateSlot;

        // 插板槽位
        this.addSlot(new Slot(plateContainer, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArmorPlateItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                LOGGER.info("插板槽位改变: {}",
                        getItem().isEmpty() ? "空" : getItem().getDisplayName().getString());
                updateChestplateNBT(playerInventory.player);
            }
        });

        // 玩家物品栏
        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 玩家快捷栏
        for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }

        initSlotFromNBT();
    }

    private void initSlotFromNBT() {
        if (!chestplate.isEmpty()) {
            ItemStack plate = getPlateFromNBT(chestplate);
            if (!plate.isEmpty()) {
                LOGGER.info("从NBT初始化插板槽位: {}", plate.getDisplayName().getString());
                plateContainer.setItem(0, plate.copy());
            } else {
                LOGGER.info("胸甲NBT中没有插板");
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            LOGGER.info("quickMoveStack: index={}, item={}",
                    index, itemstack1.getDisplayName().getString());

            if (index == 0) {
                // 从插板槽移动到背包
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onTake(player, itemstack1);

                // 插板被移出，立即更新NBT
                updateChestplateNBT(player);
            } else if (itemstack1.getItem() instanceof ArmorPlateItem) {
                // 从背包移动到插板槽
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }

                // 插板被放入，立即更新NBT
                updateChestplateNBT(player);
            } else if (index < 28) {
                // 从背包移动到快捷栏
                if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 37) {
                // 从快捷栏移动到背包
                if (!this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // 简化验证
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        LOGGER.info("PlateMenu.removed: 开始保存插板状态");

        // 获取插板槽位的物品
        ItemStack plate = plateContainer.getItem(0);

        if (!chestplate.isEmpty()) {
            if (!plate.isEmpty()) {
                // 修复关键：保存插板到胸甲NBT
                savePlateToNBT(chestplate, plate);
                LOGGER.info("PlateMenu.removed: 保存插板 {} 到胸甲NBT，耐久: {}/{}",
                        plate.getDisplayName().getString(),
                        plate.getDamageValue(), plate.getMaxDamage());
            } else {
                // 如果插板槽为空，清除插板数据
                if (chestplate.hasTag() && chestplate.getTag().contains("ArmorPlate")) {
                    chestplate.getTag().remove("ArmorPlate");
                    LOGGER.info("PlateMenu.removed: 清除胸甲中的插板数据");
                }
            }

            // 更新物品栏中的胸甲
            if (chestplateSlot >= 0 && chestplateSlot < player.getInventory().getContainerSize()) {
                player.getInventory().setItem(chestplateSlot, chestplate);
                LOGGER.info("PlateMenu.removed: 更新物品栏槽位 {} 的胸甲", chestplateSlot);
            }

            // 检查玩家当前装备的胸甲是否是这个胸甲
            ItemStack equippedChestplate = player.getItemBySlot(EquipmentSlot.CHEST);
            if (isSameChestplate(chestplate, equippedChestplate)) {
                LOGGER.info("PlateMenu.removed: 玩家穿着这个胸甲，更新装备槽");
                player.setItemSlot(EquipmentSlot.CHEST, chestplate);
            }
        }

        // 更新属性
        AttributeModifiers.updatePlateAttributes(player, player.getItemBySlot(EquipmentSlot.CHEST));

        LOGGER.info("PlateMenu.removed: 完成保存");
        hasBeenSaved = true;
    }

    private void updateChestplateNBT(Player player) {
        ItemStack plate = plateContainer.getItem(0);
        LOGGER.info("updateChestplateNBT: 插板槽内容 = {}",
                plate.isEmpty() ? "空" : plate.getDisplayName().getString());

        if (!chestplate.isEmpty()) {
            if (!plate.isEmpty()) {
                // 保存插板到NBT
                savePlateToNBT(chestplate, plate);
                LOGGER.info("保存插板到胸甲NBT: {}", plate.getDisplayName().getString());

                // 如果是ArmorPlateItem，应用属性加成
                if (plate.getItem() instanceof ArmorPlateItem armorPlate) {
                    if (!armorPlate.isBroken(plate)) {
                        double armorBonus = armorPlate.getEffectiveArmorBonus(plate);
                        double toughnessBonus = armorPlate.getEffectiveToughnessBonus(plate);
                        com.armorplate.event.AttributeModifiers.applyPlateAttributes(player, armorBonus, toughnessBonus);
                        LOGGER.info("应用属性加成 - 护甲: {}, 韧性: {}", armorBonus, toughnessBonus);
                    } else {
                        LOGGER.info("插板已损坏，不提供加成");
                        com.armorplate.event.AttributeModifiers.removePlateAttributes(player);
                    }
                }
            } else {
                // 移除插板
                removePlateFromNBT(chestplate);
                LOGGER.info("从胸甲NBT移除插板");
                com.armorplate.event.AttributeModifiers.removePlateAttributes(player);
            }

            // 更新玩家物品栏中的胸甲
            if (chestplateSlot >= 0 && chestplateSlot < player.getInventory().getContainerSize()) {
                LOGGER.info("updateChestplateNBT: 更新物品栏槽位 {}", chestplateSlot);
                player.getInventory().setItem(chestplateSlot, chestplate);
            } else if (chestplateSlot == -1) {
                // 手持胸甲的情况
                LOGGER.info("updateChestplateNBT: 更新主手持物品");
                player.getInventory().setItem(player.getInventory().selected, chestplate);
            }

            // 如果玩家当前装备着这个胸甲，也更新装备槽
            ItemStack equippedChestplate = player.getItemBySlot(EquipmentSlot.CHEST);
            if (isSameChestplate(chestplate, equippedChestplate)) {
                LOGGER.info("updateChestplateNBT: 玩家穿着这个胸甲，更新装备槽");
                player.setItemSlot(EquipmentSlot.CHEST, chestplate);
            }

            LOGGER.info("updateChestplateNBT: 完成 - 胸甲有NBT: {}", chestplate.hasTag());
        } else {
            LOGGER.info("updateChestplateNBT: 胸甲为空");
        }
    }

    // NBT操作方法
    private ItemStack getPlateFromNBT(ItemStack chestplate) {
        // 使用PlateEvents的统一读取方法
        return PlateEvents.readPlateFromChestplate(chestplate);
    }

    private void savePlateToNBT(ItemStack chestplate, ItemStack plate) {
        // 使用PlateEvents的统一保存方法
        PlateEvents.savePlateToChestplate(chestplate, plate);
    }

    private void removePlateFromNBT(ItemStack chestplate) {
        var chestplateNbt = chestplate.getTag();
        if (chestplateNbt != null && chestplateNbt.contains("ArmorPlate")) {
            chestplateNbt.remove("ArmorPlate");
            // 如果NBT为空，设置为null
            if (chestplateNbt.isEmpty()) {
                chestplate.setTag(null);
            } else {
                chestplate.setTag(chestplateNbt);
            }
        }
    }

    // 辅助方法：比较两个胸甲是否相同
    private boolean isSameChestplate(ItemStack chestplate1, ItemStack chestplate2) {
        if (chestplate1.isEmpty() || chestplate2.isEmpty()) {
            return false;
        }

        // 比较物品类型
        if (chestplate1.getItem() != chestplate2.getItem()) {
            return false;
        }

        // 比较NBT哈希（如果有的话）
        if (chestplate1.hasTag() != chestplate2.hasTag()) {
            return false;
        }

        if (chestplate1.hasTag() && chestplate2.hasTag()) {
            // 比较NBT内容
            return chestplate1.getTag().equals(chestplate2.getTag());
        }

        return true;
    }
}