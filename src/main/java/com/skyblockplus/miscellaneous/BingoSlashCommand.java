/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2022 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.ApiHandler.usernameToUuid;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BingoSlashCommand extends SlashCommand {

	public BingoSlashCommand() {
		this.name = "bingo";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.logCommand();

		if (event.invalidPlayerOption()) {
			return;
		}

		event.embed(getPlayerBingo(event.player));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get the current bingo goals and a player's bingo card")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static Object getPlayerBingo(String username) {
		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (!usernameUuidStruct.isValid()) {
			return invalidEmbed(usernameUuidStruct.failCause());
		}

		JsonElement bingoJson = null;
		JsonArray bingoArr = new JsonArray();
		JsonElement bingoInfo = getBingoInfoJson();
		try {
			bingoJson =
				streamJsonArray(
					higherDepth(
						getJson("https://api.hypixel.net/skyblock/bingo?key=" + HYPIXEL_API_KEY + "&uuid=" + usernameUuidStruct.uuid()),
						"events"
					)
						.getAsJsonArray()
				)
					.filter(e -> higherDepth(e, "key", -1) == higherDepth(bingoInfo, "id", -1))
					.findFirst()
					.orElse(null);
			bingoArr = higherDepth(bingoJson, "completed_goals").getAsJsonArray();
		} catch (Exception ignored) {}

		EmbedBuilder eb = defaultEmbed(usernameUuidStruct.username(), skyblockStatsLink(usernameUuidStruct.username(), null));
		StringBuilder regGoals = new StringBuilder();
		StringBuilder communityGoals = new StringBuilder();
		StringBuilder cardStr = new StringBuilder(); // C = community done, c = community not done, S = self done, s = self not done
		for (JsonElement goal : higherDepth(bingoInfo, "goals").getAsJsonArray()) {
			if (higherDepth(goal, "progress", -1) != -1) {
				long progress = higherDepth(goal, "progress").getAsLong();
				JsonArray tiers = higherDepth(goal, "tiers").getAsJsonArray();
				long nextTier = tiers.get(0).getAsLong();
				if (progress >= tiers.get(tiers.size() - 1).getAsLong()) {
					nextTier = tiers.get(tiers.size() - 1).getAsLong();
					cardStr.append("C");
				} else {
					cardStr.append("c");
					for (JsonElement tier : higherDepth(goal, "tiers").getAsJsonArray()) {
						if (tier.getAsLong() > progress) {
							nextTier = tier.getAsInt();
							break;
						}
					}
				}
				communityGoals
					.append("\n ")
					.append(progress >= nextTier ? client.getSuccess() : client.getError())
					.append(" ")
					.append(higherDepth(goal, "name").getAsString())
					.append(": ")
					.append(simplifyNumber(progress))
					.append("/")
					.append(simplifyNumber(nextTier))
					.append(" (")
					.append((roundProgress((double) Math.min(progress, nextTier) / nextTier)))
					.append(")");
			} else {
				boolean completedGoal = streamJsonArray(bingoArr)
					.anyMatch(g -> g.getAsString().equals(higherDepth(goal, "id").getAsString()));
				cardStr.append(completedGoal ? "S" : "s");
				regGoals
					.append("\n")
					.append(completedGoal ? client.getSuccess() : client.getError())
					.append(" ")
					.append(higherDepth(goal, "name").getAsString())
					.append(": ")
					.append(parseMcCodes(higherDepth(goal, "lore").getAsString()));
			}
		}
		eb.setDescription(
			(
				bingoJson == null
					? "**No active bingo profile found**"
					: (
						"**Goals Completed:** " +
						(bingoArr.size() + StringUtils.countOccurrencesOf(cardStr.toString(), "C")) +
						"\n**Points:** " +
						higherDepth(bingoJson, "points", 0)
					)
			)
		);
		eb.appendDescription("\n\n**Self Goals:**" + regGoals);
		eb.appendDescription("\n\n**Community Goals:**" + communityGoals);
		eb.setThumbnail(usernameUuidStruct.getAvatarlUrl());
		return new MessageBuilder().setEmbeds(eb.build()).setActionRows(ActionRow.of(Button.primary("bingo_" + cardStr, "Bingo Card")));
	}
}
