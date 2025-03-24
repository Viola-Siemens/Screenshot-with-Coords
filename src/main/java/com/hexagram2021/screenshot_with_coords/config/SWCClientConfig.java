package com.hexagram2021.screenshot_with_coords.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SWCClientConfig {
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	private static final ModConfigSpec SPEC;

	public static final ModConfigSpec.IntValue CAPTURE_INTERVAL;
	public static final ModConfigSpec.IntValue JSONL_SAVE_AFTER;

	static {
		BUILDER.push("screenshot_with_coords-client-config");
		CAPTURE_INTERVAL = BUILDER.comment("The interval (in millisecond, for example, 500 is half a second) between two captures.")
				.defineInRange("CAPTURE_INTERVAL", 500, 1, 3600000);
		JSONL_SAVE_AFTER = BUILDER.comment("How many captures is needed before detailed coords informations will be saved into jsonl file. Notice that after player leaves the level, cached informations will still be saved.")
				.defineInRange("JSONL_SAVE_AFTER", 50, 1, 65536);
		BUILDER.pop();
		SPEC = BUILDER.build();
	}

	private SWCClientConfig() {}

	public static ModConfigSpec getConfig() {
		return SPEC;
	}
}
