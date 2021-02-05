package com.SkyblockBot.API.Service;

import com.SkyblockBot.API.Models.ErrorTemplate;
import com.SkyblockBot.API.Models.GuildModel;
import com.SkyblockBot.API.Models.Template;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static com.SkyblockBot.Main.jda;

@RestController
public class GuildController {

    @GetMapping("/api/mutualGuilds")
    public Object getMutualGuilds(@RequestParam(value = "userId") String userId) {
        try {
            User user = jda.getUserById(userId);
            List<GuildModel> guildList = new ArrayList<>();
            for (Guild guild : user.getMutualGuilds()) {
                if (guild.getMember(user).hasPermission(Permission.MANAGE_SERVER)) {
                    guildList.add(new GuildModel(guild.getName(), guild.getId()));
                }
            }
            return new Template("true", guildList);
        } catch (Exception e) {
            return new ErrorTemplate("false", "Invalid field [userId]");
        }
    }
}