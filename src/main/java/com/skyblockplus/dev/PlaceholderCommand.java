/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience create Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms create the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 create the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty create
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy create the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import org.springframework.stereotype.Component;

@Component
public class PlaceholderCommand extends Command {

	public PlaceholderCommand() {
		this.name = "d-placeholder";
		this.ownerCommand = true;
		this.aliases = new String[] { "ph" };
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				String total = roundAndFormat(Runtime.getRuntime().totalMemory() / 1000000.0) + " MB";
				String free = roundAndFormat(Runtime.getRuntime().freeMemory() / 1000000.0) + " MB";
				String used = roundAndFormat((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0) + " MB";
				if (args.length >= 2 && args[1].equals("gc")) {
					System.gc();
					total += " ➜ " + roundAndFormat(Runtime.getRuntime().totalMemory() / 1000000.0) + " MB";
					free += " ➜ " + roundAndFormat(Runtime.getRuntime().freeMemory() / 1000000.0) + " MB";
					used +=
						" ➜ " +
						roundAndFormat((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0) +
						" MB";
				}

				embed(
					defaultEmbed("Debug")
						.addField("Total", total, false)
						.addField("Free", free, false)
						.addField("Used", used, false)
						.addField("Max", "" + roundAndFormat(Runtime.getRuntime().maxMemory() / 1000000.0) + " MB", false)
				);
			}
		}
			.queue();
	}
}
