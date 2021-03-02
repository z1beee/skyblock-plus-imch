package com.skyblockplus.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.skyblockplus.skills.SkillsStruct;
import com.skyblockplus.weight.Weight;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.skyblockplus.utils.Utils.*;

public class Player {
    public String invMissing = "";
    private boolean validPlayer = false;
    private JsonElement profileJson;
    private JsonElement levelTables;
    private JsonElement outerProfileJson;
    private JsonElement petLevelTable;
    private JsonElement hypixelProfileJson;
    private String playerUuid;
    private String playerUsername;
    private String profileName;
    private String playerGuildRank;

    public Player(String username) {
        if (usernameToUuid(username)) {
            return;
        }

        try {
            JsonArray profileArray = higherDepth(
                    getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid),
                    "profiles").getAsJsonArray();

            if (getLatestProfile(profileArray)) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        this.validPlayer = true;
    }

    public Player(String playerUuid, JsonElement levelTables, String playerGuildRank) {
        this.playerUuid = playerUuid;
        this.playerUsername = uuidToUsername(playerUuid);
        this.levelTables = levelTables;
        this.playerGuildRank = playerGuildRank;

        try {
            JsonArray profileArray = higherDepth(
                    getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid),
                    "profiles").getAsJsonArray();

            if (getLatestProfile(profileArray)) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        this.validPlayer = true;
    }

    public Player(String username, String profileName) {
        if (usernameToUuid(username)) {
            return;
        }

        try {

            JsonArray profileArray = higherDepth(
                    getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid),
                    "profiles").getAsJsonArray();
            if (!profileIdFromName(profileName, profileArray)) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        this.validPlayer = true;
    }

    public boolean usernameToUuid(String username) {
        try {
            JsonElement usernameJson = getJson("https://api.mojang.com/users/profiles/minecraft/" + username);
            this.playerUsername = higherDepth(usernameJson, "name").getAsString();
            this.playerUuid = higherDepth(usernameJson, "id").getAsString();
            return false;
        } catch (Exception ignored) {
        }
        return true;

    }

    public boolean isValid() {
        return validPlayer;
    }

    public int getSlayer() {
        return getWolfXp() + getZombieXp() + getSpiderXp();
    }

    public int getSlayer(String slayerName) {
        switch (slayerName) {
            case "sven":
                return getWolfXp();
            case "rev":
                return getZombieXp();
            case "tara":
                return getSpiderXp();
        }
        return -1;
    }

    public int getWolfXp() {
        JsonElement profileSlayer = higherDepth(profileJson, "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "wolf"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "wolf"), "xp").getAsInt()
                : 0;
    }

    public int getZombieXp() {
        JsonElement profileSlayer = higherDepth(profileJson, "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "zombie"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "zombie"), "xp").getAsInt()
                : 0;
    }

    public int getSpiderXp() {
        JsonElement profileSlayer = higherDepth(profileJson, "slayer_bosses");

        return higherDepth(higherDepth(profileSlayer, "spider"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "spider"), "xp").getAsInt()
                : 0;
    }

    public double getCatacombsLevel() {
        SkillsStruct catacombsInfo = getCatacombsSkill();
        if (catacombsInfo != null) {
            return catacombsInfo.skillLevel + catacombsInfo.progressToNext;
        }
        return 0;
    }

    public SkillsStruct getCatacombsSkill() {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        try {
            double skillExp = higherDepth(
                    higherDepth(higherDepth(higherDepth(profileJson, "dungeons"), "dungeon_types"), "catacombs"),
                    "experience").getAsDouble();
            return skillInfoFromExp(skillExp, "catacombs");
        } catch (Exception e) {
            return null;
        }

    }

    public SkillsStruct getSkill(String skillName) {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        try {
            double skillExp = higherDepth(profileJson, "experience_skill_" + skillName).getAsDouble();
            return skillInfoFromExp(skillExp, skillName);
        } catch (Exception ignored) {
        }
        return null;
    }

    public double getSkillAverage() {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        JsonElement skillsCap = higherDepth(levelTables, "leveling_caps");

        List<String> skills = getJsonKeys(skillsCap);
        skills.remove("catacombs");
        skills.remove("runecrafting");
        skills.remove("carpentry");

        double progressSA = 0;
        for (String skill : skills) {
            try {
                double skillExp = higherDepth(profileJson, "experience_skill_" + skill).getAsDouble();
                SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill);
                progressSA += skillInfo.skillLevel + skillInfo.progressToNext;
            } catch (Exception ignored) {
            }
        }
        progressSA /= skills.size();
        return progressSA;
    }

    public SkillsStruct skillInfoFromExp(double skillExp, String skill) {
        JsonElement skillsCap = higherDepth(levelTables, "leveling_caps");

        JsonArray skillsTable;
        if (skill.equals("catacombs")) {
            skillsTable = higherDepth(levelTables, "catacombs").getAsJsonArray();
        } else if (skill.equals("runecrafting")) {
            skillsTable = higherDepth(levelTables, "runecrafting_xp").getAsJsonArray();
        } else {
            skillsTable = higherDepth(levelTables, "leveling_xp").getAsJsonArray();
        }
        int maxLevel;
        try {
            maxLevel = higherDepth(skillsCap, skill).getAsInt();
        } catch (Exception e) {
            maxLevel = 50;
        }

        if (skill.equals("farming")) {
            maxLevel += getFarmingCapUpgrade();
        }

        long xpTotal = 0L;
        int level = 1;
        for (int i = 0; i < maxLevel; i++) {
            xpTotal += skillsTable.get(i).getAsLong();

            if (xpTotal > skillExp) {
                xpTotal -= skillsTable.get(i).getAsLong();
                break;
            } else {
                level = (i + 1);
            }
        }

        long xpCurrent = (long) Math.floor(skillExp - xpTotal);
        long xpForNext = 0;
        if (level < maxLevel)
            xpForNext = (long) Math.ceil(skillsTable.get(level).getAsLong());

        double progress = xpForNext > 0 ? Math.max(0, Math.min(((double) xpCurrent) / xpForNext, 1)) : 0;

        return new SkillsStruct(skill, level, maxLevel, (long) skillExp, xpCurrent, xpForNext, progress);
    }

    public double getBankBalance() {
        try {
            return higherDepth(higherDepth(outerProfileJson, "banking"), "balance").getAsDouble();
        } catch (Exception e) {
            return -1;
        }
    }

    public int getFairySouls() {
        try {
            return higherDepth(profileJson, "fairy_souls_collected").getAsInt();
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean getLatestProfile(JsonArray profilesArray) {
        try {
            String lastProfileSave = "";
            for (int i = 0; i < profilesArray.size(); i++) {
                String lastSaveLoop;
                try {
                     lastSaveLoop = higherDepth(
                            higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid), "last_save")
                            .getAsString();
                } catch (Exception e){
                    continue;
                }

                if (i == 0) {
                    this.profileJson = higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid);
                    this.outerProfileJson = profilesArray.get(i);
                    lastProfileSave = lastSaveLoop;
                    this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                } else if (Instant.ofEpochMilli(Long.parseLong(lastSaveLoop))
                        .isAfter(Instant.ofEpochMilli(Long.parseLong(lastProfileSave)))) {
                    this.profileJson = higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid);
                    this.outerProfileJson = profilesArray.get(i);
                    lastProfileSave = lastSaveLoop;
                    this.profileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                }
            }
            return false;
        } catch (Exception ignored) {
        }
        return true;
    }

    public boolean profileIdFromName(String profileName, JsonArray profilesArray) {
        try {
            for (int i = 0; i < profilesArray.size(); i++) {
                String currentProfileName = higherDepth(profilesArray.get(i), "cute_name").getAsString();
                if (currentProfileName.equalsIgnoreCase(profileName)) {
                    this.profileName = currentProfileName;
                    this.outerProfileJson = profilesArray.get(i);
                    this.profileJson = higherDepth(higherDepth(profilesArray.get(i), "members"), this.playerUuid);
                    return true;
                }
            }

        } catch (Exception ignored) {
        }
        return false;
    }

    public String getUsername() {
        return this.playerUsername;
    }

    public String getProfileName() {
        return this.profileName;
    }

    public String getUuid() {
        return this.playerUuid;
    }

    public String getGuildRank() {
        return playerGuildRank;
    }

    public JsonArray getPets() {
        return higherDepth(profileJson, "pets").getAsJsonArray();
    }

    public int petLevelFromXp(long petExp, String rarity) {
        if (this.petLevelTable == null) {
            this.petLevelTable = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/pets.json");
        }

        int petRarityOffset = higherDepth(higherDepth(petLevelTable, "pet_rarity_offset"), rarity.toUpperCase())
                .getAsInt();
        JsonArray petLevelsXpPer = higherDepth(petLevelTable, "pet_levels").getAsJsonArray();
        long totalExp = 0;
        for (int i = petRarityOffset; i < petLevelsXpPer.size(); i++) {
            totalExp += petLevelsXpPer.get(i).getAsLong();
            if (totalExp >= petExp) {
                return (i - petRarityOffset + 1);
            }
        }
        return 100;
    }

    public int getNumberMinionSlots() {
        try {
            int[] craftedMinionsToSlots = new int[] { 0, 5, 15, 30, 50, 75, 100, 125, 150, 175, 200, 225, 250, 275, 300,
                    350, 400, 450, 500, 550, 600 };

            int prevMax = 0;
            int craftedMinions = higherDepth(profileJson, "crafted_generators").getAsJsonArray().size();
            for (int i = 0; i < craftedMinionsToSlots.length; i++) {
                if (craftedMinions >= craftedMinionsToSlots[i]) {
                    prevMax = i;
                } else {
                    break;
                }
            }

            return (prevMax + 5);
        } catch (Exception e) {
            return 0;
        }
    }

    public double getPurseCoins() {
        try {
            return higherDepth(profileJson, "coin_purse").getAsLong();
        } catch (Exception e) {
            return -1;
        }
    }

    public int getSlayerLevel(String slayerName) {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        switch (slayerName) {
            case "sven":
                JsonArray wolfLevelArray = higherDepth(higherDepth(levelTables, "slayer_xp"), "wolf").getAsJsonArray();
                int wolfXp = getWolfXp();
                int prevWolfLevel = 0;
                for (int i = 0; i < wolfLevelArray.size(); i++) {
                    if (wolfXp >= wolfLevelArray.get(i).getAsInt()) {
                        prevWolfLevel = i;
                    } else {
                        break;
                    }
                }
                return (prevWolfLevel + 1);
            case "rev":
                JsonArray zombieLevelArray = higherDepth(higherDepth(levelTables, "slayer_xp"), "zombie")
                        .getAsJsonArray();
                int zombieXp = getZombieXp();
                int prevZombieMax = 0;
                for (int i = 0; i < zombieLevelArray.size(); i++) {
                    if (zombieXp >= zombieLevelArray.get(i).getAsInt()) {
                        prevZombieMax = i;
                    } else {
                        break;
                    }
                }
                return (prevZombieMax + 1);
            case "tara":
                JsonArray spiderLevelArray = higherDepth(higherDepth(levelTables, "slayer_xp"), "spider")
                        .getAsJsonArray();
                int spiderXp = getSpiderXp();
                int prevSpiderMax = 0;
                for (int i = 0; i < spiderLevelArray.size(); i++) {
                    if (spiderXp >= spiderLevelArray.get(i).getAsInt()) {
                        prevSpiderMax = i;
                    } else {
                        break;
                    }
                }
                return (prevSpiderMax + 1);
        }
        return 0;
    }

    public JsonElement getProfileJson() {
        return profileJson;
    }

    public Map<Integer, ArmorStruct> getWardrobe() {
        try {
            String encodedWardrobeContents = higherDepth(higherDepth(profileJson, "wardrobe_contents"), "data")
                    .getAsString();
            int equippedSlot = higherDepth(profileJson, "wardrobe_equipped_slot").getAsInt();
            NBTCompound decodedWardrobeContents = NBTReader.readBase64(encodedWardrobeContents);

            NBTList wardrobeFrames = decodedWardrobeContents.getList(".i");
            Map<Integer, String> wardrobeFramesMap = new HashMap<>();
            for (int i = 0; i < wardrobeFrames.size(); i++) {
                NBTCompound displayName = wardrobeFrames.getCompound(i).getCompound("tag.display");
                if (displayName != null) {
                    wardrobeFramesMap.put(i,
                            displayName.getString("Name", "Empty").replaceAll("§f|§a|§9|§5|§6|§d|§4|§c|§7", ""));
                } else {
                    wardrobeFramesMap.put(i, "Empty");
                }
            }

            Map<Integer, ArmorStruct> armorStructMap = new HashMap<>(18);
            for (int i = 0; i < 9; i++) {
                ArmorStruct pageOneStruct = new ArmorStruct();
                for (int j = i; j < wardrobeFramesMap.size() / 2; j += 9) {
                    String currentArmorPiece = wardrobeFramesMap.get(j);
                    if ((j - i) / 9 == 0) {
                        pageOneStruct.setHelmet(currentArmorPiece);
                    } else if ((j - i) / 9 == 1) {
                        pageOneStruct.setChestplate(currentArmorPiece);
                    } else if ((j - i) / 9 == 2) {
                        pageOneStruct.setLeggings(currentArmorPiece);
                    } else if ((j - i) / 9 == 3) {
                        pageOneStruct.setBoots(currentArmorPiece);
                    }
                }
                armorStructMap.put(i, pageOneStruct);

                ArmorStruct pageTwoStruct = new ArmorStruct();
                for (int j = (wardrobeFramesMap.size() / 2) + i; j < wardrobeFramesMap.size(); j += 9) {
                    String currentArmorPiece = wardrobeFramesMap.get(j);
                    if ((j - i) / 9 == 4) {
                        pageTwoStruct.setHelmet(currentArmorPiece);
                    } else if ((j - i) / 9 == 5) {
                        pageTwoStruct.setChestplate(currentArmorPiece);
                    } else if ((j - i) / 9 == 6) {
                        pageTwoStruct.setLeggings(currentArmorPiece);
                    } else if ((j - i) / 9 == 7) {
                        pageTwoStruct.setBoots(currentArmorPiece);
                    }
                }
                armorStructMap.put(i + 9, pageTwoStruct);
            }
            if (equippedSlot > 0) {
                armorStructMap.replace((equippedSlot - 1), getInventoryArmor().makeBold());
            }

            return armorStructMap;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<Integer, String> getTalismanBag() {
        try {
            String encodedTalismanContents = higherDepth(higherDepth(profileJson, "talisman_bag"), "data")
                    .getAsString();
            NBTCompound decodedTalismanContents = NBTReader.readBase64(encodedTalismanContents);

            NBTList talismanFrames = decodedTalismanContents.getList(".i");
            Map<Integer, String> talismanFramesMap = new HashMap<>();
            for (int i = 0; i < talismanFrames.size(); i++) {
                NBTCompound displayName = talismanFrames.getCompound(i).getCompound("tag.display");
                if (displayName != null) {
                    talismanFramesMap.put(i,
                            displayName.getString("Name", "Empty").replaceAll("§f|§a|§9|§5|§6|§d|§4|§c|", ""));
                } else {
                    talismanFramesMap.put(i, "Empty");
                }
            }

            return talismanFramesMap;

        } catch (Exception e) {
            return null;
        }
    }

    public ArmorStruct getInventoryArmor() {
        try {
            String encodedInventoryContents = higherDepth(higherDepth(profileJson, "inv_armor"), "data").getAsString();
            NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

            NBTList talismanFrames = decodedInventoryContents.getList(".i");

            Map<Integer, String> armorFramesMap = new HashMap<>();
            for (int i = 0; i < talismanFrames.size(); i++) {
                NBTCompound displayName = talismanFrames.getCompound(i).getCompound("tag.display");
                if (displayName != null) {
                    armorFramesMap.put(i,
                            displayName.getString("Name", "Empty").replaceAll("§f|§a|§9|§5|§6|§d|§4|§c|", ""));
                } else {
                    armorFramesMap.put(i, "Empty");
                }
            }
            return new ArmorStruct(armorFramesMap.get(3), armorFramesMap.get(2), armorFramesMap.get(1),
                    armorFramesMap.get(0));

        } catch (Exception e) {
            return null;
        }
    }

    public String[] getInventory() {
        try {
            String encodedInventoryContents = higherDepth(higherDepth(profileJson, "inv_contents"), "data")
                    .getAsString();
            NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

            NBTList invFrames = decodedInventoryContents.getList(".i");
            Map<Integer, String> invFramesMap = new TreeMap<>();
            for (int i = 0; i < invFrames.size(); i++) {
                NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
                if (displayName != null) {
                    invFramesMap.put(i + 1, displayName.getString("id", "empty").toLowerCase());
                } else {
                    invFramesMap.put(i + 1, "empty");
                }
            }

            StringBuilder outputStringPart1 = new StringBuilder();
            StringBuilder outputStringPart2 = new StringBuilder();
            StringBuilder curNine = new StringBuilder();
            for (Map.Entry<Integer, String> i : invFramesMap.entrySet()) {
                // System.out.println(i.getKey() + " - " + i.getValue());
                if (i.getKey() <= invFrames.size() / 2) {
                    curNine.append(itemToEmoji(i.getValue()));
                    if (i.getKey() % 9 == 0) {
                        outputStringPart1.insert(0, curNine + "\n");
                        curNine = new StringBuilder();
                    }
                } else {
                    curNine.append(itemToEmoji(i.getValue()));
                    if (i.getKey() % 9 == 0) {
                        outputStringPart2.insert(0, curNine + "\n");
                        curNine = new StringBuilder();
                    }
                }
            }
            return new String[] { outputStringPart2.toString(), outputStringPart1.toString() };

        } catch (Exception ignored) {
        }
        return null;
    }

    public String itemToEmoji(String itemName) {
        Map<String, String> emojiMap = new HashMap<>();
        emojiMap.put("empty", "<:empty:814669776201711637>");
        emojiMap.put("hyperion", "<:hyperion:814675220455096321>");
        emojiMap.put("rogue_sword", "<:rogue_sword:814675777479114803>");
        emojiMap.put("grappling_hook", "<:grappling_hook:814676007118176257>");
        emojiMap.put("runaans_bow", "<:runaans_bow:814676456897511424>");
        emojiMap.put("overflux_power_orb", "<:overflux_power_orb:814676605514678292>");
        emojiMap.put("superboom_tnt", "<:superboom_tnt:814690915318235146>");
        emojiMap.put("spirit_leap", "<:spirit_leap:814677057107263508>");
        emojiMap.put("skyblock_menu", "<:skyblock_menu:814676947602374698>");
        emojiMap.put("greater_backpack", "<:greater_backpack:814679082667081769>");
        emojiMap.put("dungeon_stone", "<:dungeon_stone:814680424994570291>");
        emojiMap.put("defuse_kit", "<:defuse_kit:814680645724012597>");
        emojiMap.put("rabbit_hat", "<:rabbit_hat:814680929117011988>");
        emojiMap.put("jerry_staff", "<:jerry_staff:814681305488818197>");
        emojiMap.put("flower_of_truth", "<:flower_of_truth:814687420413902849>");
        emojiMap.put("bone_boomerang", "<:bone_boomerang:814687704104435732>");
        emojiMap.put("snow_block", "<:snow_block:814690652569993248>");
        emojiMap.put("auger_rod", "<:auger_rod:814688099044687872>");
        emojiMap.put("death_bow", "<:death_bow:814688302707769354>");
        emojiMap.put("diver_fragment", "<:diver_fragment:814688560816324639>");
        emojiMap.put("blue_ice_hunk", "<:blue_ice_hunk:814688991769133087>");
        emojiMap.put("aspect_of_the_end", "<:aspect_of_the_end:814689179110735902>");
        emojiMap.put("enchanted_book", "<:enchanted_book:814689302960930826>");
        emojiMap.put("ice_hunk", "<:ice_hunk:814689461307703363>");
        emojiMap.put("golden_apple", "<:golden_apple:814689788359082004>");
        emojiMap.put("stonk_pickaxe", "<:stonk_pickaxe:814689918311596044>");
        emojiMap.put("white_gift", "<:white_gift:814690119591919696>");
        emojiMap.put("item_spirit_bow", "<:item_spirit_bow:815283134416551997>");
        emojiMap.put("ice_spray_wand", "<:ice_spray_wand:815283295896993813>");
        emojiMap.put("jumbo_backpack", "<:jumbo_backpack:815283480476254219>");
        emojiMap.put("kismet_feather", "<:kismet_feather:815284259517759508>");
        emojiMap.put("wither_cloak", "<:wither_cloak:815283770189021204>");
        emojiMap.put("dungeon_chest_key", "<:dungeon_chest_key:815284007767769100>");
        emojiMap.put("florid_zombie_sword", "<:florid_zombie_sword:815284420567236649>");
        emojiMap.put("medium_backpack", "<:medium_backpack:815284582353731635>");
        emojiMap.put("phantom_rod", "<:phantom_rod:815284719868182568>");
        emojiMap.put("fel_pearl", "<:fel_pearl:815286137576488980>");
        emojiMap.put("holy_fragment", "<:holy_fragment:815285660214493184>");
        emojiMap.put("unstable_fragment", "<:unstable_fragment:815285660264038400>");
        emojiMap.put("young_fragment", "<:young_fragment:815285660230352897>");
        emojiMap.put("enchanted_bone", "<:enchanted_bone:815286708764934214>");
        emojiMap.put("enchanted_rotten_flesh", "<:enchanted_rotten_flesh:815287217316560896>");
        emojiMap.put("training_weights", "<:training_weights:815287498560503820>");
        emojiMap.put("beastmaster_crest_rare", "<:beastmaster_crest_rare:815288186170245140>");
        emojiMap.put("enchanted_ice", "<:enchanted_ice:815288002649522217>");
        emojiMap.put("zombie_knight_helmet", "<:zombie_knight_helmet:815300154311049236>");
        emojiMap.put("earth_shard", "<:earth_shard:815300345345474563>");
        emojiMap.put("pumpkin_dicer", "<:pumpkin_dicer:815300807980220436>");
        emojiMap.put("infinite_superboom_tnt", "<:infinite_superboom_tnt:815305646194688000>");
        emojiMap.put("sniper_bow", "<:sniper_bow:816310296309137428>");
        emojiMap.put("thorns_boots", "<:spirit_boots:816310296217518131>");
        emojiMap.put("shadow_fury", "<:shadow_fury:816310296179245076>");
        emojiMap.put("skeleton_master_boots", "<:skeleton_master_boots:816310296096276550>");
        emojiMap.put("machine_gun_bow", "<:machine_gun_bow:816310296053678141>");
        emojiMap.put("beastmaster_crest_common", "<:beastmaster_crest_common:816310296045813770>");
        emojiMap.put("last_breath", "<:last_breath:816310296041357342>");
        emojiMap.put("beastmaster_crest_uncommon", "<:beastmaster_crest_uncommon:816310296037163068>");
        emojiMap.put("beastmaster_crest_legendary", "<:beastmaster_crest_legendary:816310296032313344>");
        emojiMap.put("crypt_bow", "<:crypt_bow:816310296011735050>");
        emojiMap.put("crypt_dreadlord_sword", "<:crypt_dreadlord_sword:816310295990370304>");
        emojiMap.put("beastmaster_crest_epic", "<:beastmaster_crest_epic:816310295907270698>");
        emojiMap.put("skeleton_master_helmet", "<:skeleton_master_helmet:816310295785242676>");
        emojiMap.put("bonzo_mask", "<:bonzo_mask:816132749982171156>");
        emojiMap.put("broken_piggy_bank", "<:broken_piggy_bank:816129549536329738>");
        emojiMap.put("cracked_piggy_bank", "<:cracked_piggy_bank:816129538601779250>");
        emojiMap.put("piggy_bank", "<:piggy_bank:816129528224546826>");
        emojiMap.put("sword_of_revelations", "<:sword_of_revelations:815310159617589288>");
        emojiMap.put("shaman_sword", "<:shaman_sword:815309622658465882>");
        emojiMap.put("adaptive_blade", "<:adaptive_blade:815309431038410833>");
        emojiMap.put("raider_axe", "<:raider_axe:815308927020564491>");
        emojiMap.put("wither_boots", "<:wither_boots:815308403219365929>");
        emojiMap.put("soul_whip", "<:soul_whip:815307925052457010>");
        emojiMap.put("rotten_leggings", "<:rotten_leggings:815307870937284699>");
        emojiMap.put("super_heavy_chestplate", "<:super_heavy_chestplate:815307825504976916>");
        emojiMap.put("midas_sword", "<:midas_sword:815307715467018242>");
        emojiMap.put("bonzo_staff", "<:bonzo_staff:815307683883647076>");
        emojiMap.put("flaming_sword", "<:flaming_sword:815307168109953024>");
        emojiMap.put("midas_staff", "<:midas_staff:815306843751448597>");
        emojiMap.put("hunter_knife", "<:hunter_knife:815306466596356137>");
        emojiMap.put("fancy_sword", "<:fancy_sword:815306055533461544>");
        emojiMap.put("sorrow", "<:sorrow:816324126956453929>");
        emojiMap.put("plasma", "<:plasma:816324127412977694>");
        emojiMap.put("volta", "<:volta:816324127307595797>");
        emojiMap.put("bag_of_cash", "<:bag_of_cash:816324127253332038>");
        emojiMap.put("ancient_claw", "<:ancient_claw:816324552405549117>");
        // emojiMap.put("", "");

        itemName = itemName.replace("starred_", "");
        if (emojiMap.containsKey(itemName)) {
            return emojiMap.get(itemName);
        }

        invMissing += "\n• " + itemName;
        return "❓";
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Integer> getPlayerSacks() {
        JsonElement sacksJson = higherDepth(profileJson, "sacks_counts");
        return new Gson().fromJson(sacksJson, HashMap.class);
    }

    public JsonArray getBankHistory() {
        try {
            return higherDepth(higherDepth(outerProfileJson, "banking"), "transactions").getAsJsonArray();
        } catch (Exception e) {
            return null;
        }
    }

    public int getSkillMaxLevel(String skillName) {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }
        if (skillName.equals("farming")) {
            return higherDepth(higherDepth(levelTables, "leveling_caps"), skillName).getAsInt()
                    + getFarmingCapUpgrade();
        }

        return higherDepth(higherDepth(levelTables, "leveling_caps"), skillName).getAsInt();
    }

    public double getSkillXp(String skillName) {
        try {
            if (skillName.equals("catacombs")) {
                return higherDepth(
                        higherDepth(higherDepth(higherDepth(profileJson, "dungeons" + skillName), "dungeon_types"),
                                "catacombs"),
                        "experience").getAsDouble();
            }
            return higherDepth(profileJson, "experience_skill_" + skillName).getAsDouble();
        } catch (Exception ignored) {
        }
        return -1;
    }

    public double getDungeonClassLevel(String className) {
        if (this.levelTables == null) {
            this.levelTables = getJson(
                    "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");
        }

        SkillsStruct dungeonClassLevel = skillInfoFromExp(getDungeonClassXp(className), "catacombs");
        return dungeonClassLevel.skillLevel + dungeonClassLevel.progressToNext;
    }

    public SkillsStruct getDungeonClass(String className) {
        return skillInfoFromExp(getDungeonClassXp(className), "catacombs");
    }

    public double getDungeonClassXp(String className) {
        try {
            return higherDepth(
                    higherDepth(higherDepth(higherDepth(profileJson, "dungeons"), "player_classes"), className),
                    "experience").getAsDouble();
        } catch (Exception e) {
            return 0;
        }
    }

    public int getFarmingCapUpgrade() {
        try {
            return higherDepth(higherDepth(higherDepth(profileJson, "jacob2"), "perks"), "farming_level_cap")
                    .getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    public double getWeight() {
        Weight playerWeight = new Weight(this);
        return playerWeight.getTotalWeight();
    }

    public Map<Integer, String[]> getInventoryItem(String itemName) {
        try {
            String encodedInventoryContents = higherDepth(higherDepth(profileJson, "inv_contents"), "data")
                    .getAsString();
            NBTCompound decodedInventoryContents = NBTReader.readBase64(encodedInventoryContents);

            NBTList invFrames = decodedInventoryContents.getList(".i");
            Map<Integer, String[]> invFramesMap = new HashMap<>();
            for (int i = 0; i < invFrames.size(); i++) {
                NBTCompound displayName = invFrames.getCompound(i).getCompound("tag.ExtraAttributes");
                if (displayName != null) {
                    if (displayName.getString("id", "empty").equalsIgnoreCase(itemName)) {
                        StringBuilder loreString = new StringBuilder();
                        for (Object loreLine : invFrames.getCompound(i).getCompound("tag.display").getList("Lore")) {
                            loreString.append("\n").append(((String) loreLine).replaceAll(
                                    "§ka|§0|§1|§2|§3|§4|§5|§6|§7|§8|§9|§a|§b|§c|§d|§e|§f|§k|§l|§m|§n|§o|§r", ""));
                        }
                        invFramesMap.put(i + 1, new String[] {
                                invFrames.getCompound(i).getString("Count", "0").toLowerCase().replace("b", "") + "x",
                                loreString.toString() });
                    }
                }
            }
            return invFramesMap;

        } catch (Exception ignored) {
        }
        return null;
    }

    public int getDungeonSecrets() {
        if (hypixelProfileJson == null) {
            this.hypixelProfileJson = getJson(
                    "https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&uuid=" + playerUuid);
        }

        try {
            return higherDepth(higherDepth(higherDepth(hypixelProfileJson, "player"), "achievements"),
                    "skyblock_treasure_hunter").getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getSelectedDungeonClass() {
        try {
            return higherDepth(higherDepth(profileJson, "dungeons"), "selected_dungeon_class").getAsString();
        } catch (Exception e) {
            return "none";
        }
    }

    public String getHyperion() {
        if (getInventory()[0].contains("hyperion") || getInventory()[1].contains("hyperion")) {
            return "yes";
        }
        return "no";
    }

    public int getBonemerang() {
        if (getInventory()[0].contains("bone_boomerang") || getInventory()[1].contains("bone_boomerang")) {
            return StringUtils.countOccurrencesOf(getInventory()[0], "bone_boomerang")
                    + StringUtils.countOccurrencesOf(getInventory()[1], "bone_boomerang");
        }
        return 0;
    }
}
