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
import net.minecraft.registry.Registries;
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
        // if the player already does have the rights to use Debug Stick, the mod should not interfere
        if (player.isCreativeLevelTwoOp()) {return;}

        Block block = state.getBlock();
        StateManager<Block, BlockState> stateManager = block.getStateManager();
        Collection<Property<?>> collection = stateManager.getProperties();

        // check if block is modifiable by the config
        if (!isBlockAllowedToModify(state.getBlock()) || collection.isEmpty()) {
            sendMessage(player, Text.of(Config.MESSAGE_nomodify));
            cir.setReturnValue(false);
            return;
        }

        // https://minecraft.fandom.com/wiki/Debug_Stick#Item_data
        // to remember the data of which property for which block is chosen,
        // Minecraft Devs decided to use NBT data for Debug Stick.
        // Who am I to disagree?
        NbtCompound nbtCompound = stack.getOrCreateSubNbt("DebugProperty");

        String blockName = Registries.BLOCK.getId(block).toString();
        String propertyName = nbtCompound.getString(blockName);

        Property<?> property = stateManager.getProperty(propertyName);

        if (player.isSneaking()) {
            // select next property
            property = getNextProperty(collection, property, block);
            // save chosen property in the NBT data of Debug Stick
            nbtCompound.putString(blockName, property.getName());

            // send the player a message of successful selecting
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

            // generate new state of chosen block with modified property
            BlockState newState = cycle(state, property, false);
            // update chosen block with its new state
            world.setBlockState(pos, newState, 18);
            // send the player a message of successful modifying
            sendMessage(player, Text.of(
                            String.format(
                                Config.MESSAGE_change,
                                property.getName(),
                                getValueString(newState, property)
                            )
                    )
            );
        }
        cir.setReturnValue(true);
    }

    /**
     * Choose next property that is appropriate for the configuration file
     * */
    private Property<?> getNextProperty(Collection<Property<?>> collection, @Nullable Property<?> property, @Nullable Block block) {
        int len = collection.size();
        do { // simply scrolling through the list of properties until suitable is found
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
