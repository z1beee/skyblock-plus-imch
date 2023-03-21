/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2023 kr45732
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

package com.skyblockplus.utils.utils;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.JsonUtils.getInternalJsonMappings;
import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;
import static java.lang.String.join;
import static java.util.Collections.nCopies;

import com.google.gson.JsonElement;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

public class StringUtils {

	private static final Pattern mcColorPattern = Pattern.compile("(?i)\\u00A7[\\dA-FK-OR]");
	private static final Pattern uuidDashRegex = Pattern.compile("(.{8})(.{4})(.{4})(.{4})(.{12})");

	public static String formatNumber(long number) {
		return NumberFormat.getInstance(Locale.US).format(number);
	}

	public static String formatNumber(double number) {
		return NumberFormat.getInstance(Locale.US).format(number);
	}

	public static String roundAndFormat(double number) {
		DecimalFormat df = new DecimalFormat("#.##");
		df.setRoundingMode(RoundingMode.HALF_UP);
		return formatNumber(Double.parseDouble(df.format(number)));
	}

	public static String roundProgress(double number) {
		DecimalFormat df = new DecimalFormat("#.##");
		df.setRoundingMode(RoundingMode.HALF_UP);
		return df.format(number * 100) + "%";
	}

	public static String simplifyNumber(double number) {
		String formattedNumber;
		DecimalFormat df = new DecimalFormat("#.##");
		df.setRoundingMode(RoundingMode.HALF_UP);
		if (1000000000000D > number && number >= 1000000000) {
			number = number >= 999999999950D ? 999999999949D : number;
			formattedNumber = df.format(number / 1000000000) + "B";
		} else if (number >= 1000000) {
			number = number >= 999999950D ? 999999949D : number;
			formattedNumber = df.format(number / 1000000) + "M";
		} else if (number >= 1000) {
			number = number >= 999950D ? 999949D : number;
			formattedNumber = df.format(number / 1000) + "K";
		} else {
			formattedNumber = df.format(number);
		}
		return formattedNumber;
	}

	public static String capitalizeString(String str) {
		return str == null
			? null
			: Stream
				.of(str.trim().split("\\s"))
				.filter(word -> word.length() > 0)
				.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	public static String parseMcCodes(String unformattedString) {
		return mcColorPattern.matcher(unformattedString.replace("\u00A7ka", "")).replaceAll("");
	}

	public static String fixUsername(String username) {
		return username.replace("_", "\\_");
	}

	public static String toPrettyTime(long millis) {
		long seconds = millis / 1000 % 60;
		long minutes = (millis / 1000 / 60) % 60;
		long hours = (millis / 1000 / 60 / 60) % 24;
		long days = (millis / 1000 / 60 / 60 / 24);

		String formattedTime = "";
		if (minutes == 0 && hours == 0 && days == 0) {
			formattedTime += seconds + "s";
		} else if (hours == 0 && days == 0) {
			formattedTime += minutes + "m" + (seconds > 0 ? seconds + "s" : "");
		} else if (days == 0) {
			if (hours <= 6) {
				formattedTime += hours + "h" + minutes + "m" + seconds + "s";
			} else {
				formattedTime += hours + "h";
			}
		} else {
			formattedTime += days + "d" + hours + "h";
		}
		return formattedTime;
	}

	public static String nameMcHyperLink(String username, String uuid) {
		return "[**" + username + "**](" + nameMcLink(uuid) + ")";
	}

	public static String nameMcLink(String uuid) {
		return "https://mine.ly/" + uuid;
	}

	public static String padStart(String string, int minLength, char padChar) {
		return string.length() >= minLength ? string : (String.valueOf(padChar).repeat(minLength - string.length()) + string);
	}

	public static UUID stringToUuid(String uuid) {
		Matcher matcher = uuidDashRegex.matcher(uuid);
		return UUID.fromString(matcher.matches() ? uuidDashRegex.matcher(uuid).replaceAll("$1-$2-$3-$4-$5") : uuid);
	}

	public static String toRomanNumerals(int number) {
		return join("", nCopies(number, "i")).replace("iiiii", "v").replace("iiii", "iv").replace("vv", "x").replace("viv", "ix");
	}

	public static String skyblockStatsLink(String username, String profileName) {
		if (username == null) {
			return null;
		}

		return (
			"https://sky.shiiyu.moe/stats/" +
			username +
			(profileName != null && !profileName.equalsIgnoreCase("Not Allowed To Quit Skyblock Ever Again") ? "/" + profileName : "")
		);
	}

	public static String nameToId(String itemName) {
		return nameToId(itemName, false);
	}

	public static String nameToId(String itemName, boolean strict) {
		String id = itemName.trim().toUpperCase().replace(" ", "_").replace("'S", "").replace("FRAG", "FRAGMENT").replace(".", "");

		switch (id) {
			case "GOD_POT":
				return "GOD_POTION_2";
			case "AOTD":
				return "ASPECT_OF_THE_DRAGON";
			case "AOTE":
				return "ASPECT_OF_THE_END";
			case "AOTV":
				return "ASPECT_OF_THE_VOID";
			case "AOTS:":
				return "AXE_OF_THE_SHREDDED";
			case "LASR_EYE":
				return "GIANT_FRAGMENT_LASER";
			case "HYPE":
				return "HYPERION";
			case "GS":
				return "GIANTS_SWORD";
			case "TERM":
				return "TERMINATOR";
			case "TREECAP":
				return "TREECAPITATOR_AXE";
			case "JUJU":
				return "JUJU_SHORTBOW";
			case "VALK":
				return "VALKYRIE";
			case "HANDLE":
				return "NECRON_HANDLE";
		}

		for (Map.Entry<String, JsonElement> entry : getInternalJsonMappings().entrySet()) {
			if (higherDepth(entry.getValue(), "name").getAsString().equalsIgnoreCase(itemName)) {
				return entry.getKey();
			}
		}

		return strict ? null : id;
	}

	public static String idToName(String id) {
		return idToName(id, false);
	}

	public static String idToName(String id, boolean strict) {
		id = id.toUpperCase();
		return higherDepth(getInternalJsonMappings(), id + ".name", strict ? null : capitalizeString(id.replace("_", " ")));
	}

	/**
	 * @param toMatch   name to match
	 * @param matchFrom list of ID (will convert to their names)
	 */
	public static String getClosestMatchFromIds(String toMatch, Collection<String> matchFrom) {
		if (matchFrom == null || matchFrom.isEmpty()) {
			return toMatch;
		}

		return FuzzySearch
			.extractOne(
				toMatch,
				matchFrom.stream().collect(Collectors.toMap(Function.identity(), StringUtils::idToName)).entrySet(),
				Map.Entry::getValue
			)
			.getReferent()
			.getKey();
	}

	public static String getClosestMatch(String toMatch, List<String> matchFrom) {
		if (matchFrom == null || matchFrom.isEmpty()) {
			return toMatch;
		}

		return FuzzySearch.extractOne(toMatch, matchFrom).getString();
	}

	public static List<String> getClosestMatch(String toMatch, List<String> matchFrom, int numMatches) {
		if (matchFrom == null || matchFrom.isEmpty()) {
			return new ArrayList<>(List.of(toMatch));
		}

		return FuzzySearch
			.extractTop(toMatch, matchFrom, numMatches)
			.stream()
			.map(ExtractedResult::getString)
			.collect(Collectors.toCollection(ArrayList::new));
	}

	public static String getItemThumbnail(String id) {
		if (PET_NAMES.contains(id.split(";")[0].trim())) {
			return getPetUrl(id);
		} else if (ENCHANT_NAMES.contains(id.split(";")[0].trim())) {
			return "https://sky.shiiyu.moe/item.gif/ENCHANTED_BOOK";
		}
		return "https://sky.shiiyu.moe/item.gif/" + id;
	}

	public static String getAvatarlUrl(String uuid) {
		return "https://cravatar.eu/helmavatar/" + uuid + "/64.png";
	}

	public static String getAuctionUrl(String uuid) {
		return "https://auctions.craftlink.xyz/players/" + uuid;
	}

	public static String getPetUrl(String petId) {
		try {
			return "https://sky.shiiyu.moe/head/" + higherDepth(getInternalJsonMappings(), petId + ".texture").getAsString();
		} catch (Exception e) {
			return null;
		}
	}

	public static String profileNameToEmoji(String profileName) {
		return profileNameToEmoji.getOrDefault(profileName, null);
	}

	public static int tryParseInt(String s, int defaultValue) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return defaultValue;
		}
	}
}