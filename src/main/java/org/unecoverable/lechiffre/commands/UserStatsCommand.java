package org.unecoverable.lechiffre.commands;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.unecoverable.lechiffre.entities.User;
import org.unecoverable.lechiffre.entities.UserStats;

import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

public class UserStatsCommand extends BaseStatsCommand implements ICommand {

	public UserStatsCommand() {
	}

	@Override
	public String getCommand() {
		return Commands.CMD_STATS;
	}

	@Override
	public String getHelp() {
		return "returns stats on either a single user (ex: !stats joe) or your stats (ex: !stats)";
	}

	@Override
	public boolean isGuildCommand() {
		return false;
	}

	@Override
	public Pair<Boolean, String> handle(IMessage message) {

		final String lContent = message.getContent();
		final String[] lChoppedContent = StringUtils.split(lContent, " ");

		// there is a user name present, return the stats for that user only
		if (lChoppedContent.length == 2) {
			return Pair.of(Boolean.TRUE, getStatsForUser(lChoppedContent[1]));
		} else {
			final IUser lAuthor = message.getAuthor();
			return Pair.of(Boolean.TRUE, getStatsForUser(lAuthor.getName()));
		}
	}

	public String getStatsForUser(String username) {
		String lUserStatsString = "I don't know anything about " + username;
		User lUser = searchForUser(username);
		if (lUser != null) {
			UserStats lStats = findUserStats(lUser);
			if (lStats != null) {
				lUserStatsString = "__**" + lUser.getName() + "**__:\n" + lStats.toString() + "\n";
			}
		}
		return lUserStatsString;
	}

}
