package com.streichelmod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Speichert An/Aus und den Stil in config/streichelmod.properties. */
public final class PetSettings {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("streichelmod.properties");

	public static boolean enabled = true;
	public static int style = 0;

	private PetSettings() {
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		Properties props = new Properties();
		try (InputStream in = Files.newInputStream(FILE)) {
			props.load(in);
			enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
			style = Integer.parseInt(props.getProperty("style", "0"));
		} catch (IOException | NumberFormatException ignored) {
			enabled = true;
			style = 0;
		}
	}

	public static void save() {
		Properties props = new Properties();
		props.setProperty("enabled", Boolean.toString(enabled));
		props.setProperty("style", Integer.toString(style));
		try (OutputStream out = Files.newOutputStream(FILE)) {
			props.store(out, "Streichel Mod");
		} catch (IOException ignored) {
		}
	}
}
