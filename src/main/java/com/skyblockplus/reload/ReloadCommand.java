package com.skyblockplus.reload;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.reload.ReloadEventWatcher.onApplyReload;
import static com.skyblockplus.reload.ReloadEventWatcher.onVerifyReload;
import static com.skyblockplus.utils.Utils.*;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        this.name = "reload";
        this.cooldown = (BOT_PREFIX.equals("+") ? 60 : 0);
        this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
    }

    @Override
    protected void execute(CommandEvent event) {
        logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());

        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        eb = defaultEmbed("Reload Settings for " + event.getGuild().getName());
        eb.addField("Apply settings reload status", onApplyReload(event.getGuild().getId()), false);
        eb.addField("Verify settings reload status", onVerifyReload(event.getGuild().getId()), false);
        ebMessage.editMessage(eb.build()).queue();
    }
}
