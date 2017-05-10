package org.unrecoverable.lechiffre.modules;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.unrecoverable.lechiffre.Bot;
import org.unrecoverable.lechiffre.commands.BotReply;
import org.unrecoverable.lechiffre.commands.Commands;
import org.unrecoverable.lechiffre.commands.ICommand;
import org.unrecoverable.lechiffre.entities.Configuration;
import org.unrecoverable.lechiffre.entities.IConfigurable;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

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

	private MetricRegistry metricRegistry;

	private String messagesProcessed = MetricRegistry.name("messages", "processed");
	private String messagesProcessedRate = MetricRegistry.name("messages", "processed", "rate");
	private String messagesPrcoessedDataRate = MetricRegistry.name("messages", "processed", "characters");
	private String commandsProcessed = MetricRegistry.name("commands", "processed");
	private String commandsProcessedRate = MetricRegistry.name("commands", "processed", "rate");
	private String messagesReply = MetricRegistry.name("messages", "replies");
	private String messagesReplyDataRate = MetricRegistry.name("messages", "replies", "characters");
	
	public CommandModule() {
	}

	@Override
	public boolean enable(IDiscordClient client) {
		metricRegistry = SharedMetricRegistries.getOrCreate(Bot.METRIC_REGISTRY_NAME);
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
		return getClass().getPackage().getImplementationVendor();
	}

	@Override
	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
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
			metricRegistry.counter(messagesProcessed).inc();
			metricRegistry.meter(messagesProcessedRate).mark();
			final IMessage lMessage = event.getMessage();
			metricRegistry.meter(messagesPrcoessedDataRate).mark(lMessage.getContent().length());
			final IUser lAuthor = lMessage.getAuthor();
			final IDiscordClient lClient = event.getClient();
			final IChannel lChannel = lMessage.getChannel();
			IChannel lPrivateChannel = lClient.getOrCreatePMChannel(lAuthor);

			Pair<BotReply, String> lReply = dispatchCommand(lMessage);
			if (lReply != null) {
				switch (lReply.getLeft()) {
				case CHANNEL:
					if (configuration.getChannelReplyWhitelist().contains(lChannel.getName())) {
						lChannel.sendMessage(lReply.getRight());
						if (lReply.getRight() != null) {
							metricRegistry.counter(messagesReply).inc();
							metricRegistry.meter(messagesReplyDataRate).mark(lReply.getRight().length());
						}
						break;
					}
				case PM:
					lPrivateChannel.sendMessage(lReply.getRight());
					if (lReply.getRight() != null) {
						metricRegistry.meter(messagesReply).mark();
						metricRegistry.meter(messagesReplyDataRate).mark(lReply.getRight().length());
					}
					break;
				case NONE:
				default:
					break;
				}
			}
		} catch (Exception e1) {
			log.warn("error while trying send reply message", e1);
		}
	}

	private Pair<BotReply, String> dispatchCommand(final IMessage message) {

		final String lContent = message.getContent();
		final IUser lAuthor = message.getAuthor();
		final IGuild lGuild = message.getGuild();
		Pair<BotReply, String> lResponse = Pair.of(BotReply.NONE, null);

		if (Commands.isCommand(lContent)) {

			// define the "unknown command" response
			lResponse = Pair.of(BotReply.PM,
					"I don't know what you would like me to do, may I suggest you '" + Commands.getCommandPrefix() + Commands.CMD_HELP + "' yourself.");

			for (ICommand lCommand : commandChain) {
				if (message.getContent().startsWith(Commands.getCommandPrefix() + lCommand.getCommand())) {

					// check if this command requires a permissions check (the guild is available)
					if (lCommand.isGuildCommand() ) {
						lResponse = executeGuildCommand(lCommand, message, lGuild, lAuthor);
					}
					else {
						// otherwise this command doesn't require a guild reference to execute
						// TODO need to add a set of permissions checking based on user ID or something
						lResponse = lCommand.handle(message);
					}
					metricRegistry.counter(commandsProcessed).inc();
					metricRegistry.meter(commandsProcessedRate).mark();
					break;
				}
			}
		}

		return lResponse;
	}

	private Pair<BotReply, String> executeGuildCommand(final ICommand command, final IMessage message, final IGuild guild, final IUser author) {

		Pair<BotReply, String> lResponse;

		if (guild == null) {
			return Pair.of(BotReply.PM, command.getCommand() + " must be requested from a channel (not via PM!)");
		}

		// check if the user that send the command can execute it using the guild permissions it was sent from
		if (configuration.userHasPermission(author, guild, command)) {
			lResponse = command.handle(message);
		}
		else {
			lResponse = Pair.of(BotReply.PM, "I'm sorry, but I can't let you run the " + Commands.getCommandPrefix() + command.getCommand() + " command");
		}

		return lResponse;
	}
}
