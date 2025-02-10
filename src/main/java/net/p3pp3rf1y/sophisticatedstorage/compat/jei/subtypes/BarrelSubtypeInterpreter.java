package net.p3pp3rf1y.sophisticatedstorage.compat.jei.subtypes;

import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

public class BarrelSubtypeInterpreter extends PropertyBasedSubtypeInterpreter {
	public BarrelSubtypeInterpreter() {
		addOptionalProperty(WoodStorageBlockItem::getWoodType, "woodName", WoodType::name);
		addOptionalProperty(StorageBlockItem::getMainColorFromComponentHolder, "mainColor", String::valueOf);
		addOptionalProperty(StorageBlockItem::getAccentColorFromComponentHolder, "accentColor", String::valueOf);
		addProperty(BarrelBlockItem::isFlatTop, "flatTop", String::valueOf);
	}
}
