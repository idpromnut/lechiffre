package org.unrecoverable.lechiffre.commands;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.lang3.tuple.Pair;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.obj.IMessage;

@Slf4j
public class AdminCommand implements ICommand {

	private static final String RECONNECT = "reconnect";
	private static final String SHUTDOWN = "shutdown";
	
	private OptionParser parser;
	
	public AdminCommand() {
		parser = new OptionParser();
		parser.accepts(RECONNECT, "instructs the bot to disconnect and then connect to Discord's servers");
		parser.accepts(SHUTDOWN, "instructs the bot to save all data, diconnect and exit");
	}
	
	@Override
	public String getCommand() {
		return Commands.CMD_ADMIN;
	}

	@Override
	public String getHelp() {
		StringWriter helpOut = new StringWriter();
		try {
			parser.printHelpOn(helpOut);
		} catch (IOException e) {
			// ignore
			log.error("could not dump help for AdminCommand", e);
		}
		return helpOut.getBuffer().toString();
	}

	@Override
	public boolean isGuildCommand() {
		return false;
	}

	@Override
	public Pair<BotReply, String> handle(IMessage message) {
		
		final String content = message.getContent();
		OptionSet options = parser.parse(content);
		String reply = "Executed.";
		
		if (options.has(RECONNECT)) {
			reply = "Reconnecting...";
		}
		else if (options.has(SHUTDOWN)) {
			reply = "Shutting down...";
		}
		log.debug(reply);
		
		return Pair.of(BotReply.PM, reply);
	}

}
