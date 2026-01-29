package com.armorplate.capability;

import com.armorplate.item.ArmorPlateItem;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlateCapability {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Capability<IPlateCapability> PLATE_CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static class PlateHandler implements IPlateCapability, ICapabilityProvider, INBTSerializable<CompoundTag> {
        private ItemStack plate = ItemStack.EMPTY;
        private final LazyOptional<IPlateCapability> holder = LazyOptional.of(() -> this);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == PLATE_CAPABILITY) {
                return holder.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public boolean hasPlate() {
            return !plate.isEmpty();
        }

        @Override
        public ItemStack getPlate() {
            return plate.copy();
        }

        @Override
        public void setPlate(ItemStack plate) {
            LOGGER.info("PlateCapability.setPlate: 设置插板 {}", plate);
            this.plate = plate.copy();
            this.plate.setCount(1);
        }

        @Override
        public void removePlate() {
            LOGGER.info("PlateCapability.removePlate: 移除插板");
            this.plate = ItemStack.EMPTY;
        }

        @Override
        public double getArmorBonus() {
            if (hasPlate() && plate.getItem() instanceof ArmorPlateItem armorPlate) {
                return armorPlate.getEffectiveArmorBonus(plate);
            }
            return 0;
        }

        @Override
        public double getToughnessBonus() {
            if (hasPlate() && plate.getItem() instanceof ArmorPlateItem armorPlate) {
                return armorPlate.getEffectiveToughnessBonus(plate);
            }
            return 0;
        }

        @Override
        public double getDamageReduction() {
            if (hasPlate() && plate.getItem() instanceof ArmorPlateItem armorPlate) {
                return armorPlate.getEffectiveDamageReduction(plate);
            }
            return 0;
        }

        @Override
        public double getDamageThreshold() {
            if (hasPlate() && plate.getItem() instanceof ArmorPlateItem armorPlate) {
                return armorPlate.getEffectiveDamageThreshold(plate);
            }
            return 0;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            if (hasPlate()) {
                tag.put("Plate", plate.save(new CompoundTag()));
                LOGGER.info("PlateCapability.serializeNBT: 保存插板到NBT");
            } else {
                LOGGER.info("PlateCapability.serializeNBT: 没有插板可保存");
            }
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            if (tag.contains("Plate")) {
                plate = ItemStack.of(tag.getCompound("Plate"));
                LOGGER.info("PlateCapability.deserializeNBT: 从NBT加载插板 {}", plate);
            } else {
                plate = ItemStack.EMPTY;
                LOGGER.info("PlateCapability.deserializeNBT: NBT中没有插板");
            }
        }
    }

    public static ICapabilityProvider createProvider() {
        return new PlateHandler();
    }
}