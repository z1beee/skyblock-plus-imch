package com.skyblockplus.auctionbaz;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.time.Duration;
import java.time.Instant;

import static com.skyblockplus.utils.Utils.*;

public class AuctionCommand extends Command {

    public AuctionCommand() {
        this.name = "auction";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"ah"};
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
                ebMessage.editMessage(getPlayerAuction(args[1]).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getPlayerAuction(String username) {
        UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
        if (usernameUuidStruct == null) {
            return defaultEmbed("Error fetching player data");
        }

        String url = "https://api.hypixel.net/skyblock/auction?key=" + HYPIXEL_API_KEY + "&player=" + usernameUuidStruct.playerUuid;

        JsonElement playerAuctions = getJson(url);
        JsonArray auctionsArray = higherDepth(playerAuctions, "auctions").getAsJsonArray();
        String[][] auctions = new String[auctionsArray.size()][2];

        int totalSoldValue = 0;
        int totalPendingValue = 0;

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
                    auctions[i][0] = parseMcCodes(higherDepth(currentAuction, "item_lore").getAsString().split("\n")[0]);
                } else {
                    auctions[i][0] = (isPet
                            ? capitalizeString(higherDepth(currentAuction, "tier").getAsString().toLowerCase()) + " "
                            : "") + higherDepth(currentAuction, "item_name").getAsString();
                }

                int highestBid = higherDepth(currentAuction, "highest_bid_amount").getAsInt();
                int startingBid = higherDepth(currentAuction, "starting_bid").getAsInt();
                if (timeUntil.length() > 0) {
                    if (bin) {
                        auction = "BIN: " + simplifyNumber(startingBid) + " coins";
                        totalPendingValue += startingBid;
                    } else {
                        auction = "Current bid: " + simplifyNumber(highestBid);
                        totalPendingValue += highestBid;
                    }
                    auction += " | Ending in " + timeUntil;
                } else {
                    if (highestBid >= startingBid) {
                        auction = "Auction sold for " + simplifyNumber(highestBid) + " coins";
                        totalSoldValue += highestBid;
                    } else {
                        auction = "Auction did not sell";
                    }

                }
                auctions[i][1] = auction;
            }
        }

        EmbedBuilder eb = defaultEmbed(usernameUuidStruct.playerUsername, "https://auctions.craftlink.xyz/players/" + usernameUuidStruct.playerUuid);
        for (String[] auction : auctions) {
            if (auction[0] != null) {
                for (String[] strings : auctions) {
                    if (strings[0] != null) {
                        eb.addField(strings[0], strings[1], false);
                    }
                }
                eb.setThumbnail("https://cravatar.eu/helmavatar/" + usernameUuidStruct.playerUuid + "/64.png");
                eb.setDescription("**Sold Auctions Value:** " + simplifyNumber(totalSoldValue) + "\n**Unsold Auctions Value:** " + simplifyNumber(totalPendingValue));
                return eb;
            }
        }
        eb.setTitle("No auctions found for " + usernameUuidStruct.playerUsername, null);
        return eb;
    }
}