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
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.TypingEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.voice.VoiceChannelCreateEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.impl.events.user.PresenceUpdateEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.modules.IModule;

/**
 * This statistics module currently tracks the following information:
 *
 * - Users:
 *   - last activity (last message to a public channel, presence/status change)
 *   -
 *   
 * TODO:
 *   - May need to delete stats for channels that are deleted (so as to not mix up the stats between same-named channels).
 *   
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
		return "StatsModule";
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

	public void trackGuild(final IGuild guild, final GuildStats stats) {
		guildStatsMap.put(guild, stats);
		for(IUser lUser: guild.getUsers()) {
			if (!isBotUser(lUser)) {
				updateUserStatsForGuild(lUser, guild);
			}
		}
		
		for(IChannel lChannel: guild.getChannels()) {
			if (stats.getChannelStats(lChannel.getStringID()) == null) {
				stats.addChannel(new Channel(lChannel.getStringID(), lChannel.getName(), false), new ChannelStats());
			}
		}
		for(IChannel lChannel: guild.getVoiceChannels()) {
			if (stats.getChannelStats(lChannel.getStringID()) == null) {
				stats.addChannel(new Channel(lChannel.getStringID(), lChannel.getName(), true), new ChannelStats());
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
			updateUserStatsForAllTrackedGuilds(lUser);
		}
	}

	@EventSubscriber
	public void handleMessageReceivedEvent(MessageReceivedEvent event) {
		final IMessage lMessage = event.getMessage();
		final IUser lAuthor = lMessage.getAuthor();
		final IChannel lChannel = lMessage.getChannel();
		final IGuild lGuild = lMessage.getGuild();
		updateUserStatsWithChannel(lAuthor, lChannel);
		updateUserMessagesAuthored(lAuthor, lMessage);
		updateUserMentions(lAuthor, lMessage);
		if (lGuild != null) {
			updateChannelStats(lChannel, lMessage, lGuild);
		}
	}

	@EventSubscriber
	public void handleUserJoinEvent(UserJoinEvent event) {
		final IUser lUser = event.getUser();
		final IGuild lGuild = event.getGuild();
		updateUserStatsForGuild(lUser, lGuild);
	}

	@EventSubscriber
	public void handleUserLeaveEvent(UserLeaveEvent event) {
		final IUser lUser = event.getUser();
		final IGuild lGuild = event.getGuild();
		updateUserStatsForGuild(lUser, lGuild);
	}

	@EventSubscriber
	public void handlePresenceUpdatedEvent(PresenceUpdateEvent event) {
		final IUser lUser = event.getUser();
		if (event != null && event.getNewPresence() != null && 
				(event.getNewPresence().getStatus() == StatusType.ONLINE)) {
			updateUserStatsForAllTrackedGuilds(lUser);
		}
	}

	@EventSubscriber
	public void handleStatusChangeEvent(PresenceUpdateEvent event) {
		final IUser lUser = event.getUser();
		updateUserStatsForAllTrackedGuilds(lUser);
	}

	@EventSubscriber
	public void handleTypingEvent(TypingEvent event) {
		final IUser lUser = event.getUser();
		final IChannel lChannel = event.getChannel();
		updateUserStatsWithChannel(lUser, lChannel);
	}

	@EventSubscriber
	public void handleUserVoiceChannelJoinEvent(UserVoiceChannelJoinEvent event) {
		final IUser lUser = event.getUser();
		final IChannel lChannel = event.getVoiceChannel();
		updateUserStatsWithChannel(lUser, lChannel);
	}
	
	@EventSubscriber
	public void handleChannelCreateEvent(ChannelCreateEvent event) {
		final IChannel lChannel = event.getChannel();
		if (isTrackableChannel(lChannel)) {
			lookupChannelStats(event.getChannel(), event.getChannel().getGuild());
		}
	}
	
	@EventSubscriber
	public void handleVoiceChannelCreateEvent(VoiceChannelCreateEvent event) {
		final IChannel lChannel = event.getVoiceChannel();
		if (isTrackableChannel(lChannel)) {
			lookupChannelStats(event.getVoiceChannel(), event.getVoiceChannel().getGuild());
		}
	}

	private void updateUserStatsWithChannel(final IUser user, final IChannel channel) {
		if (isTrackableActivity(user, channel)) {
			final IGuild lGuild = channel.getGuild();
			updateUserStatsForGuild(user, lGuild);
		}
	}
	
	private boolean isBotUser(final IUser user) {
		return ( user != null && client.getOurUser().getStringID().equals( user.getStringID() ) );
	}
	
	private boolean isTrackableChannel(final IChannel channel) {
		return (channel != null && !channel.isPrivate() && 
				channel.getGuild() != null);
	}
	
	private boolean isTrackableActivity(final IUser user, final IChannel channel) {
		return (!isBotUser(user) && isTrackableChannel(channel));
	}
	
	private void updateUserStatsForAllTrackedGuilds(final IUser user) {
		for(Map.Entry<IGuild, GuildStats> lStatsTuple: guildStatsMap.entrySet()) {
			if (lStatsTuple.getValue().isTrackedUser(user.getStringID())) {
				updateUserStatsForGuild(user, lStatsTuple.getKey());
			}
		}
	}

	private void updateUserStatsForGuild(final IUser user, final IGuild guild) {
		UserStats lUserStats = lookupUserStats(user, guild);
		if (lUserStats != null) {
			log.debug("Presence updated for {} [{}]", user.getName(), user.getPresence());
			lUserStats.getPresence().activity();
		}
	}
	
	// For the moment a user will be tracked independently per guild. It doesn't make sense to have stats for a user from another guild be
	// viewable from this guild
	private UserStats lookupUserStats(final IUser user, final IGuild guild) {
		UserStats lUserStats;
		GuildStats lGuildStats = guildStatsMap.get(guild);
		if (lGuildStats == null) {
			// We have a problem right away as we should have got a guild tracking update.
			throw new MissingGuildException("guild " + guild.getName() + "(" + guild.getStringID() + ") is unknown");
		}
		
		if (lGuildStats.isTrackedUser(user.getStringID())) {
			lUserStats = lGuildStats.getUserStats(user.getStringID());
		}
		else {
			lUserStats = trackUser(user, lGuildStats);
		}
		
		return lUserStats;
	}

	private int updateUserMessagesAuthored(final IUser user, final IMessage message) {
		int lMessagesAuthored = 0;
		IChannel lChannel = message.getChannel();

		if (isTrackableActivity(user, lChannel)) {
			IGuild lGuild = message.getGuild();
			UserStats lUserStats = lookupUserStats(user, lGuild);
			if (lUserStats != null) {
				lMessagesAuthored = lUserStats.getMessagesAuthored().incrementAndGet();
				HourlyBinnedStatistic lChannelMessageStats = lUserStats.getChannelHourlyBinnedStatById(lChannel.getStringID());
				if (lChannelMessageStats == null) {
					lChannelMessageStats = lUserStats.trackChannel(lChannel.getStringID());
				}
				lChannelMessageStats.mark(message.getCreationDate().atZone(zone));
				log.debug("Authored message count updated for {} [{}]", user.getName(), lMessagesAuthored);
			}
		}
		return lMessagesAuthored;
	}

	private void updateChannelStats(final IChannel channel, final IMessage message, final IGuild guild) {
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
			if (lGuildStats.isTrackedChannel(channel.getStringID())) {
				lChannelStats = lGuildStats.getChannelStats(channel.getStringID());
			}
			else {
				lChannelStats = new ChannelStats();
				Channel lChannel = new Channel(channel.getStringID(), channel.getName(), (channel instanceof IVoiceChannel));
				lGuildStats.addChannel(lChannel, lChannelStats);
				log.info("Added {} to list of tracked channels in {}", channel.getName(), guild.getName());
			}
		}
		else {
			throw new MissingGuildException("unknown guild: " + guild);
		}
		return lChannelStats;
	}
	
	private void updateUserMentions(final IUser author, final IMessage message) {
		IChannel lChannel = message.getChannel();
		IGuild lGuild = message.getGuild();
		UserStats lUserStats;
		List<IUser> lMentions = message.getMentions();
		for(IUser lUser: lMentions) {
			if (isTrackableActivity(lUser, lChannel)) {
				lUserStats = lookupUserStats(lUser, lGuild);
				int lMentionCount = lUserStats.getMentions().incrementAndGet();
				log.debug("Mention count by other users updated for {} [{}]", lUser.getName(), lMentionCount);
			}
		}
	}
	
	private UserStats trackUser(IUser user, GuildStats guildStats) {
		UserStats lUserStats = new UserStats();
		User lUser = new User(user.getStringID(), user.getName(), user.getDiscriminator());
		guildStats.addUser(lUser, lUserStats);
		log.info("{}'s presence is now being tracked", user.getName());
		return lUserStats;
	}
}
