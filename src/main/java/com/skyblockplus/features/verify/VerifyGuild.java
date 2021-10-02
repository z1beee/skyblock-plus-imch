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

package com.skyblockplus.features.verify;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Hypixel.getGuildFromPlayer;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.api.serversettings.automatedguild.GuildRole;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class VerifyGuild {

	public final String guildId;
	public TextChannel messageChannel;
	public Message originalMessage;
	public JsonElement verifySettings;
	public boolean enable = true;

	public VerifyGuild(TextChannel messageChannel, Message originalMessage, JsonElement verifySettings, String guildId) {
		this.messageChannel = messageChannel;
		this.originalMessage = originalMessage;
		this.verifySettings = verifySettings;
		this.guildId = guildId;
	}

	public VerifyGuild(String guildId) {
		this.enable = false;
		this.guildId = guildId;
	}

	public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (!enable) {
			return false;
		}

		if (!event.getChannel().getId().equals(messageChannel.getId())) {
			return false;
		}

		if (event.getMessage().getId().equals(originalMessage.getId())) {
			return false;
		}

		if (!event.getAuthor().getId().equals(jda.getSelfUser().getId())) {
			if (event.getAuthor().isBot()) {
				return false;
			}

			if (!event.getMessage().getContentRaw().startsWith(getGuildPrefix(event.getGuild().getId()) + "link ")) {
				event.getMessage().delete().queue();
				return true;
			}
		}

		event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
		return true;
	}

	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		if (verifySettings == null || !higherDepth(verifySettings, "enableMemberJoinSync").getAsString().equals("true")) {
			return;
		}

		JsonElement linkedUser = database.getLinkedUserByDiscordId(event.getUser().getId());
		if (linkedUser == null) {
			return;
		}

		String updatedNickname = "false";
		String updatedRoles = "false";

		try {
			String nicknameTemplate = higherDepth(verifySettings, "verifiedNickname").getAsString();
			if (!nicknameTemplate.equalsIgnoreCase("none") && !nicknameTemplate.isEmpty()) {
				nicknameTemplate = nicknameTemplate.replace("[IGN]", higherDepth(linkedUser, "minecraftUsername").getAsString());

				if (nicknameTemplate.contains("[GUILD_RANK]")) {
					try {
						HypixelResponse playerGuild = getGuildFromPlayer(higherDepth(linkedUser, "minecraftUuid").getAsString());
						if (!playerGuild.isNotValid()) {
							GuildRole settingsGuildId = database
								.getAllGuildRoles(event.getGuild().getId())
								.stream()
								.filter(guildRole -> guildRole.getGuildId().equalsIgnoreCase(playerGuild.get("_id").getAsString()))
								.findFirst()
								.orElse(null);

							if (settingsGuildId != null) {
								JsonArray guildMembers = playerGuild.get("members").getAsJsonArray();
								for (JsonElement guildMember : guildMembers) {
									if (
										higherDepth(guildMember, "uuid")
											.getAsString()
											.equals(higherDepth(linkedUser, "minecraftUuid").getAsString())
									) {
										nicknameTemplate =
											nicknameTemplate.replace("[GUILD_RANK]", higherDepth(guildMember, "rank").getAsString());
										break;
									}
								}
							}
						}
					} catch (Exception ignored) {}
				}

				event.getMember().modifyNickname(nicknameTemplate).queue();
				updatedNickname = "true";
			}
		} catch (Exception e) {
			updatedNickname = "error";
		}

		try {
			JsonArray verifyRoles = higherDepth(verifySettings, "verifiedRoles").getAsJsonArray();
			for (JsonElement verifyRole : verifyRoles) {
				try {
					event
						.getGuild()
						.addRoleToMember(event.getMember().getId(), event.getGuild().getRoleById(verifyRole.getAsString()))
						.complete();
					updatedRoles = "true";
				} catch (Exception e) {
					System.out.println(verifyRole);
					e.printStackTrace();
					updatedRoles = "error";
				}
			}
		} catch (Exception e) {
			updatedRoles = "error";
		}

		String finalUpdatedNickname = updatedNickname;
		String finalUpdatedRoles = updatedRoles;
		event
			.getUser()
			.openPrivateChannel()
			.queue(privateChannel ->
				privateChannel
					.sendMessageEmbeds(
						defaultEmbed("Member synced")
							.setDescription(
								"You have automatically been synced in `" +
								event.getGuild().getName() +
								"`" +
								(
									!finalUpdatedRoles.equals("false")
										? finalUpdatedRoles.equals("true")
											? "\n• Successfully synced your roles"
											: "\n• Error syncing your roles"
										: ""
								) +
								(
									!finalUpdatedNickname.equals("false")
										? finalUpdatedNickname.equals("true")
											? "\n• Successfully synced your nickname"
											: "\n• Error syncing your nickname"
										: ""
								)
							)
							.build()
					)
					.queue()
			);
	}

	public void reloadSettingsJson(JsonElement newVerifySettings) {
		if (higherDepth(newVerifySettings, "enableMemberJoinSync", "").equals("true")) {
			verifySettings = database.getVerifySettings(guildId);
		}
	}
}
