package com.skyblockplus.link;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import java.time.Instant;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

public class LinkAccountCommand extends Command {

    public LinkAccountCommand() {
        this.name = "link";
        this.cooldown = globalCooldown;
    }

    public static EmbedBuilder linkAccount(String username, User user, Guild guild) {
        DiscordInfoStruct playerInfo = getPlayerDiscordInfo(username);
        if (playerInfo != null) {
            if (!user.getAsTag().equals(playerInfo.discordTag)) {
                EmbedBuilder eb = defaultEmbed("Discord tag mismatch");
                eb.setDescription("Account " + playerInfo.minecraftUsername + " is linked with the discord tag "
                        + playerInfo.discordTag + "\nYour current discord tag is " + user.getAsTag());
                return eb;
            }

            LinkedAccountModel toAdd = new LinkedAccountModel("" + Instant.now().toEpochMilli(), user.getId(),
                    playerInfo.minecraftUuid, playerInfo.minecraftUsername);

            if (database.addLinkedUser(toAdd) == 200) {

                try {
                    if (!higherDepth(database.getVerifySettings(guild.getId()), "verifiedNickname").getAsString()
                            .equalsIgnoreCase("none")) {
                        String nicknameTemplate = higherDepth(database.getVerifySettings(guild.getId()),
                                "verifiedNickname").getAsString();
                        nicknameTemplate = nicknameTemplate.replace("[IGN]", playerInfo.minecraftUsername);
                        if (nicknameTemplate.contains("[GUILD_RANK]")) {
                            try {
                                String settingsGuildId = higherDepth(database.getGuildRoleSettings(guild.getId()),
                                        "guildId").getAsString();
                                JsonElement playerGuild = higherDepth(getJson("https://api.hypixel.net/guild?key="
                                        + HYPIXEL_API_KEY + "&player=" + playerInfo.minecraftUuid), "guild");
                                if (higherDepth(playerGuild, "._id").getAsString().equals(settingsGuildId)) {
                                    JsonArray guildMembers = higherDepth(playerGuild, "members").getAsJsonArray();
                                    for (JsonElement guildMember : guildMembers) {
                                        if (higherDepth(guildMember, "uuid").getAsString()
                                                .equals(playerInfo.minecraftUuid)) {
                                            nicknameTemplate = nicknameTemplate.replace("[GUILD_RANK]",
                                                    higherDepth(guildMember, "rank").getAsString());
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        guild.getMember(user).modifyNickname(nicknameTemplate).queue();
                    }
                } catch (Exception ignored) {
                }

                try {
                    Role role = guild.getRoleById(
                            higherDepth(database.getVerifySettings(guild.getId()), "verifiedRole").getAsString());
                    guild.addRoleToMember(user.getId(), role).queue();
                } catch (Exception ignored) {
                }

                return defaultEmbed("Success").setDescription(
                        "`" + user.getAsTag() + "` was linked to `" + playerInfo.minecraftUsername + "`");
            } else {
                return defaultEmbed("Error").setDescription(
                        "Error linking `" + user.getAsTag() + " to `" + playerInfo.minecraftUsername + "`");
            }
        }

        return defaultEmbed("Error").setDescription(username
                + " is not linked to a Discord account. For help on how to link view [__**this**__](https://streamable.com/sdq8tp) video");
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length == 2) {
                ebMessage.editMessage(linkAccount(args[1], event.getAuthor(), event.getGuild()).build()).queue();
                return;
            } else if (args.length == 1) {
                ebMessage.editMessage(getLinkedAccount(event).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getLinkedAccount(CommandEvent event) {
        JsonElement userInfo = database.getLinkedUserByDiscordId(event.getAuthor().getId());

        try {
            return defaultEmbed("Success").setDescription("`" + event.getAuthor().getAsTag() + "` is linked to `"
                    + (higherDepth(userInfo, "minecraftUsername").getAsString()) + "`");
        } catch (Exception e) {
            return defaultEmbed("Error").setDescription("`" + event.getAuthor().getAsTag() + "` is not linked");
        }
    }
}
