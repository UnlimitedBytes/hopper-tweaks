package de.unlimitedbytes.hoppertweaks;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HopperTweaks implements ModInitializer {
	public static final String MOD_ID = "hoppertweaks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(HopperSpeed::attachServer);
		ServerLifecycleEvents.SERVER_STOPPED.register(HopperSpeed::detachServer);

		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
				Commands.literal("hopperspeed")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
						.executes(commandContext -> showHopperSpeed(commandContext.getSource()))
						.then(Commands.argument("items_per_cycle", IntegerArgumentType.integer(
								HopperSpeed.MIN_ITEMS_PER_CYCLE,
								HopperSpeed.MAX_ITEMS_PER_CYCLE
						))
								.executes(commandContext -> setHopperSpeed(
										commandContext.getSource(),
										IntegerArgumentType.getInteger(commandContext, "items_per_cycle")
								)))
		));

		LOGGER.info("Hopper Tweaks initialized");
	}

	private static int showHopperSpeed(net.minecraft.commands.CommandSourceStack source) {
		int itemsPerCycle = HopperSpeed.getItemsPerCycle(source.getServer());
		String vanillaNote = itemsPerCycle == HopperSpeed.MIN_ITEMS_PER_CYCLE ? " (vanilla default)" : "";
		source.sendSuccess(
				() -> Component.literal("This world's hopper transfer amount is currently " + itemsPerCycle + " item(s) per cycle" + vanillaNote + "."),
				false
		);
		return itemsPerCycle;
	}

	private static int setHopperSpeed(net.minecraft.commands.CommandSourceStack source, int itemsPerCycle) {
		HopperSpeed.setItemsPerCycle(source.getServer(), itemsPerCycle);
		source.sendSuccess(
				() -> Component.literal("Hopper transfer amount for this world set to " + itemsPerCycle + " item(s) per cycle."),
				true
		);
		return itemsPerCycle;
	}
}
