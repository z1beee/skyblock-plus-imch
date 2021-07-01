package com.skyblockplus.price;

import static com.skyblockplus.Main.executor;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class AverageAuctionSlashCommand extends SlashCommand {

	public AverageAuctionSlashCommand() {
		this.name = "average";
	}

	@Override
	protected void execute(SlashCommandExecutedEvent event) {
		executor.submit(
			() -> {
				event.logCommandGuildUserCommand();

				EmbedBuilder eb = AverageAuctionCommand.getAverageAuctionPrice(event.getOptionStr("item"));

				event.getHook().editOriginalEmbeds(eb.build()).queue();
			}
		);
	}
}
