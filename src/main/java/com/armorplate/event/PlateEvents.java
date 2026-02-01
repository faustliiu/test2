package com.armorplate.event;

import com.armorplate.config.PlateConfig;
import com.armorplate.item.ArmorPlateItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

    private static final String PLATE_TAG_KEY = "ArmorPlateMod_PlateData";
    private static final String OLD_PLATE_TAG_KEY = "ArmorPlate";

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

        LOGGER.info("===== 插板伤害事件开始 ====");
        LOGGER.info("实体: {}, UUID: {}",
        event.getEntity().getName().getString(),
                currentEntity);
        LOGGER.info("伤害来源: {}, 类型: {}",
        event.getSource().getMsgId(),
                event.getSource().type().msgId());
        LOGGER.info("原始伤害值: {}", event.getAmount());

        LivingEntity livingEntity = event.getEntity();

        boolean isBulletDamage = isBulletDamage(event.getSource());
        LOGGER.info("子弹伤害检查结果: {}", isBulletDamage);

        if (!isBulletDamage) {
            LOGGER.info("不是子弹伤害，跳过处理");
            LOGGER.info("===== 插板伤害事件结束 ====");
            return;
        }

        boolean shouldProtect = shouldApplyPlateProtectionWithDGH(livingEntity);
        if (!shouldProtect) {
            LOGGER.info("不应用插板保护（没有插板或部位不受保护）");
            LOGGER.info("===== 插板伤害事件结束 ====");
            return;
        }

        ItemStack chestplate = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.isEmpty()) {
            LOGGER.info("实体未穿戴胸甲");
            LOGGER.info("===== 插板伤害事件结束 ====");
            return;
        }

        LOGGER.info("胸甲物品: {}", chestplate.getDisplayName().getString());
        LOGGER.info("胸甲有NBT: {}", chestplate.hasTag());

        ItemStack plate = readPlateFromChestplate(chestplate);

        if (plate.isEmpty()) {
            LOGGER.info("胸甲中没有插板或读取失败");
            LOGGER.info("===== 插板伤害事件结束 ====");
            return;
        }

        LOGGER.info("从NBT读取插板: {}, 数量: {}",
        plate.getDisplayName().getString(), plate.getCount());

        if (!(plate.getItem() instanceof ArmorPlateItem armorPlate)) {
            LOGGER.info("插板物品不是ArmorPlateItem");
            LOGGER.info("===== 插板伤害事件结束 ====");
            return;
        }

        if (armorPlate.isBroken(plate)) {
            LOGGER.info("插板已损坏");
            LOGGER.info("===== 插板伤害事件结束 ====");
            return;
        }

        float damage = event.getAmount();
        DamageSource source = event.getSource();

        processBulletDamage(event, livingEntity, chestplate, plate, armorPlate, damage, source);
    }

    private static boolean shouldApplyPlateProtectionWithDGH(LivingEntity livingEntity) {
        if (com.armorplate.dgh.DGHIntegrationManager.isIntegrationEnabled()) {
            com.armorplate.dgh.DGHIntegrationManager.HitData hitData =
                    com.armorplate.dgh.DGHIntegrationManager.getRecentHitData(livingEntity);

            if (hitData != null) {
                LOGGER.info("DGH命中数据: 部位={}({}), 有防护={}",
                hitData.bodyPartName, hitData.bodyPartId, hitData.hasProtection);

                return hitData.hasProtection;
            } else {
                boolean defaultProtection = com.armorplate.dgh.DGHIntegrationManager.shouldApplyDefaultProtection(livingEntity);
                if (defaultProtection) {
                    LOGGER.info("使用DGH默认保护逻辑（有子弹标记但无命中数据）");
                    return true;
                }

                LOGGER.info("没有DGH命中数据，使用原始逻辑");
                return hasPlateInChest(livingEntity);
            }
        } else {
            return hasPlateInChest(livingEntity);
        }
    }


    public static boolean hasPlateInChest(LivingEntity livingEntity) {
        ItemStack chestplate = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.isEmpty()) {
            return false;
        }
        return !readPlateFromChestplate(chestplate).isEmpty();
    }

    private static void processBulletDamage(LivingHurtEvent event, LivingEntity livingEntity, ItemStack chestplate,
                                            ItemStack plate, ArmorPlateItem armorPlate, float damage, DamageSource source) {

        double damageReduction = armorPlate.getEffectiveDamageReduction(plate);
        double damageThreshold = armorPlate.getEffectiveDamageThreshold(plate);

        LOGGER.info("插板属性 - 伤害减免: {}%, 伤害阈值: {}",
        String.format("%.1f", damageReduction * 100),
        String.format("%.1f", damageThreshold));

        LOGGER.info("原始伤害: {}", damage);

        applyDamageReduction(event, livingEntity, damage, damageThreshold, damageReduction);

        int durabilityCost = calculateDurabilityCost(damage, damageThreshold, damageReduction);

        int currentDamage = plate.getDamageValue();
        int maxDamage = plate.getMaxDamage();
        int remainingDurability = maxDamage - currentDamage;

        LOGGER.info("耐久状态: {}/{}, 剩余耐久: {}",
        currentDamage, maxDamage, remainingDurability);

        if (durabilityCost > remainingDurability) {
            durabilityCost = remainingDurability;
            LOGGER.info("调整耐久消耗为剩余耐久: {}", durabilityCost);
        }

        LOGGER.info("耐久消耗计算: 原始伤害 {}, 阈值 {}, 减伤系数 {}, 耐久成本 {}",
        damage, damageThreshold, damageReduction, durabilityCost);

        armorPlate.damagePlate(plate, durabilityCost);

        LOGGER.info("更新后插板: {}, 耐久: {}/{}",
        plate.getDisplayName().getString(),
                plate.getDamageValue(), plate.getMaxDamage());

        if (armorPlate.isBroken(plate)) {
            LOGGER.info("插板已损坏！");

            savePlateToChestplate(chestplate, plate);

            livingEntity.setItemSlot(EquipmentSlot.CHEST, chestplate);

            AttributeModifiers.updatePlateAttributes(livingEntity, chestplate);

            if (livingEntity instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("§c防弹插板已损坏！").withStyle(ChatFormatting.RED),
                        true
                );
            }

            SoundSource soundSource = (livingEntity instanceof Player) ? SoundSource.PLAYERS : SoundSource.HOSTILE;
            livingEntity.level().playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(),
                    SoundEvents.ITEM_BREAK,
                    soundSource, 0.5F, 1.0F);

            LOGGER.info("===== 插板损坏，事件结束 ====");
            return;
        }

        savePlateToChestplate(chestplate, plate);
        LOGGER.info("已保存更新后的插板NBT");

        livingEntity.setItemSlot(EquipmentSlot.CHEST, chestplate);

        LOGGER.info("已将更新后的胸甲设置回实体装备槽");

        LOGGER.info("===== 插板伤害处理完成 ====");
    }

    private static void applyDamageReduction(LivingHurtEvent event, LivingEntity livingEntity,
                                             float damage, double damageThreshold, double damageReduction) {

        LOGGER.info("应用减伤逻辑 - 伤害: {}, 阈值: {}, 减伤系数: {}",
        damage, damageThreshold, damageReduction);

        if (damage <= damageThreshold) {
            LOGGER.info("伤害 {} 低于阈值 {}，完全抵消", damage, damageThreshold);
            event.setCanceled(true);
            SoundSource soundSource = (livingEntity instanceof Player) ? SoundSource.PLAYERS : SoundSource.HOSTILE;
            livingEntity.level().playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(),
                    SoundEvents.SHIELD_BLOCK,
                    soundSource, 0.5F, 1.0F);
            LOGGER.info("===== 伤害完全抵消 ====");
            return;
        }

        float newDamage = (damage - (float)damageThreshold) * (float)(1.0 - damageReduction);

        if (newDamage < 0) {
            newDamage = 0;
        }

        LOGGER.info("减伤计算: ({} - {}) * (1 - {}) = {}",
        damage, damageThreshold, damageReduction, newDamage);

        event.setAmount(newDamage);
        LOGGER.info("设置新伤害值: {} (减少了{}点)", newDamage, damage - newDamage);

        SoundSource soundSource = (livingEntity instanceof Player) ? SoundSource.PLAYERS : SoundSource.HOSTILE;
        livingEntity.level().playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(),
                SoundEvents.ARMOR_EQUIP_IRON,
                soundSource, 0.3F, 0.8F);
    }


    public static void savePlateToChestplate(ItemStack chestplate, ItemStack plate) {
        try {
            CompoundTag chestplateTag = chestplate.getOrCreateTag();

            LOGGER.info("保存插板 - 胸甲当前NBT: {}", chestplateTag);
            LOGGER.info("保存插板 - 插板物品: {}, 耐久: {}/{}",
            plate.getDisplayName().getString(),
                    plate.getDamageValue(), plate.getMaxDamage());

            if (plate.isEmpty()) {
                LOGGER.info("插板为空，移除插板标签");

                if (chestplateTag.contains(PLATE_TAG_KEY)) {
                    chestplateTag.remove(PLATE_TAG_KEY);
                    LOGGER.info("已移除 {} 标签", PLATE_TAG_KEY);
                }

                if (chestplateTag.contains(OLD_PLATE_TAG_KEY)) {
                    chestplateTag.remove(OLD_PLATE_TAG_KEY);
                    LOGGER.info("已移除旧标签 {}", OLD_PLATE_TAG_KEY);
                }

                if (chestplateTag.isEmpty()) {
                    chestplate.setTag(null);
                } else {
                    chestplate.setTag(chestplateTag);
                }

                LOGGER.info("保存完成 - 胸甲NBT: {}", chestplate.getTag());
                return;
            }

            if (!(plate.getItem() instanceof ArmorPlateItem)) {
                LOGGER.error("尝试保存非ArmorPlateItem的物品: {}", plate.getItem().getDescriptionId());

                if (chestplateTag.contains(PLATE_TAG_KEY)) {
                    chestplateTag.remove(PLATE_TAG_KEY);
                }
                if (chestplateTag.contains(OLD_PLATE_TAG_KEY)) {
                    chestplateTag.remove(OLD_PLATE_TAG_KEY);
                }

                chestplate.setTag(chestplateTag);
                return;
            }

            CompoundTag plateData = new CompoundTag();

            CompoundTag itemTag = new CompoundTag();
            plate.save(itemTag);
            plateData.put("Item", itemTag);

            plateData.putInt("Durability", plate.getDamageValue());
            plateData.putInt("MaxDurability", plate.getMaxDamage());

            plateData.putInt("Version", 3); // 更新版本号

            plateData.putLong("SaveTime", System.currentTimeMillis());

            plateData.putString("Mod", "armorplate");
            plateData.putString("ModId", "armorplate");

            chestplateTag.put(PLATE_TAG_KEY, plateData);

            if (chestplateTag.contains(OLD_PLATE_TAG_KEY)) {
                chestplateTag.remove(OLD_PLATE_TAG_KEY);
                LOGGER.info("已移除旧格式标签 {}", OLD_PLATE_TAG_KEY);
            }

            chestplate.setTag(chestplateTag);

            if (chestplateTag.contains(PLATE_TAG_KEY)) {
                Tag savedTag = chestplateTag.get(PLATE_TAG_KEY);
                if (savedTag instanceof CompoundTag) {
                    CompoundTag verificationTag = (CompoundTag) savedTag;

                    // 检查保存的内容是否正确
                    if (!verificationTag.isEmpty() && verificationTag.contains("Item")) {
                        LOGGER.info("插板数据保存成功，键名: {}", PLATE_TAG_KEY);
                        LOGGER.info("保存的格式检查 - 包含Item标签: {}", verificationTag.contains("Item"));
                        LOGGER.info("保存的格式检查 - 包含Durability: {}", verificationTag.contains("Durability"));
                        LOGGER.info("保存的格式检查 - 包含MaxDurability: {}", verificationTag.contains("MaxDurability"));
                        LOGGER.info("保存的格式检查 - 包含Mod标识: {}", verificationTag.contains("Mod"));

                        // 验证Item标签是否可以正确读取
                        try {
                            ItemStack verifyPlate = ItemStack.of(verificationTag.getCompound("Item"));
                            if (!verifyPlate.isEmpty()) {
                                LOGGER.info("验证成功 - 可以读取插板: {}, 耐久: {}/{}",
                                verifyPlate.getDisplayName().getString(),
                                        verifyPlate.getDamageValue(), verifyPlate.getMaxDamage());
                            } else {
                                LOGGER.error("验证失败 - 从Item标签读取的插板为空");
                            }
                        } catch (Exception e) {
                            LOGGER.error("验证失败 - 无法从Item标签读取插板: ", e);
                        }
                    } else {
                        LOGGER.error("插板NBT验证失败：保存的NBT为空或缺少Item标签");
                    }
                } else {
                    LOGGER.error("插板NBT验证失败：保存的标签不是CompoundTag，而是{}",
                    savedTag.getType().getName());
                }
            } else {
                LOGGER.error("插板数据保存失败：{} 标签不存在", PLATE_TAG_KEY);
            }

        } catch (Exception e) {
            LOGGER.error("保存插板到胸甲时出错: ", e);
        }
    }


    public static ItemStack readPlateFromChestplate(ItemStack chestplate) {
        if (!chestplate.hasTag()) {
            return ItemStack.EMPTY;
        }

        CompoundTag chestplateTag = chestplate.getTag();

        if (chestplateTag.contains(PLATE_TAG_KEY)) {
            Tag armorPlateTagRaw = chestplateTag.get(PLATE_TAG_KEY);

            if (!(armorPlateTagRaw instanceof CompoundTag)) {
                LOGGER.error("{} 标签不是CompoundTag，而是{}，数据已损坏。尝试修复。",
                PLATE_TAG_KEY, armorPlateTagRaw.getType().getName());

                chestplateTag.remove(PLATE_TAG_KEY);
                chestplate.setTag(chestplateTag);
                return ItemStack.EMPTY;
            }

            CompoundTag plateTag = (CompoundTag) armorPlateTagRaw;

            if (plateTag.isEmpty()) {
                LOGGER.error("{} 标签为空！可能是保存时出错。", PLATE_TAG_KEY);
                return ItemStack.EMPTY;
            }

            LOGGER.info("读取到 {} 标签，键: {}", PLATE_TAG_KEY, plateTag.getAllKeys());

            return readPlateFromTag(chestplate, chestplateTag, plateTag);
        }

        if (chestplateTag.contains(OLD_PLATE_TAG_KEY)) {
            LOGGER.info("检测到旧格式插板数据，进行迁移...");

            Tag oldTagRaw = chestplateTag.get(OLD_PLATE_TAG_KEY);

            if (oldTagRaw instanceof CompoundTag) {
                CompoundTag oldPlateTag = (CompoundTag) oldTagRaw;

                ItemStack plate = readPlateFromTag(chestplate, chestplateTag, oldPlateTag);

                if (!plate.isEmpty()) {
                    savePlateToChestplate(chestplate, plate);
                    LOGGER.info("成功从旧格式迁移到新格式");
                }

                return plate;
            } else {
                LOGGER.error("旧格式标签类型错误: {}", oldTagRaw.getType().getName());
                chestplateTag.remove(OLD_PLATE_TAG_KEY);
                chestplate.setTag(chestplateTag);
            }
        }

        return ItemStack.EMPTY;
    }


    private static ItemStack readPlateFromTag(ItemStack chestplate, CompoundTag chestplateTag, CompoundTag plateTag) {
        if (plateTag.contains("id") && !plateTag.contains("Item")) {
            LOGGER.info("检测到旧物品格式，进行迁移...");

            try {
                String plateId = plateTag.getString("id");
                int durability = 0;
                int maxDurability = 0;

                if (plateTag.contains("Damage")) {
                    durability = plateTag.getInt("Damage");
                } else if (plateTag.contains("Durability")) {
                    durability = plateTag.getInt("Durability");
                }

                if (plateTag.contains("MaxDurability")) {
                    maxDurability = plateTag.getInt("MaxDurability");
                }

                LOGGER.info("旧格式数据 - ID: {}, 耐久: {}/{}", plateId, durability, maxDurability);

                CompoundTag newPlateTag = new CompoundTag();
                CompoundTag itemTag = new CompoundTag();

                itemTag.putString("id", plateId);
                itemTag.putInt("Count", 1);

                if (plateTag.contains("Damage")) {
                    itemTag.putInt("Damage", plateTag.getInt("Damage"));
                }

                if (plateTag.contains("tag")) {
                    itemTag.put("tag", plateTag.getCompound("tag").copy());
                }

                newPlateTag.put("Item", itemTag);
                newPlateTag.putInt("Durability", durability);
                newPlateTag.putInt("MaxDurability", maxDurability);
                newPlateTag.putString("Mod", "armorplate");

                chestplateTag.put(PLATE_TAG_KEY, newPlateTag);
                chestplate.setTag(chestplateTag);

                LOGGER.info("成功迁移旧物品格式到新格式");

                plateTag = newPlateTag;
            } catch (Exception e) {
                LOGGER.error("迁移旧物品格式时出错: ", e);
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

        LOGGER.warn("插板标签格式无法识别，标签内容: {}", plateTag);
        return ItemStack.EMPTY;
    }

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
        //处理玩家发送消息
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        LOGGER.info("===== 玩家死亡事件开始 ====");
        LOGGER.info("玩家: {}", player.getName().getString());

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.isEmpty()) {
            LOGGER.info("玩家未穿戴胸甲");
            LOGGER.info("===== 玩家死亡事件结束 ====");
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

        LOGGER.info("===== 玩家死亡事件结束 ====");
    }

    private static int calculateDurabilityCost(float originalDamage, double threshold, double reduction) {
        int baseCost = PlateConfig.BASE_DURABILITY_COST.get();
        double damageToDurabilityRatio = PlateConfig.DAMAGE_TO_DURABILITY_RATIO.get();
        int maxCost = PlateConfig.MAX_DURABILITY_COST_PER_HIT.get();

        double blockedDamage;

        if (originalDamage <= threshold) {
            // 低于阈值：完全抵挡，抵挡伤害 = 原始伤害
            blockedDamage = originalDamage;
            LOGGER.info("抵挡伤害计算（低于阈值）: 原始伤害={}, 抵挡伤害={}",
            originalDamage, blockedDamage);
        } else {
            // 高于阈值：抵挡伤害 = 阈值 + (超出阈值的伤害 * 减伤系数)
            double overThresholdDamage = originalDamage - threshold;
            double damageBlockedByReduction = overThresholdDamage * reduction;
            blockedDamage = threshold + damageBlockedByReduction;

            LOGGER.info("抵挡伤害计算（高于阈值）: 原始伤害={}, 阈值={}, 超出部分={}, 减伤系数={}, 减伤抵挡={}, 总抵挡={}",
            originalDamage, threshold, overThresholdDamage,
                    reduction, damageBlockedByReduction, blockedDamage);
        }

        // 耐久消耗 = 基础消耗 + 抵挡伤害 * 转化比例
        int additionalCost = (int)(blockedDamage * damageToDurabilityRatio);
        int totalCost = baseCost + additionalCost;

        LOGGER.info("耐久消耗: 基础={}, 抵挡伤害={}, 转化比例={}, 额外={}, 总计={}",
        baseCost, blockedDamage, damageToDurabilityRatio, additionalCost, totalCost);

        return Math.min(totalCost, maxCost);
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