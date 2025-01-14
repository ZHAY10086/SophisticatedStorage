package net.p3pp3rf1y.sophisticatedstorage.compat.sb;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.DecorationTableBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.util.DecorationHelper;

import java.util.Map;

public class SBCompat implements ICompat {
	@Override
	public void setup() {
		DecorationTableBlockEntity.registerItemDecorator(BackpackItem.class, new DecorationTableBlockEntity.IItemDecorator() {
			@Override
			public boolean supportsMaterials(ItemStack input) {
				return false;
			}

			@Override
			public boolean supportsTints(ItemStack input) {
				return true;
			}

			@Override
			public boolean supportsTopInnerTrim(ItemStack input) {
				return false;
			}

			@Override
			public ItemStack decorateWithMaterials(ItemStack input, Map<BarrelMaterial, ResourceLocation> materialsToApply) {
				return ItemStack.EMPTY;
			}

			@Override
			public DecorationTableBlockEntity.TintDecorationResult decorateWithTints(ItemStack input, int mainColorToSet, int accentColorToSet) {
				if (colorsTransparentOrSameAs(input, mainColorToSet, accentColorToSet)) {
					return DecorationTableBlockEntity.TintDecorationResult.EMPTY;
				}

				ItemStack result = input.copyWithCount(1);

				IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(result);
				int originalMainColor = backpackWrapper.getMainColor();
				int originalAccentColor = backpackWrapper.getAccentColor();

				backpackWrapper.setColors(mainColorToSet, accentColorToSet);
				return new DecorationTableBlockEntity.TintDecorationResult(result, DecorationHelper.getDyePartsNeeded(mainColorToSet, accentColorToSet, originalMainColor, originalAccentColor, 20, 4));
			}

			private boolean colorsTransparentOrSameAs(ItemStack backpack, int mainColorToSet, int accentColorToSet) {
				IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
				return (mainColorToSet == -1 || mainColorToSet == backpackWrapper.getMainColor()) && (accentColorToSet == -1 || accentColorToSet == backpackWrapper.getAccentColor());
			}
		});
	}
}
