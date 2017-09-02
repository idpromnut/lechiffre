package org.unrecoverable.lechiffre.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unrecoverable.lechiffre.entities.Channel;
import org.unrecoverable.lechiffre.entities.ChannelStats;
import org.unrecoverable.lechiffre.entities.GuildStats;
import org.unrecoverable.lechiffre.entities.User;
import org.unrecoverable.lechiffre.entities.UserStats;

import lombok.Getter;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

/**
 * TODO: need to think about refactoring all the "activity" based stats to a single set of commands:
 * !activity <channel|guild|user>
 * 
 * @author Chris Matthews
 *
 */
public class BaseStatsCommand implements IStatsCommand {

	@Getter
	private Map<IGuild, GuildStats> guildStatsMap = new HashMap<>();

	public BaseStatsCommand() {
	}

	@Override
	public void enableCommands(IGuild guild, GuildStats stats) {
		this.guildStatsMap.put(guild, stats);
	}

	public List<User> searchForUser(String searchName) {
		String lSearchName = searchName.toLowerCase();
		List<User> lUsers = new ArrayList<>();
		for(IGuild lGuild: guildStatsMap.keySet()) {
			for(IUser lUser: lGuild.getUsers()) {
				if (lUser.getDisplayName(lGuild).toLowerCase().contains(lSearchName)) {
					GuildStats lGuildStats = guildStatsMap.get(lGuild);
					lUsers.add(lGuildStats.getUsers().get(lUser.getStringID()));
				}
			}
		}
		return lUsers;
	}
	
	public String getDisplayNameForUser(User user, IGuild guild) {
		return guild.getUserByID(Long.parseUnsignedLong(user.getId())).getDisplayName(guild);
	}

	public UserStats findUserStats(User user) {
		UserStats lUserStats = null;
		for(GuildStats lGuildStats: guildStatsMap.values()) {
			lUserStats = lGuildStats.getUserStats(user.getId());
			if (lUserStats != null) break;
		}
		return lUserStats;
	}
	
	public Channel searchForChannel(String searchName) {
		String lSearch = searchName.toLowerCase();
		for(GuildStats lGuildStats: guildStatsMap.values()) {
			for(Channel lChannel: lGuildStats.getChannels().values()) {
				if (lChannel.getName().toLowerCase().startsWith(lSearch)) {
					return lChannel;
				}
			}
		}
		return null;
	}
	
	public ChannelStats findChannelStats(String snowflakeId) {
		ChannelStats lChannelStats = null;
		for(GuildStats lGuildStats: guildStatsMap.values()) {
			lChannelStats = lGuildStats.getChannelStats(snowflakeId);
			if (lChannelStats != null) break;
		}
		
		return lChannelStats;
	}
	

}
