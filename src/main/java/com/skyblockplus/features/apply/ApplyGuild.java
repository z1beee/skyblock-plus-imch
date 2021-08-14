package com.skyblockplus.features.apply;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.getApplyGuildUsersCache;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

public class ApplyGuild {

	public final List<ApplyUser> applyUserList;
	public final Message reactMessage;
	public final JsonElement currentSettings;
	public final boolean enable = true;
	public TextChannel waitInviteChannel = null;

	public ApplyGuild(Message reactMessage, JsonElement currentSettings) {
		this.reactMessage = reactMessage;
		this.currentSettings = currentSettings;
		this.applyUserList = getApplyGuildUsersCache(reactMessage.getGuild().getId(), higherDepth(currentSettings, "name").getAsString());
		try {
			this.waitInviteChannel = jda.getTextChannelById(higherDepth(currentSettings, "waitingChannelId").getAsString());
		} catch (Exception ignored) {}
	}

	public ApplyGuild(Message reactMessage, JsonElement currentSettings, List<ApplyUser> prevApplyUsers) {
		this(reactMessage, currentSettings);
		applyUserList.addAll(prevApplyUsers);
	}

	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!enable) {
			return;
		}

		onMessageReactionAdd_ExistingApplyUser(event);
	}

	public boolean onMessageReactionAdd_ExistingApplyUser(MessageReactionAddEvent event) {
		ApplyUser findApplyUser = applyUserList
			.stream()
			.filter(applyUser -> applyUser.reactMessageId.equals(event.getMessageId()))
			.findFirst()
			.orElse(null);
		if (findApplyUser != null) {
			if (findApplyUser.onMessageReactionAdd(event)) {
				applyUserList.remove(findApplyUser);
			}
			return true;
		}

		return false;
	}

	public void onTextChannelDelete(TextChannelDeleteEvent event) {
		applyUserList.removeIf(
			applyUser -> {
				if (applyUser.applicationChannelId.equals(event.getChannel().getId())) {
					return true;
				} else {
					try {
						if (applyUser.staffChannelId.equals(event.getChannel().getId())) {
							return true;
						}
					} catch (Exception ignored) {}
				}
				return false;
			}
		);
	}

	public String onMessageReactionAdd_NewApplyUser(ButtonClickEvent event) {
		if (event.getUser().isBot()) {
			return null;
		}

		if (event.getMessageIdLong() != reactMessage.getIdLong()) {
			return null;
		}

		if (!event.getButton().getId().equals("create_application_button_" + higherDepth(currentSettings, "name").getAsString())) {
			return null;
		}

		ApplyUser runningApplication = applyUserList
			.stream()
			.filter(o1 -> o1.applyingUserId.equals(event.getUser().getId()))
			.findFirst()
			.orElse(null);

		if (runningApplication != null) {
			return "❌ There is already an application open in <#" + runningApplication.applicationChannelId + ">";
		}

		JsonElement linkedAccount = database.getLinkedUserByDiscordId(event.getUser().getId());

		if (linkedAccount.isJsonNull() || !higherDepth(linkedAccount, "discordId").getAsString().equals((event.getUser().getId()))) {
			if (linkedAccount.isJsonNull()) {
				return "❌ You are not linked to the bot. Please run `+link [IGN]` and try again.";
			} else {
				return (
					"❌ Account " +
					higherDepth(linkedAccount, "minecraftUsername").getAsString() +
					" is linked with the Discord tag " +
					jda.retrieveUserById(higherDepth(linkedAccount, "discordId").getAsString()).complete().getAsTag() +
					"\nYour current Discord tag is " +
					event.getUser().getAsTag() +
					".\nPlease relink and try again"
				);
			}
		}

		Player player = new Player(higherDepth(linkedAccount, "minecraftUsername").getAsString());
		event.getGuild().getTextChannelById("").getHistory().retrievePast(100).complete().forEach(m -> m.clearReactions().queue());
		if (!player.isValid()) {
			return "❌ Unable to fetch player data. Please make sure that all APIs are enabled and/or try relinking";
		} else {
			boolean isIronman = false;
			try {
				isIronman = higherDepth(currentSettings, "ironmanOnly").getAsBoolean();
			} catch (Exception ignored) {}
			if (isIronman && player.getAllProfileNames(true).length == 0) {
				return "❌ You have no ironman profiles created";
			}
		}

		applyUserList.add(new ApplyUser(event, currentSettings, higherDepth(linkedAccount, "minecraftUsername").getAsString()));

		return "✅ A new application was created";
	}

	public String onButtonClick(ButtonClickEvent event) {
		String waitingForInvite = onButtonClick_WaitingForInviteApplyUser(event);
		if (waitingForInvite != null) {
			return waitingForInvite;
		}

		return onMessageReactionAdd_NewApplyUser(event);
	}

	public String onButtonClick_WaitingForInviteApplyUser(ButtonClickEvent event) {
		if (!event.getChannel().equals(waitInviteChannel)) {
			return null;
		}

		if (!event.getComponentId().equals("apply_user_" + higherDepth(currentSettings, "name").getAsString())) {
			return null;
		}

		if (
			!(
				event
					.getMember()
					.getRoles()
					.contains(event.getGuild().getRoleById(higherDepth(currentSettings, "staffPingRoleId").getAsString())) ||
				event.getMember().hasPermission(Permission.ADMINISTRATOR)
			)
		) {
			return "❌ You are missing the required permissions in this Guild to use that!";
		}

		event.getMessage().delete().queueAfter(3, TimeUnit.SECONDS);
		return "✅ Player was invited";
	}
}
