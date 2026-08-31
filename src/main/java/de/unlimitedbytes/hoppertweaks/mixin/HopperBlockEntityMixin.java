package de.unlimitedbytes.hoppertweaks.mixin;

import de.unlimitedbytes.hoppertweaks.HopperSpeed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Changes only the stack size handed to Vanilla's existing inventory transfer
 * code. This intentionally leaves the rest of HopperBlockEntity untouched.
 *
 * <p>The transfer amount is capped at what the receiving container can actually
 * accept, so a transfer either completes fully or fails cleanly like a vanilla
 * one. This matters because Vanilla's leftover handling only behaves correctly
 * for single items: a partial insert would skip {@code setChanged} (breaking the
 * Carpet TIS Addition scounter, which counts after that call), miscalculate the
 * restore of the source stack, and stall hopper lines feeding into nearly full
 * containers.</p>
 */
@Mixin(HopperBlockEntity.class)
abstract class HopperBlockEntityMixin {
	@Shadow
	private static boolean canPlaceItemInContainer(Container container, ItemStack stack, int slot, Direction direction) {
		throw new AssertionError();
	}

	@Shadow
	private static int[] getSlots(Container container, Direction direction) {
		throw new AssertionError();
	}

	@Shadow
	private static Container getAttachedContainer(Level level, BlockPos pos, HopperBlockEntity hopperBlockEntity) {
		throw new AssertionError();
	}

	/**
	 * Hopper pushing items into the container below or beside it ({@code ejectItems}
	 * calls the hopper's own {@code removeItem}). Composters keep the vanilla
	 * amount: they accept one item per insert, so a larger transfer would either
	 * waste items or fill the composter in a single cycle.
	 */
	@Redirect(
			method = "ejectItems",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;removeItem(II)Lnet/minecraft/world/item/ItemStack;"
			)
	)
	private static ItemStack hoppertweaks$changeEjectAmount(
			HopperBlockEntity hopper,
			int slot,
			int vanillaAmount,
			Level level,
			BlockPos pos,
			HopperBlockEntity hopperEntity
	) {
		int amount = vanillaAmount;
		if (!hoppertweaks$pushesIntoComposter(level, pos)) {
			int speed = HopperSpeed.getItemsPerCycle(level);
			Direction facing = level.getBlockState(pos).getValue(HopperBlock.FACING);
			Container destination = getAttachedContainer(level, pos, hopperEntity);
			int acceptable = hoppertweaks$acceptableAmount(destination, hopper.getItem(slot), facing.getOpposite(), speed);
			if (acceptable > 0) {
				amount = Math.min(speed, acceptable);
			}
		}
		return hopper.removeItem(slot, amount);
	}

	/**
	 * Hopper pulling items from the container above it ({@code tryTakeInItemFromSlot}
	 * calls {@code removeItem} on the source {@code Container}). The amount is
	 * capped by the hopper's own free space, so the pull never spans more than
	 * what fits (and {@code removeItem} itself caps at the source stack).
	 */
	@Redirect(
			method = "tryTakeInItemFromSlot",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/Container;removeItem(II)Lnet/minecraft/world/item/ItemStack;"
			)
	)
	private static ItemStack hoppertweaks$changeTakeAmount(
			Container source,
			int slot,
			int vanillaAmount,
			Hopper hopper,
			Container sourceAgain,
			int slotAgain,
			Direction direction
	) {
		int amount = vanillaAmount;
		int speed = HopperSpeed.getItemsPerCycle();
		int acceptable = hoppertweaks$acceptableAmount(hopper, source.getItem(slot), null, speed);
		if (acceptable > 0) {
			amount = Math.min(speed, acceptable);
		}
		return source.removeItem(slot, amount);
	}

	private static boolean hoppertweaks$pushesIntoComposter(Level level, BlockPos hopperPos) {
		BlockPos targetPos = hopperPos.relative(level.getBlockState(hopperPos).getValue(HopperBlock.FACING));
		return level.getBlockState(targetPos).is(Blocks.COMPOSTER);
	}

	/**
	 * Mirrors the slot logic of Vanilla's {@code addItem} to compute how many of
	 * the given items the destination can still take, stopping once {@code limit}
	 * is reached.
	 */
	private static int hoppertweaks$acceptableAmount(Container destination, ItemStack stack, Direction direction, int limit) {
		if (destination == null || stack.isEmpty()) {
			return Integer.MAX_VALUE;
		}

		int space = 0;
		for (int slot : getSlots(destination, direction)) {
			if (!canPlaceItemInContainer(destination, stack, slot, direction)) {
				continue;
			}
			ItemStack inSlot = destination.getItem(slot);
			if (inSlot.isEmpty()) {
				space += Math.min(destination.getMaxStackSize(), stack.getMaxStackSize());
			} else if (ItemStack.isSameItemSameComponents(inSlot, stack)) {
				space += Math.min(Math.min(destination.getMaxStackSize(), inSlot.getMaxStackSize()), stack.getMaxStackSize()) - inSlot.getCount();
			}
			if (space >= limit) {
				break;
			}
		}
		return space;
	}
}
