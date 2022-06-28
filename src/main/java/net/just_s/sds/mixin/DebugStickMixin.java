package net.just_s.sds.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DebugStickItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(DebugStickItem.class)
public class DebugStickMixin {

	@Shadow
	private static void sendMessage(PlayerEntity player, Text message) {
		// shadowing for ease access
	}

	@Shadow
	private static <T> T cycle(Iterable<T> elements, @Nullable T current, boolean inverse) {
		return null;
	}

	@Shadow
	private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property, boolean inverse) {
		return null;
	}

	@Shadow
	private static <T extends Comparable<T>> String getValueString(BlockState state, Property<T> property) {
		return null;
	}

	@Inject(at = @At("HEAD"), method = "use", cancellable = true)
	private void injectUse(PlayerEntity player, BlockState state, WorldAccess world, BlockPos pos, boolean update, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!player.isCreativeLevelTwoOp()) {
			Block block = state.getBlock();
			if (
					block.getTranslationKey().contains("stairs") |
							block.getTranslationKey().contains("wall") |
							block.getTranslationKey().contains("fence") |
							block.getTranslationKey().contains("glass_pane") |
							block.getTranslationKey().contains("iron_bars"))
			{
				StateManager<Block, BlockState> stateManager = block.getStateManager();
				Collection<Property<?>> collection = stateManager.getProperties();
				String string = Registry.BLOCK.getId(block).toString();
				if (collection.isEmpty()) {
					sendMessage(player, Text.of("Невозможно преобразовать данный блок"));
					cir.setReturnValue(false);
				} else {
					NbtCompound nbtCompound = stack.getOrCreateSubNbt("DebugProperty");
					String string2 = nbtCompound.getString(string);
					Property<?> property = stateManager.getProperty(string2);
					if (!player.shouldCancelInteraction()) {
						if (property == null) {
							property = (Property)collection.iterator().next();
						}

						if (property.getName().equals("waterlogged")) {
							property = (Property)cycle((Iterable)collection, (Object)property, false);
						}

						BlockState blockState = cycle(state, property, false);
						world.setBlockState(pos, blockState, 18);
						sendMessage(player, Text.of("Параметр «" + property.getName() +"» изменён ("+ getValueString(blockState, property) + ")"));
					} else {
						property = (Property)cycle((Iterable)collection, (Object)property, false);
						if (property.getName().equals("waterlogged")) {
							property = (Property)cycle((Iterable)collection, (Object)property, false);
						}
						String string3 = property.getName();
						nbtCompound.putString(string, string3);
						sendMessage(player, Text.of("Параметр «" + string3 +"» выбран ("+ getValueString(state, property) + ")"));
					}

					cir.setReturnValue(true);
				}
			} else {
				sendMessage(player, Text.of("Невозможно преобразовать данный блок"));
				cir.setReturnValue(false);
			}
		}
	}
}
