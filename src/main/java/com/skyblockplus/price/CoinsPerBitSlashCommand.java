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

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonElement;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
public class CoinsPerBitSlashCommand extends SlashCommand {

	public CoinsPerBitSlashCommand() {
		this.name = "coinsperbit";
	}

	@Override
	protected void execute(SlashCommandEvent event) {
		event.embed(getCoinsPerBit());
	}

	@Override
	public CommandData getCommandData() {
		return Commands.slash(name, "Get the coins to bits ratio for items in the bits shop");
	}

	public static EmbedBuilder getCoinsPerBit() {
		Map<String, Double> values = new HashMap<>();
		NetworthExecute calc = new NetworthExecute().initPrices();
		for (Map.Entry<String, JsonElement> entry : getBitsJson().entrySet()) {
			values.put(entry.getKey(), calc.getLowestPrice(entry.getKey()) / entry.getValue().getAsLong());
		}
		EmbedBuilder eb = defaultEmbed("Coins Per Bit");
		for (Map.Entry<String, Double> entry : values.entrySet().stream().sorted(Comparator.comparingDouble(v -> -v.getValue())).toList()) {
			eb.appendDescription(getEmoji(entry.getKey()) + " " + idToName(entry.getKey()) + " ➜ " + formatNumber(entry.getValue()) + "\n");
		}
		return eb;
	}
}
