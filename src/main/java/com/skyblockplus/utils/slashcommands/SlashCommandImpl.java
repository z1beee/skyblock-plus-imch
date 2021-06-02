package com.skyblockplus.utils.slashcommands;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommandImpl extends ListenerAdapter {

	private List<SlashCommand> slashCommands;
	private HashMap<String, OffsetDateTime> cooldowns;

	public SlashCommandImpl() {
		this.slashCommands = new ArrayList<>();
		this.cooldowns = new HashMap<>();
	}

	public SlashCommandImpl addSlashCommmands(SlashCommand... commands) {
		for (SlashCommand command : commands) {
			slashCommands.add(command);
		}
		return this;
	}

	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		if (event.getGuild() == null) {
			return;
		}

		event.deferReply().complete();

		SlashCommandExecutedEvent slashCommandExecutedEvent = new SlashCommandExecutedEvent(event, this);
		for (SlashCommand command : slashCommands) {
			if (command.getName().equals(event.getName())) {
				int remainingCooldown = command.getRemainingCooldown(slashCommandExecutedEvent);
				if (remainingCooldown > 0) {
					command.replyCooldown(slashCommandExecutedEvent, remainingCooldown);
				} else {
					command.execute(slashCommandExecutedEvent);
				}

				return;
			}
		}

		slashCommandExecutedEvent.getHook().editOriginal("Invalid Command").queue();
	}

	public int getRemainingCooldown(String name) {
		if (cooldowns.containsKey(name)) {
			int time = (int) Math.ceil(OffsetDateTime.now().until(cooldowns.get(name), ChronoUnit.MILLIS) / 1000D);
			if (time <= 0) {
				cooldowns.remove(name);
				return 0;
			}
			return time;
		}
		return 0;
	}

	public void applyCooldown(String name, int seconds) {
		cooldowns.put(name, OffsetDateTime.now().plusSeconds(seconds));
	}
}
