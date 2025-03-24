package com.hexagram2021.screenshot_with_coords;
import com.google.common.collect.Lists;
import com.hexagram2021.screenshot_with_coords.config.SWCClientConfig;
import com.hexagram2021.screenshot_with_coords.utils.JsonEntry;
import com.hexagram2021.screenshot_with_coords.utils.SWCLogger;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Mod(ScreenshotWithCoords.MODID)
public class ScreenshotWithCoords {
	public static final String MODID = "screenshot_with_coords";

	public ScreenshotWithCoords(IEventBus modEventBus, ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.CLIENT, SWCClientConfig.getConfig());
		NeoForge.EVENT_BUS.register(this);
	}

	private boolean isCaptureTick = false;
	@Nullable
	private static Thread UPDATE_THREAD = null;
	@Nullable
	private static File captureDirectory = null;
	private static final List<JsonEntry> CACHED_ENTRIES = Lists.newArrayList();

	@SuppressWarnings("BusyWait")
	private void updateCaptureTick(int milliseconds) {
		SWCLogger.info("Started.");
		try {
			for(;;) {
				Thread.sleep(milliseconds);
				this.isCaptureTick = true;
			}
		} catch (InterruptedException e) {
			SWCLogger.error("Interrupted during capture tick updating.", e);
		}
		SWCLogger.info("Stopped.");
	}

	@SubscribeEvent
	public void onClientPlayerTick(PlayerTickEvent.Pre event) {
		Player player = event.getEntity();
		Level level = player.level();
		Minecraft minecraft = Minecraft.getInstance();
		if(level instanceof ClientLevel clientLevel && player.equals(minecraft.player) && this.isCaptureTick && RenderSystem.isOnRenderThread() && captureDirectory != null) {
			this.isCaptureTick = false;
			String captureName = getFilename();
			Screenshot.grab(
					captureDirectory,
					captureName,
					minecraft.getMainRenderTarget(),
					component -> {
					}
			);
			CACHED_ENTRIES.add(new JsonEntry(captureName, player.getX(), player.getY(), player.getZ(), player.getViewYRot(1.0F), player.getViewXRot(1.0F)));
			if(CACHED_ENTRIES.size() >= SWCClientConfig.JSONL_SAVE_AFTER.get()) {
				saveCoordsAndClearCache();
			}
		}
	}

	@SubscribeEvent
	public void onPlayerJoinLevel(ClientPlayerNetworkEvent.LoggingIn event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = event.getPlayer();
		if(player.equals(minecraft.player) && RenderSystem.isOnRenderThread()) {
			SWCLogger.info("Hello, " + player.getScoreboardName());
			if(UPDATE_THREAD == null) {
				captureDirectory = new File(minecraft.gameDirectory, "screenshot_with_coords");
				captureDirectory.mkdir();
				UPDATE_THREAD = new Thread(() -> this.updateCaptureTick(SWCClientConfig.CAPTURE_INTERVAL.get()), "Screenshot-With-Coords Client Update");
				UPDATE_THREAD.start();
			} else {
				SWCLogger.warn("Unable to set new update thread. This may related to a memory leak problem.", new RuntimeException());
			}
		}
	}

	@SubscribeEvent
	public void onPlayerLeaveLevel(ClientPlayerNetworkEvent.LoggingOut event) {
		LocalPlayer player = event.getPlayer();
		if(player != null && player.equals(Minecraft.getInstance().player) && RenderSystem.isOnRenderThread()) {
			SWCLogger.info("Bye, " + player.getScoreboardName());
			if(UPDATE_THREAD == null) {
				SWCLogger.warn("Unable to interrupt update thread.", new NullPointerException());
			} else {
				UPDATE_THREAD.interrupt();
				UPDATE_THREAD = null;
				this.isCaptureTick = false;
				saveCoordsAndClearCache();
			}
		}
	}

	private static void saveCoordsAndClearCache() {
		try(
				Writer writer = Files.newBufferedWriter(
						new File(captureDirectory, "coords.jsonl").toPath(),
						StandardCharsets.UTF_8,
						StandardOpenOption.CREATE, StandardOpenOption.APPEND
				)
		) {
			CACHED_ENTRIES.forEach(entry -> {
				String json = entry.toJson();
				try {
					writer.write(json + "\n");
				} catch (IOException e) {
					SWCLogger.error("Error when saving coord: %s.".formatted(json), e);
				}
			});
		} catch (IOException e) {
			SWCLogger.error("Error when saving coords.", e);
		}
		CACHED_ENTRIES.clear();
	}

	private static final DateTimeFormatter FILENAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS", Locale.ROOT);
	/**
	 * @see Screenshot#getFile
	 */
	private static String getFilename() {
		return FILENAME_DATE_TIME_FORMATTER.format(ZonedDateTime.now()) + ".png";
	}
}