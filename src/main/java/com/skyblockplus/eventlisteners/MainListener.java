package com.skyblockplus.eventlisteners;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.BOT_PREFIX;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.logCommand;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MainListener extends ListenerAdapter {
    public static final Map<String, AutomaticGuild> guildMap = new HashMap<>();

    public static String onApplyReload(String guildId) {
        String reloadStatus = "Error reloading";
        if (guildMap.containsKey(guildId)) {
            if (guildMap.get(guildId).allowApplyReload()) {
                reloadStatus = guildMap.get(guildId).reloadApplyConstructor(guildId);
            } else {
                reloadStatus = "Application in progress";
            }
        }
        return reloadStatus;
    }

    public static String onVerifyReload(String guildId) {
        String reloadStatus = "Error reloading";
        if (guildMap.containsKey(guildId)) {
            reloadStatus = guildMap.get(guildId).reloadVerifyConstructor(guildId);
        }
        return reloadStatus;
    }

    public static Map<String, AutomaticGuild> getGuildMap() {
        return guildMap;
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        if (event.getGuild().getName().startsWith("Skyblock Plus - Emoji Server")) {
            return;
        }

        if (isUniqueGuild(event.getGuild().getId())) {
            guildMap.put(event.getGuild().getId(), new AutomaticGuild(event));
        } else {
            System.out.println(event.getGuild().getId() + " is not unique - ready");
        }
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        guildMap.remove(event.getGuild().getId());
        database.deleteServerSettings(event.getGuild().getId());

        try {
            logCommand(event.getGuild(), "Left guild");
        } catch (Exception ignored) {
        }

    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        if (event.getGuild().getName().startsWith("Skyblock Plus - Emoji Server")) {
            return;
        }

        if (isUniqueGuild(event.getGuild().getId())) {
            try {
                EmbedBuilder eb = defaultEmbed("Thank you!");
                eb.setDescription("- Thank you for adding me to " + event.getGuild().getName() + "\n- My prefix is `"
                        + BOT_PREFIX + "`\n- You can view my commands by running `" + BOT_PREFIX
                        + "help`\n- Make sure to check out `" + BOT_PREFIX
                        + "setup` or the forum post [**here**](https://hypixel.net/threads/3980092) on how to setup the customizable features of this bot!");
                TextChannel channel = event.getGuild().getTextChannels().stream()
                        .filter(textChannel -> textChannel.getName().toLowerCase().contains("general")).findFirst()
                        .orElse(null);
                if (channel != null) {
                    channel.sendMessage(eb.build()).queue();
                } else {
                    event.getGuild().getDefaultChannel().sendMessage(eb.build()).queue();
                }
            } catch (Exception ignored) {
            }

            logCommand(event.getGuild(), "Joined guild | #" + jda.getGuilds().size());

            guildMap.put(event.getGuild().getId(), new AutomaticGuild(event));
        } else {
            System.out.println(event.getGuild().getId() + " is not unique - join");
        }
    }

    private boolean isUniqueGuild(String guildId) {
        return !guildMap.containsKey(guildId);
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (guildMap.containsKey(event.getGuild().getId())) {
            guildMap.get(event.getGuild().getId()).onMessageReactionAdd(event);
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (guildMap.containsKey(event.getGuild().getId())) {
            guildMap.get(event.getGuild().getId()).onGuildMessageReceived(event);
        }
    }

    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        if (guildMap.containsKey(event.getGuild().getId())) {
            guildMap.get(event.getGuild().getId()).onTextChannelDelete(event);
        }
    }
}
