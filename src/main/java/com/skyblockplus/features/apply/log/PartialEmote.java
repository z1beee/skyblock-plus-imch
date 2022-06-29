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

package com.skyblockplus.features.apply.log;

import net.dv8tion.jda.api.entities.emoji.CustomEmoji;

public record PartialEmote(long id, String name, boolean animated) {
	public String getImageUrl() {
		return String.format(CustomEmoji.ICON_URL, id, animated ? "gif" : "png");
	}

	public String getAsMention() {
		return (animated ? "<a:" : "<:") + name + ":" + id + ">";
	}
}
