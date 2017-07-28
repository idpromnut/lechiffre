package org.unrecoverable.lechiffre.modules;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.modules.IModule;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

@Slf4j
public class GreetingModule implements IModule {

	@Getter @Setter
	private List<String> newUserGreetMessages = new ArrayList<>();
	
	private List<IUser> greetedUsers = new LinkedList<>();
	
	public GreetingModule() {
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
		return "GreetingModule";
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

	@EventSubscriber
	public void handleMessage(MessageReceivedEvent event) {
		IMessage lMessage = event.getMessage();
		IChannel lChannel = lMessage.getChannel();
		IUser lAuthor = lMessage.getAuthor();
		
		if (!lChannel.isPrivate() && !greetedUsers.contains(lAuthor)) {
			greetedUsers.add(lAuthor);
			try {
				String lGreet = pickRandomGreeting();
				if (lGreet.indexOf("%s") >= 0) {
					lGreet = lGreet.replaceAll("%s", lAuthor.mention());
				}
				else {
					lGreet = lAuthor.mention() + " " + lGreet;
				}
				lChannel.sendMessage(lGreet);
			} catch (MissingPermissionsException | RateLimitException | DiscordException e) {
				log.warn("could not send greeting for {}", lAuthor.getName());
			}
		}
	}
	
	private String pickRandomGreeting() {
		return newUserGreetMessages.get((int)(newUserGreetMessages.size() * Math.random()));
	}
}
