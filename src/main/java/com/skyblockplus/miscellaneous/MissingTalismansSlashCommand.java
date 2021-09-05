package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;

public class MissingTalismansSlashCommand extends SlashCommand {

	public MissingTalismansSlashCommand() {
		this.name = "missing";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(MissingTalismansCommand.getMissingTalismans(event.player, event.getOptionStr("profile")));
	}
}
