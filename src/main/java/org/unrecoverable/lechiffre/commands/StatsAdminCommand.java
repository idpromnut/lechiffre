package org.unrecoverable.lechiffre.commands;

import org.apache.commons.lang3.tuple.Pair;

import sx.blah.discord.handle.obj.IMessage;

public class StatsAdminCommand implements ICommand {

	@Override
	public String getCommand() {
		return Commands.CMD_STATS_ADMIN;
	}

	@Override
	public String getHelp() {
		return "TBD";
	}

	@Override
	public boolean isGuildCommand() {
		return false;
	}

	@Override
	public Pair<BotReply, String> handle(IMessage message) {
		return null;
	}

}
