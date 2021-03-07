package com.skyblockplus.guilds;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.Utils.*;

public class GuildCommands extends Command {
    private final EventWaiter waiter;
    private int counter = 0;

    public GuildCommands(EventWaiter waiter) {
        this.name = "guild";
        this.cooldown = globalCooldown;
        this.waiter = waiter;
        this.aliases = new String[]{"g"};
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        String content = event.getMessage().getContentRaw();

        String[] args = content.split(" ");

        System.out.println(content);

        if (args.length == 3 && ("experience".equals(args[1]) || "exp".equals(args[1]))) {
            if (args[2].toLowerCase().startsWith("u-")) {
                String username = args[2].split("-")[1];
                GuildStruct guildExp = getGuildExp(username);
                if (guildExp.outputArr.length == 0) {
                    ebMessage.editMessage(guildExp.eb.build()).queue();
                } else {
                    CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(2)
                            .setItemsPerPage(20).showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                                try {
                                    m.clearReactions().queue();
                                } catch (PermissionException ex) {
                                    m.delete().queue();
                                }
                            }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true)
                            .setColor(botColor).setCommandUser(event.getAuthor());

                    paginateBuilder.addItems(guildExp.outputArr);
                    ebMessage.delete().queue();
                    paginateBuilder.build().paginate(event.getChannel(), 0);
                }
                return;
            }
        } else if (args.length >= 3 && "info".equals(args[1])) {
            if (args[2].toLowerCase().startsWith("u-")) {
                String usernameInfo = args[2].split("-")[1];
                ebMessage.editMessage(getGuildInfo(usernameInfo).build()).queue();
                return;
            } else if (args[2].toLowerCase().startsWith("g-")) {
                String guildName = content.split("-")[1];
                ebMessage.editMessage(guildInfoFromGuildName(guildName).build()).queue();
                return;
            }
        } else if (args.length == 3 && "members".equals(args[1])) {
            if (args[2].toLowerCase().startsWith("u-")) {
                String usernameMembers = args[2].split("-")[1];
                GuildStruct guildMembers = getGuildMembers(usernameMembers);
                if (guildMembers.outputArr.length == 0) {
                    ebMessage.editMessage(guildMembers.eb.build()).queue();
                } else {
                    CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(3)
                            .setItemsPerPage(27).showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                                try {
                                    m.clearReactions().queue();
                                } catch (PermissionException ex) {
                                    m.delete().queue();
                                }
                            }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true)
                            .setColor(botColor).setCommandUser(event.getAuthor());

                    paginateBuilder.addItems(guildMembers.outputArr);

                    ebMessage.delete().queue();
                    paginateBuilder.build().paginate(event.getChannel(), 0);
                    return;
                }
            }
        } else if (args.length == 2) {
            ebMessage.editMessage(getGuildPlayer(args[1]).build()).queue();
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private GuildStruct getGuildExp(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return new GuildStruct(defaultEmbed("Error fetching player data"), null);
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return new GuildStruct(defaultEmbed("Error fetching guild data"), null);
        }

        JsonElement members = higherDepth(higherDepth(guildJson, "guild"), "members");
        JsonArray membersArr = members.getAsJsonArray();
        Map<String, Integer> guildExpMap = new HashMap<>();

        for (int i = 0; i < membersArr.size(); i++) {
            String currentUsername = uuidToUsername(higherDepth(membersArr.get(i), "uuid").getAsString());
            if (currentUsername == null) {
                continue;
            }

            JsonElement expHistory = higherDepth(membersArr.get(i), "expHistory");
            ArrayList<String> keys = getJsonKeys(expHistory);
            int totalPlayerExp = 0;

            for (String value : keys) {
                totalPlayerExp += higherDepth(expHistory, value).getAsInt();
            }

            guildExpMap.put(currentUsername, totalPlayerExp);
        }

        String[] outputStrArr = new String[guildExpMap.size()];
        guildExpMap.entrySet().stream().sorted((k1, k2) -> -k1.getValue().compareTo(k2.getValue())).forEach(k -> {
            try {
                outputStrArr[counter] = "**" + (counter + 1) + ")** " + fixUsername(k.getKey()) + ": "
                        + formatNumber(k.getValue()) + " EXP\n";
            } catch (Exception ignored) {
            }
            counter++;
        });

        return new GuildStruct(null, outputStrArr);
    }

    private EmbedBuilder getGuildPlayer(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return defaultEmbed("Error fetching player data");
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return defaultEmbed("Error fetching guild data");
        }

        try {
            String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();
            EmbedBuilder eb = defaultEmbed(uuidUsername.playerUsername + " is in " + guildName, "https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString());
            eb.addField("Guild statistics:", getGuildInfo(guildJson), false);
            return eb;
        } catch (Exception e) {
            return defaultEmbed(uuidUsername.playerUsername + " is not in a guild");
        }

    }

    private EmbedBuilder getGuildInfo(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return defaultEmbed("Error fetching player data");
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return defaultEmbed("Error fetching guild data");
        }

        String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

        EmbedBuilder eb = defaultEmbed(guildName, "https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString());
        eb.addField("Guild statistics:", getGuildInfo(guildJson), false);

        return eb;
    }

    private EmbedBuilder guildInfoFromGuildName(String guildName) {
        try {
            String guildId = higherDepth(getJson("https://api.hypixel.net/findGuild?key=" + HYPIXEL_API_KEY + "&byName="
                    + guildName.replace(" ", "%20")), "guild").getAsString();
            JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&id=" + guildId);
            if (guildJson == null) {
                return defaultEmbed("Error fetching guild data");
            }
            guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

            EmbedBuilder eb = defaultEmbed(guildName, "https://hypixel-leaderboard.senither.com/guilds/" + higherDepth(higherDepth(guildJson, "guild"), "_id").getAsString());
            eb.addField("Guild statistics:", getGuildInfo(guildJson), false);
            return eb;
        } catch (Exception e) {
            return defaultEmbed("Error fetching guild data");
        }
    }

    private String getGuildInfo(JsonElement guildJson) {
        String guildInfo = "";
        String guildName = higherDepth(higherDepth(guildJson, "guild"), "name").getAsString();

        JsonElement created = higherDepth(higherDepth(guildJson, "guild"), "created");
        String[] date = Date.from(Instant.ofEpochMilli(created.getAsLong())).toString().split(" ");
        guildInfo += ("• " + guildName + " was created on " + date[1] + " " + date[2] + ", " + date[5]) + "\n";

        JsonArray guildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();
        for (int i = 0; i < guildMembers.size(); i++) {
            JsonElement currentMember = guildMembers.get(i).getAsJsonObject();
            if (higherDepth(currentMember, "rank").getAsString().equals("Guild Master")) {
                guildInfo += ("• " + guildName + "'s guild master is "
                        + uuidToUsername(higherDepth(currentMember, "uuid").getAsString())) + "\n";
                break;
            }
        }

        int numGuildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray().size();
        guildInfo += ("• " + guildName + " has " + numGuildMembers + " members") + "\n";
        JsonArray preferredGames;
        try {
            preferredGames = higherDepth(higherDepth(guildJson, "guild"), "preferredGames").getAsJsonArray();
        } catch (Exception e) {
            preferredGames = new JsonArray();
        }
        if (preferredGames.size() > 1) {
            String prefString = preferredGames.toString();
            prefString = prefString.substring(1, prefString.length() - 1).toLowerCase().replace("\"", "").replace(",",
                    ", ");
            String firstHalf = prefString.substring(0, prefString.lastIndexOf(","));
            String lastHalf = prefString.substring(prefString.lastIndexOf(",") + 1);
            if (preferredGames.size() > 2) {
                guildInfo += ("• " + guildName + "'s preferred games are " + firstHalf + ", and" + lastHalf) + "\n";
            } else {
                guildInfo += ("• " + guildName + "'s preferred games are " + firstHalf + " and" + lastHalf) + "\n";
            }
        } else if (preferredGames.size() == 1) {
            guildInfo += ("• " + guildName + "'s preferred game is "
                    + preferredGames.get(0).getAsString().toLowerCase()) + "\n";
        }

        int guildExp = higherDepth(higherDepth(guildJson, "guild"), "exp").getAsInt();

        guildInfo += ("• " + guildName + " is guild level " + guildExpToLevel(guildExp)) + "\n";

        return guildInfo;
    }

    private GuildStruct getGuildMembers(String username) {
        UsernameUuidStruct uuidUsername = usernameToUuidUsername(username);
        if (uuidUsername == null) {
            return new GuildStruct(defaultEmbed("Error fetching player data"), null);
        }

        JsonElement guildJson = getJson(
                "https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + uuidUsername.playerUuid);
        if (guildJson == null) {
            return new GuildStruct(defaultEmbed("Error fetching guild data"), null);
        }

        JsonArray guildMembers = higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray();
        String[] members = new String[guildMembers.size()];
        for (int i = 0; i < guildMembers.size(); i++) {
            String currentMember = uuidToUsername(
                    higherDepth(guildMembers.get(i).getAsJsonObject(), "uuid").getAsString());
            members[i] = "• " + fixUsername(currentMember) + "  \n";
        }

        return new GuildStruct(null, members);
    }

    private int guildExpToLevel(int guildExp) {
        int[] guildExpTable = new int[]{100000, 150000, 250000, 500000, 750000, 1000000, 1250000, 1500000, 2000000,
                2500000, 2500000, 2500000, 2500000, 2500000, 3000000};
        int guildLevel = 0;

        for (int i = 0; ; i++) {
            int expNeeded = i >= guildExpTable.length ? guildExpTable[guildExpTable.length - 1] : guildExpTable[i];
            guildExp -= expNeeded;
            if (guildExp < 0) {
                return guildLevel;
            } else {
                guildLevel++;
            }
        }

    }
}
