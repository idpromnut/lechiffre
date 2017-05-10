package org.unrecoverable.lechiffre.commands;

import java.util.ArrayList;
import java.util.List;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;

/**
 * 
 * @author Chris Matthews
 *
 */
public class VoiceChannelStatsCommand extends ChannelStatsCommand {

	public VoiceChannelStatsCommand() {
	}

	@Override
	public String getCommand() {
		return Commands.CMD_VOICE_CHANNEL_STATS;
	}

	@Override
	public String getHelp() {
		return String.format("prints stats for a specific channel (i.e.: %s%s<channel name>). Answer will be sent as a PM.",
				Commands.getCommandPrefix(), getCommand());
	}

	@Override
	protected List<IChannel> getChannels(IDiscordClient client) {
		return new ArrayList<IChannel>(client.getVoiceChannels());
	}
}
