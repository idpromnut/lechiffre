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

/**
 * This module listens for "commands" from users for the bot to process. A command is
 * a message that starts with a {@link  org.unrecoverable.lechiffre.commands.Commands#CMD_PREFIX  Command prefix}.
 *
 *
 * @author Chris Matthews
 *
 */
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
		for(ICommand lCommand: commandChain) {
			if (lCommand instanceof IConfigurable) {
				((IConfigurable) lCommand).configure(configuration);
			}
		}
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

		if (Commands.isCommand(lContent)) {

			// define the "unknown command" response
			Pair<Boolean, String> lResponse = Pair.of(Boolean.TRUE,
					"I don't know what you would like me to do, may I suggest you '" + Commands.CMD_PREFIX + Commands.CMD_HELP + "' yourself.");

			for (ICommand lCommand : commandChain) {
				if (message.getContent().startsWith(Commands.CMD_PREFIX + lCommand.getCommand())) {

					// check if this command requires that user sent the command from a guild channel
					if (lCommand.isGuildCommand() ) {
						lResponse = executeGuildCommand(lCommand, message, lGuild, lAuthor);
					}
					else {
						// otherwise this command doesn't require a guild reference to execute
						// TODO need to add a set of permissions checking based on user ID or something
						lResponse = lCommand.handle(message);
					}
					break;
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

	private Pair<Boolean, String> executeGuildCommand(final ICommand command, final IMessage message, final IGuild guild, final IUser author) {

		Pair<Boolean, String> lResponse;

		if (guild == null) {
			return Pair.of(Boolean.TRUE, command.getCommand() + " must be requested from a channel (not via PM!)");
		}

		// check if the user that send the command can execute it using the guild permissions it was sent from
		if (configuration.userHasPermission(author, guild, command)) {
			lResponse = command.handle(message);
		}
		else {
			lResponse = Pair.of(Boolean.TRUE, "I'm sorry, but I can't let you run the " + Commands.CMD_PREFIX + command.getCommand() + " command");
		}

		return lResponse;
	}

//	private Pair<Boolean, String> executeCommand(final ICommand command, final IMessage message, final IUser author) {
//
//	}
}
