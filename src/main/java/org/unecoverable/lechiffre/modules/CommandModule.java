package org.unecoverable.lechiffre.modules;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.unecoverable.lechiffre.commands.Commands;
import org.unecoverable.lechiffre.commands.ICommand;
import org.unecoverable.lechiffre.entities.Configuration;
import org.unecoverable.lechiffre.entities.IConfigurable;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.modules.IModule;

@Slf4j
public class CommandModule implements IModule, IConfigurable {

	@Getter
	private List<ICommand> commandChain = new LinkedList<>();

	private Configuration configuration = new Configuration();

	public CommandModule() {
	}

	@Override
	public boolean enable(IDiscordClient client) {
		return true;
	}

	@Override
	public void disable() {
	}

	@Override
	public String getName() {
		return "CommandModule";
	}

	@Override
	public String getAuthor() {
		return "nybbles";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getMinimumDiscord4JVersion() {
		return "1.0";
	}

	@Override
	public void configure(Configuration configuration) {
		this.configuration = configuration;
	}

	@EventSubscriber
	public void handleMessageReceived(MessageReceivedEvent event) {
		try {
			final IMessage lMessage = event.getMessage();
			final IUser lAuthor = lMessage.getAuthor();
			final IDiscordClient lClient = event.getClient();
			final IChannel lChannel = lMessage.getChannel();
			IChannel lReplyChannel = lChannel;
			if (!lChannel.isPrivate()) {
				lReplyChannel = lClient.getOrCreatePMChannel(lAuthor);
			}

			String lReplyMessage = dispatchCommand(lMessage);
			if (StringUtils.isNotBlank(lReplyMessage)) {
				lReplyChannel.sendMessage(lReplyMessage);
			}
		} catch (Exception e1) {
			log.warn("error while trying send reply message", e1);
		}
	}

	private String dispatchCommand(final IMessage message) {

		final String lContent = message.getContent();
		final IUser lAuthor = message.getAuthor();
		final IGuild lGuild = message.getGuild();

		if (lGuild == null) {
			return "commands must be requested from a server (not via PM!)";
		}
		else if (Commands.isCommand(lContent)) {
			Pair<Boolean, String> lResponse = Pair.of(Boolean.TRUE,
					"I don't know what you would like me to do, may I suggest you '!help' yourself.");
			for (ICommand lCommand : commandChain) {
				if (message.getContent().startsWith(Commands.CMD_PREFIX + lCommand.getCommand())) {
					if (configuration.userHasPermission(lAuthor, lGuild, lCommand)) {
						lResponse = lCommand.handle(message);
						if (lResponse != null)
							break;
					}
					else {
						return "I'm sorry, but I can't let you run the " + Commands.CMD_PREFIX + lCommand.getCommand() + " command";
					}
				}
			}

			// is the response to be returned to the originator of the message?
			if (lResponse.getLeft() == true) {
				return lResponse.getRight();
			} else {
				return null;
			}
		}
		// otherwise do nothing
		else {
			return null;
		}
	}
}
