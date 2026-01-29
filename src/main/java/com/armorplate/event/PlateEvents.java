package com.armorplate.event;

import com.armorplate.config.PlateConfig;
import com.armorplate.item.ArmorPlateItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = "armorplate", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlateEvents {
    private static final Logger LOGGER = LogManager.getLogger();

    private static long lastProcessedTick = -1;
    private static String lastProcessedEntity = "";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        long currentTick = event.getEntity().level().getGameTime();
        String currentEntity = event.getEntity().getStringUUID();

        if (currentTick == lastProcessedTick && currentEntity.equals(lastProcessedEntity)) {
            return;
        }

        lastProcessedTick = currentTick;
        lastProcessedEntity = currentEntity;

        if (!PlateConfig.ENABLE_SPECIAL_PROTECTION.get()) {
            return;
        }

        LOGGER.info("===== 插板伤害事件开始 =====");
        LOGGER.info("实体: {}, UUID: {}",
                event.getEntity().getName().getString(),
                currentEntity);
        LOGGER.info("伤害来源: {}, 类型: {}",
                event.getSource().getMsgId(),
                event.getSource().type().msgId());
        LOGGER.info("原始伤害值: {}", event.getAmount());

        if (!(event.getEntity() instanceof Player player)) {
            LOGGER.info("受伤实体不是玩家，跳过");
            LOGGER.info("===== 插板伤害事件结束 =====");
            return;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.isEmpty()) {
            LOGGER.info("玩家未穿戴胸甲");
            LOGGER.info("===== 插板伤害事件结束 =====");
            return;
        }

        LOGGER.info("胸甲物品: {}", chestplate.getDisplayName().getString());
        LOGGER.info("胸甲有NBT: {}", chestplate.hasTag());

        // 使用统一的读取方法
        ItemStack plate = readPlateFromChestplate(chestplate);

        if (plate.isEmpty()) {
            LOGGER.info("胸甲中没有插板或读取失败");
            LOGGER.info("===== 插板伤害事件结束 =====");
            return;
        }

        LOGGER.info("从NBT读取插板: {}, 数量: {}",
                plate.getDisplayName().getString(), plate.getCount());

        if (!(plate.getItem() instanceof ArmorPlateItem armorPlate)) {
            LOGGER.info("插板物品不是ArmorPlateItem");
            LOGGER.info("===== 插板伤害事件结束 =====");
            return;
        }

        if (armorPlate.isBroken(plate)) {
            LOGGER.info("插板已损坏");
            LOGGER.info("===== 插板伤害事件结束 =====");
            return;
        }

        float damage = event.getAmount();
        DamageSource source = event.getSource();

        boolean isBulletDamage = isBulletDamage(source);
        LOGGER.info("子弹伤害检查结果: {}", isBulletDamage);

        if (isBulletDamage) {
            processBulletDamage(event, player, chestplate, plate, armorPlate, damage, source);
        } else {
            LOGGER.info("不是子弹伤害，跳过处理");
            LOGGER.info("===== 插板伤害事件结束 =====");
        }
    }

    private static void processBulletDamage(LivingHurtEvent event, Player player, ItemStack chestplate,
                                            ItemStack plate, ArmorPlateItem armorPlate, float damage, DamageSource source) {
        double damageReduction = armorPlate.getEffectiveDamageReduction(plate);
        double damageThreshold = armorPlate.getEffectiveDamageThreshold(plate);

        LOGGER.info("插板属性 - 伤害减免: {}%, 伤害阈值: {}",
                String.format("%.1f", damageReduction * 100),
                String.format("%.1f", damageThreshold));

        if (source.getDirectEntity() != null) {
            LOGGER.info("伤害直接实体: {}", source.getDirectEntity().getName().getString());
        }
        if (source.getEntity() != null) {
            LOGGER.info("伤害来源实体: {}", source.getEntity().getName().getString());
        }

        int durabilityCost = calculateDurabilityCost(damage, damageReduction);
        LOGGER.info("耐久消耗计算: 基础伤害 {}, 减免后 {}, 耐久成本 {}",
                damage, damage * (1 - damageReduction), durabilityCost);

        int currentDamage = plate.getDamageValue();
        int maxDamage = plate.getMaxDamage();
        LOGGER.info("耐久状态: {}/{}, 剩余耐久: {}",
                currentDamage, maxDamage, maxDamage - currentDamage);

        // 应用耐久消耗
        armorPlate.damagePlate(plate, durabilityCost);

        LOGGER.info("更新后插板: {}, 耐久: {}/{}",
                plate.getDisplayName().getString(),
                plate.getDamageValue(), plate.getMaxDamage());

        // 如果插板损坏了
        if (armorPlate.isBroken(plate)) {
            LOGGER.info("插板已损坏！保留损坏的插板数据");

            // 使用更新后的插板数据
            savePlateToChestplate(chestplate, plate);

            // 更新属性（损坏的插板不提供加成）
            AttributeModifiers.updatePlateAttributes(player, chestplate);

            player.displayClientMessage(
                    Component.literal("§c防弹插板已损坏！").withStyle(ChatFormatting.RED),
                    true
            );

            LOGGER.info("===== 插板损坏，事件结束 =====");
            return;
        }

        // 保存更新后的插板数据到胸甲
        savePlateToChestplate(chestplate, plate);
        LOGGER.info("已保存更新后的插板NBT");

        if (damage <= damageThreshold) {
            LOGGER.info("伤害 {} <= 阈值 {}，完全抵消伤害", damage, damageThreshold);
            event.setCanceled(true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
            LOGGER.info("===== 伤害完全抵消 =====");
            return;
        }

        float newDamage = damage * (float)(1.0 - damageReduction);
        LOGGER.info("伤害减免计算: {} * (1 - {}) = {}", damage, damageReduction, newDamage);

        if (newDamage <= 0.01f) {
            LOGGER.info("减免后伤害极低，取消伤害");
            event.setCanceled(true);
        } else {
            event.setAmount(newDamage);
            LOGGER.info("设置新伤害值: {} (减少了{}点)", newDamage, damage - newDamage);
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.3F, 0.8F);

        LOGGER.info("===== 插板伤害处理完成 =====");
    }

    /**
     * 统一的保存插板方法，使用新格式
     */
    public static void savePlateToChestplate(ItemStack chestplate, ItemStack plate) {
        try {
            // 确保胸甲有NBT
            CompoundTag chestplateTag = chestplate.getOrCreateTag();

            // 验证插板数据
            if (plate.isEmpty()) {
                LOGGER.error("尝试保存空的插板到胸甲！");
                return;
            }

            // 创建插板数据标签（新格式）
            CompoundTag plateData = new CompoundTag();

            // 保存插板物品数据到Item标签
            CompoundTag itemTag = new CompoundTag();
            plate.save(itemTag); // 保存完整物品数据
            plateData.put("Item", itemTag);

            // 保存耐久度信息
            int currentDurability = plate.getDamageValue();
            int maxDurability = plate.getMaxDamage();
            plateData.putInt("Durability", currentDurability);
            plateData.putInt("MaxDurability", maxDurability);

            // 保存创建时间戳（用于调试）
            plateData.putLong("SaveTime", System.currentTimeMillis());

            // 将插板数据放入胸甲NBT中
            chestplateTag.put("ArmorPlate", plateData);

            // 验证保存是否成功
            if (chestplateTag.contains("ArmorPlate")) {
                CompoundTag verificationTag = chestplateTag.getCompound("ArmorPlate");
                if (!verificationTag.isEmpty()) {
                    LOGGER.info("插板数据保存成功，验证通过");

                    // 调试：显示保存的格式
                    LOGGER.info("保存的格式检查 - 包含Item标签: {}", verificationTag.contains("Item"));
                    LOGGER.info("保存的格式检查 - 包含Durability: {}", verificationTag.contains("Durability"));
                    LOGGER.info("保存的格式检查 - 包含MaxDurability: {}", verificationTag.contains("MaxDurability"));
                } else {
                    LOGGER.error("插板NBT验证失败：保存的NBT为空");
                }
            } else {
                LOGGER.error("插板数据保存失败：ArmorPlate标签不存在");
            }

        } catch (Exception e) {
            LOGGER.error("保存插板到胸甲时出错: ", e);
        }
    }

    /**
     * 统一的读取插板方法，支持旧格式迁移
     */
    public static ItemStack readPlateFromChestplate(ItemStack chestplate) {
        if (!chestplate.hasTag()) {
            return ItemStack.EMPTY;
        }

        CompoundTag chestplateTag = chestplate.getTag();
        if (!chestplateTag.contains("ArmorPlate")) {
            return ItemStack.EMPTY;
        }

        CompoundTag plateTag = chestplateTag.getCompound("ArmorPlate");

        LOGGER.info("读取到ArmorPlate标签，键: {}", plateTag.getAllKeys());

        // 检测并迁移旧格式（直接包含id的格式）
        if (plateTag.contains("id") && !plateTag.contains("Item")) {
            LOGGER.info("检测到旧格式插板，进行迁移...");

            try {
                // 从旧格式读取数据
                String plateId = plateTag.getString("id");
                int durability = 0;
                int maxDurability = 0;

                // 尝试读取可能的耐久字段
                if (plateTag.contains("Damage")) {
                    durability = plateTag.getInt("Damage");
                } else if (plateTag.contains("Durability")) {
                    durability = plateTag.getInt("Durability");
                }

                if (plateTag.contains("MaxDurability")) {
                    maxDurability = plateTag.getInt("MaxDurability");
                }

                LOGGER.info("旧格式数据 - ID: {}, 耐久: {}/{}", plateId, durability, maxDurability);

                // 创建新格式
                CompoundTag newPlateTag = new CompoundTag();
                CompoundTag itemTag = new CompoundTag();

                // 重建物品数据
                itemTag.putString("id", plateId);
                itemTag.putInt("Count", 1);

                // 如果有Damage标签，也保存
                if (plateTag.contains("Damage")) {
                    itemTag.putInt("Damage", plateTag.getInt("Damage"));
                }

                // 如果有自定义标签，也复制
                if (plateTag.contains("tag")) {
                    itemTag.put("tag", plateTag.getCompound("tag").copy());
                }

                newPlateTag.put("Item", itemTag);
                newPlateTag.putInt("Durability", durability);
                newPlateTag.putInt("MaxDurability", maxDurability);

                // 保存回胸甲
                chestplateTag.put("ArmorPlate", newPlateTag);
                chestplate.setTag(chestplateTag);

                LOGGER.info("成功迁移旧格式到新格式");

                // 使用新格式继续处理
                plateTag = newPlateTag;
            } catch (Exception e) {
                LOGGER.error("迁移旧格式插板时出错: ", e);
                return ItemStack.EMPTY;
            }
        }

        // 处理新格式
        if (plateTag.contains("Item")) {
            CompoundTag itemTag = plateTag.getCompound("Item");
            try {
                ItemStack plate = ItemStack.of(itemTag);

                // 如果插板有效，尝试从Durability字段恢复耐久度
                if (!plate.isEmpty() && plateTag.contains("Durability")) {
                    int savedDurability = plateTag.getInt("Durability");
                    // 注意：ItemStack的setDamageValue设置的是损坏值，不是剩余耐久
                    if (savedDurability >= 0 && savedDurability <= plate.getMaxDamage()) {
                        plate.setDamageValue(savedDurability);
                    }
                }

                LOGGER.info("从新格式成功读取插板: {}, 耐久: {}/{}",
                        plate.getDisplayName().getString(),
                        plate.getDamageValue(), plate.getMaxDamage());

                return plate;
            } catch (Exception e) {
                LOGGER.error("从Item标签创建插板时出错: ", e);
                return ItemStack.EMPTY;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 获取插板耐久度
     */
    public static int[] getPlateDurability(ItemStack chestplate) {
        int[] result = new int[]{0, 0}; // [current, max]

        ItemStack plate = readPlateFromChestplate(chestplate);
        if (plate.isEmpty()) {
            return result;
        }

        result[0] = plate.getDamageValue();
        result[1] = plate.getMaxDamage();

        return result;
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        LOGGER.info("===== 玩家死亡事件开始 =====");
        LOGGER.info("玩家: {}", player.getName().getString());

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.isEmpty()) {
            LOGGER.info("玩家未穿戴胸甲");
            LOGGER.info("===== 玩家死亡事件结束 =====");
            return;
        }

        LOGGER.info("死亡时胸甲物品: {}", chestplate.getDisplayName().getString());
        LOGGER.info("死亡时胸甲有NBT: {}", chestplate.hasTag());

        ItemStack plate = readPlateFromChestplate(chestplate);
        if (!plate.isEmpty()) {
            LOGGER.info("死亡时胸甲中的插板: {}, 耐久: {}/{}",
                    plate.getDisplayName().getString(),
                    plate.getDamageValue(), plate.getMaxDamage());

            // 重新保存以确保格式正确
            savePlateToChestplate(chestplate, plate);
            LOGGER.info("已重新保存插板数据到死亡胸甲");

            player.setItemSlot(EquipmentSlot.CHEST, chestplate);
        } else {
            LOGGER.info("死亡时胸甲中没有插板");
        }

        LOGGER.info("===== 玩家死亡事件结束 =====");
    }

    private static int calculateDurabilityCost(float damage, double reduction) {
        int baseCost = 1;
        double reducedDamage = damage * reduction;
        int additionalCost = (int)(reducedDamage / 5);
        return Math.max(1, baseCost + additionalCost);
    }

    private static boolean isBulletDamage(DamageSource source) {
        try {
            String damageType = source.getMsgId();
            LOGGER.info("伤害类型字符串: {}", damageType);

            var damageTypeHolder = source.typeHolder();
            if (damageTypeHolder != null) {
                var damageTypeKey = damageTypeHolder.unwrapKey();
                if (damageTypeKey.isPresent()) {
                    ResourceLocation damageTypeId = damageTypeKey.get().location();
                    LOGGER.info("伤害类型注册名: {}", damageTypeId);

                    String namespace = damageTypeId.getNamespace();
                    String path = damageTypeId.getPath();

                    if ("tacz".equals(namespace)) {
                        boolean isTaczBullet = path.contains("bullet") || path.contains("gun");
                        LOGGER.info("TACZ伤害检测: {}, 路径: {}", isTaczBullet, path);
                        return isTaczBullet;
                    }

                    if ("superbwarfare".equals(namespace)) {
                        boolean isSuperbWarfare = path.contains("gunfire") ||
                                path.contains("laser") ||
                                path.contains("projectile") ||
                                path.contains("bullet") ||
                                path.contains("shock") ||
                                path.contains("phosphorus") ||
                                path.contains("explosion");
                        LOGGER.info("SuperbWarfare伤害检测: {}, 路径: {}", isSuperbWarfare, path);
                        return isSuperbWarfare;
                    }
                }
            }

            if (source.getDirectEntity() != null) {
                String entityType = source.getDirectEntity().getType().toString();
                LOGGER.info("直接实体类型: {}", entityType);

                if (entityType.contains("bullet") || entityType.contains("projectile") ||
                        entityType.contains("tacz") || entityType.contains("gun")) {
                    LOGGER.info("通过实体类型识别为子弹伤害");
                    return true;
                }
            }

            String sourceString = source.toString();
            LOGGER.info("伤害来源字符串: {}", sourceString);

            if (sourceString.contains("tacz") || sourceString.contains("bullet") ||
                    sourceString.contains("gun") || sourceString.contains("superbwarfare")) {
                LOGGER.info("通过字符串匹配识别为子弹伤害");
                return true;
            }

            LOGGER.info("未识别为子弹伤害");
            return false;

        } catch (Exception e) {
            LOGGER.error("检查子弹伤害时发生错误: ", e);
            return false;
        }
    }
}