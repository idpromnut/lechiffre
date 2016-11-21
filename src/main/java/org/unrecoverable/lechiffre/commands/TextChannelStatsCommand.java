package org.unrecoverable.lechiffre.commands;

import java.util.List;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;

/**
 * 
 * @author Chris Matthews
 *
 */
public class TextChannelStatsCommand extends ChannelStatsCommand {

	public TextChannelStatsCommand() {
	}

	@Override
	public String getCommand() {
		return Commands.CMD_TEXT_CHANNEL_STATS;
	}

	@Override
	public String getHelp() {
		return String.format("prints stats for a specific text channel (i.e.: %s%s<channel name>). Answer will be sent as a PM.",
				Commands.CMD_PREFIX, getCommand());
	}

	@Override
	protected List<IChannel> getChannels(IDiscordClient client) {
		return client.getChannels();
	}
}
