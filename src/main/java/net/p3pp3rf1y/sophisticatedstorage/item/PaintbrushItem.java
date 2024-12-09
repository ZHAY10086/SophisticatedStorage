package net.p3pp3rf1y.sophisticatedstorage.item;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.*;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageTranslationHelper;
import net.p3pp3rf1y.sophisticatedstorage.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedstorage.util.DecorationHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class PaintbrushItem extends ItemBase {
	public static final Codec<Map<ResourceLocation, Integer>> REMAINING_PARTS_CODEC =
			Codec.unboundedMap(ResourceLocation.CODEC, ExtraCodecs.POSITIVE_INT);

	public static final StreamCodec<FriendlyByteBuf, Map<ResourceLocation, Integer>> REMAINING_PARTS_STREAM_CODEC =
			StreamCodec.of((buf, map) -> buf.writeMap(map, ResourceLocation.STREAM_CODEC, ByteBufCodecs.INT),
					buf -> buf.readMap(ResourceLocation.STREAM_CODEC, ByteBufCodecs.INT));

	public PaintbrushItem() {
		super(new Properties().stacksTo(1));
	}

	public static void setBarrelMaterials(ItemStack paintbrush, Map<BarrelMaterial, ResourceLocation> materials) {
		paintbrush.set(ModDataComponents.BARREL_MATERIALS, Map.copyOf(materials));
		resetMainColor(paintbrush);
		resetAccentColor(paintbrush);
	}

	public static Optional<ItemRequirements> getItemRequirements(ItemStack paintbrush, Player player, Level level, BlockPos lookingAtPos) {
		Map<BarrelMaterial, ResourceLocation> materialsToApply = new HashMap<>(getBarrelMaterials(paintbrush));
		BlockEntity be = level.getBlockEntity(lookingAtPos);
		if (be == null) {
			return Optional.empty();
		}

		if (!materialsToApply.isEmpty()) {
			return getMaterialItemRequirements(paintbrush, player, be, materialsToApply);
		} else {
			return getDyeItemRequirements(paintbrush, player, be);
		}
	}

	private static Optional<ItemRequirements> getMaterialItemRequirements(ItemStack paintbrush, Player player, BlockEntity be, Map<BarrelMaterial, ResourceLocation> materialsToApply) {
		Map<ResourceLocation, Integer> allPartsNeeded = new HashMap<>();
		if (be instanceof StorageBlockEntity storageBe) {
			allPartsNeeded = getStorageMaterialPartsNeeded(materialsToApply, storageBe);
		} else if (be instanceof ControllerBlockEntity controllerBe) {
			for (BlockPos storagePosition : controllerBe.getStoragePositions()) {
				addStorageMaterialPartsNeeded(materialsToApply, controllerBe, storagePosition, allPartsNeeded);
			}
		}

		if (allPartsNeeded.isEmpty()) {
			return Optional.empty();
		}
		Map<ResourceLocation, Integer> remainingParts = getRemainingParts(paintbrush);
		DecorationHelper.ConsumptionResult result = DecorationHelper.consumeMaterialPartsNeeded(allPartsNeeded, remainingParts, InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player), true);

		List<ItemStack> itemsPresent = new ArrayList<>();
		List<ItemStack> itemsMissing = new ArrayList<>();

		for (Map.Entry<ResourceLocation, Integer> entry : allPartsNeeded.entrySet()) {
			ResourceLocation part = entry.getKey();
			int count = Math.ceilDiv(entry.getValue() - remainingParts.getOrDefault(part, 0), DecorationHelper.BLOCK_TOTAL_PARTS);
			int missing = Math.ceilDiv(result.missingParts().getOrDefault(part, 0), DecorationHelper.BLOCK_TOTAL_PARTS);
			int present = count - missing;
			BuiltInRegistries.ITEM.getOptional(part).ifPresent(item -> {
				if (missing > 0) {
					itemsMissing.add(new ItemStack(item, missing));
				}
				if (present > 0) {
					itemsPresent.add(new ItemStack(item, present));
				}
			});
		}

		return Optional.of(new ItemRequirements(itemsPresent, itemsMissing));
	}

	private static void addStorageMaterialPartsNeeded(Map<BarrelMaterial, ResourceLocation> materialsToApply, ControllerBlockEntity controllerBe, BlockPos storagePosition, Map<ResourceLocation, Integer> allPartsNeeded) {
		WorldHelper.getBlockEntity(controllerBe.getLevel(), storagePosition, StorageBlockEntity.class).ifPresent(storageBe -> {
			Map<ResourceLocation, Integer> storagePartsNeeded = getStorageMaterialPartsNeeded(materialsToApply, storageBe);
			storagePartsNeeded.forEach((part, count) -> allPartsNeeded.merge(part, count, Integer::sum));
		});
	}

	private static Optional<ItemRequirements> getDyeItemRequirements(ItemStack paintbrush, Player player, BlockEntity be) {
		int mainColorToSet = getMainColor(paintbrush);
		int accentColorToSet = getAccentColor(paintbrush);

		Map<TagKey<Item>, Integer> allPartsNeeded = new HashMap<>();
		if (be instanceof StorageBlockEntity storageBe) {
			allPartsNeeded = getStorageDyePartsNeeded(mainColorToSet, accentColorToSet, storageBe);
		} else if (be instanceof ControllerBlockEntity controllerBe) {
			for (BlockPos storagePosition : controllerBe.getStoragePositions()) {
				addStorageDyePartsNeeded(mainColorToSet, accentColorToSet, controllerBe, storagePosition, allPartsNeeded);
			}
		}

		if (allPartsNeeded.isEmpty()) {
			return Optional.empty();
		}
		Map<ResourceLocation, Integer> remainingParts = getRemainingParts(paintbrush);
		DecorationHelper.ConsumptionResult result = DecorationHelper.consumeDyePartsNeeded(allPartsNeeded, InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player), remainingParts, true);

		return compileDyeItemRequirements(allPartsNeeded, remainingParts, result);
	}

	private static void addStorageDyePartsNeeded(int mainColorToSet, int accentColorToSet, ControllerBlockEntity controllerBe, BlockPos storagePosition, Map<TagKey<Item>, Integer> allPartsNeeded) {
		WorldHelper.getBlockEntity(controllerBe.getLevel(), storagePosition, StorageBlockEntity.class).ifPresent(storageBe -> {
			Map<TagKey<Item>, Integer> storagePartsNeeded = getStorageDyePartsNeeded(mainColorToSet, accentColorToSet, storageBe);
			storagePartsNeeded.forEach((part, count) -> allPartsNeeded.merge(part, count, Integer::sum));
		});
	}

	private static Map<TagKey<Item>, Integer> getStorageDyePartsNeeded(int mainColorToSet, int accentColorToSet, StorageBlockEntity storageBe) {
		StorageWrapper storageWrapper = storageBe.getStorageWrapper();
		return DecorationHelper.getDyePartsNeeded(mainColorToSet, accentColorToSet, storageWrapper.getMainColor(), storageWrapper.getAccentColor());
	}



	private static @NotNull Optional<ItemRequirements> compileDyeItemRequirements(Map<TagKey<Item>, Integer> allPartsNeeded, Map<ResourceLocation, Integer> remainingParts, DecorationHelper.ConsumptionResult result) {
		List<ItemStack> itemsPresent = new ArrayList<>();
		List<ItemStack> itemsMissing = new ArrayList<>();

		for (Map.Entry<TagKey<Item>, Integer> entry : allPartsNeeded.entrySet()) {
			TagKey<Item> part = entry.getKey();
			int count = Math.ceilDiv(entry.getValue() - remainingParts.getOrDefault(part.location(), 0), DecorationHelper.BLOCK_TOTAL_PARTS);
			int missing = Math.ceilDiv(result.missingParts().getOrDefault(part.location(), 0), DecorationHelper.BLOCK_TOTAL_PARTS);
			int present = count - missing;

			Item dyeItem;
			if (part == Tags.Items.DYES_RED) {
				dyeItem = Items.RED_DYE;
			} else if (part == Tags.Items.DYES_GREEN) {
				dyeItem = Items.GREEN_DYE;
			} else if (part == Tags.Items.DYES_BLUE) {
				dyeItem = Items.BLUE_DYE;
			} else {
				continue;
			}

			if (missing > 0) {
				itemsMissing.add(new ItemStack(dyeItem, missing));
			}
			if (present > 0) {
				itemsPresent.add(new ItemStack(dyeItem, present));
			}
		}

		return Optional.of(new ItemRequirements(itemsPresent, itemsMissing));
	}

	private static Map<ResourceLocation, Integer> getStorageMaterialPartsNeeded(Map<BarrelMaterial, ResourceLocation> materialsToApply, StorageBlockEntity storageBe) {
		if (storageBe instanceof BarrelBlockEntity barrelBe) {
			Map<BarrelMaterial, ResourceLocation> originalMaterials = new HashMap<>(barrelBe.getMaterials());
			BarrelBlockItem.uncompactMaterials(originalMaterials);
			return DecorationHelper.getMaterialPartsNeeded(originalMaterials, materialsToApply);
		}
		return Collections.emptyMap();
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack paintbrush, UseOnContext context) {
		if (!hasMainColor(paintbrush) && !hasAccentColor(paintbrush) && !hasBarrelMaterials(paintbrush)) {
			return InteractionResult.PASS;
		}

		Level level = context.getLevel();
		BlockEntity be = level.getBlockEntity(context.getClickedPos());
		if (be instanceof StorageBlockEntity storageBe) {
			if (!level.isClientSide()) {
				paintStorage(context.getPlayer(), paintbrush, storageBe, 1);
			}
			return InteractionResult.SUCCESS;
		} else if (be instanceof ControllerBlockEntity controllerBe) {
			if (!level.isClientSide()) {
				paintConnectedStorages(context.getPlayer(), level, paintbrush, controllerBe);
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private void paintConnectedStorages(@Nullable Player player, Level level, ItemStack paintbrush, ControllerBlockEntity controllerBe) {
		if (player == null) {
			return;
		}

		for (BlockPos pos : controllerBe.getStoragePositions()) {
			WorldHelper.getBlockEntity(level, pos, StorageBlockEntity.class).ifPresent(storageBe -> paintStorage(player, paintbrush, storageBe, 0.6f));
		}
	}

	private static void paintStorage(@Nullable Player player, ItemStack paintbrush, StorageBlockEntity storageBe, float soundVolume) {
		if (player == null) {
			return;
		}
		List<IItemHandler> itemHandlers = InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player);
		Map<ResourceLocation, Integer> remainingParts = new HashMap<>(getRemainingParts(paintbrush));
		if (hasBarrelMaterials(paintbrush)) {
			if (!(storageBe instanceof BarrelBlockEntity barrelBe)) {
				return;
			}

			Map<BarrelMaterial, ResourceLocation> originalMaterials = new HashMap<>(barrelBe.getMaterials());
			Map<BarrelMaterial, ResourceLocation> materialsToApply = new HashMap<>(getBarrelMaterials(paintbrush));
			if (originalMaterials.equals(materialsToApply)) {
				return;
			}

			BarrelBlockItem.uncompactMaterials(originalMaterials);

			if (!DecorationHelper.consumeMaterials(remainingParts, itemHandlers, originalMaterials, materialsToApply, true)) {
				return;
			}

			DecorationHelper.consumeMaterials(remainingParts, itemHandlers, originalMaterials, materialsToApply, false);
			setRemainingParts(paintbrush, remainingParts);

			barrelBe.getStorageWrapper().setMainColor(-1);
			barrelBe.getStorageWrapper().setAccentColor(-1);
			BarrelBlockItem.compactMaterials(materialsToApply);
			barrelBe.setMaterials(materialsToApply);

			playSoundAndParticles(player.level(), storageBe.getBlockPos(), storageBe, soundVolume);

			WorldHelper.notifyBlockUpdate(storageBe);
		} else {
			StorageWrapper storageWrapper = storageBe.getStorageWrapper();
			int mainColorToSet = getMainColor(paintbrush);
			int accentColorToSet = getAccentColor(paintbrush);
			if (storageBe instanceof ChestBlockEntity chestBe) {
				storageWrapper = chestBe.getMainStorageWrapper();
				storageBe = chestBe.getMainChestBlockEntity();
			}

			int originalMainColor = storageWrapper.getMainColor();
			int originalAccentColor = storageWrapper.getAccentColor();

			if (originalMainColor == mainColorToSet && originalAccentColor == accentColorToSet) {
				return;
			}

			if (!DecorationHelper.consumeDyes(mainColorToSet, accentColorToSet, remainingParts, itemHandlers, originalMainColor, originalAccentColor, true)) {
				return;
			}

			if (hasMainColor(paintbrush)) {
				storageWrapper.setMainColor(mainColorToSet);
			}
			if (hasAccentColor(paintbrush)) {
				storageWrapper.setAccentColor(accentColorToSet);
			}

			if (storageBe instanceof BarrelBlockEntity barrelBe) {
				barrelBe.setMaterials(Collections.emptyMap());
			}

			DecorationHelper.consumeDyes(mainColorToSet, accentColorToSet, remainingParts, itemHandlers, originalMainColor, originalAccentColor, false);
			setRemainingParts(paintbrush, remainingParts);

			playSoundAndParticles(player.level(), storageBe.getBlockPos(), storageBe, soundVolume);

			WorldHelper.notifyBlockUpdate(storageBe);
		}
	}

	private static void playSoundAndParticles(Level level, BlockPos pos, StorageBlockEntity storageBe, float soundVolume) {
		BlockState state = storageBe.getBlockState();
		level.playSound(null, pos, state.getSoundType(level, pos, null).getPlaceSound(), SoundSource.BLOCKS, soundVolume, 1);

		if (state.getBlock() instanceof StorageBlockBase storageBlock && level instanceof ServerLevel serverLevel) {
			Direction facing = storageBlock.getFacing(state);
			double x = pos.getX() + 0.5D + facing.getStepX() * 0.6D;
			double y = pos.getY() + 0.5D + facing.getStepY() * 0.6D;
			double z = pos.getZ() + 0.5D + facing.getStepZ() * 0.6D;
			double xOffset;
			double yOffset;
			double zOffset;
			if (facing.getAxis().isVertical()) {
				xOffset = 0.4D;
				yOffset = 0.1D;
				zOffset = 0.4D;
			} else {
				xOffset = 0.1D + facing.getStepZ() * 0.3D;
				yOffset = 0.4D;
				zOffset = 0.1D + facing.getStepX() * 0.3D;
			}

			serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 4, xOffset, yOffset, zOffset, 1f);
		}
	}

	private static void resetBarrelMaterials(ItemStack paintbrush) {
		paintbrush.remove(ModDataComponents.BARREL_MATERIALS);
	}

	public static void resetMainColor(ItemStack paintbrush) {
		paintbrush.remove(ModCoreDataComponents.MAIN_COLOR);
	}

	public static void resetAccentColor(ItemStack paintbrush) {
		paintbrush.remove(ModCoreDataComponents.ACCENT_COLOR);
	}

	public static void setMainColor(ItemStack paintbrush, int mainColor) {
		paintbrush.set(ModCoreDataComponents.MAIN_COLOR, mainColor);
		resetBarrelMaterials(paintbrush);
	}

	public static void setAccentColor(ItemStack paintbrush, int secondaryColor) {
		paintbrush.set(ModCoreDataComponents.ACCENT_COLOR, secondaryColor);
		resetBarrelMaterials(paintbrush);
	}

	public static void setRemainingParts(ItemStack paintbrush, Map<ResourceLocation, Integer> remainingParts) {
		paintbrush.set(ModDataComponents.REMAINING_PARTS, remainingParts);
	}

	public static Map<ResourceLocation, Integer> getRemainingParts(ItemStack paintbrush) {
		return paintbrush.getOrDefault(ModDataComponents.REMAINING_PARTS, Collections.emptyMap());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, context, tooltip, tooltipFlag);

		tooltip.addAll(StorageTranslationHelper.INSTANCE.getTranslatedLines(StorageTranslationHelper.INSTANCE.translItemTooltip(stack.getItem()), null, ChatFormatting.DARK_GRAY));

		if (hasBarrelMaterials(stack)) {
			tooltip.add(Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip("paintbrush") + ".materials").withStyle(ChatFormatting.GRAY));
			Map<BarrelMaterial, ResourceLocation> barrelMaterials = getBarrelMaterials(stack);
			barrelMaterials.forEach((barrelMaterial, blockName) -> {
				BuiltInRegistries.BLOCK.getOptional(blockName).ifPresent(block -> {
					tooltip.add(
							Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip("paintbrush") + ".material",
									Component.translatable(StorageTranslationHelper.INSTANCE.translGui("barrel_part." + barrelMaterial.getSerializedName())),
									block.getName().withStyle(ChatFormatting.DARK_AQUA)
							).withStyle(ChatFormatting.GRAY)
					);
				});
			});
		}

		if (hasMainColor(stack)) {
			int mainColor = getMainColor(stack);
			tooltip.add(Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip("paintbrush") + ".main_color",
							Component.literal(ColorHelper.getHexColor(mainColor)).withStyle(Style.EMPTY.withColor(mainColor))
					).withStyle(ChatFormatting.GRAY)
			);
		}

		if (hasAccentColor(stack)) {
			int accentColor = getAccentColor(stack);
			tooltip.add(Component.translatable(StorageTranslationHelper.INSTANCE.translItemTooltip("paintbrush") + ".accent_color",
							Component.literal(ColorHelper.getHexColor(accentColor)).withStyle(Style.EMPTY.withColor(accentColor))
					).withStyle(ChatFormatting.GRAY)
			);
		}
	}

	private static boolean hasMainColor(ItemStack paintbrush) {
		return paintbrush.has(ModCoreDataComponents.MAIN_COLOR);
	}

	private static boolean hasAccentColor(ItemStack paintbrush) {
		return paintbrush.has(ModCoreDataComponents.ACCENT_COLOR);
	}

	private static boolean hasBarrelMaterials(ItemStack paintbrush) {
		return paintbrush.has(ModDataComponents.BARREL_MATERIALS);
	}

	public static int getMainColor(ItemStack paintbrush) {
		return paintbrush.getOrDefault(ModCoreDataComponents.MAIN_COLOR, -1);
	}

	public static int getAccentColor(ItemStack paintbrush) {
		return paintbrush.getOrDefault(ModCoreDataComponents.ACCENT_COLOR, -1);
	}

	public static Map<BarrelMaterial, ResourceLocation> getBarrelMaterials(ItemStack paintbrush) {
		return paintbrush.getOrDefault(ModDataComponents.BARREL_MATERIALS, Collections.emptyMap());
	}

	public record ItemRequirements(List<ItemStack> itemsPresent, List<ItemStack> itemsMissing) {}
}
