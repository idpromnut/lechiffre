package org.unrecoverable.lechiffre.entities;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@JsonInclude
public class GuildStats {

	/** A map of IUser snowflake IDs to User objects (for internal tracking of user details). */
	@Getter
	private Map<String, User> users = new HashMap<>();

	/** A map of IUser snowflake IDs to user stats objects. */
	@Getter
	private Map<String, UserStats> userStats = new HashMap<>();

	@Getter
	private Map<String, Channel> channels = new HashMap<>();

	/** A map of IChannel snowflake IDs to channel stats objects. */
	@Getter
	private Map<String, ChannelStats> channelStats = new HashMap<>();


	public GuildStats() {
	}

	public void addUser(final User user, final UserStats stats) {
		users.put(user.getId(), user);
		this.userStats.put(user.getId(), stats);
	}

	@JsonIgnore
	public UserStats getUserStats(final String userId) {
		return userStats.get(userId);
	}

	public void addChannel(final Channel channel, final ChannelStats channelStats) {
		channels.put(channel.getId(), channel);
		this.channelStats.put(channel.getId(), channelStats);
	}

	@JsonIgnore
	public ChannelStats getChannelStats(final String channelId) {
		return channelStats.get(channelId);
	}

	@JsonIgnore
	public boolean isTrackedUser(final String userId) {
		boolean lTracked = false;
		if (users.containsKey(userId) && userStats.containsKey(userId)) {
			lTracked = true;
		}
		else if (users.containsKey(userId)) {
			users.remove(userId);
		}
		else {
			userStats.remove(userId);
		}
		return lTracked;
	}

	@JsonIgnore
	public boolean isTrackedChannel(final String channelId) {
		boolean lIsGuildChannel = false;
		if (channels.containsKey(channelId) && channelStats.containsKey(channelId)) {
			lIsGuildChannel = true;
		}
		else if (channels.containsKey(channelId)) {
			channels.remove(channelId);
		}
		else {
			channelStats.remove(channelId);
		}
		return lIsGuildChannel;
	}

	@JsonIgnore
	public int totalMessagesPosted() {
		int lTotalGuildMessagesSent = 0;
		for(UserStats lStats: getUserStats().values()) {
			lTotalGuildMessagesSent += lStats.getMessagesAuthored().get();
		}

		return lTotalGuildMessagesSent;
	}

	@JsonIgnore
	public int totalMentions() {
		int lTotalMentions = 0;
		for(UserStats lStats: getUserStats().values()) {
			lTotalMentions += lStats.getMentions().get();
		}

		return lTotalMentions;
	}
}
