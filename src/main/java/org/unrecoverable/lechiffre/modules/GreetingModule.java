package org.unrecoverable.lechiffre.modules;

import java.util.LinkedList;
import java.util.List;

import org.unrecoverable.lechiffre.commands.TauntCommand;
import org.unrecoverable.lechiffre.entities.Configuration;
import org.unrecoverable.lechiffre.entities.IConfigurable;

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
public class GreetingModule implements IModule, IConfigurable {

	@Setter
	private TauntCommand tauntCommand;
	
	private Configuration configuration;
	
	private List<IUser> greetedUsers = new LinkedList<>();
	
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
		
		if (configuration.isGreetingEnabled() && !lChannel.isPrivate() && !greetedUsers.contains(lAuthor) && tauntCommand != null) {
			greetedUsers.add(lAuthor);
			try {
				lChannel.sendMessage(tauntCommand.taunt(lAuthor));
			} catch (MissingPermissionsException | RateLimitException | DiscordException e) {
				log.warn("could not send greeting for {}", lAuthor.getName());
			}
		}
	}

	@Override
	public void configure(Configuration configuration) {
		this.configuration = configuration;
	}
	
}
