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

package com.skyblockplus.miscellaneous.weight.lily;

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonArray;
import com.skyblockplus.miscellaneous.weight.weight.SkillsWeight;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.SkillsStruct;
import com.skyblockplus.utils.structs.WeightStruct;

public class LilySkillsWeight extends SkillsWeight {

	public LilySkillsWeight(Player player) {
		super(player);
	}

	@Override
	public WeightStruct getSkillsWeight(String skillName) {
		return getSkillsWeight(skillName, player.getSkill(skillName, Player.WeightType.LILY));
	}

	@Override
	public WeightStruct getSkillsWeight(String skillName, SkillsStruct skillsStruct) {
		double skillAverage = 0;
		for (String skill : SKILL_NAMES) {
			try {
				skillAverage += (skill.equals(skillName) ? skillsStruct : player.getSkill(skill, Player.WeightType.LILY)).currentLevel();
			} catch (Exception e) {
				return new WeightStruct();
			}
		}
		skillAverage /= SKILL_NAMES.size();

		JsonArray srwTable = higherDepth(SKILL_RATIO_WEIGHT, skillName).getAsJsonArray();
		double base =
			(
				(12 * Math.pow((skillAverage / 60), 2.44780217148309)) *
				srwTable.get(skillsStruct.currentLevel()).getAsDouble() *
				srwTable.get(srwTable.size() - 1).getAsDouble()
			) +
			(srwTable.get(srwTable.size() - 1).getAsDouble() * Math.pow(skillsStruct.currentLevel() / 60.0, Math.pow(2, 0.5)));
		base *= 1.8162162162162162;
		double overflow = 0;
		if (skillsStruct.totalExp() > SKILLS_LEVEL_60_XP) {
			double factor = higherDepth(SKILL_FACTORS, skillName).getAsDouble();
			double effectiveOver = effectiveXP(skillsStruct.totalExp() - SKILLS_LEVEL_60_XP, factor);
			double t = (effectiveOver / SKILLS_LEVEL_60_XP) * (higherDepth(SKILL_OVERFLOW_MULTIPLIERS, skillName).getAsDouble());
			if (t > 0) {
				overflow += 1.8162162162162162 * t;
			}
		}

		return weightStruct.add(new WeightStruct(base, overflow));
	}

	private double effectiveXP(double xp, double factor) {
		if (xp < SKILLS_LEVEL_60_XP) {
			return xp;
		} else {
			double remainingXP = xp;
			double z = 0;
			for (int i = 0; i <= (int) (xp / SKILLS_LEVEL_60_XP); i++) {
				if (remainingXP >= SKILLS_LEVEL_60_XP) {
					remainingXP -= SKILLS_LEVEL_60_XP;
					z += Math.pow(factor, i);
				}
			}
			return z * SKILLS_LEVEL_60_XP;
		}
	}
}