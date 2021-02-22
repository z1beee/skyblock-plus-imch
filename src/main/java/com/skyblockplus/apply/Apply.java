package com.skyblockplus.apply;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.skyblockplus.reload.ReloadEventWatcher.isUniqueApplyGuild;
import static com.skyblockplus.utils.BotUtils.*;

public class Apply extends ListenerAdapter {
    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        try {
            JsonElement currentSettings = getJson(
                    API_BASE_URL + "api/discord/serverSettings/get/apply?serverId=" + event.getGuild().getId());

            if (currentSettings != null) {
                if (higherDepth(currentSettings, "enable").getAsBoolean()) {
                    if (isUniqueApplyGuild(event.getGuild().getId())) {
                        TextChannel reactChannel = event.getGuild()
                                .getTextChannelById(higherDepth(currentSettings, "messageTextChannelId").getAsString());

                        reactChannel.sendMessage("Loading...").complete();
                        reactChannel.sendMessage("Loading...").complete();
                        List<Message> deleteMessages = reactChannel.getHistory().retrievePast(25).complete();
                        reactChannel.deleteMessages(deleteMessages).complete();

                        EmbedBuilder eb = defaultEmbed("Apply For Guild");
                        eb.setDescription(higherDepth(currentSettings, "messageText").getAsString());
                        Message reactMessage = reactChannel.sendMessage(eb.build()).complete();
                        reactMessage.addReaction("✅").queue();

                        event.getJDA().addEventListener(new ApplyGuild(reactMessage, currentSettings));
                    }
                } else {
                    event.getJDA().addEventListener(new ApplyGuild(event.getGuild().getId(), currentSettings));
                }
            }
        } catch (Exception ignored) {
        }
    }
}