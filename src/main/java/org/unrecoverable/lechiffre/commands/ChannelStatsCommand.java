package org.unrecoverable.lechiffre.commands;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.unrecoverable.lechiffre.entities.User;

import sx.blah.discord.handle.obj.IMessage;

public class ChannelStatsCommand extends BaseStatsCommand implements ICommand {

	public ChannelStatsCommand() {
	}

	@Override
	public String getCommand() {
		return Commands.CMD_CHANNEL_STATS;
	}

	@Override
	public String getHelp() {
		return "prints stats for the current channel or a specific channel (i.e.: chanstats <channel name>). Answer will be sent as a PM.";
	}

	@Override
	public boolean isGuildCommand() {
		return true;
	}

	@Override
	public Pair<BotReply, String> handle(IMessage message) {

		final String lContent = message.getContent();
		final String[] lChoppedContent = StringUtils.split(lContent, " ");

		// there is a channel name present, return the stats for that user only
		if (lChoppedContent.length == 2) {

			User lUser = searchForUser(lChoppedContent[1]);
			if (message.getChannel().isPrivate() && lUser != null) {
//				gitDatChart(lUser, findUserStats(lUser), message.getChannel());
			}
//			return Pair.of(BotReply.PM, getStatsForUser(lChoppedContent[1]));
		}

		return Pair.of(BotReply.NONE, null);
	}

}
