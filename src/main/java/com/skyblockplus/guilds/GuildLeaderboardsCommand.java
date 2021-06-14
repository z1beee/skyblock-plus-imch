package com.skyblockplus.guilds;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.*;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.Utils.checkHypixelKey;

public class GuildLeaderboardsCommand extends Command {
	public static HashMap<String, HypixelKeyInformation> keyCooldownMap = new HashMap<>();

	public GuildLeaderboardsCommand() {
		this.name = "guild-leaderboard";
		this.cooldown = globalCooldown + 1;
		this.aliases = new String[] { "g-lb" };
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");
				logCommand(event.getGuild(), event.getAuthor(), content);

				if (args.length != 3) {
					eb = errorMessage(this.name);
					ebMessage.editMessage(eb.build()).queue();
					return;
				}

				if (args[2].toLowerCase().startsWith("u:")) {
					eb = getLeaderboard(args[1], args[2].split(":")[1], event);

					if (eb == null) {
						ebMessage.delete().queue();
					} else {
						ebMessage.editMessage(eb.build()).queue();
					}
					return;
				}

				ebMessage.editMessage(errorMessage(this.name).build()).queue();
			}
		)
			.start();
	}

	private EmbedBuilder getLeaderboard(String lbType, String username, CommandEvent event) {
		String HYPIXEL_KEY = database.getServerHypixelApiKey(event.getGuild().getId());

		EmbedBuilder eb = checkHypixelKey(HYPIXEL_KEY);
		if (eb != null) {
			return eb;
		}

		if (!(lbType.equals("slayer") || lbType.equals("skills") || lbType.equals("catacombs") || lbType.equals("weight"))) {
			return defaultEmbed(lbType + " is an invalid leaderboard type");
		}

		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct == null) {
			return defaultEmbed("Invalid username");
		}

		JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_KEY + "&player=" + usernameUuidStruct.playerUuid);
		String guildName;

		try {
			guildName = higherDepth(guildJson, "guild.name").getAsString();
		} catch (Exception e) {
			return defaultEmbed(usernameUuidStruct.playerUsername + " is not in a guild");
		}

		JsonArray guildMembers = higherDepth(guildJson, "guild.members").getAsJsonArray();
		List<CompletableFuture<CompletableFuture<String>>> futuresList = new ArrayList<>();

		for (JsonElement guildMember : guildMembers) {
			String guildMemberUuid = higherDepth(guildMember, "uuid").getAsString();
			try {
				if (keyCooldownMap.get(HYPIXEL_KEY).remainingLimit.get() < 5) {
					System.out.println("Sleeping for " + keyCooldownMap.get(HYPIXEL_KEY).timeTillReset + " seconds");
					TimeUnit.SECONDS.sleep(keyCooldownMap.get(HYPIXEL_KEY).timeTillReset.get());
				}
			} catch (Exception ignored) {}

			futuresList.add(
				asyncHttpClient
					.prepareGet("https://api.ashcon.app/mojang/v2/user/" + guildMemberUuid)
					.execute()
					.toCompletableFuture()
					.thenApply(
						uuidToUsernameResponse -> {
							try {
								return higherDepth(JsonParser.parseString(uuidToUsernameResponse.getResponseBody()), "username")
									.getAsString();
							} catch (Exception ignored) {}
							return null;
						}
					)
					.thenApply(
						guildMemberUsernameResponse ->
							asyncHttpClient
								.prepareGet("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_KEY + "&uuid=" + guildMemberUuid)
								.execute()
								.toCompletableFuture()
								.thenApply(
									guildMemberOuterProfileJsonResponse -> {
										try {
											try {
												keyCooldownMap.get(HYPIXEL_KEY).remainingLimit.set(
													Integer.parseInt(guildMemberOuterProfileJsonResponse.getHeader("RateLimit-Remaining"))
												);
												keyCooldownMap.get(HYPIXEL_KEY).timeTillReset.set(
													Integer.parseInt(guildMemberOuterProfileJsonResponse.getHeader("RateLimit-Reset"))
												);
											} catch (Exception ignored) {}

											JsonElement guildMemberOuterProfileJson = JsonParser.parseString(
												guildMemberOuterProfileJsonResponse.getResponseBody()
											);

											Player guildMemberPlayer = new Player(
												guildMemberUuid,
												guildMemberUsernameResponse,
												guildMemberOuterProfileJson
											);

											if (guildMemberPlayer.isValid()) {
												switch (lbType) {
													case "slayer":
														return guildMemberUsernameResponse + "=:=" + guildMemberPlayer.getTotalSlayer();
													case "skills":
														int totalSkillsXp = guildMemberPlayer.getTotalSkillsXp();
														if (totalSkillsXp != -1) {
															return guildMemberUsernameResponse + "=:=" + totalSkillsXp;
														}
														break;
													case "catacombs":
														return (
															guildMemberUsernameResponse +
															"=:=" +
															guildMemberPlayer.getCatacombsSkill().totalSkillExp
														);
													case "weight":
														return guildMemberUsernameResponse + "=:=" + guildMemberPlayer.getWeight();
												}
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
										return null;
									}
								)
					)
			);
		}

		List<String> guildMemberPlayersList = new ArrayList<>();
		for (CompletableFuture<CompletableFuture<String>> future : futuresList) {
			try {
				String playerFutureResponse = future.get().get();
				if (playerFutureResponse != null) {
					guildMemberPlayersList.add(playerFutureResponse);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		guildMemberPlayersList.sort(Comparator.comparingDouble(o1 -> -Double.parseDouble(o1.split("=:=")[1])));

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(2).setItemsPerPage(20);

		int guildRank = -1;
		String amt = "-1";
		for (int i = 0, guildMemberPlayersListSize = guildMemberPlayersList.size(); i < guildMemberPlayersListSize; i++) {
			String[] guildPlayer = guildMemberPlayersList.get(i).split("=:=");
			String formattedAmt = roundAndFormat(Double.parseDouble(guildPlayer[1]));
			paginateBuilder.addItems("`" + (i + 1) + ")` " + fixUsername(guildPlayer[0]) + ": " + formattedAmt);

			if (guildPlayer[0].equals(usernameUuidStruct.playerUsername)) {
				guildRank = i;
				amt = formattedAmt;
			}
		}

		String ebStr =
			"**Player:** " +
			usernameUuidStruct.playerUsername +
			"\n**Guild Rank:** #" +
			(guildRank + 1) +
			"\n**" +
			capitalizeString(lbType) +
			":** " +
			amt;

		paginateBuilder
			.setPaginatorExtras(
				new PaginatorExtras()
					.setEveryPageTitle(guildName)
					.setEveryPageText(ebStr)
					.setEveryPageTitleUrl(
						"https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(guildJson, "guild._id").getAsString()
					)
			)
			.build()
			.paginate(event.getChannel(), 0);

		return null;
	}
}
