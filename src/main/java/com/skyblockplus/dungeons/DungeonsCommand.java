package com.skyblockplus.dungeons;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Constants.dungeonClassNames;
import static com.skyblockplus.utils.Constants.dungeonEmojiMap;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.SkillsStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class DungeonsCommand extends Command {

	public DungeonsCommand() {
		this.name = "dungeons";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "cata", "catacombs" };
	}

	public static EmbedBuilder getPlayerDungeons(
		String username,
		String profileName,
		User user,
		MessageChannel channel,
		InteractionHook hook
	) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			try {
				CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, user).setColumns(3).setItemsPerPage(9);
				PaginatorExtras extras = new PaginatorExtras();
				extras
					.setEveryPageTitle(player.getUsername())
					.setEveryPageTitleUrl(player.skyblockStatsLink())
					.setEveryPageText("**Secrets:** " + formatNumber(player.getDungeonSecrets()));

				SkillsStruct skillInfo = player.getCatacombsSkill();
				extras.addEmbedField(
					dungeonEmojiMap.get("catacombs") + " " + capitalizeString(skillInfo.skillName) + " (" + skillInfo.skillLevel + ")",
					simplifyNumber(skillInfo.expCurrent) +
					" / " +
					simplifyNumber(skillInfo.expForNext) +
					"\nTotal XP: " +
					simplifyNumber(skillInfo.totalSkillExp) +
					"\nProgress: " +
					roundProgress(skillInfo.progressToNext),
					true
				);

				extras.addBlankField(true).addBlankField(true);

				for (String className : dungeonClassNames) {
					skillInfo = player.getDungeonClass(className);
					extras.addEmbedField(
						dungeonEmojiMap.get(className) + " " + capitalizeString(className) + " (" + skillInfo.skillLevel + ")",
						simplifyNumber(skillInfo.expCurrent) +
						" / " +
						simplifyNumber(skillInfo.expForNext) +
						"\nTotal XP: " +
						simplifyNumber(skillInfo.totalSkillExp) +
						"\nProgress: " +
						roundProgress(skillInfo.progressToNext),
						true
					);
				}

				extras.addBlankField(true);

				for (String dungeonType : getJsonKeys(higherDepth(player.profileJson(), "dungeons.dungeon_types"))) {
					JsonElement curDungeonType = higherDepth(player.profileJson(), "dungeons.dungeon_types." + dungeonType);
					int min = (dungeonType.equals("catacombs") ? 0 : 1);
					int max = (dungeonType.equals("catacombs") ? 8 : 7);
					for (int i = min; i < max; i++) {
						int fastestSPlusInt = higherDepth(curDungeonType, "fastest_time_s_plus." + i, -1);
						int minutes = fastestSPlusInt / 1000 / 60;
						int seconds = fastestSPlusInt / 1000 % 60;
						String name = i == 0 ? "Entrance" : ((dungeonType.equals("catacombs") ? "Floor " : "Master ") + i);

						String ebStr = "Completions: " + higherDepth(curDungeonType, "tier_completions." + i, 0);
						ebStr += "\nBest Score: " + higherDepth(curDungeonType, "best_score." + i, 0);
						ebStr +=
							"\nFastest S+: " + (fastestSPlusInt != -1 ? minutes + ":" + (seconds >= 10 ? seconds : "0" + seconds) : "None");

						extras.addEmbedField(dungeonEmojiMap.get(dungeonType + "_" + i) + " " + capitalizeString(name), ebStr, true);
					}

					if (dungeonType.equals("catacombs")) {
						extras.addBlankField(true);
					}
				}

				if (channel != null) {
					paginateBuilder.setPaginatorExtras(extras).build().paginate(channel, 0);
				} else {
					paginateBuilder.setPaginatorExtras(extras).build().paginate(hook, 0);
				}
				return null;
			} catch (Exception e) {
				return invalidEmbed("Player has not played dungeons");
			}
		}

		return invalidEmbed(player.getFailCause());
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 || args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerDungeons(username, args.length == 3 ? args[2] : null, event.getAuthor(), event.getChannel(), null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
