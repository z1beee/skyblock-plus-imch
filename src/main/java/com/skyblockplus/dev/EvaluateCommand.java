package com.skyblockplus.dev;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.eventlisteners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.logCommand;
import static com.skyblockplus.utils.Utils.makeHastePost;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import groovy.lang.GroovyShell;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EvaluateCommand extends Command {
    private String importString = "";

    public EvaluateCommand() {
        this.name = "evaluate";
        this.ownerCommand = true;
        this.aliases = new String[] { "eval", "ev" };

        String[] packageImports = { "java.io", "java.lang", "java.math", "java.time", "java.util",
                "java.util.concurrent", "java.util.stream", "net.dv8tion.jda.api", "net.dv8tion.jda.api.entities",
                "net.dv8tion.jda.api.entities.impl", "net.dv8tion.jda.api.managers",
                "net.dv8tion.jda.api.managers.impl", "net.dv8tion.jda.api.utils" };
        for (String packageImport : packageImports) {
            importString += "import " + packageImport + ".*\n";
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            Message ebMessage = event.getChannel().sendMessage("Loading").complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split("\\s+", 2);

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length < 2) {
                ebMessage.editMessage("Invalid Input").queue();
                return;
            }

            String arg = args[1];

            if (arg.startsWith("```") && arg.endsWith("```")) {
                arg = arg.replaceAll("```(.*)\n", "").replaceAll("\n?```", "");
            }

            MessageReceivedEvent jdaEvent = event.getEvent();

            GroovyShell shell = new GroovyShell();

            try {
                shell.setProperty("event", jdaEvent);
                shell.setProperty("message", jdaEvent.getMessage());
                shell.setProperty("channel", jdaEvent.getChannel());
                shell.setProperty("args", args);
                shell.setProperty("jda", jdaEvent.getJDA());
                shell.setProperty("guilds", guildMap);
                shell.setProperty("db", database);
                if (jdaEvent.isFromType(ChannelType.TEXT)) {
                    shell.setProperty("guild", jdaEvent.getGuild());
                    shell.setProperty("member", jdaEvent.getMember());
                }

                String script = importString + arg;
                Object out = shell.evaluate(script);

                if (out == null) {
                    ebMessage.editMessage("Success (null output)").queue();
                } else if (out.toString().length() >= 2000) {
                    ebMessage.editMessage(makeHastePost(out.toString()) + ".json").queue();
                } else {
                    ebMessage.editMessage(out.toString()).queue();
                }
            } catch (Exception e) {
                ebMessage.editMessage(e.getMessage()).queue();
            }
        }).start();
    }
}
