package com.armorplate.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArmorPlateItem extends Item {
    private final PlateTier tier;
    private final int maxDurability;

    public ArmorPlateItem(PlateTier tier, Properties properties) {
        super(properties.defaultDurability(getMaxDurabilityForTier(tier)).setNoRepair());
        this.tier = tier;
        this.maxDurability = getMaxDurabilityForTier(tier);
    }

    private static int getMaxDurabilityForTier(PlateTier tier) {
        return switch (tier) {
            case STEEL -> 200;
            case CERAMIC -> 150;
            case COMPOSITE -> 300;
        };
    }

    public PlateTier getTier() {
        return tier;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getCurrentDurability(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    public boolean isBroken(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    public void damagePlate(ItemStack stack, int amount) {
        if (stack.isDamageableItem()) {
            int newDamage = stack.getDamageValue() + amount;

            // 重要：不限制最大值，允许损坏
            stack.setDamageValue(newDamage);

            // 如果损坏，标记为损坏状态
            if (isBroken(stack)) {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putBoolean("Broken", true);
                LOGGER.info("插板已标记为损坏状态");
            }
        }
    }

    public double getEffectiveArmorBonus(ItemStack stack) {
        if (isBroken(stack)) return 0;
        return tier.getArmorBonus();
    }

    public double getEffectiveToughnessBonus(ItemStack stack) {
        if (isBroken(stack)) return 0;
        return tier.getToughnessBonus();
    }

    public double getEffectiveDamageReduction(ItemStack stack) {
        if (isBroken(stack)) return 0;

        // 耐久越低，效果越差
        float durabilityRatio = (float)getCurrentDurability(stack) / maxDurability;
        return tier.getDamageReduction() * durabilityRatio;
    }

    public double getEffectiveDamageThreshold(ItemStack stack) {
        if (isBroken(stack)) return 0;

        // 耐久越低，阈值越低
        float durabilityRatio = (float)getCurrentDurability(stack) / maxDurability;
        return tier.getDamageThreshold() * durabilityRatio;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return maxDurability;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !isBroken(stack); // 损坏的插板不显示耐久条
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (isBroken(stack)) return 0;

        float durabilityRatio = Math.max(0.0F, (float)(stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage());
        return Math.round(13.0F * durabilityRatio);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (isBroken(stack)) return 0xFF0000; // 损坏时为红色

        float durabilityRatio = (float)(stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
        if (durabilityRatio > 0.7F) return 0x00FF00; // 绿色
        if (durabilityRatio > 0.3F) return 0xFFFF00; // 黄色
        return 0xFF0000; // 红色
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // 显示满耐久时的属性
        tooltip.add(Component.translatable("tooltip.armorplate.armor_bonus")
                .append(": +" + String.format("%.1f", tier.getArmorBonus()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.armorplate.toughness_bonus")
                .append(": +" + String.format("%.1f", tier.getToughnessBonus()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.armorplate.damage_reduction")
                .append(": " + (int)(tier.getDamageReduction() * 100) + "%")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.armorplate.threshold")
                .append(": " + String.format("%.1f", tier.getDamageThreshold()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§7手持可安装插板胸甲时按R键打开插板管理界面")
                .withStyle(ChatFormatting.DARK_GRAY));

        // 显示当前耐久效果
        if (!isBroken(stack)) {
            tooltip.add(Component.literal("当前效果:")
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("  伤害减免: " +
                            (int)(getEffectiveDamageReduction(stack) * 100) + "%")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("  伤害阈值: " +
                            String.format("%.1f", getEffectiveDamageThreshold(stack)))
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("  耐久: " + getCurrentDurability(stack) + "/" + maxDurability)
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.literal("§c已损坏 - 不提供任何效果")
                    .withStyle(ChatFormatting.RED));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.armorplate.instructions")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    // 添加一个简单的日志记录器
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger();
}