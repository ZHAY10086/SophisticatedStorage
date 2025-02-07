package net.p3pp3rf1y.sophisticatedstorage.compat.jei;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedstorage.SophisticatedStorage;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.DoubleChestTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeRecipe;
import net.p3pp3rf1y.sophisticatedstorage.crafting.StorageTierUpgradeShapelessRecipe;
import net.p3pp3rf1y.sophisticatedstorage.item.ChestBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class TierUpgradeRecipesMaker {
	private TierUpgradeRecipesMaker() {}

	public static List<RecipeHolder<CraftingRecipe>> getShapedCraftingRecipes() {
		RecipeConstructor<StorageTierUpgradeRecipe> constructRecipe = (originalRecipe, ingredients, result) -> {
			ShapedRecipePattern pattern = new ShapedRecipePattern(originalRecipe.getWidth(), originalRecipe.getHeight(), ingredients, Optional.empty());
			return new ShapedRecipe("", CraftingBookCategory.MISC, pattern, result);
		};
		List<RecipeHolder<CraftingRecipe>> craftingRecipes = getCraftingRecipes(constructRecipe, StorageTierUpgradeRecipe.class, TierUpgradeRecipesMaker::getStorageItems);
		RecipeConstructor<DoubleChestTierUpgradeRecipe> constructDoubleChestRecipe = (originalRecipe, ingredients, result) -> {
			ShapedRecipePattern pattern = new ShapedRecipePattern(originalRecipe.getWidth(), originalRecipe.getHeight(), ingredients, Optional.empty());
			return new ShapedRecipe("", CraftingBookCategory.MISC, pattern, result);
		};
		craftingRecipes.addAll(getCraftingRecipes(constructDoubleChestRecipe, DoubleChestTierUpgradeRecipe.class, TierUpgradeRecipesMaker::getDoubleChestItems));
		return craftingRecipes;
	}

	public static List<RecipeHolder<CraftingRecipe>> getShapelessCraftingRecipes() {
		RecipeConstructor<StorageTierUpgradeShapelessRecipe> constructRecipe = (originalRecipe, ingredients, result) -> new ShapelessRecipe("", CraftingBookCategory.MISC, result, ingredients);
		List<RecipeHolder<CraftingRecipe>> craftingRecipes = getCraftingRecipes(constructRecipe, StorageTierUpgradeShapelessRecipe.class, TierUpgradeRecipesMaker::getStorageItems);
		RecipeConstructor<DoubleChestTierUpgradeShapelessRecipe> constructDoubleChestRecipe = (originalRecipe, ingredients, result) -> new ShapelessRecipe("", CraftingBookCategory.MISC, result, ingredients);
		craftingRecipes.addAll(getCraftingRecipes(constructDoubleChestRecipe, DoubleChestTierUpgradeShapelessRecipe.class, TierUpgradeRecipesMaker::getDoubleChestItems));
		return craftingRecipes;
	}

	@NotNull
	private static <T extends CraftingRecipe> List<RecipeHolder<CraftingRecipe>> getCraftingRecipes(RecipeConstructor<T> constructRecipe, Class<T> originalRecipeClass, Function<CraftingRecipe, List<ItemStack>> getStorageItems) {
		return ClientRecipeHelper.transformAllRecipesOfTypeIntoMultiple(RecipeType.CRAFTING, originalRecipeClass, recipe -> {
			List<RecipeHolder<CraftingRecipe>> itemGroupRecipes = new ArrayList<>();
			getStorageItems.apply(recipe).forEach(storageItem -> {
				NonNullList<Ingredient> ingredients = recipe.getIngredients();
				CraftingContainer craftinginventory = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
					@Override
					public ItemStack quickMoveStack(Player player, int index) {
						return ItemStack.EMPTY;
					}

					public boolean stillValid(Player playerIn) {
						return false;
					}
				}, 3, 3);
				NonNullList<Ingredient> ingredientsCopy = NonNullList.createWithCapacity(ingredients.size());
				int i = 0;
				for (Ingredient ingredient : ingredients) {
					ItemStack[] ingredientItems = ingredient.getItems();
					if (ingredientItems.length == 1 && storageItem.getItem() == ingredientItems[0].getItem()) {
						ingredientsCopy.add(i, Ingredient.of(storageItem));
						craftinginventory.setItem(i, storageItem.copy());
					} else {
						ingredientsCopy.add(i, ingredient);
						if (!ingredient.isEmpty()) {
							craftinginventory.setItem(i, ingredientItems[0]);
						}
					}
					i++;
				}
				ItemStack result = ClientRecipeHelper.assemble(recipe, craftinginventory.asCraftInput());
				ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SophisticatedStorage.MOD_ID, "tier_upgrade_" + getItemPath(storageItem) + "_to_" + getItemPath(result) + result.getComponentsPatch().toString().toLowerCase(Locale.ROOT).replaceAll("[{\",}:>=@\\[\\]\\s]", "_"));
				itemGroupRecipes.add(new RecipeHolder<>(id, constructRecipe.construct(recipe, ingredientsCopy, result)));
			});
			return itemGroupRecipes;
		});
	}

	private static @NotNull String getItemPath(ItemStack storageItem) {
		return BuiltInRegistries.ITEM.getKey(storageItem.getItem()).getPath();
	}

	private static List<ItemStack> getDoubleChestItems(CraftingRecipe recipe) {
		NonNullList<ItemStack> doubleChestItems = NonNullList.create();
		for (Ingredient ingredient : recipe.getIngredients()) {
			ItemStack[] ingredientItems = ingredient.getItems();

			for (ItemStack ingredientItem : ingredientItems) {
				Item item = ingredientItem.getItem();
				if (item instanceof ChestBlockItem chestBlockItem) {
					chestBlockItem.addCreativeTabItems(stack -> {
						ChestBlockItem.setDoubleChest(stack, true);
						doubleChestItems.add(stack);
					});
				}
			}
		}

		return doubleChestItems;
	}

	private static List<ItemStack> getStorageItems(CraftingRecipe recipe) {
		NonNullList<ItemStack> storageItems = NonNullList.create();
		for (Ingredient ingredient : recipe.getIngredients()) {
			ItemStack[] ingredientItems = ingredient.getItems();

			for (ItemStack ingredientItem : ingredientItems) {
				Item item = ingredientItem.getItem();
				if (item instanceof StorageBlockItem storageBlockItem) {
					storageBlockItem.addCreativeTabItems(storageItems::add);
				}
			}
		}

		return storageItems;
	}

	private interface RecipeConstructor<T extends Recipe<?>> {
		CraftingRecipe construct(T originalRecipe, NonNullList<Ingredient> ingredients, ItemStack result);
	}
}
