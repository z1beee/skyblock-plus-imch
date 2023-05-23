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

package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.getClosestMatchesFromIds;
import static com.skyblockplus.utils.utils.Utils.*;
import static com.skyblockplus.utils.utils.Utils.getEmoji;

import com.google.gson.JsonElement;
import com.skyblockplus.miscellaneous.networth.NetworthExecute;
import com.skyblockplus.utils.command.SlashCommandEvent;
import com.skyblockplus.utils.utils.StringUtils;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.apache.groovy.util.Maps;

public class CraftCommandHandler {

	public static final List<String> ignoredCategories = List.of(
		"TRAVEL_SCROLL",
		"SHEARS",
		"NONE",
		"DUNGEON_PASS",
		"ARROW_POISON",
		"PORTAL",
		"PET_ITEM",
		"REFORGE_STONE",
		"COSMETIC",
		"BAIT",
		"ARROW"
	);
	private static final List<String> woodSingularityItems = List.of(
		"WOOD_SWORD",
		"ASPECT_OF_THE_JERRY",
		"SCORPION_FOIL",
		"TACTICIAN_SWORD",
		"SWORD_OF_REVELATIONS",
		"SWORD_OF_BAD_HEALTH",
		"GREAT_SPOOK_SWORD"
	);
	private static final List<String> weaponCategories = List.of("SWORD", "LONGSWORD", "BOW", "FISHING_ROD", "FISHING_WEAPON");
	private static final List<String> armorCategories = List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");
	private static final List<String> dyeMaterials = List.of("LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS");
	private static final List<String> necronBladeScrollItems = List.of("NECRON_BLADE", "HYPERION", "VALKYRIE", "ASTRAEA", "SCYLLA");
	// --
	private static final List<String> accessoryEnrichments = List.of(
		"TALISMAN_ENRICHMENT_ATTACK_SPEED",
		"TALISMAN_ENRICHMENT_CRITICAL_CHANCE",
		"TALISMAN_ENRICHMENT_CRITICAL_DAMAGE",
		"TALISMAN_ENRICHMENT_DEFENSE",
		"TALISMAN_ENRICHMENT_FEROCITY",
		"TALISMAN_ENRICHMENT_HEALTH",
		"TALISMAN_ENRICHMENT_INTELLIGENCE",
		"TALISMAN_ENRICHMENT_MAGIC_FIND",
		"TALISMAN_ENRICHMENT_SEA_CREATURE_CHANCE",
		"TALISMAN_ENRICHMENT_STRENGTH",
		"TALISMAN_ENRICHMENT_WALK_SPEED"
	);
	private static final List<String> dyes = List.of(
		"DYE_AQUAMARINE",
		"DYE_BINGO_BLUE",
		"DYE_BONE",
		"DYE_BRICK_RED",
		"DYE_BYZANTIUM",
		"DYE_CARMINE",
		"DYE_CELESTE",
		"DYE_DARK_PURPLE",
		"DYE_EMERALD",
		"DYE_FLAME",
		"DYE_HOLLY",
		"DYE_MANGO",
		"DYE_NADESHIKO",
		"DYE_NECRON",
		"DYE_NYANZA",
		"DYE_PURE_BLACK",
		"DYE_PURE_WHITE",
		"DYE_WILD_STRAWBERRY"
	);
	private static final List<String> drillUpgradeModules = List.of(
		"GOBLIN_OMELETTE",
		"GOBLIN_OMELETTE_BLUE_CHEESE",
		"GOBLIN_OMELETTE_PESTO",
		"GOBLIN_OMELETTE_SPICY",
		"GOBLIN_OMELETTE_SUNNY_SIDE"
	);
	private static final List<String> drillFuelTanks = List.of(
		"MITHRIL_FUEL_TANK",
		"TITANIUM_FUEL_TANK",
		"GEMSTONE_FUEL_TANK",
		"PERFECTLY_CUT_FUEL_TANK"
	);
	private static final List<String> drillEngines = List.of(
		"MITHRIL_DRILL_ENGINE",
		"TITANIUM_DRILL_ENGINE",
		"RUBY_POLISHED_DRILL_ENGINE",
		"SAPPHIRE_POLISHED_DRILL_ENGINE",
		"AMBER_POLISHED_DRILL_ENGINE"
	);
	private static final List<String> powerScrolls = List.of(
		"AMBER_POWER_SCROLL",
		"AMETHYST_POWER_SCROLL",
		"JASPER_POWER_SCROLL",
		"RUBY_POWER_SCROLL",
		"SAPPHIRE_POWER_SCROLL",
		"OPAL_POWER_SCROLL"
	);
	private static final List<String> gemstoneTiers = List.of("ROUGH", "FLAWED", "FINE", "FLAWLESS", "PERFECT");
	private static final Map<String, List<String>> slotTypeToGemstones = Maps.of(
		"AMBER",
		List.of("AMBER"),
		"TOPAZ",
		List.of("TOPAZ"),
		"SAPPHIRE",
		List.of("SAPPHIRE"),
		"AMETHYST",
		List.of("AMETHYST"),
		"JASPER",
		List.of("JASPER"),
		"RUBY",
		List.of("RUBY"),
		"JADE",
		List.of("JADE"),
		"OPAL",
		List.of("OPAL"),
		"COMBAT",
		List.of("SAPPHIRE", "AMETHYST", "JASPER", "RUBY"),
		"OFFENSIVE",
		List.of("SAPPHIRE", "JASPER"),
		"DEFENSIVE",
		List.of("AMETHYST", "RUBY", "OPAL"),
		"MINING",
		List.of("JADE", "AMBER", "TOPAZ"),
		"UNIVERSAL",
		List.of("AMBER", "TOPAZ", "SAPPHIRE", "AMETHYST", "JASPER", "RUBY", "JADE", "OPAL")
	);
	// --
	private final SlashCommandEvent slashCommandEvent;
	private Message message;
	private final String category;
	private final NetworthExecute calculator;
	private final String itemId;
	// Added values
	private boolean recombobulated = false;
	private final Set<String> enchants = new HashSet<>();
	private int hpbCount = 0;
	private int fpbCount = 0;
	private int stars = 0;
	private String reforge = null;
	private String rune = null;
	private final Map<String, String> gemstones = new HashMap<>();
	private String drillUpgradeModule = null;
	private String drillFuelTank = null;
	private String drillEngine = null;
	private String dye = null;
	private String accessoryEnrichment = null;
	private int manaDisintegratorCount = 0;
	private int ffdCount = 0;
	private final Set<String> necronBladeScrolls = new HashSet<>();
	private String skin = null;
	private String powerScroll = null;
	private boolean woodSingularityApplied = false;
	private boolean artOfWarApplied = false;
	private boolean artOfPeaceApplied = false;

	public CraftCommandHandler(String itemId, SlashCommandEvent slashCommandEvent) {
		// TODO: Pets (pet skin & held item), attributes

		this.itemId = itemId;
		this.slashCommandEvent = slashCommandEvent;
		this.calculator = new NetworthExecute().initPrices();

		JsonElement itemInfo = getSkyblockItemsJson().get(itemId);

		this.category = higherDepth(itemInfo, "category", null);
		if (category == null || ignoredCategories.contains(category)) {
			slashCommandEvent.embed(errorEmbed("This item is not supported"));
			return;
		}

		StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu
			.create("craft_command_main")
			.addOption("Toggle Recombobulator", "toggle_recombobulator");

		if (higherDepth(getEnchantsJson(), "enchants." + category) != null) {
			selectMenuBuilder.addOption("Enchants", "enchants");
		}

		if (weaponCategories.contains(category) || armorCategories.contains(category)) {
			selectMenuBuilder.addOption("Potato Books", "potato_books");
		}

		if (higherDepth(getEssenceCostsJson(), itemId) != null) {
			selectMenuBuilder.addOption("Stars", "stars");
		}

		if (CATEGORY_TO_REFORGES.containsKey(category)) {
			selectMenuBuilder.addOption("Reforge", "reforge");
		}

		if (weaponCategories.contains(category) || category.equals("CHESTPLATE")) {
			selectMenuBuilder.addOption("Rune", "rune");
		}

		if (higherDepth(itemInfo, "gemstone_slots") != null) {
			selectMenuBuilder.addOption("Gemstones", "gemstones");
		}

		if (category.equals("DRILL")) {
			selectMenuBuilder.addOption("Drill Upgrades", "drill_upgrades");
		}

		if (dyeMaterials.contains(higherDepth(itemInfo, "material", ""))) {
			selectMenuBuilder.addOption("Dye", "dye");
		}

		if (category.equals("ACCESSORY")) {
			selectMenuBuilder.addOption("Accessory Enrichment", "accessory_enrichment");
		}

		if (category.equals("DEPLOYABLE") || category.equals("WAND")) {
			selectMenuBuilder.addOption("Mana Disintegrator", "mana_disintegrator");
		}

		if (category.equals("HOE") || category.equals("AXE")) {
			selectMenuBuilder.addOption("Farming For Dummies", "farming_for_dummies");
		}

		if (necronBladeScrollItems.contains(itemId)) {
			selectMenuBuilder.addOption("Necron's Blade Scrolls", "necron_blade_scrolls");
		}

		if (higherDepth(getInternalJsonMappings(), itemId + ".skins.[0]") != null) {
			selectMenuBuilder.addOption("Skin", "skin");
		}

		if (higherDepth(getInternalJsonMappings(), itemId + ".scrollable", false)) {
			selectMenuBuilder.addOption("Power Scroll", "power_scroll");
		}

		if (woodSingularityItems.contains(itemId)) {
			selectMenuBuilder.addOption("Toggle Wood Singularity", "toggle_wood_singularity");
		}

		if (weaponCategories.contains(category)) {
			selectMenuBuilder.addOption("Toggle Art Of War", "toggle_art_of_war");
		}

		if (armorCategories.contains(category)) {
			selectMenuBuilder.addOption("Toggle Art Of Peace", "toggle_art_of_peace");
		}

		double basePrice = calculator.getLowestPrice(itemId);
		slashCommandEvent
			.getHook()
			.editOriginalEmbeds(
				defaultEmbed("Craft Helper For " + idToName(itemId))
					.setDescription(
						getEmoji("ENCHANTED_GOLD") +
						" **Total Price:** " +
						roundAndFormat(basePrice) +
						"\n\n" +
						getEmoji(itemId) +
						" Base Price: " +
						roundAndFormat(basePrice)
					)
					.build()
			)
			.setActionRow(selectMenuBuilder.build())
			.queue(
				m -> {
					message = m;
					waitForEvent();
				},
				ignore
			);
	}

	private boolean condition(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof StringSelectInteractionEvent event) {
			if (event.isFromGuild() && event.getUser().getId().equals(slashCommandEvent.getUser().getId())) {
				if (event.getMessageId().equals(message.getId())) {
					return true;
				}

				if (event.getComponentId().startsWith("craft_command_")) {
					String[] split = event.getComponentId().split("_");
					return split[split.length - 1].equals(message.getId());
				}
			}
		} else if (genericEvent instanceof ModalInteractionEvent event) {
			if (event.isFromGuild() && event.getUser().getId().equals(slashCommandEvent.getUser().getId())) {
				if (event.getModalId().startsWith("craft_command_")) {
					String[] split = event.getModalId().split("_");
					return split[split.length - 1].equals(message.getId());
				}
			}
		}
		return false;
	}

	private void action(GenericInteractionCreateEvent genericEvent) {
		if (genericEvent instanceof StringSelectInteractionEvent event) {
			onStringSelectInteraction(event);
		} else if (genericEvent instanceof ModalInteractionEvent event) {
			onModalEvent(event);
		}

		waitForEvent();
	}

	private void onStringSelectInteraction(StringSelectInteractionEvent event) {
		if (event.getComponentId().equals("craft_command_main")) {
			SelectOption selectedOption = event.getSelectedOptions().get(0);
			switch (selectedOption.getValue()) {
				case "toggle_recombobulator" -> {
					recombobulated = !recombobulated;
					event
						.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Recombobulated: " + recombobulated).build())
						.setEphemeral(true)
						.queue();
					updateMainMessage();
				}
				case "enchants" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select an option from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_enchants_main_" + message.getId())
							.addOption("Add Enchant", "add_enchant")
							.addOption("Remove Enchant", "remove_enchant")
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "gemstones" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select an option from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_gemstones_main_" + message.getId())
							.addOption("Add Gemstone", "add_gemstone")
							.addOption("Remove Gemstone", "remove_gemstone")
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "drill_upgrades" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select an option from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_drill_upgrades_main_" + message.getId())
							.addOption("Drill Upgrade Module", "drill_upgrade_module")
							.addOption("Drill Fuel Tank", "drill_fuel_tank")
							.addOption("Drill Engine", "drill_engine")
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "potato_books" -> event
					.replyModal(
						Modal
							.create("craft_command_pbs_" + message.getId(), "Craft Helper - Potato Books")
							.addActionRow(
								TextInput
									.create("hpb", "Hot Potato Books Count", TextInputStyle.SHORT)
									.setValue("" + hpbCount)
									.setRequired(false)
									.build()
							)
							.addActionRow(
								TextInput
									.create("fpb", "Fuming Potato Books Count", TextInputStyle.SHORT)
									.setValue("" + fpbCount)
									.setRequired(false)
									.build()
							)
							.build()
					)
					.queue();
				case "dye" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a dye from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_dye_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								dyes.stream().map(o -> SelectOption.of(idToName(o), o)).collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "accessory_enrichment" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select an accessory enrichment from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_accessory_enrichments_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								accessoryEnrichments
									.stream()
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "mana_disintegrator" -> event
					.replyModal(
						Modal
							.create("craft_command_mana_disintegrator_" + message.getId(), "Craft Helper - Mana Disintegrator")
							.addActionRow(
								TextInput
									.create("value", "Mana Disintegrator Count", TextInputStyle.SHORT)
									.setValue("" + manaDisintegratorCount)
									.setRequired(false)
									.build()
							)
							.build()
					)
					.queue();
				case "farming_for_dummies" -> event
					.replyModal(
						Modal
							.create("craft_command_ffd_" + message.getId(), "Craft Helper - Farming For Dummies")
							.addActionRow(
								TextInput
									.create("value", "Farming For Dummies Count", TextInputStyle.SHORT)
									.setValue("" + ffdCount)
									.setRequired(false)
									.build()
							)
							.build()
					)
					.queue();
				case "necron_blade_scrolls" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select necron's blade scrolls from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_necron_blade_scrolls_" + message.getId())
							.addOption("None", "none")
							.addOption("Implosion", "IMPLOSION_SCROLL")
							.addOption("Shadow Warp", "SHADOW_WARP_SCROLL")
							.addOption("Wither Shield", "WITHER_SHIELD_SCROLL")
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "skin" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a skin from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_skin_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								streamJsonArray(higherDepth(getInternalJsonMappings(), itemId + ".skins"))
									.map(o -> SelectOption.of(idToName(o.getAsString()), o.getAsString()))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "power_scroll" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a scroll from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_skin_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								powerScrolls
									.stream()
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "stars" -> event
					.replyModal(
						Modal
							.create("craft_command_stars_" + message.getId(), "Craft Helper - Stars")
							.addActionRow(TextInput.create("value", "Stars", TextInputStyle.SHORT).setValue("" + stars).build())
							.build()
					)
					.queue();
				case "reforge" -> event
					.replyModal(
						Modal
							.create("craft_command_reforge_" + message.getId(), "Craft Helper - Reforge")
							.addActionRow(TextInput.create("value", "Reforge", TextInputStyle.SHORT).setValue(reforge).build())
							.build()
					)
					.queue();
				case "rune" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a rune from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_rune_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								networthRunes
									.stream()
									.filter(o -> category.equals("CHESTPLATE") ^ o.startsWith("MUSIC_RUNE"))
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "toggle_wood_singularity" -> {
					woodSingularityApplied = !woodSingularityApplied;
					event
						.replyEmbeds(
							defaultEmbed("Craft Helper").setDescription("Wood Singularity Applied: " + woodSingularityApplied).build()
						)
						.setEphemeral(true)
						.queue();
					updateMainMessage();
				}
				case "toggle_art_of_war" -> {
					artOfWarApplied = !artOfWarApplied;
					event
						.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Art Of War Applied: " + artOfWarApplied).build())
						.setEphemeral(true)
						.queue();
					updateMainMessage();
				}
				case "toggle_art_of_peace" -> {
					artOfPeaceApplied = !artOfPeaceApplied;
					event
						.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Art Of Peace Applied: " + artOfPeaceApplied).build())
						.setEphemeral(true)
						.queue();
					updateMainMessage();
				}
			}
		} else if (event.getComponentId().startsWith("craft_command_enchants_main_")) {
			SelectOption selectedOption = event.getSelectedOptions().get(0);
			switch (selectedOption.getValue()) {
				case "add_enchant" -> event
					.replyModal(
						Modal
							.create("craft_command_enchants_add_" + message.getId(), "Craft Helper - Add Enchant")
							.addActionRow(TextInput.create("value", "Enchant Name & Level", TextInputStyle.SHORT).build())
							.build()
					)
					.queue();
				case "remove_enchant" -> {
					if (enchants.isEmpty()) {
						event.editMessageEmbeds(errorEmbed("No enchants added").build()).queue();
					} else {
						event
							.replyModal(
								Modal
									.create("craft_command_enchants_remove_" + message.getId(), "Craft Helper - Remove Enchant")
									.addActionRow(TextInput.create("value", "Enchant Name & Level", TextInputStyle.SHORT).build())
									.build()
							)
							.queue();
					}
				}
			}
		} else if (event.getComponentId().startsWith("craft_command_gemstones_main_")) {
			SelectOption selectedOption = event.getSelectedOptions().get(0);
			switch (selectedOption.getValue()) {
				case "add_gemstone" -> event
					.editMessageEmbeds(
						defaultEmbed("Craft Helper").setDescription("Select a gemstone slot to add from the menu below").build()
					)
					.setActionRow(
						StringSelectMenu
							.create("craft_command_gemstones_add_" + message.getId())
							.addOptions(
								streamJsonArray(higherDepth(getSkyblockItemsJson().get(itemId), "gemstone_slots"))
									.map(e -> higherDepth(e, "slot_type").getAsString())
									.map(e -> SelectOption.of(capitalizeString(e) + " Slot", e))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.queue();
				case "remove_gemstone" -> {
					if (gemstones.isEmpty()) {
						event.editMessageEmbeds(errorEmbed("No gemstones added").build()).queue();
					} else {
						event
							.editMessageEmbeds(
								defaultEmbed("Craft Helper").setDescription("Select a gemstone slot to remove from the menu below").build()
							)
							.setActionRow(
								StringSelectMenu
									.create("craft_command_gemstones_remove_" + message.getId())
									.addOptions(
										gemstones
											.entrySet()
											.stream()
											.map(e ->
												SelectOption.of(capitalizeString(e.getKey()) + " - " + idToName(e.getValue()), e.getKey())
											)
											.collect(Collectors.toCollection(ArrayList::new))
									)
									.build()
							)
							.queue();
					}
				}
			}
		} else if (event.getComponentId().startsWith("craft_command_gemstones_add_")) {
			String gemstoneSlot = event.getSelectedOptions().get(0).getValue();
			event
				.editMessageEmbeds(
					defaultEmbed("Craft Helper")
						.setDescription("Select a gemstone variety for the " + gemstoneSlot.toLowerCase() + " slot from the menu below")
						.build()
				)
				.setActionRow(
					StringSelectMenu
						.create("craft_command_gemstones_variety_add_" + gemstoneSlot + "_" + message.getId())
						.addOptions(
							slotTypeToGemstones
								.get(gemstoneSlot)
								.stream()
								.map(e -> SelectOption.of(capitalizeString(e) + " Variety", e))
								.collect(Collectors.toCollection(ArrayList::new))
						)
						.build()
				)
				.queue();
		} else if (event.getComponentId().startsWith("craft_command_gemstones_variety_add_")) {
			String gemstoneSlot = event.getComponentId().split("craft_command_gemstones_variety_add_")[1].split("_")[0];
			String gemstoneVariety = event.getSelectedOptions().get(0).getValue();
			event
				.editMessageEmbeds(
					defaultEmbed("Craft Helper")
						.setDescription("Select a gemstone tier for the " + gemstoneSlot.toLowerCase() + " slot from the menu below")
						.build()
				)
				.setActionRow(
					StringSelectMenu
						.create("craft_command_gemstones_tier_add_" + gemstoneSlot + "_" + message.getId())
						.addOptions(
							gemstoneTiers
								.stream()
								.map(e -> e + "_" + gemstoneVariety + "_GEM")
								.map(e -> SelectOption.of(idToName(e), e))
								.collect(Collectors.toCollection(ArrayList::new))
						)
						.build()
				)
				.queue();
		} else if (event.getComponentId().startsWith("craft_command_gemstones_tier_add_")) {
			String gemstoneSlot = event.getComponentId().split("craft_command_gemstones_tier_add_")[1].split("_")[0];
			SelectOption gemstone = event.getSelectedOptions().get(0);
			gemstones.put(gemstoneSlot, gemstone.getValue());

			event
				.editMessageEmbeds(
					defaultEmbed("Craft Helper")
						.setDescription("Added " + gemstoneSlot.toLowerCase() + " slot: " + gemstone.getLabel())
						.build()
				)
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_gemstones_remove_")) {
			SelectOption gemstoneSlot = event.getSelectedOptions().get(0);
			gemstones.remove(gemstoneSlot.getValue());

			event.editMessageEmbeds(errorEmbed("Removed gemstone slot: " + gemstoneSlot.getLabel()).build()).queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_drill_upgrades_main_")) {
			SelectOption selectedOption = event.getSelectedOptions().get(0);
			switch (selectedOption.getValue()) {
				case "drill_upgrade_module" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a drill upgrade module from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_drill_upgrades_upgrade_module_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								drillUpgradeModules
									.stream()
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "drill_fuel_tank" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a drill fuel tank from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_drill_upgrades_fuel_tank_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								drillFuelTanks
									.stream()
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
				case "drill_engine" -> event
					.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a drill engine from the menu below").build())
					.setActionRow(
						StringSelectMenu
							.create("craft_command_drill_upgrades_engine_" + message.getId())
							.addOption("None", "none")
							.addOptions(
								drillEngines
									.stream()
									.map(o -> SelectOption.of(idToName(o), o))
									.collect(Collectors.toCollection(ArrayList::new))
							)
							.build()
					)
					.setEphemeral(true)
					.queue();
			}
		} else if (event.getComponentId().startsWith("craft_command_enchants_add_choose_")) {
			String enchant = event.getSelectedOptions().get(0).getValue();
			enchants.add(enchant);

			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Added enchant: " + idToName(enchant)).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_enchants_remove_choose_")) {
			String enchant = event.getSelectedOptions().get(0).getValue();
			enchants.remove(enchant);

			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Removed enchant: " + idToName(enchant)).build())
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_reforge_choose_")) {
			reforge = event.getSelectedOptions().get(0).getValue();

			event.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Reforge: " + reforge).build()).setComponents().queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_rune_")) {
			SelectOption runeValue = event.getSelectedOptions().get(0);
			rune = runeValue.getValue().equals("none") ? null : runeValue.getValue();

			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Rune: " + runeValue.getLabel()).build())
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_necron_blade_scrolls_")) {
			List<SelectOption> selectedScrolls = event.getSelectedOptions();
			for (SelectOption selectedScroll : selectedScrolls) {
				if (selectedScroll.getValue().equals("none")) {
					necronBladeScrolls.clear();
					event
						.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Removed all necron's blade scrolls").build())
						.setComponents()
						.queue();
					updateMainMessage();
					return;
				} else {
					necronBladeScrolls.add(selectedScroll.getValue());
				}
			}

			event
				.editMessageEmbeds(
					defaultEmbed("Craft Helper")
						.setDescription(
							"Added necron's blade scrolls: " +
							necronBladeScrolls.stream().map(StringUtils::idToName).collect(Collectors.joining(", "))
						)
						.build()
				)
				.setComponents()
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_skin_")) {
			SelectOption skinValue = event.getSelectedOptions().get(0);
			skin = skinValue.getValue().equals("none") ? null : skinValue.getValue();

			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Skin: " + skinValue.getLabel()).build())
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_power_scroll_")) {
			SelectOption powerScrollValue = event.getSelectedOptions().get(0);
			powerScroll = powerScrollValue.getValue().equals("none") ? null : powerScrollValue.getValue();

			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Power Scroll: " + powerScrollValue.getLabel()).build())
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_drill_upgrades_upgrade_module_")) {
			SelectOption drillUpgradeModuleValue = event.getSelectedOptions().get(0);
			drillUpgradeModule = drillUpgradeModuleValue.getValue().equals("none") ? null : drillUpgradeModuleValue.getValue();

			event
				.replyEmbeds(
					defaultEmbed("Craft Helper").setDescription("Drill upgrade module: " + drillUpgradeModuleValue.getLabel()).build()
				)
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_drill_upgrades_fuel_tank_")) {
			SelectOption drillFuelTankValue = event.getSelectedOptions().get(0);
			drillFuelTank = drillFuelTankValue.getValue().equals("none") ? null : drillFuelTankValue.getValue();

			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Drill fuel tank: " + drillFuelTankValue.getLabel()).build())
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_drill_upgrades_engine_")) {
			SelectOption drillEngineValue = event.getSelectedOptions().get(0);
			drillEngine = drillEngineValue.getValue().equals("none") ? null : drillEngineValue.getValue();

			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Drill engine: " + drillEngineValue.getLabel()).build())
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_dye_")) {
			SelectOption dyeValue = event.getSelectedOptions().get(0);
			dye = dyeValue.getValue().equals("none") ? null : dyeValue.getValue();

			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Dye: " + dyeValue.getLabel()).build())
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getComponentId().startsWith("craft_command_accessory_enrichments_")) {
			SelectOption accessoryEnrichmentValue = event.getSelectedOptions().get(0);
			accessoryEnrichment = accessoryEnrichmentValue.getValue().equals("none") ? null : accessoryEnrichmentValue.getValue();

			event
				.replyEmbeds(
					defaultEmbed("Craft Helper").setDescription("Accessory enrichment: " + accessoryEnrichmentValue.getLabel()).build()
				)
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		}
	}

	private void onModalEvent(ModalInteractionEvent event) {
		if (event.getModalId().startsWith("craft_command_enchants_add_")) {
			Set<String> validEnchants = new HashSet<>();
			for (JsonElement enchant : higherDepth(getEnchantsJson(), "enchants." + category).getAsJsonArray()) {
				String enchantStr = enchant.getAsString().toUpperCase();

				for (int i = 1; i <= 10; i++) {
					String enchantNameLevel = enchantStr + ";" + i;
					if (higherDepth(getInternalJsonMappings(), enchantNameLevel) != null) {
						validEnchants.add(enchantNameLevel);
					}
				}
			}

			List<String> closestMatches = getClosestMatchesFromIds(event.getValues().get(0).getAsString(), validEnchants, 10);
			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Select an enchant to add from the menu below").build())
				.setActionRow(
					StringSelectMenu
						.create("craft_command_enchants_add_choose_" + message.getId())
						.addOptions(
							closestMatches
								.stream()
								.map(e -> SelectOption.of(idToName(e), e))
								.collect(Collectors.toCollection(ArrayList::new))
						)
						.build()
				)
				.queue();
		} else if (event.getModalId().startsWith("craft_command_enchants_remove_")) {
			List<String> closestMatches = getClosestMatchesFromIds(event.getValues().get(0).getAsString(), enchants, 10);
			event
				.editMessageEmbeds(defaultEmbed("Craft Helper").setDescription("Select an enchant to remove from the menu below").build())
				.setActionRow(
					StringSelectMenu
						.create("craft_command_enchants_remove_choose_" + message.getId())
						.addOptions(
							closestMatches
								.stream()
								.map(e -> SelectOption.of(idToName(e), e))
								.collect(Collectors.toCollection(ArrayList::new))
						)
						.build()
				)
				.queue();
		} else if (event.getModalId().startsWith("craft_command_pbs_")) {
			String hpbValue = event.getValue("hpb").getAsString();
			int hpbValueInt = 0;
			if (!hpbValue.isEmpty()) {
				try {
					hpbValueInt = Integer.parseInt(hpbValue);
				} catch (Exception e) {
					hpbValueInt = -1;
				}

				if (hpbValueInt < 0 || hpbValueInt > 10) {
					event.replyEmbeds(errorEmbed("Hot potato book count must be between 0 and 10").build()).setEphemeral(true).queue();
					return;
				}
			}

			String fpbValue = event.getValue("fpb").getAsString();
			int fpbValueInt = 0;
			if (!fpbValue.isEmpty()) {
				try {
					fpbValueInt = Integer.parseInt(fpbValue);
				} catch (Exception e) {
					fpbValueInt = -1;
				}

				if (fpbValueInt < 0 || fpbValueInt > 5) {
					event.replyEmbeds(errorEmbed("Fuming potato book count must be between 0 and 5").build()).setEphemeral(true).queue();
					return;
				}
			}

			if (fpbValueInt > 0 && hpbValueInt != 10) {
				event
					.replyEmbeds(errorEmbed("Fuming potato books can only be added if hot potato books are maxed out").build())
					.setEphemeral(true)
					.queue();
				return;
			}

			hpbCount = hpbValueInt;
			fpbCount = fpbValueInt;

			event
				.replyEmbeds(
					defaultEmbed("Craft Helper")
						.setDescription("Hot potato book count: " + hpbCount + "\nFuming potato book count: " + fpbCount)
						.build()
				)
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getModalId().startsWith("craft_command_stars_")) {
			String starsValue = event.getValues().get(0).getAsString();
			int starsValueInt = -1;
			try {
				starsValueInt = Integer.parseInt(starsValue);
			} catch (Exception ignored) {}

			JsonElement essenceInfo = higherDepth(getEssenceCostsJson(), itemId);

			int maxStars = 5;
			for (int i = 15; i >= 1; i--) {
				if (higherDepth(essenceInfo, "" + i) != null) {
					maxStars = i;
					break;
				}
			}

			if (higherDepth(getSkyblockItemsJson().get(itemId), "dungeon_item", false)) {
				maxStars += 5;
			}

			if (starsValueInt < 0 || starsValueInt > maxStars) {
				event.replyEmbeds(errorEmbed("Stars must be between 0 and " + maxStars).build()).setEphemeral(true).queue();
				return;
			}

			stars = starsValueInt;

			event.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Stars: " + stars).build()).setEphemeral(true).queue();
			updateMainMessage();
		} else if (event.getModalId().startsWith("craft_command_reforge_")) {
			List<String> closestMatches = getClosestMatches(
				event.getValues().get(0).getAsString().toLowerCase(),
				CATEGORY_TO_REFORGES.get(category),
				10
			);
			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Select a reforge from the menu below").build())
				.setActionRow(
					StringSelectMenu
						.create("craft_command_reforge_choose_" + message.getId())
						.addOptions(
							closestMatches.stream().map(e -> SelectOption.of(e, e)).collect(Collectors.toCollection(ArrayList::new))
						)
						.build()
				)
				.setEphemeral(true)
				.queue();
		} else if (event.getModalId().startsWith("craft_command_mana_disintegrator_")) {
			String manaDisintegratorValue = event.getValues().get(0).getAsString();
			int manaDisintegratorValueInt = 0;
			if (!manaDisintegratorValue.isEmpty()) {
				try {
					manaDisintegratorValueInt = Integer.parseInt(manaDisintegratorValue);
				} catch (Exception e) {
					manaDisintegratorValueInt = -1;
				}

				if (manaDisintegratorValueInt < 0 || manaDisintegratorValueInt > 10) {
					event.replyEmbeds(errorEmbed("Mana disintegrator count must be between 0 and 10").build()).setEphemeral(true).queue();
					return;
				}
			}
			manaDisintegratorCount = manaDisintegratorValueInt;

			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Mana disintegrator count: " + manaDisintegratorCount).build())
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		} else if (event.getModalId().startsWith("craft_command_ffd_")) {
			String ffdValue = event.getValues().get(0).getAsString();
			int ffdValueInt = 0;
			if (!ffdValue.isEmpty()) {
				try {
					ffdValueInt = Integer.parseInt(ffdValue);
				} catch (Exception e) {
					ffdValueInt = -1;
				}

				if (ffdValueInt < 0 || ffdValueInt > 5) {
					event.replyEmbeds(errorEmbed("Farming for dummies count must be between 0 and 5").build()).setEphemeral(true).queue();
					return;
				}
			}
			ffdCount = ffdValueInt;

			event
				.replyEmbeds(defaultEmbed("Craft Helper").setDescription("Farming for dummies count: " + ffdCount).build())
				.setEphemeral(true)
				.queue();
			updateMainMessage();
		}
	}

	private void updateMainMessage() {
		EmbedBuilder eb = defaultEmbed("Craft Helper For " + idToName(itemId)).setThumbnail(getItemThumbnail(itemId));
		double totalPrice = 0;

		double basePrice = calculator.getLowestPrice(itemId);
		totalPrice += basePrice;
		eb.appendDescription("\n" + getEmoji(itemId) + " Base Price: " + roundAndFormat(basePrice));

		if (recombobulated) {
			double recombobulatorPrice = calculator.getLowestPrice("RECOMBOBULATOR_3000");
			totalPrice += recombobulatorPrice;
			eb.appendDescription("\n" + getEmoji("RECOMBOBULATOR_3000") + " Recombobulator: " + roundAndFormat(recombobulatorPrice));
		}

		if (!enchants.isEmpty()) {
			for (String enchant : enchants) {
				double enchantPrice = calculator.getLowestPriceEnchant(enchant);
				totalPrice += enchantPrice;
				eb.appendDescription("\n" + getEmoji(enchant) + " " + idToName(enchant) + ": " + roundAndFormat(enchantPrice));
			}
		}

		if (hpbCount > 0) {
			double hpbPrice = hpbCount * calculator.getLowestPrice("HOT_POTATO_BOOK");
			totalPrice += hpbPrice;
			eb.appendDescription("\n" + getEmoji("HOT_POTATO_BOOK") + " Hot Potato Book (" + hpbCount + "): " + roundAndFormat(hpbPrice));
		}
		if (fpbCount > 0) {
			double fpbPrice = fpbCount * calculator.getLowestPrice("FUMING_POTATO_BOOK");
			totalPrice += fpbPrice;
			eb.appendDescription(
				"\n" + getEmoji("FUMING_POTATO_BOOK") + " Fuming Potato Book (" + fpbCount + "): " + roundAndFormat(fpbPrice)
			);
		}

		if (stars > 0) {
			JsonElement essenceInfo = higherDepth(getEssenceCostsJson(), itemId);
			String essenceType = higherDepth(essenceInfo, "type").getAsString();
			int essenceCount = 0;
			int masterStarCount = 0;
			Map<String, Integer> extraItems = new HashMap<>();

			for (int i = 0; i <= stars; i++) {
				essenceCount += higherDepth(essenceInfo, "" + i, 0);

				JsonElement items = higherDepth(essenceInfo, "items." + i);
				if (items != null) {
					for (JsonElement item : items.getAsJsonArray()) {
						String[] itemSplit = item.getAsString().split(":");
						extraItems.compute(itemSplit[0], (k, v) -> (v != null ? v : 0) + Integer.parseInt(itemSplit[1]));
					}
				}

				if (i > 5 && !essenceType.equals("Crimson")) {
					masterStarCount++;
				}
			}

			String essenceTypeFormatted = "ESSENCE_" + essenceType.toUpperCase();
			double essencePrice = essenceCount * calculator.getLowestPrice(essenceTypeFormatted);
			totalPrice += essencePrice;
			StringBuilder ebStr = new StringBuilder(
				getEmoji(essenceTypeFormatted) +
				" " +
				idToName(essenceTypeFormatted) +
				" (" +
				formatNumber(essenceCount) +
				"): " +
				roundAndFormat(essencePrice)
			);

			if (masterStarCount > 0) {
				double masterStarCost = 0;
				for (int i = 1; i <= masterStarCount; i++) {
					masterStarCost +=
						calculator.getLowestPrice(
							switch (i) {
								case 5 -> "FIFTH_MASTER_STAR";
								case 4 -> "FOURTH_MASTER_STAR";
								case 3 -> "THIRD_MASTER_STAR";
								case 2 -> "SECOND_MASTER_STAR";
								case 1 -> "FIRST_MASTER_STAR";
								default -> throw new IllegalStateException("Unexpected value: " + i);
							}
						);
				}
				ebStr
					.append("\n")
					.append(getEmoji("FIRST_MASTER_STAR"))
					.append(" Master Stars (")
					.append(masterStarCount)
					.append("): ")
					.append(formatNumber(masterStarCost));
			}

			for (Map.Entry<String, Integer> extraItem : extraItems.entrySet()) {
				double extraItemPrice = extraItem.getValue() * calculator.getLowestPrice(extraItem.getKey());
				totalPrice += extraItemPrice;
				ebStr
					.append("\n")
					.append(getEmoji(extraItem.getKey()))
					.append(" ")
					.append(idToName(extraItem.getKey()))
					.append(" (")
					.append(extraItem.getValue())
					.append("): ")
					.append(roundAndFormat(totalPrice));
			}

			eb.addField("Stars (" + stars + ")", ebStr.toString(), false);
		}

		if (!gemstones.isEmpty()) {
			StringBuilder ebStr = new StringBuilder();
			Map<String, JsonElement> slotTypeToJson = streamJsonArray(higherDepth(getSkyblockItemsJson().get(itemId), "gemstone_slots"))
				.collect(Collectors.toMap(e -> higherDepth(e, "slot_type").getAsString(), e -> e));

			for (Map.Entry<String, String> gemstone : gemstones.entrySet()) {
				JsonElement gemstoneSlotJson = slotTypeToJson.get(gemstone.getKey());
				if (higherDepth(gemstoneSlotJson, "costs") != null) {
					double gemstoneSlotUnlockPrice = 0;
					for (JsonElement cost : higherDepth(gemstoneSlotJson, "costs").getAsJsonArray()) {
						boolean costIsCoins = higherDepth(cost, "type").getAsString().equals("COINS");
						String costItemId = costIsCoins ? "SKYBLOCK_COIN" : higherDepth(cost, "item_id").getAsString();
						int costCount = (costIsCoins ? higherDepth(cost, "coins") : higherDepth(cost, "amount")).getAsInt();
						gemstoneSlotUnlockPrice += calculator.getLowestPrice(costItemId) * costCount;
					}
					totalPrice += gemstoneSlotUnlockPrice;
					ebStr
						.append("\n")
						.append(getEmoji("GEMSTONE_MIXTURE"))
						.append(" ")
						.append(capitalizeString(gemstone.getKey()))
						.append(" Slot Cost: ")
						.append(roundAndFormat(gemstoneSlotUnlockPrice));
				}

				double gemstonePrice = calculator.getLowestPrice(gemstone.getValue());
				totalPrice += gemstonePrice;
				ebStr
					.append("\n")
					.append(getEmoji(gemstone.getValue()))
					.append(" ")
					.append(idToName(gemstone.getValue()))
					.append(": ")
					.append(roundAndFormat(gemstonePrice));
			}

			eb.addField("Gemstones (" + gemstones.size() + ")", ebStr.toString(), false);
		}

		if (reforge != null) {
			String baseRarity = higherDepth(getInternalJsonMappings(), itemId + ".base_rarity").getAsString();
			if (recombobulated) {
				baseRarity = NUMBER_TO_RARITY_MAP.get("" + (Integer.parseInt(RARITY_TO_NUMBER_MAP.get(baseRarity).replace(";", "")) + 1));
			}

			Map.Entry<String, JsonElement> reforgeStone = getReforgeStonesJson()
				.entrySet()
				.stream()
				.filter(e -> reforge.equalsIgnoreCase(higherDepth(e.getValue(), "reforgeName").getAsString()))
				.findFirst()
				.get();

			double reforgePrice = calculator.getLowestPriceModifier(reforge, baseRarity);
			totalPrice += reforgePrice;
			eb.appendDescription(
				"\n" + getEmoji(reforgeStone.getKey()) + " " + capitalizeString(reforge) + ": " + roundAndFormat(reforgePrice)
			);
		}

		if (rune != null) {
			double runePrice = calculator.getLowestPrice(rune);
			totalPrice += runePrice;
			eb.appendDescription("\n" + getEmoji(rune) + " " + idToName(rune) + ": " + roundAndFormat(runePrice));
		}

		if (drillUpgradeModule != null) {
			double drillUpgradeModulePrice = calculator.getLowestPrice(drillUpgradeModule);
			totalPrice += drillUpgradeModulePrice;
			eb.appendDescription(
				"\n" + getEmoji(drillUpgradeModule) + " " + idToName(drillUpgradeModule) + ": " + roundAndFormat(drillUpgradeModulePrice)
			);
		}

		if (drillFuelTank != null) {
			double drillFuelTankPrice = calculator.getLowestPrice(drillFuelTank);
			totalPrice += drillFuelTankPrice;
			eb.appendDescription(
				"\n" + getEmoji(drillFuelTank) + " " + idToName(drillFuelTank) + ": " + roundAndFormat(drillFuelTankPrice)
			);
		}

		if (drillEngine != null) {
			double drillEnginePrice = calculator.getLowestPrice(drillEngine);
			totalPrice += drillEnginePrice;
			eb.appendDescription("\n" + getEmoji(drillEngine) + " " + idToName(drillEngine) + ": " + roundAndFormat(drillEnginePrice));
		}

		if (dye != null) {
			double dyePrice = calculator.getLowestPrice(dye);
			totalPrice += dyePrice;
			eb.appendDescription("\n" + getEmoji(dye) + " " + idToName(dye) + ": " + roundAndFormat(dyePrice));
		}

		if (accessoryEnrichment != null) {
			double accessoryEnrichmentPrice = calculator.getLowestPrice(accessoryEnrichment);
			totalPrice += accessoryEnrichmentPrice;
			eb.appendDescription(
				"\n" + getEmoji(accessoryEnrichment) + " " + idToName(accessoryEnrichment) + ": " + roundAndFormat(accessoryEnrichmentPrice)
			);
		}

		if (manaDisintegratorCount > 0) {
			double manaDisintegratorPrice = manaDisintegratorCount * calculator.getLowestPrice("MANA_DISINTEGRATOR");
			totalPrice += manaDisintegratorPrice;
			eb.appendDescription(
				"\n" +
				getEmoji("MANA_DISINTEGRATOR") +
				" Mana Disintegrator (" +
				manaDisintegratorCount +
				"): " +
				roundAndFormat(manaDisintegratorPrice)
			);
		}

		if (ffdCount > 0) {
			double ffdPrice = ffdCount * calculator.getLowestPrice("FARMING_FOR_DUMMIES");
			totalPrice += ffdPrice;
			eb.appendDescription(
				"\n" + getEmoji("FARMING_FOR_DUMMIES") + " Farming For Dummies (" + ffdCount + "): " + roundAndFormat(ffdPrice)
			);
		}

		if (!necronBladeScrolls.isEmpty()) {
			StringBuilder ebStr = new StringBuilder();
			for (String necronBladeScroll : necronBladeScrolls) {
				double necronBladeScrollPrice = calculator.getLowestPrice(necronBladeScroll);
				totalPrice += necronBladeScrollPrice;
				ebStr
					.append("\n")
					.append(getEmoji(necronBladeScroll))
					.append(" ")
					.append(idToName(necronBladeScroll))
					.append(": ")
					.append(roundAndFormat(necronBladeScrollPrice));
			}
			eb.addField("Necron's Blade Scrolls", ebStr.toString(), false);
		}

		if (skin != null) {
			double skinPrice = calculator.getLowestPrice(skin);
			totalPrice += skinPrice;
			eb.appendDescription("\n" + getEmoji(skin) + " " + idToName(skin) + ": " + roundAndFormat(skinPrice));
		}

		if (powerScroll != null) {
			double powerScrollPrice = calculator.getLowestPrice(powerScroll);
			totalPrice += powerScrollPrice;
			eb.appendDescription("\n" + getEmoji(powerScroll) + " " + idToName(powerScroll) + ": " + roundAndFormat(powerScrollPrice));
		}

		if (woodSingularityApplied) {
			double woodSingularityPrice = calculator.getLowestPrice("WOOD_SINGULARITY");
			totalPrice += woodSingularityPrice;
			eb.appendDescription("\n" + getEmoji("WOOD_SINGULARITY") + " Wood Singularity: " + roundAndFormat(woodSingularityPrice));
		}

		if (artOfWarApplied) {
			double artOfWarPrice = calculator.getLowestPrice("THE_ART_OF_WAR");
			totalPrice += artOfWarPrice;
			eb.appendDescription("\n" + getEmoji("THE_ART_OF_WAR") + " Art Of War: " + roundAndFormat(artOfWarPrice));
		}

		if (artOfPeaceApplied) {
			double artOfPeacePrice = calculator.getLowestPrice("THE_ART_OF_PEACE");
			totalPrice += artOfPeacePrice;
			eb.appendDescription("\n" + getEmoji("THE_ART_OF_PEACE") + " Art Of Peace: " + roundAndFormat(artOfPeacePrice));
		}

		eb.getDescriptionBuilder().insert(0, getEmoji("ENCHANTED_GOLD") + " **Total Price:** " + roundAndFormat(totalPrice) + "\n");
		message.editMessageEmbeds(eb.build()).queue();
	}

	private void waitForEvent() {
		waiter.waitForEvent(
			GenericInteractionCreateEvent.class,
			this::condition,
			this::action,
			1,
			TimeUnit.MINUTES,
			() -> message.editMessageComponents().queue(ignore, ignore)
		);
	}
}
