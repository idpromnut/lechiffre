package org.unrecoverable.lechiffre.commands;

import org.apache.commons.lang3.tuple.Pair;

import sx.blah.discord.handle.obj.IMessage;

public interface ICommand {

	String getCommand();

	String getHelp();

	boolean isGuildCommand();

	Pair<BotReply, String> handle(IMessage message);
}
