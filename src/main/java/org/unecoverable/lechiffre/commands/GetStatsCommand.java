package org.unecoverable.lechiffre.commands;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.unecoverable.lechiffre.entities.User;
import org.unecoverable.lechiffre.entities.UserStats;

import sx.blah.discord.handle.obj.IMessage;

public class GetStatsCommand extends BaseStatsCommand implements ICommand {

	public GetStatsCommand() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getCommand() {
		return Commands.CMD_STATS;
	}

	@Override
	public String getHelp() {
		return "returns stats on either a single user (ex: !stats joe) or the entire server (ex: !stats)";
	}

	@Override
	public Pair<Boolean, String> handle(IMessage message) {

		final String lContent = message.getContent();
		final String[] lChoppedContent = StringUtils.split(lContent, " ");

		// there is a user name present, return the stats for that user only
		if (lChoppedContent.length == 2) {
			return Pair.of(Boolean.TRUE, getStatsForUser(lChoppedContent[1]));
		} else {
			// todo return the stats for the guild of the message author
		}
		return Pair.of(Boolean.FALSE, null);
	}

	public String getStatsForUser(String username) {
		String lUserStatsString = "I don't know anything about " + username;
		User lUser = searchForUser(username);
		if (lUser != null) {
			UserStats lStats = findUserStats(lUser);
			if (lStats != null) {
				lUserStatsString = lUser.getName() + ": " + lStats.toString();
			}
		}
		return lUserStatsString;
	}

}
