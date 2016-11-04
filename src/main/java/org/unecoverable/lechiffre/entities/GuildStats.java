package org.unecoverable.lechiffre.entities;

import java.util.HashMap;
import java.util.Map;

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

	public UserStats getStats(final String userId) {
		return userStats.get(userId);
	}

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
}
