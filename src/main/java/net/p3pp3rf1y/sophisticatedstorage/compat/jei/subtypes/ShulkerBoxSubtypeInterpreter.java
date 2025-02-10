package net.p3pp3rf1y.sophisticatedstorage.compat.jei.subtypes;

import net.p3pp3rf1y.sophisticatedcore.compat.jei.subtypes.PropertyBasedSubtypeInterpreter;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

public class ShulkerBoxSubtypeInterpreter extends PropertyBasedSubtypeInterpreter {
	public ShulkerBoxSubtypeInterpreter() {
		addOptionalProperty(StorageBlockItem::getMainColorFromComponentHolder, "mainColor", String::valueOf);
		addOptionalProperty(StorageBlockItem::getAccentColorFromComponentHolder, "accentColor", String::valueOf);
	}
}
