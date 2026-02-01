package com.armorplate.event;

import com.armorplate.item.ArmorPlateItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "armorplate", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AttributeModifiers {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final UUID PLATE_ARMOR_UUID = UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6F");
    private static final UUID PLATE_TOUGHNESS_UUID = UUID.fromString("D8499B04-0E66-4726-AB29-64469D9B0693");

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        LOGGER.info("装备变化事件: 槽位={}, 旧物品={}, 新物品={}",
                event.getSlot(),
                event.getFrom().isEmpty() ? "空" : event.getFrom().getDisplayName().getString(),
                event.getTo().isEmpty() ? "空" : event.getTo().getDisplayName().getString());

        if (event.getSlot() == EquipmentSlot.CHEST) {
            LivingEntity entity = event.getEntity();
            ItemStack eventTo = event.getTo();
            ItemStack actual = entity.getItemBySlot(EquipmentSlot.CHEST);

            LOGGER.info("装备变化事件诊断:");
            LOGGER.info("  event.getTo() identityHash={}, tag keys={}",
                    System.identityHashCode(eventTo),
                    eventTo.hasTag() && eventTo.getTag() != null ?
                            eventTo.getTag().getAllKeys() : "null或空");

            LOGGER.info("  entity.getItemBySlot() identityHash={}, tag keys={}",
                    System.identityHashCode(actual),
                    actual.hasTag() && actual.getTag() != null ?
                            actual.getTag().getAllKeys() : "null或空");

            if (eventTo.hasTag()) {
                CompoundTag tag = eventTo.getTag();
                if (tag.contains("ArmorPlate")) {
                    Tag armorPlateTag = tag.get("ArmorPlate");
                    LOGGER.info("  检测到其他模组的ArmorPlate标签，类型: {}", armorPlateTag.getType().getName());
                    if (armorPlateTag instanceof net.minecraft.nbt.DoubleTag) {
                        LOGGER.info("  这是其他模组的DoubleTag数据，忽略它");
                    }
                }

                if (tag.contains("ArmorPlateMod_PlateData")) {
                    Tag ourTag = tag.get("ArmorPlateMod_PlateData");
                    LOGGER.info("  检测到我们的插板标签，类型: {}", ourTag.getType().getName());
                }
            }

            updatePlateAttributes(entity, actual);
        }
    }

    public static void removePlateAttributes(LivingEntity entity) {
        if (entity == null) return;

        AttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        AttributeInstance toughnessAttr = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);

        boolean removedArmor = false;
        boolean removedToughness = false;

        if (armorAttr != null && armorAttr.getModifier(PLATE_ARMOR_UUID) != null) {
            armorAttr.removeModifier(PLATE_ARMOR_UUID);
            removedArmor = true;
            LOGGER.info("成功移除护甲修饰符: 实体={}", entity.getName().getString());
        }

        if (toughnessAttr != null && toughnessAttr.getModifier(PLATE_TOUGHNESS_UUID) != null) {
            toughnessAttr.removeModifier(PLATE_TOUGHNESS_UUID);
            removedToughness = true;
            LOGGER.info("成功移除韧性修饰符: 实体={}", entity.getName().getString());
        }

        if (removedArmor || removedToughness) {
            LOGGER.info("移除属性修饰符: 实体={}, 移除了护甲={}, 韧性={}",
                    entity.getName().getString(), removedArmor, removedToughness);
        }
    }

    public static void updatePlateAttributes(LivingEntity entity, ItemStack chestplate) {
        removePlateAttributes(entity);

        if (chestplate.isEmpty() || !(chestplate.getItem() instanceof ArmorItem)) {
            LOGGER.info("更新属性: 胸甲为空或不是护甲 - 物品: {}",
                    chestplate.isEmpty() ? "空" : chestplate.getItem().getDescriptionId());
            return;
        }

        ArmorItem armorItem = (ArmorItem) chestplate.getItem();
        if (armorItem.getEquipmentSlot() != EquipmentSlot.CHEST) {
            LOGGER.info("更新属性: 物品不是胸甲 - 槽位: {}", armorItem.getEquipmentSlot());
            return;
        }

        LOGGER.info("更新属性: 实体={}, 胸甲={}, 有NBT={}",
                entity.getName().getString(),
                chestplate.getDisplayName().getString(),
                chestplate.hasTag());

        ItemStack plate = PlateEvents.readPlateFromChestplate(chestplate);

        if (plate.isEmpty()) {
            LOGGER.info("胸甲中没有我们的插板或读取失败");

            if (chestplate.hasTag()) {
                CompoundTag tag = chestplate.getTag();
                if (tag.contains("ArmorPlate")) {
                    LOGGER.info("胸甲中有其他模组的ArmorPlate标签，忽略它");
                }
            }

            return;
        }

        LOGGER.info("成功读取插板: {}, 耐久: {}/{}",
                plate.getDisplayName().getString(),
                plate.getDamageValue(), plate.getMaxDamage());

        if (plate.getItem() instanceof ArmorPlateItem armorPlate) {
            if (!armorPlate.isBroken(plate)) {
                double armorBonus = armorPlate.getEffectiveArmorBonus(plate);
                double toughnessBonus = armorPlate.getEffectiveToughnessBonus(plate);

                LOGGER.info("插板属性: 护甲加成={}, 韧性加成={}", armorBonus, toughnessBonus);

                applyPlateAttributes(entity, armorBonus, toughnessBonus);
                return;
            } else {
                LOGGER.info("插板已损坏，不提供属性加成");
            }
        } else {
            LOGGER.info("读取的物品不是ArmorPlateItem，物品类型: {}",
                    plate.getItem().getDescriptionId());
        }

        removePlateAttributes(entity);
    }

    public static void applyPlateAttributes(LivingEntity entity, double armorBonus, double toughnessBonus) {
        if (entity == null || (armorBonus <= 0 && toughnessBonus <= 0)) return;

        AttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        AttributeInstance toughnessAttr = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);

        boolean appliedArmor = false;
        boolean appliedToughness = false;

        if (armorAttr != null && armorBonus > 0) {
            if (armorAttr.getModifier(PLATE_ARMOR_UUID) != null) {
                armorAttr.removeModifier(PLATE_ARMOR_UUID);
            }

            AttributeModifier modifier = new AttributeModifier(
                    PLATE_ARMOR_UUID,
                    "Armor Plate Bonus",
                    armorBonus,
                    AttributeModifier.Operation.ADDITION
            );

            armorAttr.addPermanentModifier(modifier);
            appliedArmor = true;

            LOGGER.info("应用护甲加成: +{} 给实体 {}", armorBonus, entity.getName().getString());
        }

        if (toughnessAttr != null && toughnessBonus > 0) {
            if (toughnessAttr.getModifier(PLATE_TOUGHNESS_UUID) != null) {
                toughnessAttr.removeModifier(PLATE_TOUGHNESS_UUID);
            }

            AttributeModifier modifier = new AttributeModifier(
                    PLATE_TOUGHNESS_UUID,
                    "Armor Toughness Bonus",
                    toughnessBonus,
                    AttributeModifier.Operation.ADDITION
            );

            toughnessAttr.addPermanentModifier(modifier);
            appliedToughness = true;

            LOGGER.info("应用韧性加成: +{} 给实体 {}", toughnessBonus, entity.getName().getString());
        }

        if (!appliedArmor && !appliedToughness) {
            LOGGER.info("没有应用任何属性加成");
        }
    }
}