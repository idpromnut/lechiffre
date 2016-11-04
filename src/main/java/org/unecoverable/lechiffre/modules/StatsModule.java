package org.unecoverable.lechiffre.modules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unecoverable.lechiffre.entities.GuildStats;
import org.unecoverable.lechiffre.entities.User;
import org.unecoverable.lechiffre.entities.UserStats;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.PresenceUpdateEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.StatusChangeEvent;
import sx.blah.discord.handle.impl.events.TypingEvent;
import sx.blah.discord.handle.impl.events.UserJoinEvent;
import sx.blah.discord.handle.impl.events.UserLeaveEvent;
import sx.blah.discord.handle.impl.events.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Presences;
import sx.blah.discord.modules.IModule;

public class StatsModule implements IModule {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatsModule.class);

	private IDiscordClient client;
	private Map<IGuild, GuildStats> guildStatsMap = new HashMap<>();

	public StatsModule() {
	}

	@Override
	public boolean enable(IDiscordClient client) {
		this.client = client;
		return true;
	}

	@Override
	public void disable() {
		this.client = null;
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

	public void trackGuild(final IGuild guild, final GuildStats stats) {
		guildStatsMap.put(guild, stats);
		IDiscordClient lClient = this.client; // do this to make sure the reference we hold is never null, just possibly stale :)
		if (lClient != null) {
			for(IUser lUser: guild.getUsers()) {
				updateUserStats(lUser, lClient);
			}
		}
	}

	public Map<User, UserStats> getUserStats(final IGuild guild) {
		Map<User, UserStats> lUserStatsMap = new HashMap<>();
		GuildStats lGuildStats = guildStatsMap.get(guild);
		if (lGuildStats != null) {
			final Map<String, User> lUserMap = lGuildStats.getUsers();
			final Map<String, UserStats> lStatsMap = lGuildStats.getUserStats();
			for(String lUserId: lUserMap.keySet()) {
				lUserStatsMap.put(lUserMap.get(lUserId), lStatsMap.get(lUserId));
			}
		}
		return lUserStatsMap;
	}

	@EventSubscriber
	public void handleReadyEvent(ReadyEvent event) {
		final IDiscordClient lClient = event.getClient();
		for(IUser lUser: lClient.getUsers()) {
			updateUserStats(lUser, lClient);
		}
	}

	@EventSubscriber
	public void handleMessageReceivedEvent(MessageReceivedEvent event) {
		final IDiscordClient lClient = event.getClient();
		final IMessage lMessage = event.getMessage();
		final IUser lAuthor = lMessage.getAuthor();
		updateUserStats(lAuthor, lClient);
		updateUserMessagesAuthored(lAuthor, lClient);
		updateUserMentions(lAuthor, lMessage, lClient);
	}

	@EventSubscriber
	public void handleUserJoinEvent(UserJoinEvent event) {
		final IUser lUser = event.getUser();
		final IDiscordClient lClient = event.getClient();
		updateUserStats(lUser, lClient);
	}

	@EventSubscriber
	public void handleUserLeaveEvent(UserLeaveEvent event) {
		final IUser lUser = event.getUser();
		final IDiscordClient lClient = event.getClient();
		updateUserStats(lUser, lClient);
	}

	@EventSubscriber
	public void handlePresenceUpdatedEvent(PresenceUpdateEvent event) {
		final IUser lUser = event.getUser();
		final IDiscordClient lClient = event.getClient();
		if (event.getNewPresence() == Presences.ONLINE || event.getNewPresence() == Presences.STREAMING) {
			updateUserStats(lUser, lClient);
		}
	}

	@EventSubscriber
	public void handleStatusChangeEvent(StatusChangeEvent event) {
		final IUser lUser = event.getUser();
		final IDiscordClient lClient = event.getClient();
		updateUserStats(lUser, lClient);
	}

	@EventSubscriber
	public void handleTypingEvent(TypingEvent event) {
		final IUser lUser = event.getUser();
		final IDiscordClient lClient = event.getClient();
		updateUserStats(lUser, lClient);
	}

	@EventSubscriber
	public void handleUserVoiceChannelJoinEvent(UserVoiceChannelJoinEvent event) {
		final IUser lUser = event.getUser();
		final IDiscordClient lClient = event.getClient();
		updateUserStats(lUser, lClient);
	}

	private void updateUserStats(final IUser user, final IDiscordClient client) {
		if (!client.getOurUser().getID().equals(user.getID())) {
			UserStats lUserStats = lookupUserStats(user);
			if (lUserStats != null) {
				LOGGER.debug("Presence updated for {} [{}]", user.getName(), user.getPresence());
				lUserStats.getPresence().activity();
			}
		}
	}

	private UserStats lookupUserStats(final IUser user) {
		UserStats lUserStats = null;
		for(GuildStats lGuildStats: guildStatsMap.values()) {
			if (lGuildStats.isTracked(user.getID())) {
				lUserStats = lGuildStats.getStats(user.getID());
			} else {
				lUserStats = new UserStats();
				User lUser = new User(user.getID(), user.getName(), user.getDiscriminator());
				lGuildStats.addUser(lUser, lUserStats);
				LOGGER.info("{}'s presence is now being tracked", user.getName());
			}
		}
		return lUserStats;
	}

	private int updateUserMessagesAuthored(final IUser user, final IDiscordClient client) {
		int lMessagesAuthored = 0;
		if (!client.getOurUser().getID().equals(user.getID())) {
			UserStats lUserStats = lookupUserStats(user);
			if (lUserStats != null) {
				lMessagesAuthored = lUserStats.getMessagesAuthored().incrementAndGet();
				LOGGER.debug("Authored message count updated for {} [{}]", user.getName(), lMessagesAuthored);
			}
		}
		return lMessagesAuthored;
	}

	private void updateUserMentions(final IUser author, final IMessage message, final IDiscordClient client) {
		if (!client.getOurUser().getID().equals(author.getID())) {
			UserStats lUserStats;
			List<IUser> lMentions = message.getMentions();
			for(IUser lUser: lMentions) {
				for(GuildStats lGuildStats: guildStatsMap.values()) {
					if (lGuildStats.isTracked(lUser.getID())) {
						lUserStats = lGuildStats.getUserStats().get(lUser.getID());
						int lMentionCount = lUserStats.getMentions().incrementAndGet();
						LOGGER.debug("Mention count by other users updated for {} [{}]", lUser.getName(), lMentionCount);

						break;  // only want to update the mentions once per user found in the message
					}
				}
			}
		}
	}
}
