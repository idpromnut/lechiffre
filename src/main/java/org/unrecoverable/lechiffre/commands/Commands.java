package org.unrecoverable.lechiffre.commands;

import java.util.ArrayList;
import java.util.List;

public final class Commands {

	private static final String DEFAULT_CMD_PREFIX = "!";
	
	private static String cmdPrefix = DEFAULT_CMD_PREFIX;

	public static final String CMD_STATS = "stats";

	public static final String CMD_SKILLZ = "skillz";

	public static final String CMD_LOGOUT = "logout";

	public static final String CMD_LAST_SEEN = "lastseen";

	public static final String CMD_SAVE = "save";

	public static final String CMD_HELP = "help";

	public static final String CMD_GUILD_STATS = "guildstats";

	public static final String CMD_TEXT_CHANNEL_STATS = "textstats";

	public static final String CMD_VOICE_CHANNEL_STATS = "voicestats";

	public static final String CMD_MOST_ACTIVE_STATS = "mostactive";


	public static boolean isCommand(String input) {
		return (input != null && input.startsWith(getCommandPrefix()));
	}
	
	public static String getCommandPrefix() {
		return cmdPrefix;
	}
	
	public static void setCommandPrefix(String prefix) {
		cmdPrefix = prefix;
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
		lCommands.add(CMD_TEXT_CHANNEL_STATS);
		lCommands.add(CMD_VOICE_CHANNEL_STATS);
		return lCommands;
	}
}
