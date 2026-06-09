package net.just_s.sds;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.just_s.sds.config.Config;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SDSMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("sds");

	@Override
	public void onInitialize() {
		Config.loadOrCreate();
		LOGGER.info("SDS initialized successfully!");
		AttackBlockCallback.EVENT.register(
				(player, world, hand, pos, direction) -> {
					// callback hooks before the spectator check
					if (player.isSpectator() || player.isCreative()) {
						return InteractionResult.PASS;
					}

					// check if player actually holds debug stick while punching
					ItemStack stack = player.getItemInHand(hand);
					if (!stack.is(Items.DEBUG_STICK)) {
						return InteractionResult.PASS;
					}

					// While in creative, you break every block with one click.
					// Original Debug Stick behavior hooks its "changing" ability
					// to a player breaking block.
					// We want to imitate this behaviour with a simple one-click punch

					// But there is another problem:
					// AttackBlockCallback fires EVERY tick, which produces spam with "changing" ability
					// in other words, states cycle every tick while you are pressing left-click
					// This is not cool!

					// So I decided to put custom nbt timer to prevent spamming
					// (It is still buggy, suggestions appreciated)
					CustomData component = stack.get(DataComponents.CUSTOM_DATA);
					if (component != null) {
						CompoundTag nbtData = component.copyTag();
						long lastModified = nbtData.getLong("LastModified").orElse(0L);
						if (world.getGameTime() < lastModified + 5) {
							return InteractionResult.PASS;
						}
					}

					CompoundTag newNbtData = new CompoundTag();
					newNbtData.put("LastModified", LongTag.valueOf(world.getGameTime()));
					stack.applyComponents(
							DataComponentPatch.builder()
									.set(DataComponents.CUSTOM_DATA, CustomData.of(newNbtData))
									.build()
					);
					stack.getItem().canDestroyBlock(stack, world.getBlockState(pos), world, pos, player);
					return InteractionResult.PASS;
				}
		);
	}
}
