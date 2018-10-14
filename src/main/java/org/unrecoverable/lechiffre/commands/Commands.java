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
	
	public static final String CMD_STATS_ADMIN = "statsadmin";
	
	public static final String CMD_ADMIN = "admin";

	public static final String CMD_ROLL = "roll";
	
	public static final String CMD_TAUNT = "taunt";

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
		List<String> commands = new ArrayList<>();
		commands.add(CMD_HELP);
		commands.add(CMD_LAST_SEEN);
		commands.add(CMD_LOGOUT);
		commands.add(CMD_SAVE);
		commands.add(CMD_STATS);
		commands.add(CMD_GUILD_STATS);
		commands.add(CMD_SKILLZ);
		commands.add(CMD_TEXT_CHANNEL_STATS);
		commands.add(CMD_VOICE_CHANNEL_STATS);
		commands.add(CMD_STATS_ADMIN);
		commands.add(CMD_ADMIN);
		return commands;
	}
}
