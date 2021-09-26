/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.price;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Hypixel.*;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.PaginatorExtras;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Duration;
import java.time.Instant;
import me.nullicorn.nedit.NBTReader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class AuctionCommand extends Command {

	public AuctionCommand() {
		this.name = "auctions";
		this.cooldown = globalCooldown;
		this.aliases = new String[] { "ah", "auction" };
		this.botPermissions = defaultPerms();
	}

	public static EmbedBuilder getPlayerAuction(String username, User user, MessageChannel channel, InteractionHook hook) {
		UsernameUuidStruct usernameUuidStruct = usernameToUuid(username);
		if (usernameUuidStruct.isNotValid()) {
			return invalidEmbed(usernameUuidStruct.failCause);
		}

		HypixelResponse auctionsResponse = getAuctionFromPlayer(usernameUuidStruct.playerUuid);
		if (auctionsResponse.isNotValid()) {
			return invalidEmbed(auctionsResponse.failCause);
		}
		JsonArray auctionsArray = auctionsResponse.response.getAsJsonArray();

		String[][] auctions = new String[auctionsArray.size()][2];

		long totalSoldValue = 0;
		long totalPendingValue = 0;

		for (int i = 0; i < auctionsArray.size(); i++) {
			JsonElement currentAuction = auctionsArray.get(i);
			if (!higherDepth(currentAuction, "claimed").getAsBoolean()) {
				String auction;
				boolean isPet = higherDepth(currentAuction, "item_lore").getAsString().toLowerCase().contains("pet");
				boolean bin = false;
				try {
					bin = higherDepth(currentAuction, "bin").getAsBoolean();
				} catch (NullPointerException ignored) {}

				Instant endingAt = Instant.ofEpochMilli(higherDepth(currentAuction, "end").getAsLong());
				Duration duration = Duration.between(Instant.now(), endingAt);
				String timeUntil = instantToDHM(duration);

				if (higherDepth(currentAuction, "item_name").getAsString().equals("Enchanted Book")) {
					auctions[i][0] = parseMcCodes(higherDepth(currentAuction, "item_lore").getAsString().split("\n")[0]);
				} else {
					auctions[i][0] =
						(isPet ? capitalizeString(higherDepth(currentAuction, "tier").getAsString().toLowerCase()) + " " : "") +
						higherDepth(currentAuction, "item_name").getAsString();
				}

				long highestBid = higherDepth(currentAuction, "highest_bid_amount", 0);
				long startingBid = higherDepth(currentAuction, "starting_bid", 0);
				if (duration.toMillis() > 0) {
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

		CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, user).setColumns(1).setItemsPerPage(10);
		PaginatorExtras extras = new PaginatorExtras()
			.setEveryPageTitle(usernameUuidStruct.playerUsername)
			.setEveryPageTitleUrl(skyblockStatsLink(usernameUuidStruct.playerUsername, null))
			.setEveryPageThumbnail("https://cravatar.eu/helmavatar/" + usernameUuidStruct.playerUuid + "/64.png")
			.setEveryPageText(
				"**Sold Auctions Value:** " +
				simplifyNumber(totalSoldValue) +
				"\n**Unsold Auctions Value:** " +
				simplifyNumber(totalPendingValue)
			);

		for (String[] auction : auctions) {
			if (auction[0] != null) {
				for (String[] strings : auctions) {
					if (strings[0] != null) {
						extras.addEmbedField(strings[0], strings[1], false);
					}
				}
				if (channel != null) {
					paginateBuilder.setPaginatorExtras(extras).build().paginate(channel, 0);
				} else {
					paginateBuilder.setPaginatorExtras(extras).build().paginate(hook, 0);
				}
				return null;
			}
		}

		EmbedBuilder eb = defaultEmbed(
			usernameUuidStruct.playerUsername,
			"https://auctions.craftlink.xyz/players/" + usernameUuidStruct.playerUuid
		);
		eb.setTitle("No auctions found for " + usernameUuidStruct.playerUsername, null);
		return eb;
	}

	public static EmbedBuilder getAuctionByUuid(String auctionUuid) {
		HypixelResponse auctionResponse = getAuctionFromUuid(auctionUuid);
		if (auctionResponse.isNotValid()) {
			return invalidEmbed(auctionResponse.failCause);
		}

		JsonElement auctionJson = auctionResponse.response.getAsJsonArray().get(0);
		EmbedBuilder eb = defaultEmbed("Auction from UUID");
		String itemName = higherDepth(auctionJson, "item_name").getAsString();

		String itemId = "None";
		try {
			itemId =
				NBTReader
					.readBase64(higherDepth(auctionJson, "item_bytes").getAsString())
					.getList("i")
					.getCompound(0)
					.getString("tag.ExtraAttributes.id", "None");
		} catch (Exception ignored) {}

		if (itemId.equals("ENCHANTED_BOOK")) {
			itemName = parseMcCodes(higherDepth(auctionJson, "item_lore").getAsString().split("\n")[0]);
		} else {
			itemName =
				(itemId.equals("PET") ? capitalizeString(higherDepth(auctionJson, "tier").getAsString().toLowerCase()) + " " : "") +
				itemName;
		}

		Instant endingAt = Instant.ofEpochMilli(higherDepth(auctionJson, "end").getAsLong());
		Duration duration = Duration.between(Instant.now(), endingAt);
		String timeUntil = instantToDHM(duration);

		String ebStr = "**Item name:** " + itemName;
		ebStr += "\n**Seller:** " + uuidToUsername(higherDepth(auctionJson, "auctioneer").getAsString()).playerUsername;
		ebStr += "\n**Command:** `/viewauction " + higherDepth(auctionJson, "uuid").getAsString() + "`";
		long highestBid = higherDepth(auctionJson, "highest_bid_amount", 0L);
		long startingBid = higherDepth(auctionJson, "starting_bid", 0L);
		JsonArray bidsArr = higherDepth(auctionJson, "bids").getAsJsonArray();
		boolean bin = higherDepth(auctionJson, "bin") != null;

		if (duration.toMillis() > 0) {
			if (bin) {
				ebStr += "\n**BIN:** " + simplifyNumber(startingBid) + " coins | Ending in " + timeUntil;
			} else {
				ebStr += "\n**Current bid:** " + simplifyNumber(highestBid) + " | Ending in " + timeUntil;
				ebStr +=
					bidsArr.size() > 0
						? "\n**Highest bidder:** " +
						uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString()).playerUsername
						: "";
			}
		} else {
			if (highestBid >= startingBid) {
				ebStr +=
					"\n**Auction sold** for " +
					simplifyNumber(highestBid) +
					" coins to " +
					uuidToUsername(higherDepth(bidsArr.get(bidsArr.size() - 1), "bidder").getAsString()).playerUsername;
			} else {
				ebStr = "\n**Auction did not sell**";
			}
		}

		eb.setThumbnail("https://sky.shiiyu.moe/item.gif/" + itemId);

		return eb.setDescription(ebStr);
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 3 && args[1].equals("uuid")) {
					embed(getAuctionByUuid(args[2]));
					return;
				} else if (args.length == 2 || args.length == 1) {
					if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
						return;
					}

					paginate(getPlayerAuction(username, event.getAuthor(), event.getChannel(), null));
					return;
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
