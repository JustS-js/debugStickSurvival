package net.just_s.sds;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SDSMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("sds");

	@Override
	public void onInitialize() {
		LOGGER.info("SDS initialized successfully!");
	}
}
