package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG;

public class ItemTagProvider extends ItemTagsProvider {
	public ItemTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTagProvider, @Nullable ExistingFileHelper existingFileHelper) {
		super(packOutput, lookupProvider, blockTagProvider, SophisticatedStorage.MOD_ID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider pProvider) {
		tag(BASE_TIER_WOODEN_STORAGE_TAG).add(ModBlocks.BARREL_ITEM.get(), ModBlocks.CHEST_ITEM.get());

		IntrinsicTagAppender<Item> allStorageTag = tag(ModBlocks.ALL_STORAGE_TAG);
		ForgeRegistries.ITEMS.getEntries().stream().map(Map.Entry::getValue).filter(item -> item instanceof StorageBlockItem).forEach(allStorageTag::add);

		IntrinsicTagAppender<Item> upgradeTag = tag(ModItems.STORAGE_UPGRADE_TAG);
		ForgeRegistries.ITEMS.getEntries().stream()
				.filter(entry -> entry.getKey().location().getNamespace().equals(SophisticatedStorage.MOD_ID) && entry.getValue() instanceof UpgradeItemBase)
				.map(Map.Entry::getValue).forEach(upgradeTag::add);
	}
}
