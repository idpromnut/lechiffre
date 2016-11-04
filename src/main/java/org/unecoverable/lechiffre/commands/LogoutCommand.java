package org.unecoverable.lechiffre.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;

public class LogoutCommand implements ICommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(LogoutCommand.class);

	@Getter
	private List<ICommand> preLogoutCommands = new ArrayList<>();

	public LogoutCommand() {
	}

	@Override
	public String getCommand() {
		return Commands.CMD_LOGOUT;
	}

	@Override
	public String getHelp() {
		return "instructs me to bugger off (log out of all servers)";
	}

	@Override
	public Pair<Boolean, String> handle(IMessage message) {

		final IChannel lChannel = message.getChannel();
		if (lChannel.isPrivate() && message.getContent().startsWith(Commands.CMD_LOGOUT)) {
			final IDiscordClient lClient = message.getClient();
			final IUser lAuthor = message.getAuthor();
			try {
				IChannel lReplyChannel = lChannel;
				if (!lReplyChannel.isPrivate()) {
					lReplyChannel = lClient.getOrCreatePMChannel(lAuthor);
				}
				try {
					lReplyChannel.sendMessage("ggez no-re biatch!11!!");
				} catch (Exception e1) {
					// ignore any error as this is a "courtesy reply"
					LOGGER.debug("error while sending courtesy logout message to {}", lAuthor.getName(), e1);
				}
				LOGGER.info("{} asked us to bugger off, {} over and OUT!", lAuthor.getName(), lClient.getOurUser().getName());
				lClient.changePresence(true);
				executePreLogoutCommands(message);
				lClient.logout();
				LOGGER.info("Logged out");
			} catch (DiscordException | RateLimitException e1) {
				LOGGER.warn("error while trying to log out", e1);
			}
			System.exit(0);
		}

		return Pair.of(Boolean.FALSE, null);
	}

	private void executePreLogoutCommands(IMessage message) {
		for(ICommand lCommand: preLogoutCommands) {
			lCommand.handle(message);
		}
	}
}
