package com.skyblockplus.eventlisteners.apply;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

public class ApplyUser implements Serializable {
    public final String applyingUserId;
    public final String applicationChannelId;
    public final String currentSettingsString;
    public final String guildId;
    public final String[] profileNames;
    public final List<String> profileNameEmojis = new ArrayList<>();
    public String reactMessageId;
    public int state = 0;
    public String staffChannelId;
    public boolean shouldDeleteChannel = false;
    public String playerSlayer;
    public String playerSkills;
    public String playerCatacombs;
    public String playerWeight;
    public String playerUsername;
    public String ironmanSymbol;
    public String playerProfileName;

    public ApplyUser(MessageReactionAddEvent event, JsonElement currentSettings, String playerUsername) {
        User applyingUser = event.getUser();

        logCommand(event.getGuild(), applyingUser, "apply " + applyingUser.getName());

        this.applyingUserId = applyingUser.getId();
        this.currentSettingsString = new Gson().toJson(currentSettings);
        this.guildId = event.getGuild().getId();
        this.playerUsername = playerUsername;

        String channelPrefix = higherDepth(currentSettings, "newChannelPrefix").getAsString();
        Category applyCategory = event.getGuild()
                .getCategoryById(higherDepth(currentSettings, "newChannelCategory").getAsString());
        TextChannel applicationChannel = applyCategory.createTextChannel(channelPrefix + "-" + applyingUser.getName())
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .complete();
        this.applicationChannelId = applicationChannel.getId();

        applicationChannel.sendMessage("Welcome " + applyingUser.getAsMention() + "!").complete();

        Player player = new Player(playerUsername);
        this.profileNames = player.getAllProfileNames();

        EmbedBuilder welcomeEb = defaultEmbed("Application for " + applyingUser.getName());
        welcomeEb.setDescription(
                "Please react with the emoji that corresponds to the profile you want to apply with or react with ❌ to cancel the application");
        Message reactMessage = applicationChannel.sendMessage(welcomeEb.build()).complete();
        this.reactMessageId = reactMessage.getId();

        for (String profileName : profileNames) {
            this.profileNameEmojis.add(profileNameToEmoji(profileName));
            reactMessage.addReaction(profileNameToEmoji(profileName)).queue();
        }

        reactMessage.addReaction("❌").queue();
    }

    public String getGuildId() {
        return guildId;
    }

    public String getApplicationChannelId() {
        return applicationChannelId;
    }

    public String getStaffChannelId() {
        return staffChannelId;
    }

    public String getMessageReactId() {
        return reactMessageId;
    }

    public boolean onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) {
            return false;
        }

        if (state == 4) {
            return onMessageReactionAddStaff(event);
        }

        if (!event.getMessageId().equals(reactMessageId)) {
            return false;
        }

        User applyingUser = jda.getUserById(applyingUserId);
        TextChannel applicationChannel = jda.getTextChannelById(applicationChannelId);
        Message reactMessage = applicationChannel.retrieveMessageById(reactMessageId).complete();
        JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

        if (!event.getUser().equals(applyingUser)) {
            if (!(event.getMember().getRoles().contains(
                    event.getGuild().getRoleById(higherDepth(currentSettings, "staffPingRoleId").getAsString()))
                    || event.getMember().hasPermission(Permission.ADMINISTRATOR))) {
                reactMessage.removeReaction(event.getReactionEmote().getAsReactionCode(), event.getUser()).queue();
                return false;
            }
        }

        if (event.getReactionEmote().getAsReactionCode().equals("❌")) {
            state = 3;
        } else if (event.getReactionEmote().getAsReactionCode().equals("↩️") && state == 1) {
            state = 2;
        } else if (!((profileNameEmojis.contains(event.getReactionEmote().getAsReactionCode()) && (state == 0))
                || (event.getReactionEmote().getAsReactionCode().equals("✅") && (state == 1 || state == 5)))) {
            reactMessage.clearReactions(event.getReactionEmote().getAsReactionCode()).queue();
            return false;
        }

        reactMessage.clearReactions().queue();

        switch (state) {
            case 0:
                Player player = new Player(playerUsername,
                        emojiToProfileName(event.getReactionEmote().getAsReactionCode()));

                JsonArray currentReqs = higherDepth(currentSettings, "applyReqs").getAsJsonArray();

                boolean meetReqs = false;
                StringBuilder missingReqsStr = new StringBuilder();
                if (currentReqs.size() == 0) {
                    meetReqs = true;
                } else {
                    for (JsonElement req : currentReqs) {
                        int slayerReq = higherDepth(req, "slayerReq").getAsInt();
                        int skillsReq = higherDepth(req, "skillsReq").getAsInt();
                        int cataReq = higherDepth(req, "catacombsReq").getAsInt();
                        int weightReq = higherDepth(req, "weightReq").getAsInt();

                        if (player.getSlayer() >= slayerReq && player.getSkillAverage() >= skillsReq
                                && player.getCatacombsLevel() >= cataReq && player.getWeight() >= weightReq) {
                            meetReqs = true;
                            break;
                        } else {
                            missingReqsStr.append("• Slayer - ").append(formatNumber(slayerReq))
                                    .append(" | Skill Average - ").append(formatNumber(skillsReq))
                                    .append(" | Catacombs - ").append(formatNumber(cataReq)).append(" | Weight - ")
                                    .append(formatNumber(weightReq)).append("\n");
                        }
                    }
                }

                if (!meetReqs) {
                    EmbedBuilder reqEmbed = defaultEmbed("Does not meet requirements");
                    reqEmbed.setDescription("You do not meet any of the following requirements:\n" + missingReqsStr);
                    reqEmbed.appendDescription(
                            "\n\n• If you think these values are incorrect make sure all your APIs are enabled and/or try relinking");
                    reqEmbed.appendDescription("\n• React with ✅ to close the channel");

                    reactMessage = applicationChannel.sendMessage(reqEmbed.build()).complete();
                    reactMessage.addReaction("✅").queue();
                    this.reactMessageId = reactMessage.getId();
                    state = 5;
                    break;
                }

                try {
                    playerSlayer = formatNumber(player.getSlayer());
                } catch (Exception e) {
                    playerSlayer = "0";
                }

                try {
                    playerSkills = roundAndFormat(player.getSkillAverage());
                } catch (Exception e) {
                    playerSkills = "API disabled";
                }

                playerSkills = playerSkills.equals("-1") ? "API disabled" : playerSkills;

                try {
                    playerCatacombs = roundAndFormat(
                            player.getCatacombsSkill().skillLevel + player.getCatacombsSkill().progressToNext);
                } catch (Exception e) {
                    playerCatacombs = "API disabled";
                }

                try {
                    playerWeight = roundAndFormat(player.getWeight());
                } catch (Exception e) {
                    playerWeight = "API disabled";
                }

                playerUsername = player.getUsername();
                ironmanSymbol = higherDepth(player.getOuterProfileJson(), "game_mode") != null ? " ♻️" : "";
                playerProfileName = player.getProfileName();

                EmbedBuilder statsEmbed = player.defaultPlayerEmbed();
                statsEmbed.setDescription("**Total Skyblock weight:** " + playerWeight);
                statsEmbed.addField("Total slayer", playerSlayer, true);
                statsEmbed.addField("Progress skill level", playerSkills, true);
                statsEmbed.addField("Catacombs level", "" + playerCatacombs, true);
                statsEmbed.addField("Are the above stats correct?",
                        "React with ✅ for yes, ↩️ to retry, and ❌ to cancel", false);

                reactMessage = applicationChannel.sendMessage(statsEmbed.build()).complete();
                reactMessage.addReaction("✅").queue();
                reactMessage.addReaction("↩️").queue();
                reactMessage.addReaction("❌").queue();
                this.reactMessageId = reactMessage.getId();
                state = 1;
                break;
            case 1:
                EmbedBuilder finishApplyEmbed = defaultEmbed("Thank you for applying!");
                finishApplyEmbed.setDescription(
                        "**Your stats have been submitted to staff**\nYou will be notified once staff review your stats");
                applicationChannel.sendMessage(finishApplyEmbed.build()).queue();

                state = 4;
                staffCaseConstructor();
                break;
            case 2:
                EmbedBuilder retryEmbed = defaultEmbed("Application for " + applyingUser.getName());
                retryEmbed.setDescription(
                        "Please react with the emoji that corresponds to the profile you want to apply with or react with ❌ to cancel the application");
                reactMessage = applicationChannel.sendMessage(retryEmbed.build()).complete();
                this.reactMessageId = reactMessage.getId();

                for (String profileName : profileNames) {
                    this.profileNameEmojis.add(profileNameToEmoji(profileName));
                    reactMessage.addReaction(profileNameToEmoji(profileName)).queue();
                }
                reactMessage.addReaction("❌").queue();
                state = 0;
                break;
            case 3:
                EmbedBuilder cancelEmbed = defaultEmbed("Canceling application");
                cancelEmbed.setDescription("Channel closing");
                applicationChannel.sendMessage(cancelEmbed.build()).queue();
                event.getGuild().getTextChannelById(event.getChannel().getId()).delete().reason("Application canceled")
                        .queueAfter(5, TimeUnit.SECONDS);
                return true;
            case 5:
                EmbedBuilder closeChannelEmbed = defaultEmbed("Channel closing");
                applicationChannel.sendMessage(closeChannelEmbed.build()).queue();
                event.getGuild().getTextChannelById(event.getChannel().getId()).delete().reason("Application closed")
                        .queueAfter(5, TimeUnit.SECONDS);
                return true;
        }
        return false;
    }

    private boolean onMessageReactionAddStaff(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) {
            return false;
        }
        TextChannel applicationChannel = jda.getTextChannelById(applicationChannelId);
        try {
            if (shouldDeleteChannel && (event.getMessageId().equals(reactMessageId))) {
                if (event.getReactionEmote().getName().equals("✅")) {
                    event.getReaction().clearReactions().queue();
                    EmbedBuilder eb = defaultEmbed("Channel closing");
                    applicationChannel.sendMessage(eb.build()).queue();
                    applicationChannel.delete().reason("Applicant read final message").queueAfter(10, TimeUnit.SECONDS);
                    return true;
                } else {
                    event.getReaction().removeReaction(event.getUser()).queue();
                }
                return false;
            }
        } catch (Exception ignored) {

        }
        if (!event.getMessageId().equals(reactMessageId)) {
            return false;
        }
        JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

        TextChannel staffChannel = jda.getTextChannelById(staffChannelId);
        User applyingUser = jda.getUserById(applyingUserId);
        Message reactMessage = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
        if (event.getReactionEmote().getEmoji().equals("❌")) {
            reactMessage.clearReactions().queue();
            reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);

            try {
                staffChannel.sendMessage(playerUsername + " (" + applyingUser.getAsMention() + ") was denied by "
                        + event.getUser().getName() + " (" + event.getUser().getAsMention() + ")").queue();
                applicationChannel.sendMessage(applyingUser.getAsMention()).queue();
            } catch (Exception e) {
                staffChannel.sendMessage(playerUsername + " was denied by " + event.getUser().getName() + " ("
                        + event.getUser().getAsMention() + ")").queue();
            }

            EmbedBuilder eb = defaultEmbed("Application Not Accepted");
            eb.setDescription(higherDepth(currentSettings, "denyMessageText").getAsString()
                    + "\n**React with ✅ to close the channel**");

            reactMessage = applicationChannel.sendMessage(eb.build()).complete();
            reactMessage.addReaction("✅").queue();
            this.reactMessageId = reactMessage.getId();
            shouldDeleteChannel = true;

        } else if (event.getReactionEmote().getEmoji().equals("✅")) {
            reactMessage.clearReactions().queue();
            reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);

            try {
                staffChannel.sendMessage(playerUsername + " (" + applyingUser.getAsMention() + ") was accepted by "
                        + event.getUser().getName() + " (" + event.getUser().getAsMention() + ")").queue();
                applicationChannel.sendMessage(applyingUser.getAsMention()).queue();
            } catch (Exception e) {
                staffChannel.sendMessage(playerUsername + " was accepted by " + event.getUser().getName() + " ("
                        + event.getUser().getAsMention() + ")").queue();
            }

            EmbedBuilder eb = defaultEmbed("Application Accepted");
            eb.setDescription(higherDepth(currentSettings, "acceptMessageText").getAsString()
                    + "\n**React with ✅ to close the channel**");

            reactMessage = applicationChannel.sendMessage(eb.build()).complete();
            reactMessage.addReaction("✅").queue();

            try {
                JsonElement guildRoleSettings = database.getGuildRoleSettings(guildId);
                Guild guild = jda.getGuildById(guildId);

                guild.addRoleToMember(applyingUserId,
                        guild.getRoleById(higherDepth(guildRoleSettings, "roleId").getAsString())).queue();
            } catch (Exception ignored) {
            }

            this.reactMessageId = reactMessage.getId();
            shouldDeleteChannel = true;
        } else if (event.getReactionEmote().getEmoji().equals("\uD83D\uDD50")) {
            if (higherDepth(currentSettings, "waitlistedMessageText") != null
                    && !higherDepth(currentSettings, "waitlistedMessageText").getAsString().equals("none")) {
                reactMessage.clearReactions().queue();
                reactMessage.delete().queueAfter(5, TimeUnit.SECONDS);

                try {
                    staffChannel
                            .sendMessage(playerUsername + " (" + applyingUser.getAsMention() + ") was waitlisted by "
                                    + event.getUser().getName() + " (" + event.getUser().getAsMention() + ")")
                            .queue();
                    applicationChannel.sendMessage(applyingUser.getAsMention()).queue();
                } catch (Exception e) {
                    staffChannel.sendMessage(playerUsername + " was waitlisted by " + event.getUser().getName() + " ("
                            + event.getUser().getAsMention() + ")").queue();

                }

                EmbedBuilder eb = defaultEmbed("Application waitlisted");
                eb.setDescription(higherDepth(currentSettings, "waitlistedMessageText").getAsString()
                        + "\n**React with ✅ to close the channel**");

                reactMessage = applicationChannel.sendMessage(eb.build()).complete();
                reactMessage.addReaction("✅").queue();
            }

            this.reactMessageId = reactMessage.getId();
            shouldDeleteChannel = true;
        }
        return false;
    }

    private void staffCaseConstructor() {
        JsonElement currentSettings = JsonParser.parseString(currentSettingsString);

        TextChannel staffChannel = jda
                .getTextChannelById(higherDepth(currentSettings, "messageStaffChannelId").getAsString());
        staffChannelId = staffChannel.getId();

        EmbedBuilder applyPlayerStats = defaultPlayerEmbed();
        applyPlayerStats.setDescription("**Total Skyblock weight:** " + playerWeight);
        applyPlayerStats.addField("Total slayer", playerSlayer, true);
        applyPlayerStats.addField("Progress average skill level", playerSkills, true);
        applyPlayerStats.addField("Catacombs level", "" + playerCatacombs, true);
        applyPlayerStats.addField("To accept the application,", "React with ✅", true);
        if (higherDepth(currentSettings, "waitlistedMessageText") != null
                && !higherDepth(currentSettings, "waitlistedMessageText").getAsString().equals("none")) {
            applyPlayerStats.addField("To waitlist the application,", "React with \uD83D\uDD50", true);
        }
        applyPlayerStats.addField("To deny the application,", "React with ❌", true);
        staffChannel.sendMessage("<@&" + higherDepth(currentSettings, "staffPingRoleId").getAsString() + ">")
                .complete();
        Message reactMessage = staffChannel.sendMessage(applyPlayerStats.build()).complete();
        reactMessage.addReaction("✅").queue();
        if (higherDepth(currentSettings, "waitlistedMessageText") != null
                && !higherDepth(currentSettings, "waitlistedMessageText").getAsString().equals("none")) {
            reactMessage.addReaction("\uD83D\uDD50").queue();
        }
        reactMessage.addReaction("❌").queue();
        reactMessageId = reactMessage.getId();
    }

    public EmbedBuilder defaultPlayerEmbed() {
        return defaultEmbed(fixUsername(playerUsername) + ironmanSymbol,
                "https://sky.shiiyu.moe/stats/" + playerUsername + "/" + playerProfileName);
    }
}
