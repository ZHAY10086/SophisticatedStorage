package net.p3pp3rf1y.sophisticatedstorage.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.ITintableBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BarrelMaterialRecipe extends CustomRecipe {
	public BarrelMaterialRecipe(ResourceLocation registryName, CraftingBookCategory category) {
		super(registryName, category);
	}

	@Override
	public boolean matches(CraftingContainer container, Level level) {
		int barrelRow = -1;
		int barrelCol = -1;
		int minRowWithBlock = Integer.MAX_VALUE;
		int minColWithBlock = Integer.MAX_VALUE;
		int maxRowWithBlock = Integer.MIN_VALUE;
		int maxColWithBlock = Integer.MIN_VALUE;

		Map<Integer, Integer> rowCounts = new HashMap<>();

		for (int row = 0; row < container.getHeight(); row++) {
			for (int col = 0; col < container.getWidth(); col++) {
				ItemStack item = container.getItem(col + row * container.getWidth());
				if (item.isEmpty()) {
					continue;
				}

				if (item.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BarrelBlock) {
					if (barrelRow < 0) {
						barrelRow = row;
						barrelCol = col;
					} else {
						return false;
					}
				} else if (item.getItem() instanceof BlockItem) {
					boolean isBottomMiddleAndBottomLeftHasBlock = barrelCol == col && barrelRow < row && rowCounts.getOrDefault(row, 0) > 0;
					if (isBottomMiddleAndBottomLeftHasBlock) {
						return false;
					}

					rowCounts.compute(row, (k, v) -> v == null ? 1 : v + 1);
					if (row < minRowWithBlock) {
						minRowWithBlock = row;
					}
					if (col < minColWithBlock) {
						minColWithBlock = col;
					}
					if (row > maxRowWithBlock) {
						maxRowWithBlock = row;
					}
					if (col > maxColWithBlock) {
						maxColWithBlock = col;
					}
				} else {
					return false;
				}
			}
		}

		if (barrelRow < 0 || rowCounts.isEmpty() || minRowWithBlock < barrelRow - 1 || maxRowWithBlock > barrelRow + 1 || minColWithBlock < barrelCol - 1 || maxColWithBlock > barrelCol + 1) {
			return false;
		}

		return rowCounts.getOrDefault(barrelRow - 1, 0) <= 3 && rowCounts.getOrDefault(barrelRow, 0) <= 2 && rowCounts.getOrDefault(barrelRow + 1, 0) <= 2;
	}

	@Override
	public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
		int barrelColumn = -1;
		int barrelRow = -1;
		ItemStack barrelStackCopy = ItemStack.EMPTY;

		for (int row = 0; row < container.getHeight(); row++) {
			for (int col = 0; col < container.getWidth(); col++) {
				ItemStack item = container.getItem(col + row * container.getWidth());
				if (item.isEmpty()) {
					continue;
				}

				if (barrelColumn < 0 && item.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BarrelBlock) {
					barrelColumn = col;
					barrelRow = row;
					barrelStackCopy = item.copy();
					barrelStackCopy.setCount(1);
				}
			}
		}

		Map<BarrelMaterial, ResourceLocation> materials = new EnumMap<>(BarrelMaterial.class);
		materials.putAll(BarrelBlockItem.getMaterials(barrelStackCopy));
		BarrelBlockItem.uncompactMaterials(materials);

		fillGridMaterials(container, barrelColumn, barrelRow, materials);
		fillEmptyMaterialsWithDefaults(materials);
		BarrelBlockItem.compactMaterials(materials);

		BarrelBlockItem.setMaterials(barrelStackCopy, materials);

		BarrelBlockItem.removeCoveredTints(barrelStackCopy, materials);

		return barrelStackCopy;
	}

	private static void fillEmptyMaterialsWithDefaults(Map<BarrelMaterial, ResourceLocation> materials) {
		for (BarrelMaterial material : BarrelMaterial.values()) {
			if (material.isLeaf() && !materials.containsKey(material)) {
				for (BarrelMaterial fillFromDefault : BarrelMaterial.getFillFromDefaults(material)) {
					if (materials.containsKey(fillFromDefault)) {
						materials.put(material, materials.get(fillFromDefault));
						break;
					}
				}
			}
		}
	}

	private void fillGridMaterials(CraftingContainer container, int barrelColumn, int barrelRow, Map<BarrelMaterial, ResourceLocation> materials) {
		for (int row = 0; row < container.getHeight(); row++) {
			for (int col = 0; col < container.getWidth(); col++) {
				ItemStack item = container.getItem(col + row * container.getWidth());
				if ((row == barrelRow && col == barrelColumn) || item.isEmpty()) {
					continue;
				}

				if (item.getItem() instanceof BlockItem blockItem) {
					List<BarrelMaterial> barrelMaterials = getBarrelMaterials(row, col, barrelRow, barrelColumn);
					boolean firstMaterial = true;
					for (BarrelMaterial barrelMaterial : barrelMaterials) {
						if (!materials.containsKey(barrelMaterial) || firstMaterial) {
							materials.put(barrelMaterial, ForgeRegistries.BLOCKS.getKey(blockItem.getBlock()));
						}
						firstMaterial = false;
					}
				}
			}
		}
	}

	private List<BarrelMaterial> getBarrelMaterials(int row, int col, int barrelRow, int barrelColumn) {
		if (row < barrelRow) {
			if (col < barrelColumn) {
				return List.of(BarrelMaterial.TOP);
			} else if (col == barrelColumn) {
				return List.of(BarrelMaterial.TOP_INNER_TRIM, BarrelMaterial.TOP, BarrelMaterial.TOP_TRIM);
			} else {
				return List.of(BarrelMaterial.TOP_TRIM);
			}
		} else if (row == barrelRow) {
			if (col < barrelColumn) {
				return List.of(BarrelMaterial.SIDE);
			} else {
				return List.of(BarrelMaterial.SIDE_TRIM);
			}
		} else {
			if (col < barrelColumn) {
				return List.of(BarrelMaterial.BOTTOM);
			} else if (col == barrelColumn) {
				return List.of(BarrelMaterial.BOTTOM, BarrelMaterial.BOTTOM_TRIM);
			} else {
				return List.of(BarrelMaterial.BOTTOM_TRIM);
			}
		}
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height > 1;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModBlocks.BARREL_MATERIAL_RECIPE_SERIALIZER.get();
	}
}
