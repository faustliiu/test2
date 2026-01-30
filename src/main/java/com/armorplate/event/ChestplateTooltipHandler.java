package com.armorplate.event;

import com.armorplate.item.ArmorPlateItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "armorplate", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChestplateTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();

        // 只在有玩家上下文时显示（避免在创造模式物品栏中显示）
        if (player == null) {
            return;
        }

        if (stack.getItem() instanceof ArmorItem armorItem) {
            if (armorItem.getEquipmentSlot() == EquipmentSlot.CHEST) {
                // 使用PlateEvents的统一读取方法
                ItemStack plate = PlateEvents.readPlateFromChestplate(stack);

                if (!plate.isEmpty()) {
                    // 添加分隔线
                    event.getToolTip().add(Component.empty());

                    // 添加插板信息标题
                    event.getToolTip().add(Component.literal("§6§l[防弹插板]").withStyle(ChatFormatting.GOLD));

                    // 显示插板类型
                    event.getToolTip().add(Component.literal(" §7类型: §f" + plate.getHoverName().getString()));

                    // 显示插板耐久
                    if (plate.getItem() instanceof ArmorPlateItem armorPlate) {
                        int durability = armorPlate.getCurrentDurability(plate);
                        int maxDurability = armorPlate.getMaxDurability();
                        int durabilityPercent = (int)((float)durability / maxDurability * 100);

                        // 根据耐久度选择颜色
                        ChatFormatting color;
                        if (durabilityPercent >= 70) color = ChatFormatting.GREEN;
                        else if (durabilityPercent >= 30) color = ChatFormatting.YELLOW;
                        else color = ChatFormatting.RED;

                        event.getToolTip().add(Component.literal(" §7耐久: ")
                                .append(Component.literal(durability + "/" + maxDurability).withStyle(color))
                                .append(Component.literal(" (" + durabilityPercent + "%)").withStyle(ChatFormatting.GRAY)));

                        // 显示插板属性（只显示非0的属性）
                        double armorBonus = armorPlate.getEffectiveArmorBonus(plate);
                        double toughnessBonus = armorPlate.getEffectiveToughnessBonus(plate);
                        double damageReduction = armorPlate.getEffectiveDamageReduction(plate) * 100;
                        double damageThreshold = armorPlate.getEffectiveDamageThreshold(plate);

                        if (armorBonus > 0) {
                            event.getToolTip().add(Component.literal(" §7护甲加成: §a+" + String.format("%.1f", armorBonus)));
                        }
                        if (toughnessBonus > 0) {
                            event.getToolTip().add(Component.literal(" §7韧性加成: §a+" + String.format("%.1f", toughnessBonus)));
                        }
                        if (damageReduction > 0) {
                            event.getToolTip().add(Component.literal(" §7子弹减伤: §b" + String.format("%.1f", damageReduction) + "%"));
                        }
                        if (damageThreshold > 0) {
                            event.getToolTip().add(Component.literal(" §7伤害阈值: §e" + String.format("%.1f", damageThreshold)));
                        }
                    }

                    // 添加操作提示
                    event.getToolTip().add(Component.empty());
                    event.getToolTip().add(Component.literal("§8手持时按 §7[R] §8打开插板界面").withStyle(ChatFormatting.DARK_GRAY));

                    // 如果玩家按着Shift，显示更多信息
                    if (event.getFlags().isAdvanced() || player.isShiftKeyDown()) {
                        event.getToolTip().add(Component.empty());
                        event.getToolTip().add(Component.literal("§7NBT数据:").withStyle(ChatFormatting.DARK_GRAY));
                        event.getToolTip().add(Component.literal(" §7物品ID: §f" +
                                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(plate.getItem()).toString()));
                    }
                }
            }
        }
    }
}