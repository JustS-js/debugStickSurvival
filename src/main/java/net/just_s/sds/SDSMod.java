package net.just_s.sds;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.just_s.sds.config.Config;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtLong;
import net.minecraft.util.ActionResult;
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
						return ActionResult.PASS;
					}

					// check if player actually holds debug stick while punching
					ItemStack stack = player.getStackInHand(hand);
					if (!stack.isOf(Items.DEBUG_STICK)) {
						return ActionResult.PASS;
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
					NbtCompound nbtData = stack.getOrCreateNbt();
					long lastModified = nbtData.getLong("LastModified");
					if (world.getTime() < lastModified + 5) {
						return ActionResult.PASS;
					}

					nbtData.put("LastModified", NbtLong.of(world.getTime()));
					stack.getItem().canMine(world.getBlockState(pos), world, pos, player);
					return ActionResult.PASS;
				}
		);
	}
}
