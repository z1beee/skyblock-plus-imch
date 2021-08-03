package com.skyblockplus.link;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Hypixel.getGuildFromPlayer;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.api.serversettings.automatedguild.GuildRole;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class LinkCommand extends Command {

	public LinkCommand() {
		this.name = "link";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder linkAccount(String username, Member member, Guild guild) {
		DiscordInfoStruct playerInfo = getPlayerDiscordInfo(username);
		if (playerInfo.isNotValid()) {
			return invalidEmbed(playerInfo.failCause);
		}

		if (!member.getUser().getAsTag().equals(playerInfo.discordTag)) {
			EmbedBuilder eb = defaultEmbed("Discord tag mismatch");
			eb.setDescription(
				"**Player Username:** `" +
				playerInfo.minecraftUsername +
				"`\n**API Discord Tag:** `" +
				playerInfo.discordTag +
				"`\n**Your Discord Tag:** `" +
				member.getUser().getAsTag() +
				"`"
			);
			return eb;
		}

		LinkedAccountModel toAdd = new LinkedAccountModel(
			"" + Instant.now().toEpochMilli(),
			member.getId(),
			playerInfo.minecraftUuid,
			playerInfo.minecraftUsername
		);

		if (database.addLinkedUser(toAdd) == 200) {
			JsonElement verifySettings = database.getVerifySettings(guild.getId());
			if (verifySettings != null) {
				try {
					String nicknameTemplate = higherDepth(verifySettings, "verifiedNickname").getAsString();
					if (!nicknameTemplate.equalsIgnoreCase("none")) {
						nicknameTemplate = nicknameTemplate.replace("[IGN]", playerInfo.minecraftUsername);

						if (nicknameTemplate.contains("[GUILD_RANK]")) {
							try {
								HypixelResponse playerGuild = getGuildFromPlayer(playerInfo.minecraftUuid);
								if (!playerGuild.isNotValid()) {
									GuildRole settingsGuildId = database
										.getAllGuildRoles(guild.getId())
										.stream()
										.filter(guildRole -> guildRole.getGuildId().equalsIgnoreCase(playerGuild.get("_id").getAsString()))
										.findFirst()
										.orElse(null);

									if (settingsGuildId != null) {
										JsonArray guildMembers = playerGuild.get("members").getAsJsonArray();
										for (JsonElement guildMember : guildMembers) {
											if (higherDepth(guildMember, "uuid").getAsString().equals(playerInfo.minecraftUuid)) {
												nicknameTemplate =
													nicknameTemplate.replace(
														"[GUILD_RANK]",
														higherDepth(guildMember, "rank").getAsString()
													);
												break;
											}
										}
									}
								}
							} catch (Exception ignored) {}
						}

						member.modifyNickname(nicknameTemplate).queue();
					}
				} catch (Exception ignored) {}

				try {
					JsonArray verifyRoles = higherDepth(verifySettings, "verifiedRoles").getAsJsonArray();
					for (JsonElement verifyRole : verifyRoles) {
						try {
							guild.addRoleToMember(member.getId(), guild.getRoleById(verifyRole.getAsString())).complete();
						} catch (Exception e) {
							System.out.println(verifyRole);
							e.printStackTrace();
						}
					}
				} catch (Exception ignored) {}
			}

			return defaultEmbed("Success")
				.setDescription("`" + member.getUser().getAsTag() + "` was linked to `" + playerInfo.minecraftUsername + "`");
		} else {
			return invalidEmbed("Error linking `" + member.getUser().getAsTag() + " to `" + playerInfo.minecraftUsername + "`");
		}
	}

	public static EmbedBuilder getLinkedAccount(User user) {
		JsonElement userInfo = database.getLinkedUserByDiscordId(user.getId());

		try {
			return defaultEmbed("Success")
				.setDescription(
					"`" + user.getAsTag() + "` is linked to `" + (higherDepth(userInfo, "minecraftUsername").getAsString()) + "`"
				);
		} catch (Exception e) {
			return invalidEmbed("`" + user.getAsTag() + "` is not linked");
		}
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2) {
					embed(linkAccount(args[1], event.getMember(), event.getGuild()));
					return;
				} else if (args.length == 1) {
					embed(getLinkedAccount(event.getAuthor()));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
