package org.unrecoverable.lechiffre.commands;

import java.util.HashMap;
import java.util.Map;

import org.unrecoverable.lechiffre.entities.GuildStats;
import org.unrecoverable.lechiffre.entities.User;
import org.unrecoverable.lechiffre.entities.UserStats;

import lombok.Getter;
import sx.blah.discord.handle.obj.IGuild;

public class BaseStatsCommand implements IStatsCommand {

	@Getter
	private Map<IGuild, GuildStats> guildStatsMap = new HashMap<>();

	public BaseStatsCommand() {
	}

	@Override
	public void enableCommands(IGuild guild, GuildStats stats) {
		this.guildStatsMap.put(guild, stats);
	}

	public User searchForUser(String iSearch) {
		String lSearch = iSearch.toLowerCase();
		for(GuildStats lGuildStats: guildStatsMap.values()) {
			for(User lUser: lGuildStats.getUsers().values()) {
				if (lUser.getName().toLowerCase().startsWith(lSearch)) {
					return lUser;
				}
			}
		}
		return null;
	}

	public UserStats findUserStats(User user) {
		UserStats lUserStats = null;
		for(GuildStats lGuildStats: guildStatsMap.values()) {
			lUserStats = lGuildStats.getStats(user.getId());
			if (lUserStats != null) break;
		}
		return lUserStats;
	}
}
