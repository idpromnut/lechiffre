package org.unrecoverable.lechiffre.commands;

import org.apache.commons.lang3.tuple.Pair;
import org.unrecoverable.lechiffre.entities.User;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

public class SelfStatsCommand extends UserStatsCommand {

	public SelfStatsCommand() {
	}

	@Override
	public String getCommand() {
		return Commands.CMD_SKILLZ;
	}

	@Override
	public String getHelp() {
		return "returns your stats (ex: " + Commands.CMD_PREFIX + Commands.CMD_SKILLZ + "). PMing the command to me will result in extra info being PM'd back to you.";
	}

	@Override
	public boolean isGuildCommand() {
		return false;
	}

	@Override
	public Pair<BotReply, String> handle(IMessage message) {
		final IUser lAuthor = message.getAuthor();
		if (lAuthor != null) {
			User lUser = searchForUser(lAuthor.getName());
			if (message.getChannel().isPrivate() && lUser != null) {
				pushUserActivityChart(lUser, findUserStats(lUser), message.getChannel());
			}
			return Pair.of(BotReply.CHANNEL, getStatsForUser(lAuthor.getName()));
		}
		else {
			return Pair.of(BotReply.NONE, null);
		}
	}

}
