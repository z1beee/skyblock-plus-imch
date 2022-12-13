/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
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

package com.skyblockplus.guild;

import static com.skyblockplus.utils.ApiHandler.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class CheckGuildApiSlashCommand extends SlashCommand {

	public CheckGuildApiSlashCommand() {
		this.name = "check-guild-api";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getGuildCheckApi(event.player, event.getOptionStr("exclude", ""), event));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get which Skyblock APIs players have enabled or disabled for a guild")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOption(OptionType.STRING, "exclude", "Exclude certain APIs from being checked (comma separated)", false);
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getGuildCheckApi(String username, String exclude, SlashCommandEvent event) {
		List<String> excludeArr = new ArrayList<>();
		if (!exclude.isEmpty()) {
			excludeArr.addAll(List.of(exclude.toLowerCase().split(",")));
			for (String s : excludeArr) {
				if (!List.of("inventory", "bank", "collections", "vault", "skills").contains(s)) {
					return invalidEmbed("Invalid exclude type: " + s);
				}
			}
		}

		String hypixelKey = database.getServerHypixelApiKey(event.getGuild().getId());

		EmbedBuilder eb = checkHypixelKey(hypixelKey);
		if (eb != null) {
			return eb;
		}

		UsernameUuidStruct usernameUuid = usernameToUuid(username);
		if (!usernameUuid.isValid()) {
			return invalidEmbed(usernameUuid.failCause());
		}

		HypixelResponse guildResponse = getGuildFromPlayer(usernameUuid.uuid());
		if (!guildResponse.isValid()) {
			return invalidEmbed(guildResponse.failCause());
		}

		JsonArray guildMembers = guildResponse.get("members").getAsJsonArray();
		List<CompletableFuture<String>> futuresList = new ArrayList<>();
		List<Player> players = new ArrayList<>();

		for (JsonElement guildMember : guildMembers) {
			String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();

			try {
				if (keyCooldownMap.get(hypixelKey).isRateLimited()) {
					System.out.println("Sleeping for " + keyCooldownMap.get(hypixelKey).getTimeTillReset() + " seconds");
					TimeUnit.SECONDS.sleep(keyCooldownMap.get(hypixelKey).getTimeTillReset());
				}
			} catch (Exception ignored) {}

			futuresList.add(
				asyncSkyblockProfilesFromUuid(guildMemberUuid, hypixelKey)
					.thenApplyAsync(
						guildMemberProfileJsonResponse -> {
							Player player = new Player(
								guildMemberUuid,
								uuidToUsername(guildMemberUuid).username(),
								guildMemberProfileJsonResponse,
								false
							);

							if (player.isValid()) {
								boolean invEnabled = excludeArr.contains("inventory") || player.isInventoryApiEnabled();
								boolean bankEnabled = excludeArr.contains("bank") || player.isBankApiEnabled();
								boolean collectionsEnabled = excludeArr.contains("collections") || player.isCollectionsApiEnabled();
								boolean vaultEnabled = excludeArr.contains("vault") || player.isVaultApiEnabled();
								boolean skillsEnabled = excludeArr.contains("skills") || player.isSkillsApiEnabled();

								if (invEnabled && bankEnabled && collectionsEnabled && vaultEnabled && skillsEnabled) {
									return client.getSuccess() + " **" + player.getUsernameFixed() + ":** all APIs enabled";
								} else {
									String out =
										(invEnabled ? "" : "Inventory API, ") +
										(bankEnabled ? "" : "Bank API, ") +
										(collectionsEnabled ? "" : "Collections API, ") +
										(vaultEnabled ? "" : "Vault API, ") +
										(skillsEnabled ? "" : "Skills API, ");

									return (
										client.getError() + " **" + player.getUsernameFixed() + ":** " + out.substring(0, out.length() - 2)
									);
								}
							}
							return client.getError() + " **" + player.getUsernameFixed() + ":** unable to get data";
						},
						executor
					)
			);
		}

		List<String> out = new ArrayList<>();
		for (CompletableFuture<String> future : futuresList) {
			try {
				out.add(future.get());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		leaderboardDatabase.insertIntoLeaderboard(players);

		out.sort(Comparator.comparing(o -> !o.contains(client.getError())));
		CustomPaginator.Builder paginator = event.getPaginator().setItemsPerPage(20);
		paginator.addItems(out);
		event.paginate(
			paginator.updateExtras(extra ->
				extra
					.setEveryPageTitle(guildResponse.get("name").getAsString())
					.setEveryPageText(
						"**API Disabled Count:** " +
						out.stream().filter(o -> o.contains(client.getError())).count() +
						"\n" +
						(!excludeArr.isEmpty() ? "**Excluded APIs:** " + String.join(", ", excludeArr) + "\n" : "")
					)
			)
		);
		return null;
	}
}
