package org.unecoverable.lechiffre.commands;

import java.util.ArrayList;
import java.util.List;

public final class Commands {

	public static final String CMD_PREFIX = "!";

	public static final String CMD_STATS = "stats";

	public static final String CMD_SKILLZ = "skillz";

	public static final String CMD_LOGOUT = "logout";

	public static final String CMD_LAST_SEEN = "lastseen";

	public static final String CMD_SAVE = "save";

	public static final String CMD_HELP = "help";

	public static final String CMD_GUILD_STATS = "guildstats";

	public static final String CMD_CHANNEL_STATS = "channelstats";

	public static final String CMD_MOST_ACTIVE_STATS = "mostactive";


	public static boolean isCommand(String input) {
		return (input != null && input.startsWith(CMD_PREFIX));
	}

	public static List<String> getCommands() {
		List<String> lCommands = new ArrayList<>();
		lCommands.add(CMD_HELP);
		lCommands.add(CMD_LAST_SEEN);
		lCommands.add(CMD_LOGOUT);
		lCommands.add(CMD_SAVE);
		lCommands.add(CMD_STATS);
		lCommands.add(CMD_GUILD_STATS);
		lCommands.add(CMD_SKILLZ);
		return lCommands;
	}
}
