package org.unecoverable.lechiffre.commands;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.unecoverable.lechiffre.entities.Configuration;
import org.unecoverable.lechiffre.entities.IConfigurable;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

public class HelpCommand implements ICommand, IConfigurable {

	private List<ICommand> commands = new LinkedList<>();

	private Configuration configuration;

	public HelpCommand(List<ICommand> commands) {
		this.commands.addAll(commands);
		this.commands.add(this);
	}

	@Override
	public void configure(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String getCommand() {
		return Commands.CMD_HELP;
	}

	@Override
	public String getHelp() {
		return "I am help, I help with help that helps with help that helps ... AARGH!! *implodes*";
	}

	@Override
	public Pair<Boolean, String> handle(IMessage message) {
		final String[] lChoppedMessage = StringUtils.split(message.getContent(), " ");
		final IUser lAuthor = message.getAuthor();
		final IGuild lGuild = message.getGuild();

		String lHelpMessage;
		if (lGuild != null) {
			if (lChoppedMessage.length == 2) {
				lHelpMessage = "I don't know anything about a " + lChoppedMessage[1];
				for(ICommand lCommand: commands) {
					if (lCommand.getCommand().contentEquals(lChoppedMessage[1].toLowerCase())) {
						lHelpMessage = lCommand.getHelp();
						break;
					}
				}
			}
			else {
				lHelpMessage = "Commands I know about:\n\n";
				for(ICommand lCommand: commands) {
					if (configuration.userHasPermission(lAuthor, lGuild, lCommand)) {
						lHelpMessage += "**" + Commands.CMD_PREFIX + lCommand.getCommand() + " -** " + lCommand.getHelp() + "\n";
					}
				}
			}
		}
		else {
			lHelpMessage = "The dark gods deem you unfit for help.";
		}

		return Pair.of(Boolean.TRUE, lHelpMessage);
	}
}
