package org.unrecoverable.lechiffre.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

@Slf4j
public class TauntCommand extends BaseStatsCommand implements ICommand, IStateful {
	
	private List<String> availableTaunts = new ArrayList<>();
	
	private File tauntsFile = new File("etc", "taunts.txt");
	
	@Override
	public boolean load(File stateDirectory) {
		// Load taunts from taunt file (either in etc/ or form the state directory that is configured)
		BufferedReader lGreetingMessageReader = null;
		File loadableTauntsFile = null;
		try {
			loadableTauntsFile = new File(stateDirectory, tauntsFile.getCanonicalPath());
			if (!loadableTauntsFile.exists()) {
				loadableTauntsFile = tauntsFile;
			}
			log.debug("using taunts.txt file from {}", loadableTauntsFile);
			lGreetingMessageReader = new BufferedReader(new FileReader(tauntsFile));
			availableTaunts.clear();
			while(lGreetingMessageReader.ready()) {
				availableTaunts.add(lGreetingMessageReader.readLine());
			}
			log.info("Loaded greetings from {}", loadableTauntsFile);
		} catch (IOException e1) {
			log.error("could not load greetings from {}", loadableTauntsFile, e1);
		}
		finally {
			IOUtils.closeQuietly(lGreetingMessageReader);
		}

		return true;
	}

	@Override
	public boolean save(File stateDirectory) {
		return true;
	}

	@Override
	public String getCommand() {
		return Commands.CMD_TAUNT;
	}

	@Override
	public String getHelp() {
		return "usage: " + Commands.getCommandPrefix() + Commands.CMD_TAUNT + " <user>  (note that you can only taunt a single user at a time)";
	}

	@Override
	public boolean isGuildCommand() {
		return true;
	}

	@Override
	public Pair<BotReply, String> handle(IMessage message) {
		IChannel channel = message.getChannel();
		
		String[] choppedMessage = message.getContent().split(" ");
		if (choppedMessage.length == 2 && !channel.isPrivate()) {
			List<IUser> foundUsers = searchForUser(choppedMessage[1], channel.getGuild());
			if (foundUsers.size() == 1) {
				return Pair.of(BotReply.CHANNEL, taunt(foundUsers.get(0)));
			}
			else {
				return Pair.of(BotReply.PM, createFoundMultipleMatchingUsersResponse(choppedMessage[1], foundUsers));
			}
		}

		return Pair.of(BotReply.PM, getHelp());
	}
	
	public String taunt(IUser user) {
		String taunt = pickRandomGreeting();
		if (taunt.indexOf("%s") >= 0) {
			taunt = taunt.replaceAll("%s", user.mention());
		}
		else {
			taunt = user.mention() + " " + taunt;
		}
		
		return taunt;
	}
	
	private String pickRandomGreeting() {
		return availableTaunts.get((int)(availableTaunts.size() * Math.random()));
	}
}
