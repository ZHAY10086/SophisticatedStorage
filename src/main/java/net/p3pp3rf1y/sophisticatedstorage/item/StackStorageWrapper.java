package net.p3pp3rf1y.sophisticatedstorage.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.util.BlockItemBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedstorage.Config;
import net.p3pp3rf1y.sophisticatedstorage.block.*;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class StackStorageWrapper extends StorageWrapper {
	private static final String CONTENTS_TAG = "contents";
	private final ItemStack storageStack;

	public StackStorageWrapper(ItemStack storageStack) {
		super(() -> () -> {}, () -> {}, () -> {});
		this.storageStack = storageStack;
	}

	private UUID getNewUuid() {
		UUID newUuid = UUID.randomUUID();
		setContentsUuid(newUuid);
		return newUuid;
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.ofNullable(contentsUuid);
	}

	public boolean hasContents() {
		return StorageBlockItem.getEntityWrapperTagFromStack(storageStack).isPresent() || contentsUuid != null;
	}

	@Override
	public void setContentsUuid(@Nullable UUID contentsUuid) {
		super.setContentsUuid(contentsUuid);
		if (contentsUuid != null) {
			NBTHelper.setUniqueId(storageStack, "uuid", contentsUuid);
			ItemContentsStorage itemContentsStorage = ItemContentsStorage.get();
			CompoundTag storageContents = itemContentsStorage.getOrCreateStorageContents(contentsUuid);
			if (!storageContents.contains(StorageBlockEntity.STORAGE_WRAPPER_TAG)) {
				CompoundTag storageWrapperTag = new CompoundTag();
				storageWrapperTag.put(CONTENTS_TAG, new CompoundTag());
				storageContents.put(StorageBlockEntity.STORAGE_WRAPPER_TAG, storageWrapperTag);
			}

			onContentsNbtUpdated();
		}
	}

	@Override
	protected CompoundTag getContentsNbt() {
		return StorageBlockItem.getEntityWrapperTagFromStack(storageStack).map(wrapperTag -> wrapperTag.getCompound(CONTENTS_TAG)).orElseGet(() -> {
			if (contentsUuid == null) {
				contentsUuid = getNewUuid();
			}
			return ItemContentsStorage.get().getOrCreateStorageContents(contentsUuid).getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG).getCompound(CONTENTS_TAG);
		});
	}

	@Override
	protected void onUpgradeRefresh() {
		//noop - there should be no upgrade refresh happening here
	}

	@Override
	public int getDefaultNumberOfInventorySlots() {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfInventorySlots() : 0;
	}

	@Override
	protected void loadSlotNumbers(CompoundTag tag) {
		StorageBlockItem.getEntityWrapperTagFromStack(storageStack).ifPresentOrElse(wrapperTag -> {
			numberOfInventorySlots = wrapperTag.getInt("numberOfInventorySlots");
			numberOfUpgradeSlots = wrapperTag.getInt("numberOfUpgradeSlots");
		}, () -> {
			numberOfInventorySlots = NBTHelper.getInt(storageStack, "numberOfInventorySlots").orElse(0);
			numberOfUpgradeSlots = NBTHelper.getInt(storageStack, "numberOfUpgradeSlots").orElse(0);
		});

	}

	@Override
	public int getDefaultNumberOfUpgradeSlots() {
		return storageStack.getItem() instanceof BlockItemBase blockItem && blockItem.getBlock() instanceof IStorageBlock storageBlock ? storageBlock.getNumberOfUpgradeSlots() : 0;
	}

	@Override
	public String getStorageType() {
		return "irrelevant"; //because this is only used when determining upgrade errors in gui which storage stacks can't have open
	}

	@Override
	public Component getDisplayName() {
		return Component.empty(); //because this is only used when determining upgrade errors in gui which storage stacks can't have open
	}

	@Override
	protected boolean isAllowedInStorage(ItemStack stack) {
		if (!(storageStack.getItem() instanceof ShulkerBoxItem)) {
			return false;
		}

		Block block = Block.byItem(stack.getItem());
		return !(block instanceof ShulkerBoxBlock) && !(block instanceof net.minecraft.world.level.block.ShulkerBoxBlock) && !Config.SERVER.shulkerBoxDisallowedItems.isItemDisallowed(stack.getItem());
	}
}
