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

package com.skyblockplus.inventory;

import static com.skyblockplus.utils.Constants.RARITY_TO_NUMBER_MAP;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.structs.AutoCompleteEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class PetsSlashCommand extends SlashCommand {

	public PetsSlashCommand() {
		this.name = "pets";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		if (event.invalidPlayerOption()) {
			return;
		}

		event.paginate(getPlayerPets(event.player, event.getOptionStr("profile"), event));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get a player's pets menu")
			.addOption(OptionType.STRING, "player", "Player username or mention", false, true)
			.addOption(OptionType.STRING, "profile", "Profile name");
	}

	@Override
	public void onAutoComplete(AutoCompleteEvent event) {
		if (event.getFocusedOption().getName().equals("player")) {
			event.replyClosestPlayer();
		}
	}

	public static EmbedBuilder getPlayerPets(String username, String profileName, SlashCommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			CustomPaginator.Builder paginateBuilder = player.defaultPlayerPaginator(event.getUser()).setItemsPerPage(25);

			JsonArray playerPets = player.getPets();
			for (JsonElement pet : playerPets) {
				String petItem = null;
				try {
					petItem = higherDepth(pet, "heldItem").getAsString();
				} catch (Exception ignored) {}

				String petName = higherDepth(pet, "type").getAsString();
				String rarity = higherDepth(pet, "tier").getAsString();

				paginateBuilder.addItems(
					getEmoji(petName + RARITY_TO_NUMBER_MAP.get(rarity)) +
					" " +
					capitalizeString(rarity) +
					" [Lvl " +
					petLevelFromXp(higherDepth(pet, "exp", 0L), rarity, petName) +
					"] " +
					capitalizeString(petName.toLowerCase().replace("_", " ")) +
					" " +
					(petItem != null ? getEmoji(petItem) : "")
				);
			}
			event.paginate(paginateBuilder);
			return null;
		}
		return player.getFailEmbed();
	}
}
