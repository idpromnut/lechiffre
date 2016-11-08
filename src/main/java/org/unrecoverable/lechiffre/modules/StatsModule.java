package org.unrecoverable.lechiffre.modules;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unrecoverable.lechiffre.MissingGuildException;
import org.unrecoverable.lechiffre.entities.Channel;
import org.unrecoverable.lechiffre.entities.ChannelStats;
import org.unrecoverable.lechiffre.entities.GuildStats;
import org.unrecoverable.lechiffre.entities.User;
import org.unrecoverable.lechiffre.entities.UserStats;
import org.unrecoverable.lechiffre.stats.HourlyBinnedStatistic;

import lombok.extern.slf4j.Slf4j;
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
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Presences;
import sx.blah.discord.modules.IModule;

/**
 * This statistics module currently tracks the following information:
 *
 * - Users:
 *   - last activity (last message to a public channel, presence/status change)
 *   -
 * @author Chris Matthews
 *
 */
@Slf4j
public class StatsModule implements IModule {

	private IDiscordClient client;
	private Map<IGuild, GuildStats> guildStatsMap = new HashMap<>();
	private ZoneId zone = ZoneOffset.systemDefault();

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
		final IChannel lChannel = lMessage.getChannel();
		final IGuild lGuild = lMessage.getGuild();
		updateUserStats(lAuthor, lClient);
		updateUserMessagesAuthored(lAuthor, lMessage, lClient);
		updateUserMentions(lAuthor, lMessage, lClient);
		if (lGuild != null) {
			updateChannelStats(lChannel, lMessage, lGuild, lClient);
		}
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
				log.debug("Presence updated for {} [{}]", user.getName(), user.getPresence());
				lUserStats.getPresence().activity();
			}
		}
	}

	// TODO: need to fix this so that a user would be added to the /right/ guild. What happens if the same user is in multiple guilds?
	private UserStats lookupUserStats(final IUser user) {
		UserStats lUserStats = null;
		for(GuildStats lGuildStats: guildStatsMap.values()) {
			if (lGuildStats.isTrackedUser(user.getID())) {
				lUserStats = lGuildStats.getUserStats(user.getID());
			} else {
				lUserStats = new UserStats();
				User lUser = new User(user.getID(), user.getName(), user.getDiscriminator());
				lGuildStats.addUser(lUser, lUserStats);
				log.info("{}'s presence is now being tracked", user.getName());
			}
		}
		return lUserStats;
	}

	private int updateUserMessagesAuthored(final IUser user, final IMessage message, final IDiscordClient client) {
		int lMessagesAuthored = 0;
		IChannel lChannel = message.getChannel();
		if (!client.getOurUser().getID().equals(user.getID())) {
			UserStats lUserStats = lookupUserStats(user);
			if (lUserStats != null) {
				lMessagesAuthored = lUserStats.getMessagesAuthored().incrementAndGet();
				HourlyBinnedStatistic lChannelMessageStats = lUserStats.getChannelHourlyBinnedStatById(lChannel.getID());
				lChannelMessageStats.mark(message.getCreationDate().atZone(zone));
				log.debug("Authored message count updated for {} [{}]", user.getName(), lMessagesAuthored);
			}
		}
		return lMessagesAuthored;
	}

	private void updateChannelStats(final IChannel channel, final IMessage message, final IGuild guild, final IDiscordClient client) {
		if (!channel.isPrivate()) {
			try {
				ChannelStats lChannelStats = lookupChannelStats(channel, guild);
				lChannelStats.getMessages().mark(message.getCreationDate().atZone(zone));
				log.debug("Channel message stats updated for {} [{}]", channel.getName(), lChannelStats.getMessages().getBinSum());
			}
			catch(MissingGuildException e) {
				log.error("Could not update channel stats for {}", channel.getName(), e);
			}
		}
	}

	private ChannelStats lookupChannelStats(final IChannel channel, final IGuild guild) {
		ChannelStats lChannelStats = null;
		GuildStats lGuildStats = guildStatsMap.get(guild);

		if (lGuildStats != null) {
			if (lGuildStats.isTrackedChannel(channel.getID())) {
				lChannelStats = lGuildStats.getChannelStats(channel.getID());
			}
			else {
				lChannelStats = new ChannelStats();
				Channel lChannel = new Channel(channel.getID(), channel.getName());
				lGuildStats.addChannel(lChannel, lChannelStats);
			}
			log.info("Added {} to list of tracked channels in {}", channel.getName());
		}
		else {
			throw new MissingGuildException("unknown guild: " + guild);
		}
		return lChannelStats;
	}

	private void updateUserMentions(final IUser author, final IMessage message, final IDiscordClient client) {
		if (!client.getOurUser().getID().equals(author.getID())) {
			UserStats lUserStats;
			List<IUser> lMentions = message.getMentions();
			for(IUser lUser: lMentions) {
				for(GuildStats lGuildStats: guildStatsMap.values()) {
					if (lGuildStats.isTrackedUser(lUser.getID())) {
						lUserStats = lGuildStats.getUserStats().get(lUser.getID());
						int lMentionCount = lUserStats.getMentions().incrementAndGet();
						log.debug("Mention count by other users updated for {} [{}]", lUser.getName(), lMentionCount);

						break;  // only want to update the mentions once per user found in the message
					}
				}
			}
		}
	}
}
