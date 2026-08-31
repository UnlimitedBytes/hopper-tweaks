package de.unlimitedbytes.hoppertweaks;

import de.unlimitedbytes.hoppertweaks.mixin.MinecraftServerAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the hopper transfer amount of each loaded world (save).
 *
 * <p>Vanilla's value is one. The value lives in a {@code hoppertweaks.properties}
 * file inside the world folder, so every world keeps its own speed. The command
 * deliberately limits the value to a normal Minecraft stack size, so a transfer
 * cannot exceed the capacity of a single destination slot.</p>
 */
public final class HopperSpeed {
	public static final int MIN_ITEMS_PER_CYCLE = 1;
	public static final int MAX_ITEMS_PER_CYCLE = 64;

	private static final Logger LOGGER = LoggerFactory.getLogger(HopperTweaks.MOD_ID);
	private static final String CONFIG_KEY = "itemsPerCycle";
	private static final String CONFIG_FILE_NAME = "hoppertweaks.properties";
	/** Pre per-world storage, the value lived in the global config folder. Kept as a one-time migration source. */
	private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("hopper-tweaks.properties");

	private static final ConcurrentHashMap<MinecraftServer, Integer> ITEMS_PER_CYCLE = new ConcurrentHashMap<>();
	/** Hopper transfers tick on the server thread, so the running server identifies the current world. */
	private static final AtomicReference<MinecraftServer> ACTIVE_SERVER = new AtomicReference<>();

	private HopperSpeed() {
	}

	public static int getItemsPerCycle(Level level) {
		return getItemsPerCycle(level != null ? level.getServer() : ACTIVE_SERVER.get());
	}

	/** Resolves the value for code that has no {@link Level} at hand; hopper logic only runs for the active server. */
	public static int getItemsPerCycle(MinecraftServer server) {
		if (server == null) {
			return MIN_ITEMS_PER_CYCLE;
		}
		return ITEMS_PER_CYCLE.getOrDefault(server, MIN_ITEMS_PER_CYCLE);
	}

	public static int getItemsPerCycle() {
		return getItemsPerCycle(ACTIVE_SERVER.get());
	}

	/** Loads the world's saved value when its server starts. */
	public static void attachServer(MinecraftServer server) {
		ACTIVE_SERVER.set(server);

		int itemsPerCycle = MIN_ITEMS_PER_CYCLE;
		Path worldConfig = configPath(server);
		if (Files.isRegularFile(worldConfig)) {
			itemsPerCycle = read(worldConfig);
		} else if (Files.isRegularFile(LEGACY_CONFIG_PATH)) {
			// migrate the old global setting into this world on first load
			itemsPerCycle = read(LEGACY_CONFIG_PATH);
			write(worldConfig, itemsPerCycle);
		}
		ITEMS_PER_CYCLE.put(server, itemsPerCycle);
	}

	/** Forgets the world's runtime value when its server stops. */
	public static void detachServer(MinecraftServer server) {
		ITEMS_PER_CYCLE.remove(server);
		ACTIVE_SERVER.compareAndSet(server, null);
	}

	public static void setItemsPerCycle(MinecraftServer server, int itemsPerCycle) {
		validate(itemsPerCycle);
		ITEMS_PER_CYCLE.put(server, itemsPerCycle);
		write(configPath(server), itemsPerCycle);
	}

	/**
	 * Applies a change made in the client config screen to the currently running
	 * (integrated) server. Returns {@code false} when no world is running, since
	 * the value is stored per world and cannot be set from the main menu.
	 */
	public static boolean setItemsPerCycleOnActiveServer(int itemsPerCycle) {
		MinecraftServer server = ACTIVE_SERVER.get();
		if (server == null) {
			return false;
		}
		setItemsPerCycle(server, itemsPerCycle);
		return true;
	}

	private static Path configPath(MinecraftServer server) {
		return ((MinecraftServerAccessor) server)
				.hoppertweaks$getStorageSource()
				.getLevelPath(LevelResource.ROOT)
				.resolve(CONFIG_FILE_NAME);
	}

	private static void validate(int itemsPerCycle) {
		if (itemsPerCycle < MIN_ITEMS_PER_CYCLE || itemsPerCycle > MAX_ITEMS_PER_CYCLE) {
			throw new IllegalArgumentException("itemsPerCycle must be between 1 and 64");
		}
	}

	private static int read(Path path) {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(path)) {
			properties.load(input);
			int itemsPerCycle = Integer.parseInt(properties.getProperty(CONFIG_KEY));
			validate(itemsPerCycle);
			return itemsPerCycle;
		} catch (IOException | IllegalArgumentException exception) {
			LOGGER.warn("Could not load {}; using the vanilla hopper speed.", path, exception);
			return MIN_ITEMS_PER_CYCLE;
		}
	}

	private static void write(Path path, int itemsPerCycle) {
		Properties properties = new Properties();
		properties.setProperty(CONFIG_KEY, Integer.toString(itemsPerCycle));

		try {
			Files.createDirectories(path.getParent());
			try (OutputStream output = Files.newOutputStream(path)) {
				properties.store(output, "Hopper Tweaks configuration for this world");
			}
		} catch (IOException exception) {
			LOGGER.warn("Could not save {}.", path, exception);
		}
	}
}
