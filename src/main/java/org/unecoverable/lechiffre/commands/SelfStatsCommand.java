package org.unecoverable.lechiffre.commands;

import org.apache.commons.lang3.tuple.Pair;

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
		return "returns your stats (ex: " + Commands.CMD_PREFIX + Commands.CMD_SKILLZ + ") and sends them to the same place you executed the command (PM or public channel)";
	}

	@Override
	public boolean isGuildCommand() {
		return false;
	}

	@Override
	public Pair<BotReply, String> handle(IMessage message) {
		final IUser lAuthor = message.getAuthor();
		if (lAuthor != null) {
			return Pair.of(BotReply.CHANNEL, getStatsForUser(lAuthor.getName()));
		}
		else {
			return Pair.of(BotReply.NONE, null);
		}
	}

}
