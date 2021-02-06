package com.skyblockplus.auction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.time.Duration;
import java.time.Instant;

import static com.skyblockplus.utils.BotUtils.*;

public class AuctionCommands extends Command {
    Message ebMessage;
    String playerUsername;
    String playerUuid;

    public AuctionCommands() {
        this.name = "auction";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"ah"};
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading auction data...", null);
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length != 2) { // No args or too many args are given
            eb.setTitle(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        eb = getPlayerAuction(args[1]);

        ebMessage.editMessage(eb.build()).queue();

    }

    public EmbedBuilder getPlayerAuction(String username) {
        if (!usernameToUuid(username)) {
            return defaultEmbed("Error fetching player data", null);
        }
        String url = "https://api.hypixel.net/skyblock/auction?key=" + key + "&player=" + this.playerUuid;

        JsonElement playerAuctions = getJson(url);
        JsonArray auctionsArray = higherDepth(playerAuctions, "auctions").getAsJsonArray();
        String[][] auctions = new String[auctionsArray.size()][2];

        for (int i = 0; i < auctionsArray.size(); i++) {
            JsonElement currentAuction = auctionsArray.get(i);
            if (!higherDepth(currentAuction, "claimed").getAsBoolean()) {

                String auction;
                boolean isPet = higherDepth(currentAuction, "item_lore").getAsString().toLowerCase().contains("pet");
                boolean bin = false;
                try {
                    bin = higherDepth(currentAuction, "bin").getAsBoolean();
                } catch (NullPointerException ignored) {
                }

                Instant endingAt = Instant.ofEpochMilli(higherDepth(currentAuction, "end").getAsLong());
                Duration duration = Duration.between(Instant.now(), endingAt);
                long daysUntil = duration.toMinutes() / 1400;
                long hoursUntil = duration.toMinutes() / 60 % 24;
                long minutesUntil = duration.toMinutes() % 60;
                String timeUntil = daysUntil > 0 ? daysUntil + "d " : "";
                timeUntil += hoursUntil > 0 ? hoursUntil + "h " : "";
                timeUntil += minutesUntil > 0 ? minutesUntil + "m " : "";

                if (higherDepth(currentAuction, "item_name").getAsString().equals("Enchanted Book")) {
                    auctions[i][0] = higherDepth(currentAuction, "item_lore").getAsString().split("\n")[0].replaceAll("§9|§d|§l", "");
                } else {
                    auctions[i][0] = (isPet ? capitalizeString(higherDepth(currentAuction, "tier").getAsString().toLowerCase()) + " "
                            : "") + higherDepth(currentAuction, "item_name").getAsString();
                }

                int highestBid = higherDepth(currentAuction, "highest_bid_amount").getAsInt();
                int startingBid = higherDepth(currentAuction, "starting_bid").getAsInt();
                if (timeUntil.length() > 0) {
                    if (bin) {
                        auction = "BIN: " + simplifyNumber(startingBid) + " coins";
                    } else {
                        auction = "Current bid: " + simplifyNumber(highestBid);
                    }
                    auction += " | Ending in " + timeUntil;
                } else {
                    if (highestBid >= startingBid) { // Auction sold
                        auction = "Auction sold for " + simplifyNumber(highestBid) + " coins";
                    } else {
                        auction = "Auction did not sell";
                    }

                }
                auctions[i][1] = auction;
            }
        }

        EmbedBuilder eb = defaultEmbed(this.playerUsername + "'s auctions", null);
        for (String[] auction : auctions) {
            if (auction[0] != null) {
                for (String[] strings : auctions) {
                    if (strings[0] != null) {
                        eb.addField(strings[0], strings[1], false);
                    }
                }
                return eb;
            }
        }
        eb.setTitle("No auctions found for " + this.playerUsername, null);
        return eb;
    }

    public boolean usernameToUuid(String username) {
        try {
            JsonElement usernameJson = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);
            this.playerUsername = higherDepth(usernameJson, "name").getAsString();
            this.playerUuid = higherDepth(usernameJson, "id").getAsString();
            return true;
        } catch (Exception e) {
            System.out.println("Null - uuid - " + username);
        }
        return false;

    }

}