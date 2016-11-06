package org.unecoverable.lechiffre.commands;

import org.apache.commons.lang3.tuple.Pair;
import org.unecoverable.lechiffre.entities.GuildStats;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Presences;

public class GuildStatsCommand extends BaseStatsCommand implements ICommand, IStatsCommand {

	public GuildStatsCommand() {
	}

	@Override
	public String getCommand() {
		return Commands.CMD_GUILD_STATS;
	}

	@Override
	public String getHelp() {
		return "prints out some statistics for the server";
	}

	@Override
	public boolean isGuildCommand() {
		return true;
	}

	@Override
	public Pair<BotReply, String> handle(final IMessage message) {

		final IGuild lGuild = message.getGuild();
		String lReply = "no guild stats found";

		if (getGuildStatsMap() != null) {
			GuildStats lGuildStats = getGuildStatsMap().get(lGuild);
			if (lGuildStats != null) {
				lReply = String.format(
						"**%d users\n%d online\n%d message sent\n%d mentions\n\n:100: AWESOME!**",
						getTotalUsers(lGuild),
						getOnlineUsers(lGuild),
						lGuildStats.totalMessagesPosted(),
						lGuildStats.totalMentions());
			}
		}

		return Pair.of(BotReply.CHANNEL, lReply);
	}

	public int getTotalUsers(final IGuild guild) {
		return guild.getUsers().size();
	}

	public int getOnlineUsers(final IGuild guild) {
		int lOnlineUsers = 0;
		for(IUser lUser: guild.getUsers()) {
			if (Presences.ONLINE.equals(lUser.getPresence())) {
				++lOnlineUsers;
			}
		}

		return lOnlineUsers;
	}
}
