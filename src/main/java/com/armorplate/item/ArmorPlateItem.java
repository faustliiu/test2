package com.armorplate.item;

import com.armorplate.config.PlateConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArmorPlateItem extends Item {
    private final PlateTier tier;

    public ArmorPlateItem(PlateTier tier, Properties properties) {
        super(properties.defaultDurability(tier.getMaxDurability()));
        this.tier = tier;
    }

    private double getDurabilityEffectMultiplier(ItemStack stack) {
        if (!PlateConfig.ENABLE_DURABILITY_EFFECT.get()) {
            return 1.0;
        }

        double durabilityPercent = getDurabilityPercent(stack);

        if (durabilityPercent <= PlateConfig.DURABILITY_EFFECT_THRESHOLD_1.get()) {
            return PlateConfig.DURABILITY_EFFECT_MULTIPLIER_1.get();
        } else if (durabilityPercent <= PlateConfig.DURABILITY_EFFECT_THRESHOLD_2.get()) {
            return PlateConfig.DURABILITY_EFFECT_MULTIPLIER_2.get();
        } else if (durabilityPercent <= PlateConfig.DURABILITY_EFFECT_THRESHOLD_3.get()) {
            return PlateConfig.DURABILITY_EFFECT_MULTIPLIER_3.get();
        } else {
            return PlateConfig.DURABILITY_EFFECT_MULTIPLIER_4.get();
        }
    }

    public double getEffectiveArmorBonus(ItemStack stack) {
        if (isBroken(stack) || !tier.isEnabled()) {
            return 0.0;
        }
        return tier.getArmorBonus() * getDurabilityEffectMultiplier(stack);
    }

    public double getEffectiveToughnessBonus(ItemStack stack) {
        if (isBroken(stack) || !tier.isEnabled()) {
            return 0.0;
        }
        return tier.getToughnessBonus() * getDurabilityEffectMultiplier(stack);
    }

    public double getEffectiveDamageReduction(ItemStack stack) {
        if (isBroken(stack) || !tier.isEnabled()) {
            return 0.0;
        }
        double reduction = tier.getDamageReduction() * getDurabilityEffectMultiplier(stack);
        return Math.min(reduction, 1.0);
    }

    public double getEffectiveDamageThreshold(ItemStack stack) {
        if (isBroken(stack) || !tier.isEnabled()) {
            return 0.0;
        }
        return tier.getDamageThreshold() * getDurabilityEffectMultiplier(stack);
    }

    private double getDurabilityPercent(ItemStack stack) {
        int currentDamage = stack.getDamageValue();
        return Math.max(0.0, (double)(this.getMaxDurability() - currentDamage) / this.getMaxDurability());
    }

    public int getCurrentDurability(ItemStack stack) {
        return Math.max(0, this.getMaxDurability() - stack.getDamageValue());
    }

    public int getMaxDurability() {
        return tier.getMaxDurability();
    }

    public boolean isBroken(ItemStack stack) {
        return stack.getDamageValue() >= this.getMaxDurability();
    }

    public void damagePlate(ItemStack stack, float damageAmount) {
        if (isBroken(stack)) {
            return;
        }

        int durabilityCost = calculateDurabilityCost(damageAmount);
        int newDamage = stack.getDamageValue() + durabilityCost;

        if (newDamage >= this.getMaxDurability()) {
            stack.setDamageValue(this.getMaxDurability());
            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean("Broken", true);
            stack.setTag(tag);
        } else {
            stack.setDamageValue(newDamage);
        }
    }

    private int calculateDurabilityCost(float damageAmount) {
        int baseCost = PlateConfig.BASE_DURABILITY_COST.get();
        double damageRatio = PlateConfig.DAMAGE_TO_DURABILITY_RATIO.get();
        int maxCost = PlateConfig.MAX_DURABILITY_COST_PER_HIT.get();

        int additionalCost = (int) Math.max(0, damageAmount * damageRatio);
        int totalCost = baseCost + additionalCost;

        return Math.min(totalCost, maxCost);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        boolean showDetails = flag.isAdvanced() || isShiftKeyDown();

        tooltip.add(Component.empty());

        if (showDetails) {
            tooltip.add(Component.literal("§6§l[防弹插板]").withStyle(ChatFormatting.GOLD));

            Component displayName = stack.getHoverName();
            tooltip.add(Component.literal(" §7类型: ").append(displayName));

            if (!tier.isEnabled()) {
                tooltip.add(Component.literal(" §c§l已禁用").withStyle(ChatFormatting.RED));
                return;
            }

            if (isBroken(stack)) {
                tooltip.add(Component.literal(" §c§l已损坏").withStyle(ChatFormatting.RED));
                return;
            }

            int current = getCurrentDurability(stack);
            int max = getMaxDurability();
            double percent = getDurabilityPercent(stack) * 100;

            ChatFormatting color;
            if (percent >= 70) color = ChatFormatting.GREEN;
            else if (percent >= 30) color = ChatFormatting.YELLOW;
            else color = ChatFormatting.RED;

            tooltip.add(Component.literal(" §7耐久: ")
                    .append(Component.literal(current + "/" + max).withStyle(color))
                    .append(Component.literal(" (" + String.format("%.0f", percent) + "%)").withStyle(ChatFormatting.GRAY)));

            double armorBonus = getEffectiveArmorBonus(stack);
            double toughnessBonus = getEffectiveToughnessBonus(stack);
            double damageReduction = getEffectiveDamageReduction(stack) * 100;
            double damageThreshold = getEffectiveDamageThreshold(stack);

            tooltip.add(Component.literal(" §7属性:"));

            if (armorBonus > 0) {
                tooltip.add(Component.literal("  §7+§a" + String.format("%.1f", armorBonus) + " 护甲值"));
            }
            if (toughnessBonus > 0) {
                tooltip.add(Component.literal("  §7+§a" + String.format("%.1f", toughnessBonus) + " 韧性"));
            }
            if (damageReduction > 0) {
                tooltip.add(Component.literal("  §7+§b" + String.format("%.1f", damageReduction) + "% 子弹减伤"));
            }
            if (damageThreshold > 0) {
                tooltip.add(Component.literal("  §7+§e" + String.format("%.1f", damageThreshold) + " 伤害阈值"));
            }

            if (PlateConfig.ENABLE_DURABILITY_EFFECT.get() && percent < 100) {
                double effectMultiplier = getDurabilityEffectMultiplier(stack) * 100;
                if (effectMultiplier < 100) {
                    tooltip.add(Component.literal(" §7当前效果: §e" + String.format("%.0f", effectMultiplier) + "%"));
                }
            }

            if (flag.isAdvanced()) {
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("§7配置信息:").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal(" §7基础护甲: §f" + String.format("%.1f", tier.getArmorBonus())));
                tooltip.add(Component.literal(" §7基础韧性: §f" + String.format("%.1f", tier.getToughnessBonus())));
                tooltip.add(Component.literal(" §7基础减伤: §f" + String.format("%.1f%%", tier.getDamageReduction() * 100)));
                tooltip.add(Component.literal(" §7基础阈值: §f" + String.format("%.1f", tier.getDamageThreshold())));
            }
        } else {
            tooltip.add(Component.literal("§7防弹插板 - ").append(stack.getHoverName()));

            if (!tier.isEnabled()) {
                tooltip.add(Component.literal("§c[已禁用]"));
            } else if (isBroken(stack)) {
                tooltip.add(Component.literal("§c[已损坏]"));
            } else {
                int current = getCurrentDurability(stack);
                int max = getMaxDurability();
                double percent = getDurabilityPercent(stack) * 100;

                ChatFormatting color;
                if (percent >= 70) color = ChatFormatting.GREEN;
                else if (percent >= 30) color = ChatFormatting.YELLOW;
                else color = ChatFormatting.RED;

                tooltip.add(Component.literal("§7耐久: ").append(
                        Component.literal(String.format("%.0f%%", percent)).withStyle(color)
                ));
            }

            tooltip.add(Component.literal("§8按住§7 Shift §8查看详情").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private boolean isShiftKeyDown() {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (float)(1.0 - (double)stack.getDamageValue() / this.getMaxDurability()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        double percent = getDurabilityPercent(stack);
        if (percent > 0.7) return ChatFormatting.GREEN.getColor();
        if (percent > 0.3) return ChatFormatting.YELLOW.getColor();
        return ChatFormatting.RED.getColor();
    }

    public PlateTier getTier() {
        return this.tier;
    }

    public double getBaseArmorBonus() {
        return tier.getArmorBonus();
    }

    public double getBaseToughnessBonus() {
        return tier.getToughnessBonus();
    }

    public double getBaseDamageReduction() {
        return tier.getDamageReduction();
    }

    public double getBaseDamageThreshold() {
        return tier.getDamageThreshold();
    }
}