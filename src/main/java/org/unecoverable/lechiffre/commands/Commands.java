package org.unecoverable.lechiffre.commands;

import java.util.ArrayList;
import java.util.List;

public final class Commands {

	public static final String CMD_PREFIX = "!";

	public static final String CMD_STATS = "stats";

	public static final String CMD_LOGOUT = "logout";

	public static final String CMD_LAST_SEEN = "lastseen";

	public static final String CMD_SAVE = "save";

	public static final String CMD_HELP = "help";


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
		return lCommands;
	}
}
