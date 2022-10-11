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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.features.jacob.JacobContest.CROP_NAME_TO_EMOJI;
import static com.skyblockplus.utils.Utils.*;

import com.skyblockplus.features.jacob.JacobContest;
import com.skyblockplus.features.jacob.JacobData;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorExtras;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;

@Component
public class JacobSlashCommand extends SlashCommand {

	public JacobSlashCommand() {
		this.name = "jacob";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.paginate(getJacobEmbed(event.getOptionStr("crop", "all"), event));
	}

	@Override
	public CommandData getCommandData() {
		return Commands
			.slash(name, "Get a list of upcoming farming contests")
			.addOptions(
				new OptionData(OptionType.STRING, "crop", "Crop to filter by")
					.addChoices(CROP_NAME_TO_EMOJI.keySet().stream().map(c -> new Command.Choice(c, c)).collect(Collectors.toList()))
			);
	}

	public static EmbedBuilder getJacobEmbed(String crop, SlashCommandEvent event) {
		crop = capitalizeString(crop);
		if (!CROP_NAME_TO_EMOJI.containsKey(crop) && !crop.equals("All")) {
			return invalidEmbed("Invalid crop");
		}

		JacobData data = JacobHandler.getJacobData();

		if (data.getContests().isEmpty()) {
			return defaultEmbed("Jacob Contests")
				.setDescription("**Year:** " + data.getYear())
				.addField("Contests", "None left for this year!", false);
		}

		PaginatorExtras extras = new PaginatorExtras(PaginatorExtras.PaginatorType.EMBED_FIELDS).setEveryPageTitle("Jacob Contests");
		String finalCrop = crop;
		for (JacobContest contest : crop.equals("All")
			? data.getContests()
			: data.getContests().stream().filter(c -> c.getCrops().stream().anyMatch(thisCrop -> thisCrop.equals(finalCrop))).toList()) {
			extras.addEmbedField(
				"Contest",
				"**In:** <t:" + contest.getTimeInstant().getEpochSecond() + ":R>\n**Crops:**\n" + contest.getCropsFormatted(false),
				true
			);
		}
		for (int i = 0; i < 3 - extras.getEmbedFields().size() % 3; i++) {
			extras.addBlankField(true);
		}
		CustomPaginator.Builder paginateBuilder = event.getPaginator().setItemsPerPage(12);
		event.paginate(paginateBuilder.setPaginatorExtras(extras));
		return null;
	}
}
