package org.unecoverable.lechiffre.entities;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@JsonInclude
public class GuildStats {

	@Getter
	private Map<String, User> users = new HashMap<>();

	@Getter
	private Map<String, UserStats> userStats = new HashMap<>();

	public GuildStats() {
	}

	public void addUser(final User user, final UserStats stats) {
		users.put(user.getId(), user);
		userStats.put(user.getId(), stats);
	}

	@JsonIgnore
	public UserStats getStats(final String userId) {
		return userStats.get(userId);
	}

	@JsonIgnore
	public boolean isTracked(final String userId) {
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
