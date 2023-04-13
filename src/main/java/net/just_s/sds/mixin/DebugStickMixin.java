package net.just_s.sds.mixin;

import net.just_s.sds.Config;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DebugStickItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
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
    private static void sendMessage(PlayerEntity player, Text message) {}

    @Shadow
    private static <T extends Comparable<T>> String getValueString(BlockState state, Property<T> property) {return null;}

    @Shadow
    private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property, boolean inverse) {
        return null;
    }

    @Inject(at = @At("HEAD"), method = "use", cancellable = true)
    private void onUSE(PlayerEntity player, BlockState state, WorldAccess world, BlockPos pos, boolean update, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (player.isCreativeLevelTwoOp()) {return;}

        Block block = state.getBlock();
        StateManager<Block, BlockState> stateManager = block.getStateManager();
        Collection<Property<?>> collection = stateManager.getProperties();

        if (!isBlockAllowedToModify(state.getBlock()) || collection.isEmpty()) {
            sendMessage(player, Text.of(Config.MESSAGE_nomodify));
            cir.setReturnValue(false);
            return;
        }

        NbtCompound nbtCompound = stack.getOrCreateSubNbt("DebugProperty");

        String blockName = Registry.BLOCK.getId(block).toString();
        String propertyName = nbtCompound.getString(blockName);

        Property<?> property = stateManager.getProperty(propertyName);


        if (player.isSneaking()) {
            // select next property
            property = getNextProperty(collection, property, block);
            nbtCompound.putString(blockName, property.getName());

            sendMessage(player, Text.of(
                            String.format(
                                Config.MESSAGE_select,
                                property.getName(),
                                getValueString(state, property)
                            )
                    )
            );
        } else {
            // change value of property
            if (property == null) {
                property = getNextProperty(collection, null, block);
            }

            BlockState blockState = cycle(state, property, false);
            world.setBlockState(pos, blockState, 18);
            sendMessage(player, Text.of(
                            String.format(
                                Config.MESSAGE_change,
                                property.getName(),
                                getValueString(state, property)
                            )
                    )
            );
        }
        cir.setReturnValue(true);
    }

    private Property<?> getNextProperty(Collection<Property<?>> collection, @Nullable Property<?> property, @Nullable Block block) {
        int len = collection.size();
        do {
            property = Util.next(collection, property);
            len--;
        } while (len > 0 && !isPropertyModifiable(property, block));
        return property;
    }

    /**
     * Check via config if chosen block is able to be modified in survival
     * */
    private boolean isBlockAllowedToModify(Block block) {
        return Config.isBlockAllowed(block);
    }

    /**
     * Check via config if chosen property is able to be modified in survival
     * */
    private boolean isPropertyModifiable(Property<?> property, @Nullable Block block) {
        return Config.isPropertyAllowed(property.getName(), block);
    }
}
