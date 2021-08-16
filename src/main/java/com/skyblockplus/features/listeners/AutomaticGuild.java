package com.skyblockplus.features.listeners;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.features.skyblockevent.SkyblockEventCommand.endSkyblockEvent;
import static com.skyblockplus.utils.Hypixel.getGuildFromId;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.*;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.api.serversettings.automatedapply.AutomatedApply;
import com.skyblockplus.api.serversettings.automatedguild.GuildRank;
import com.skyblockplus.api.serversettings.automatedguild.GuildRole;
import com.skyblockplus.api.serversettings.skyblockevent.EventMember;
import com.skyblockplus.features.apply.ApplyGuild;
import com.skyblockplus.features.apply.ApplyUser;
import com.skyblockplus.features.setup.SetupCommandHandler;
import com.skyblockplus.features.skyblockevent.SkyblockEvent;
import com.skyblockplus.features.verify.VerifyGuild;
import com.skyblockplus.utils.structs.HypixelResponse;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.apache.commons.collections4.ListUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticGuild {

	private static final Logger log = LoggerFactory.getLogger(AutomaticGuild.class);

	/* Automated Apply */
	public final List<ApplyGuild> applyGuild = new ArrayList<>();
	/* Miscellaneous */
	public final String guildId;
	public final List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();
	/* Automated Verify */
	public VerifyGuild verifyGuild = new VerifyGuild();
	/* Skyblock event */
	public SkyblockEvent skyblockEvent = new SkyblockEvent();
	public List<EventMember> eventMemberList = new ArrayList<>();
	public Instant eventMemberListLastUpdated = null;
	/* Mee6 Roles */
	public JsonElement currentMee6Settings;
	public Instant lastMee6RankUpdate = null;
	public String prefix;

	/* Constructor */
	public AutomaticGuild(GenericGuildEvent event) {
		guildId = event.getGuild().getId();
		applyConstructor(event);
		verifyConstructor(event);
		schedulerConstructor();
		currentMee6Settings = database.getMee6Settings(guildId);
		prefix = database.getPrefix(guildId);
	}

	public static String getGuildPrefix(String guildId) {
		AutomaticGuild automaticGuild = guildMap.getOrDefault(guildId, null);
		return automaticGuild != null ? automaticGuild.prefix : DEFAULT_PREFIX;
	}

	/* Automated Apply Methods */
	public void applyConstructor(GenericGuildEvent event) {
		List<AutomatedApply> currentSettings = database.getAllApplySettings(event.getGuild().getId());
		if (currentSettings == null) {
			return;
		}

		for (AutomatedApply currentSetting : currentSettings) {
			try {
				if (currentSetting.getEnable() == null || currentSetting.getEnable().equalsIgnoreCase("false")) {
					continue;
				}

				TextChannel reactChannel = event.getGuild().getTextChannelById(currentSetting.getMessageTextChannelId());

				EmbedBuilder eb = defaultEmbed("Apply For Guild");
				eb.setDescription(currentSetting.getMessageText());

				try {
					Message reactMessage = reactChannel.retrieveMessageById(currentSetting.getPreviousMessageId()).complete();
					reactMessage
						.editMessageEmbeds(eb.build())
						.setActionRow(Button.primary("create_application_button_" + currentSetting.getName(), "Apply Here"))
						.queue();

					applyGuild.removeIf(o1 -> higherDepth(o1.currentSettings, "name").getAsString().equals(currentSetting.getName()));
					applyGuild.add(new ApplyGuild(reactMessage, new Gson().toJsonTree(currentSetting)));
				} catch (Exception e) {
					Message reactMessage = reactChannel
						.sendMessageEmbeds(eb.build())
						.setActionRow(Button.primary("create_application_button_" + currentSetting.getName(), "Apply Here"))
						.complete();

					currentSetting.setPreviousMessageId(reactMessage.getId());
					database.setApplySettings(event.getGuild().getId(), new Gson().toJsonTree(currentSetting));

					applyGuild.removeIf(o1 -> higherDepth(o1.currentSettings, "name").getAsString().equals(currentSetting.getName()));
					applyGuild.add(new ApplyGuild(reactMessage, new Gson().toJsonTree(currentSetting)));
				}
			} catch (Exception e) {
				log.error("Apply constructor error - " + event.getGuild().getId(), e);
			}
		}
	}

	public String reloadApplyConstructor(String guildId) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			return "Invalid guild";
		}

		List<AutomatedApply> currentSettings = database.getAllApplySettings(guildId);
		currentSettings.removeIf(o1 -> o1.getName() == null);

		if (currentSettings.size() == 0) {
			return "No enabled apply settings";
		}

		StringBuilder applyStr = new StringBuilder();
		for (AutomatedApply currentSetting : currentSettings) {
			try {
				if (currentSetting.getEnable().equalsIgnoreCase("true")) {
					TextChannel reactChannel = guild.getTextChannelById(currentSetting.getMessageTextChannelId());

					EmbedBuilder eb = defaultEmbed("Apply For Guild");
					eb.setDescription(currentSetting.getMessageText());

					List<ApplyUser> curApplyUsers = new ArrayList<>();
					for (Iterator<ApplyGuild> iterator = applyGuild.iterator(); iterator.hasNext();) {
						ApplyGuild applyG = iterator.next();

						if (higherDepth(applyG.currentSettings, "name").getAsString().equals(currentSetting.getName())) {
							curApplyUsers.addAll(applyG.applyUserList);
							iterator.remove();
							break;
						}
					}

					try {
						Message reactMessage = reactChannel.retrieveMessageById(currentSetting.getPreviousMessageId()).complete();
						reactMessage
							.editMessageEmbeds(eb.build())
							.setActionRow(Button.primary("create_application_button_" + currentSetting.getName(), "Apply Here"))
							.queue();

						applyGuild.add(new ApplyGuild(reactMessage, new Gson().toJsonTree(currentSetting), curApplyUsers));
						applyStr.append("• Reloaded `").append(currentSetting.getName()).append("`\n");
					} catch (Exception e) {
						Message reactMessage = reactChannel
							.sendMessageEmbeds(eb.build())
							.setActionRow(Button.primary("create_application_button_" + currentSetting.getName(), "Apply Here"))
							.complete();

						currentSetting.setPreviousMessageId(reactMessage.getId());
						database.setApplySettings(guild.getId(), new Gson().toJsonTree(currentSetting));

						applyGuild.add(new ApplyGuild(reactMessage, new Gson().toJsonTree(currentSetting), curApplyUsers));
						applyStr.append("• Reloaded `").append(currentSetting.getName()).append("`\n");
					}
				} else {
					applyGuild.removeIf(o1 -> higherDepth(o1.currentSettings, "name").getAsString().equals(currentSetting.getName()));
					applyStr.append("• `").append(currentSetting.getName()).append("` is disabled\n");
				}
			} catch (Exception e) {
				log.error("Reload apply constructor error - " + guildId, e);
				if (e.getMessage() != null && e.getMessage().contains("Missing permission")) {
					applyStr
						.append("• Error Reloading for `")
						.append(currentSetting.getName())
						.append("` - missing permission(s): ")
						.append(e.getMessage().split("Missing permission: ")[1])
						.append("\n");
				} else {
					applyStr.append("• Error Reloading for `").append(currentSetting.getName()).append("`\n");
				}
			}
		}
		return applyStr.length() > 0 ? applyStr.toString() : "• Error reloading";
	}

	/* Automated Verify Methods */
	public void verifyConstructor(GenericGuildEvent event) {
		JsonElement currentSettings = database.getVerifySettings(event.getGuild().getId());
		if (currentSettings == null) {
			return;
		}

		try {
			if (
				higherDepth(currentSettings, "enable") == null ||
				(higherDepth(currentSettings, "enable") != null && !higherDepth(currentSettings, "enable").getAsBoolean())
			) {
				return;
			}

			if (higherDepth(currentSettings, "enable").getAsBoolean()) {
				TextChannel reactChannel = event
					.getGuild()
					.getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
				try {
					Message reactMessage = reactChannel
						.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString())
						.complete();
					if (reactMessage != null) {
						reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

						verifyGuild = new VerifyGuild(reactChannel, reactMessage);
						return;
					}
				} catch (Exception ignored) {}

				Message reactMessage = reactChannel
					.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
					.addFile(new File("src/main/java/com/skyblockplus/features/verify/Link_Discord_To_Hypixel.mp4"))
					.complete();

				JsonObject newSettings = currentSettings.getAsJsonObject();
				newSettings.addProperty("previousMessageId", reactMessage.getId());
				database.setVerifySettings(event.getGuild().getId(), newSettings);

				verifyGuild = new VerifyGuild(reactChannel, reactMessage);
			}
		} catch (Exception e) {
			log.error("Verify constructor error - " + event.getGuild().getId(), e);
		}
	}

	public String reloadVerifyConstructor(String guildId) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			return "Invalid guild";
		}

		JsonElement currentSettings = database.getVerifySettings(guild.getId());
		if (currentSettings == null) {
			return "No settings found";
		}

		try {
			if (higherDepth(currentSettings, "enable").getAsBoolean()) {
				TextChannel reactChannel = guild.getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());
				try {
					Message reactMessage = reactChannel
						.retrieveMessageById(higherDepth(currentSettings, "previousMessageId").getAsString())
						.complete();
					if (reactMessage != null) {
						reactMessage.editMessage(higherDepth(currentSettings, "messageText").getAsString()).queue();

						verifyGuild = new VerifyGuild(reactChannel, reactMessage);
						return "Reloaded";
					}
				} catch (Exception ignored) {}

				Message reactMessage = reactChannel
					.sendMessage(higherDepth(currentSettings, "messageText").getAsString())
					.addFile(new File("src/main/java/com/skyblockplus/features/verify/Link_Discord_To_Hypixel.mp4"))
					.complete();

				JsonObject newSettings = currentSettings.getAsJsonObject();
				newSettings.addProperty("previousMessageId", reactMessage.getId());
				database.setVerifySettings(guild.getId(), newSettings);

				verifyGuild = new VerifyGuild(reactChannel, reactMessage);
				return "Reloaded";
			} else {
				verifyGuild = new VerifyGuild();
				return "Not enabled";
			}
		} catch (Exception e) {
			log.error("Reload verify constructor error - " + guildId, e);
			if (e.getMessage().contains("Missing permission")) {
				return ("Error Reloading\nMissing permission: " + e.getMessage().split("Missing permission: ")[1]);
			}
		}
		return "Error Reloading";
	}

	/* Automated Guild Methods */
	public void updateGuild() {
		try {
			long startTime = System.currentTimeMillis();

			Guild guild = jda.getGuildById(guildId);
			List<GuildRole> currentSettings = database.getAllGuildRoles(guild.getId());

			if (currentSettings == null) {
				return;
			}

			boolean anyGuildRoleRankEnable = false;
			for (int i = currentSettings.size() - 1; i >= 0; i--) {
				GuildRole curSettings = currentSettings.get(i);
				if (curSettings.getName() == null) {
					currentSettings.remove(i);
				} else if (
					curSettings.getEnableGuildRole().equalsIgnoreCase("true") || curSettings.getEnableGuildRanks().equalsIgnoreCase("true")
				) {
					anyGuildRoleRankEnable = true;
				} else if (
					curSettings.getEnableGuildUserCount() == null || curSettings.getEnableGuildUserCount().equalsIgnoreCase("false")
				) {
					currentSettings.remove(i);
				}
			}

			if (currentSettings.size() == 0) {
				return;
			}

			Set<String> memberCountList = new HashSet<>();
			List<Member> inGuildUsers = new ArrayList<>();
			Map<String, String> discordIdToUuid = new HashMap<>();
			int counterUpdate = 0;
			if (anyGuildRoleRankEnable) {
				List<LinkedAccountModel> linkedUsers = database.getLinkedUsers();
				List<List<LinkedAccountModel>> linkedUsersLists = ListUtils.partition(linkedUsers, 100);
				AtomicInteger requestCount = new AtomicInteger();
				CountDownLatch latch = new CountDownLatch(1);
				for (List<LinkedAccountModel> linkedUsersList : linkedUsersLists) {
					List<String> linkedUsersIds = new ArrayList<>();
					for (LinkedAccountModel linkedUser : linkedUsersList) {
						linkedUsersIds.add(linkedUser.getDiscordId());
						discordIdToUuid.put(linkedUser.getDiscordId(), linkedUser.getMinecraftUuid());
					}

					guild
						.retrieveMembersByIds(linkedUsersIds.toArray(new String[0]))
						.onSuccess(
							members -> {
								inGuildUsers.addAll(members);
								requestCount.incrementAndGet();
								if (requestCount.get() == linkedUsersLists.size()) {
									latch.countDown();
								}
							}
						)
						.onError(
							error -> {
								requestCount.incrementAndGet();
								if (requestCount.get() == linkedUsersLists.size()) {
									latch.countDown();
								}
							}
						);
				}

				try {
					latch.await(15, TimeUnit.SECONDS);
				} catch (Exception e) {
					log.error("updateGuild latch - " + guildId, e);
				}
			}

			for (GuildRole currentSetting : currentSettings) {
				HypixelResponse response = getGuildFromId(currentSetting.getGuildId());
				if (response.isNotValid()) {
					continue;
				}

				JsonArray guildMembers = response.get("members").getAsJsonArray();

				boolean enableGuildRole = currentSetting.getEnableGuildRole().equalsIgnoreCase("true");
				boolean enableGuildRanks = currentSetting.getEnableGuildRanks().equalsIgnoreCase("true");
				if (enableGuildRanks || enableGuildRole) {
					Map<String, String> uuidToRankMap = new HashMap<>();
					for (JsonElement guildMember : guildMembers) {
						uuidToRankMap.put(
							higherDepth(guildMember, "uuid").getAsString(),
							higherDepth(guildMember, "rank").getAsString().replace(" ", "_")
						);
					}

					try {
						if (guild.getId().equals("782154976243089429")) {
							TextChannel ignoreChannel = guild.getTextChannelById("846493091233792066");
							String[] messageContent = ignoreChannel
								.retrieveMessageById(ignoreChannel.getLatestMessageId())
								.complete()
								.getContentRaw()
								.split(" ");

							for (String removeM : messageContent) {
								uuidToRankMap.replace(removeM, "null");
							}
						}
					} catch (Exception ignored) {}

					Role guildMemberRole = enableGuildRole ? guild.getRoleById(currentSetting.getRoleId()) : null;
					for (Member linkedUser : inGuildUsers) {
						List<Role> rolesToAdd = new ArrayList<>();
						List<Role> rolesToRemove = new ArrayList<>();

						if (enableGuildRole) {
							if (uuidToRankMap.containsKey(discordIdToUuid.get(linkedUser.getId()))) {
								rolesToAdd.add(guildMemberRole);
							} else {
								rolesToRemove.add(guildMemberRole);
							}
						}

						if (enableGuildRanks) {
							List<GuildRank> guildRanksArr = currentSetting.getGuildRanks();
							if (!uuidToRankMap.containsKey(discordIdToUuid.get(linkedUser.getId()))) {
								for (GuildRank guildRank : guildRanksArr) {
									rolesToRemove.add(guild.getRoleById(guildRank.getDiscordRoleId()));
								}
							} else {
								String currentRank = uuidToRankMap.get(discordIdToUuid.get(linkedUser.getId()));
								for (GuildRank guildRank : guildRanksArr) {
									Role currentRankRole = guild.getRoleById(guildRank.getDiscordRoleId());
									if (guildRank.getMinecraftRoleName().equalsIgnoreCase(currentRank)) {
										rolesToAdd.add(currentRankRole);
									} else {
										rolesToRemove.add(currentRankRole);
									}
								}
							}
						}

						try {
							guild.modifyMemberRoles(linkedUser, rolesToAdd, rolesToRemove).complete();
						} catch (Exception ignored) {}

						memberCountList.add(linkedUser.getId());
					}
				}

				if (currentSetting.getEnableGuildUserCount() != null && currentSetting.getEnableGuildUserCount().equals("true")) {
					VoiceChannel curVc;
					try {
						curVc = guild.getVoiceChannelById(currentSetting.getGuildUserChannelId());
					} catch (Exception e) {
						currentSetting.setEnableGuildUserCount("false");
						database.setGuildRoleSettings(guild.getId(), currentSetting);
						continue;
					}

					if (curVc.getName().contains(guildMembers.size() + "/125")) {
						continue;
					}

					if (curVc.getName().split(":").length == 2) {
						curVc.getManager().setName(curVc.getName().split(":")[0].trim() + ": " + guildMembers.size() + "/125").complete();
					} else {
						curVc
							.getManager()
							.setName(response.get("name").getAsString() + " Members: " + guildMembers.size() + "/125")
							.complete();
					}

					counterUpdate++;
				}
			}

			logCommand(
				guild,
				"Guild Role | Users (" +
				memberCountList.size() +
				") | Time (" +
				((System.currentTimeMillis() - startTime) / 1000) +
				"s) | Counters (" +
				counterUpdate +
				")"
			);
		} catch (Exception e) {
			log.error("updateGuild - " + guildId, e);
		}
	}

	/* Skyblock Event Methods */
	public void setEventMemberList(List<EventMember> eventMemberList) {
		this.eventMemberList = eventMemberList;
	}

	public void updateSkyblockEvent() {
		try {
			if (database.getSkyblockEventActive(guildId)) {
				JsonElement currentSettings = database.getRunningEventSettings(guildId);
				Instant endingTime = Instant.ofEpochSecond(higherDepth(currentSettings, "timeEndingSeconds").getAsLong());
				if (Duration.between(Instant.now(), endingTime).toMinutes() <= 0) {
					endSkyblockEvent(guildId);
				}
			}
		} catch (Exception e) {
			log.error("updateSkyblockEvent - " + guildId, e);
		}
	}

	public void setEventMemberListLastUpdated(Instant eventMemberListLastUpdated) {
		this.eventMemberListLastUpdated = eventMemberListLastUpdated;
	}

	public void createSkyblockEvent(CommandEvent event) {
		if (skyblockEvent != null && skyblockEvent.scheduledFuture != null) {
			skyblockEvent.scheduledFuture.cancel(true);
		}
		skyblockEvent = new SkyblockEvent(event);
	}

	public void setSkyblockEvent(SkyblockEvent skyblockEvent) {
		this.skyblockEvent = skyblockEvent;
	}

	/* Mee6 Roles Methods */
	public String reloadMee6Settings(String guildId) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			return "Invalid guild";
		}

		JsonElement currentSettings = database.getMee6Settings(guild.getId());
		if (currentSettings == null) {
			return "No settings found";
		}

		currentMee6Settings = currentSettings;
		boolean enabled = higherDepth(currentSettings, "enable") != null && higherDepth(currentSettings, "enable").getAsBoolean();
		return "Mee6 roles are " + (enabled ? "enabled" : "disabled");
	}

	public boolean mee6Roles(GuildMessageReceivedEvent event) {
		if (event.getMessage().getContentRaw().toLowerCase().startsWith("!rank")) {
			try {
				if (!higherDepth(currentMee6Settings, "enable").getAsBoolean()) {
					return true;
				}
			} catch (Exception e) {
				return true;
			}

			if (lastMee6RankUpdate != null && Duration.between(lastMee6RankUpdate, Instant.now()).toMinutes() <= 3) {
				return true;
			}

			lastMee6RankUpdate = Instant.now();

			int pageNum = 0;
			while (true) {
				JsonArray leaderboardArr = getMee6Leaderboard(pageNum);
				if (leaderboardArr == null || leaderboardArr.size() == 0) {
					return true;
				}

				Member member;
				if (event.getMessage().getMentionedMembers().isEmpty()) {
					member = event.getMember();
				} else {
					member = event.getMessage().getMentionedMembers().get(0);
				}

				for (JsonElement player : leaderboardArr) {
					if (higherDepth(player, "id").getAsString().equals(member.getId())) {
						int playerLevel = higherDepth(player, "level", 0);
						JsonArray curRoles = higherDepth(currentMee6Settings, "mee6Ranks").getAsJsonArray();
						List<Role> toAdd = new ArrayList<>();
						List<Role> toRemove = new ArrayList<>();
						for (JsonElement curRole : curRoles) {
							if (playerLevel >= higherDepth(curRole, "value", 0)) {
								toAdd.add(event.getGuild().getRoleById(higherDepth(curRole, "roleId").getAsString()));
							} else {
								toRemove.add(event.getGuild().getRoleById(higherDepth(curRole, "roleId").getAsString()));
							}
						}
						event.getGuild().modifyMemberRoles(member, toAdd, toRemove).queue();
						return true;
					}
				}

				pageNum++;
			}
		}

		return false;
	}

	public JsonArray getMee6Leaderboard(int pageNumber) {
		try {
			return higherDepth(
				getJson("https://mee6.xyz/api/plugins/levels/leaderboard/" + guildId + "?limit=1000&page=" + pageNumber),
				"players"
			)
				.getAsJsonArray();
		} catch (Exception e) {
			return null;
		}
	}

	/* Events */
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		applyGuild.forEach(o1 -> o1.onMessageReactionAdd(event));
	}

	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getGuild().getId().equals("796790757947867156") && event.getChannel().getId().equals("869278025018114108")) {
			if (
				event.getMessage().getEmbeds().size() > 0 &&
				event.getMessage().getEmbeds().get(0).getDescription() != null &&
				event
					.getMessage()
					.getEmbeds()
					.get(0)
					.getDescription()
					.contains("https://github.com/Moulberry/NotEnoughUpdates-REPO/commit/")
			) {
				if (IS_API) {
					updateItemMappings();
				}

				scheduler.schedule(
					() -> {
						internalJsonMappings =
							getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json")
								.getAsJsonObject();
						priceOverrideJson =
							getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/PriceOverrides.json")
								.getAsJsonObject();
					},
					5,
					TimeUnit.MINUTES
				);
			}
			return;
		}

		if (mee6Roles(event)) {
			return;
		}

		if (verifyGuild.onGuildMessageReceived(event)) {
			return;
		}

		skyblockEvent.onGuildMessageReceived(event);
	}

	public void onTextChannelDelete(TextChannelDeleteEvent event) {
		applyGuild.forEach(o1 -> o1.onTextChannelDelete(event));
	}

	public void onButtonClick(ButtonClickEvent event) {
		if (event.getComponentId().startsWith("setup_command_")) {
			event.deferReply().complete();
			SetupCommandHandler handler = new SetupCommandHandler(event, event.getComponentId().split("setup_command_")[1]);
			if (handler.isValid()) {
				return;
			}
		} else {
			event.deferReply(true).complete();
		}

		for (ApplyGuild o1 : applyGuild) {
			String buttonClickReply = o1.onButtonClick(event);
			if (buttonClickReply != null) {
				event.getHook().editOriginal(buttonClickReply).queue();
				return;
			}
		}

		if (event.getMessage() != null) {
			event
				.editButton(
					event
						.getButton()
						.asDisabled()
						.withId(event.getButton().getId() + "_disabled")
						.withLabel("Disabled")
						.withStyle(ButtonStyle.DANGER)
				)
				.queue();
		}

		event.getHook().editOriginal("❌ This button has been disabled").queue();
	}

	public void onGuildLeave() {
		if (skyblockEvent.scheduledFuture != null) {
			skyblockEvent.scheduledFuture.cancel(true);
		}

		for (ScheduledFuture<?> scheduledFuture : scheduledFutures) {
			scheduledFuture.cancel(true);
		}
	}

	/* Miscellaneous */
	public void schedulerConstructor() {
		int eventDelay = (int) (Math.random() * 60 + 1);
		scheduledFutures.add(scheduler.scheduleWithFixedDelay(this::updateGuild, eventDelay, 210, TimeUnit.MINUTES));
		scheduledFutures.add(scheduler.scheduleWithFixedDelay(this::updateSkyblockEvent, eventDelay, 60, TimeUnit.MINUTES));
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void updateItemMappings() {
		try {
			File neuDir = new File("src/main/java/com/skyblockplus/json/neu");
			if (neuDir.exists()) {
				FileUtils.deleteDirectory(neuDir);
			}
			neuDir.mkdir();

			File skyblockPlusDir = new File("src/main/java/com/skyblockplus/json/skyblock_plus");
			if (skyblockPlusDir.exists()) {
				FileUtils.deleteDirectory(skyblockPlusDir);
			}
			skyblockPlusDir.mkdir();

			Git neuRepo = Git
				.cloneRepository()
				.setURI("https://github.com/Moulberry/NotEnoughUpdates-REPO.git")
				.setDirectory(neuDir)
				.call();

			Git skyblockPlusDataRepo = Git
				.cloneRepository()
				.setURI("https://github.com/kr45732/skyblock-plus-data.git")
				.setDirectory(skyblockPlusDir)
				.call();

			try (Writer writer = new FileWriter("src/main/java/com/skyblockplus/json/skyblock_plus/InternalNameMappings.json")) {
				new GsonBuilder().setPrettyPrinting().create().toJson(getUpdatedItemMappingsJson(), writer);
				writer.flush();
			}

			try (Writer writer = new FileWriter("src/main/java/com/skyblockplus/json/skyblock_plus/PriceOverrides.json")) {
				new GsonBuilder().setPrettyPrinting().create().toJson(getUpdatedPriceOverridesJson(), writer);
				writer.flush();
			}

			skyblockPlusDataRepo.add().addFilepattern("InternalNameMappings.json").addFilepattern("PriceOverrides.json").call();
			skyblockPlusDataRepo
				.commit()
				.setAuthor("kr45632", "52721908+kr45732@users.noreply.github.com")
				.setCommitter("kr45632", "52721908+kr45732@users.noreply.github.com")
				.setMessage("Automatic update (" + neuRepo.log().setMaxCount(1).call().iterator().next().getName() + ")")
				.call();
			skyblockPlusDataRepo.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(GITHUB_TOKEN, "")).call();

			FileUtils.deleteDirectory(neuDir);
			FileUtils.deleteDirectory(skyblockPlusDir);
		} catch (Exception e) {
			log.error("Exception while automatically updating item mappings", e);
		}
	}

	public JsonElement getUpdatedItemMappingsJson() {
		File dir = new File("src/main/java/com/skyblockplus/json/neu/items");
		JsonObject outputArray = new JsonObject();

		Map<String, String> rarityMapRev = new HashMap<>();
		rarityMapRev.put("5", "Mythic");
		rarityMapRev.put("4", "Legendary");
		rarityMapRev.put("3", "Epic");
		rarityMapRev.put("2", "Rare");
		rarityMapRev.put("1", "Uncommon");
		rarityMapRev.put("0", "Common");

		for (File child : dir.listFiles()) {
			try {
				JsonElement itemJson = JsonParser.parseReader(new FileReader(child));
				String itemName = parseMcCodes(higherDepth(itemJson, "displayname").getAsString()).replace("�", "");
				String internalName = higherDepth(itemJson, "internalname").getAsString();
				if (itemName.contains("(")) {
					continue;
				}

				if (itemName.startsWith("[Lvl")) {
					itemName = rarityMapRev.get(internalName.split(";")[1]) + " " + itemName.split("] ")[1];
				}
				if (itemName.equals("Enchanted Book")) {
					itemName = parseMcCodes(higherDepth(itemJson, "lore").getAsJsonArray().get(0).getAsString());
				}
				if (itemName.contains("⚚")) {
					itemName = itemName.replace("⚚ ", "STARRED ");
				}
				if (itemName.contains("Melody\\u0027s Hair")) {
					itemName = "MELODY_HAIR";
				}
				itemName = itemName.replace("™", "").replace("\u0027s", "").toUpperCase().replace("\u0027", "").replace(" ", "_");
				if (itemName.contains("MELODY_HAIR")) {
					itemName = "MELODY_HAIR";
				}
				if (internalName.contains("-")) {
					internalName = internalName.replace("-", ":");
				}

				JsonArray toAdd = new JsonArray();
				toAdd.add(internalName);
				if (outputArray.has(itemName)) {
					toAdd.addAll(outputArray.get(itemName).getAsJsonArray());
				}

				outputArray.add(itemName, toAdd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return outputArray;
	}

	public JsonElement getUpdatedPriceOverridesJson() {
		File dir = new File("src/main/java/com/skyblockplus/json/neu/items");
		JsonElement bazaarJson = higherDepth(getBazaarJson(), "products");
		JsonArray sbzPricesJson = getSbzPricesJson();
		JsonObject outputObject = new JsonObject();

		for (File child : dir.listFiles()) {
			try {
				JsonObject itemJson = JsonParser.parseReader(new FileReader(child)).getAsJsonObject();
				if (itemJson.has("vanilla")) {
					String name = parseMcCodes(itemJson.get("displayname").getAsString());
					String id = itemJson.get("internalname").getAsString();
					long price = 0;

					try {
						higherDepth(higherDepth(bazaarJson, id + ".buy_summary").getAsJsonArray().get(0), "pricePerUnit").getAsDouble();
						continue;
					} catch (Exception ignored) {}

					for (JsonElement itemPrice : sbzPricesJson) {
						String itemNamePrice = higherDepth(itemPrice, "name").getAsString();
						if (itemNamePrice.equalsIgnoreCase(id) || itemNamePrice.equalsIgnoreCase(name.replace(" ", "_"))) {
							price = higherDepth(itemPrice, "low").getAsLong();
							break;
						}
					}

					outputObject.addProperty(id, price);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return outputObject;
	}
}
