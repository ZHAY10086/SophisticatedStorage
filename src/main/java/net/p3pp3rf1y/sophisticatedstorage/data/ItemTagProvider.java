package net.p3pp3rf1y.sophisticatedstorage.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

import static net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks.BASE_TIER_WOODEN_STORAGE_TAG;

public class ItemTagProvider extends ItemTagsProvider {
	public ItemTagProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTagProvider, @Nullable ExistingFileHelper existingFileHelper) {
		super(packOutput, lookupProvider, blockTagProvider, SophisticatedStorage.MOD_ID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.Provider pProvider) {
		tag(BASE_TIER_WOODEN_STORAGE_TAG).add(ModBlocks.BARREL_ITEM.get(), ModBlocks.CHEST_ITEM.get());
		tag(ModBlocks.ALL_STORAGE_TAG)
				.add(ModBlocks.BARREL_ITEM.get(), ModBlocks.COPPER_BARREL_ITEM.get(), ModBlocks.IRON_BARREL_ITEM.get(), ModBlocks.GOLD_BARREL_ITEM.get(), ModBlocks.DIAMOND_BARREL_ITEM.get(), ModBlocks.NETHERITE_BARREL_ITEM.get())
				.add(ModBlocks.CHEST_ITEM.get(), ModBlocks.COPPER_CHEST_ITEM.get(), ModBlocks.IRON_CHEST_ITEM.get(), ModBlocks.GOLD_CHEST_ITEM.get(), ModBlocks.DIAMOND_CHEST_ITEM.get(), ModBlocks.NETHERITE_CHEST_ITEM.get())
				.add(ModBlocks.SHULKER_BOX_ITEM.get(), ModBlocks.COPPER_SHULKER_BOX_ITEM.get(), ModBlocks.IRON_SHULKER_BOX_ITEM.get(), ModBlocks.GOLD_SHULKER_BOX_ITEM.get(), ModBlocks.DIAMOND_SHULKER_BOX_ITEM.get(), ModBlocks.NETHERITE_SHULKER_BOX_ITEM.get())
				.add(ModBlocks.LIMITED_BARREL_1_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_1_ITEM.get(), ModBlocks.LIMITED_IRON_BARREL_1_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_1_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_1_ITEM.get(), ModBlocks.LIMITED_NETHERITE_BARREL_1_ITEM.get())
				.add(ModBlocks.LIMITED_BARREL_2_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_2_ITEM.get(), ModBlocks.LIMITED_IRON_BARREL_2_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_2_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_2_ITEM.get(), ModBlocks.LIMITED_NETHERITE_BARREL_2_ITEM.get())
				.add(ModBlocks.LIMITED_BARREL_3_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_3_ITEM.get(), ModBlocks.LIMITED_IRON_BARREL_3_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_3_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_3_ITEM.get(), ModBlocks.LIMITED_NETHERITE_BARREL_3_ITEM.get())
				.add(ModBlocks.LIMITED_BARREL_4_ITEM.get(), ModBlocks.LIMITED_COPPER_BARREL_4_ITEM.get(), ModBlocks.LIMITED_IRON_BARREL_4_ITEM.get(), ModBlocks.LIMITED_GOLD_BARREL_4_ITEM.get(), ModBlocks.LIMITED_DIAMOND_BARREL_4_ITEM.get(), ModBlocks.LIMITED_NETHERITE_BARREL_4_ITEM.get());
	}
}
