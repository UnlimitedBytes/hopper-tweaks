package de.unlimitedbytes.hoppertweaks.gui;

import de.unlimitedbytes.hoppertweaks.HopperSpeed;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Per-world settings screen. The value shown is the one of the currently
 * running (integrated) server world; saving writes it to that world's
 * {@code hoppertweaks.properties}, exactly like the {@code /hopperspeed}
 * command does. From the main menu there is no world to configure, so the
 * screen reports that instead.
 */
public final class HopperTweaksConfigScreen {
	private static final MutableComponent TITLE = Component.literal("Hopper Tweaks");
	private static final MutableComponent ITEMS_PER_CYCLE = Component.literal("Items per hopper transfer");
	private static final MutableComponent TOOLTIP = Component.literal(
			"How many items a hopper moves in one transfer cycle (vanilla: 1). Composters always receive one item per cycle.");

	private HopperTweaksConfigScreen() {
	}

	public static net.minecraft.client.gui.screens.Screen build(net.minecraft.client.gui.screens.Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(TITLE);

		int[] editedValue = {HopperSpeed.getItemsPerCycle()};
		boolean[] worldRunning = {true};

		builder.setSavingRunnable(() -> {
			if (worldRunning[0] && !HopperSpeed.setItemsPerCycleOnActiveServer(editedValue[0])) {
				worldRunning[0] = false;
			}
		});

		ConfigEntryBuilder entryBuilder = builder.entryBuilder();
		ConfigCategory category = builder.getOrCreateCategory(Component.literal("General"));
		category.addEntry(entryBuilder.startIntField(ITEMS_PER_CYCLE, editedValue[0])
				.setMin(HopperSpeed.MIN_ITEMS_PER_CYCLE)
				.setMax(HopperSpeed.MAX_ITEMS_PER_CYCLE)
				.setTooltip(TOOLTIP)
				.setSaveConsumer(value -> editedValue[0] = value)
				.build());

		if (HopperSpeed.getItemsPerCycle() == HopperSpeed.MIN_ITEMS_PER_CYCLE) {
			category.addEntry(entryBuilder.startTextDescription(
							Component.literal("Currently at the vanilla default."))
					.build());
		}

		return builder.build();
	}
}
