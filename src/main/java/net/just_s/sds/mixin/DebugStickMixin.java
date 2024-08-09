package net.just_s.sds.mixin;

import net.just_s.sds.config.Config;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DebugStickStateComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DebugStickItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(DebugStickItem.class)
public abstract class DebugStickMixin extends Item {
    public DebugStickMixin(Settings settings) {
        super(settings);
    }

    @Shadow
    private static void sendMessage(PlayerEntity player, Text message) {}

    @Shadow
    private static <T extends Comparable<T>> String getValueString(BlockState state, Property<T> property) {return null;}

    @Shadow
    private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property, boolean inverse) {
        return null;
    }

    @Shadow
    private static <T> T cycle(Iterable<T> values, @Nullable T value, boolean inverse) {
        return null;
    }

    @Inject(at = @At("HEAD"), method = "use", cancellable = true)
    private void onUSE(PlayerEntity player, BlockState state, WorldAccess world, BlockPos pos, boolean update, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // if the player already does have the rights to use Debug Stick, the mod should not interfere
        if (player.isCreativeLevelTwoOp()) {return;}

        Block block = state.getBlock();
        RegistryEntry<Block> registryEntry = state.getRegistryEntry();
        StateManager<Block, BlockState> stateManager = (registryEntry.value()).getStateManager();
        Collection<Property<?>> collection = stateManager.getProperties();

        // check if block is modifiable by the config
        if (!isBlockAllowedToModify(block) || collection.isEmpty()) {
            sendMessage(player, Text.translatable(this.getTranslationKey() + ".empty", new Object[]{registryEntry.getIdAsString()}));
            cir.setReturnValue(false);
            return;
        }

        // https://minecraft.wiki/w/Debug_Stick
        // to remember the data of which property for which block is chosen,
        // Minecraft Devs decided to use Component for Debug Stick.
        // Who am I to disagree? (btw thx to @MrBretze for example code)
        DebugStickStateComponent stateComponent = stack.get(DataComponentTypes.DEBUG_STICK_STATE);

        if (stateComponent == null) {
            return;
        }

        Property<?> property = stateComponent.properties().get(registryEntry);

        if (update) {
            // change value of property
            if (property == null) {
                property = getNextProperty(collection, null, block, player.shouldCancelInteraction());
            }
            // check if given property is allowed
            if (!isPropertyModifiable(property, block)) {
                sendMessage(player, Text.translatable(this.getTranslationKey() + ".empty", new Object[]{registryEntry.getIdAsString()}));
                cir.setReturnValue(false);
                return;
            }

            // generate new state of chosen block with modified property
            BlockState newState = getNextBlockState(state, property, player.shouldCancelInteraction());
            // update chosen block with its new state
            world.setBlockState(pos, newState, 18);
            // send the player a message of successful modifying
            sendMessage(
                    player,
                    Text.translatable(
                            this.getTranslationKey() + ".update",
                            new Object[]{property.getName(), getValueString(newState, property)}
                    )
            );
        } else {
            // select next property
            property = getNextProperty(collection, property, block, player.shouldCancelInteraction());
            // check if given property is allowed
            if (!isPropertyModifiable(property, block)) {
                sendMessage(player, Text.translatable(this.getTranslationKey() + ".empty", new Object[]{registryEntry.getIdAsString()}));
                cir.setReturnValue(false);
                return;
            }
            // save chosen property in the NBT data of Debug Stick
            stack.set(DataComponentTypes.DEBUG_STICK_STATE, stateComponent.with(registryEntry, property));

            // send the player a message of successful selecting
            sendMessage(
                    player,
                    Text.translatable(
                            this.getTranslationKey() + ".select",
                            new Object[]{property.getName(), getValueString(state, property)}
                    )
            );
        }
        cir.setReturnValue(true);
    }

    /**
     * Choose next property that is appropriate for the configuration file
     * */
    @Unique
    private Property<?> getNextProperty(Collection<Property<?>> collection, @Nullable Property<?> property, @Nullable Block block, boolean inverse) {
        int i = 0;
        do { // simply scrolling through the list of properties until suitable is found
            property = cycle(collection, property, inverse);
            i++;
        } while (i < collection.size() && !isPropertyModifiable(property, block));
        return property;
    }

    /**
     * Choose next property that is appropriate for the configuration file
     * */
    @Unique
    private <T extends Comparable<T>> BlockState getNextBlockState(BlockState state, Property<T> property, boolean inverse) {
        int i = 0;
        Collection<T> collection = property.getValues();
        T value =  state.get(property);
        do { // simply scrolling through the list of property values until suitable is found
            value = cycle(collection, value, inverse);
            i++;
        } while (i < collection.size() && !isPropertyValueAllowed(state.getBlock(), property, value));
        if (!isPropertyValueAllowed(state.getBlock(), property, value) && i == collection.size()) {
            return state;
        }
        return state.with(property, value);
    }

    /**
     * Check via config if chosen block is able to be modified in survival
     * */
    @Unique
    private boolean isBlockAllowedToModify(Block block) {
        return Config.isBlockAllowed(block);
    }

    /**
     * Check via config if chosen block state is allowed
     * */
    @Unique
    private <T extends Comparable<T>> boolean isPropertyValueAllowed(Block block, Property<T> property, T value) {
        return Config.isPropertyValueAllowed(block, property.getName(), value.toString());
    }

    /**
     * Check via config if chosen property is able to be modified in survival
     * */
    @Unique
    private boolean isPropertyModifiable(Property<?> property, @Nullable Block block) {
        return Config.isPropertyAllowed(property.getName(), block);
    }
}
