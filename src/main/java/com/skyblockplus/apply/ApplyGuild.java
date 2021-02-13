package com.skyblockplus.apply;

import static com.skyblockplus.reload.ReloadEventWatcher.addApplyGuild;
import static com.skyblockplus.utils.BotUtils.higherDepth;

import com.google.gson.JsonElement;

import org.jetbrains.annotations.NotNull;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ApplyGuild extends ListenerAdapter {
    final Message reactMessage;
    final JsonElement currentSettings;

    public ApplyGuild(Message reactMessage, JsonElement currentSettings) {
        this.reactMessage = reactMessage;
        this.currentSettings = currentSettings;
        addApplyGuild(reactMessage.getGuild().getId(), this);
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }
        if (event.getUser().isBot()) {
            return;
        }

        event.getReaction().removeReaction(event.getUser()).queue();
        if (!event.getReactionEmote().getName().equals("✅")) {
            return;
        }

        if (event.getGuild().getTextChannelsByName(higherDepth(currentSettings, "newChannelPrefix").getAsString() + "-"
                + event.getUser().getName().replace(" ", "-"), true).size() > 0) {
            return;
        }

        event.getJDA().addEventListener(new ApplyUser(event, event.getUser(), currentSettings));
    }
}
