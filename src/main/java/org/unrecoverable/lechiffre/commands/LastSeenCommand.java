package org.unrecoverable.lechiffre.commands;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.unrecoverable.lechiffre.entities.User;
import org.unrecoverable.lechiffre.entities.UserStats;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

/**
 * This command returns the date+time of the last activity of the queried user. Note that this
 * will not include PMs sent to other users, but will include PMs sent to the bot.
 *
 * @author Chris Matthews
 */
@Slf4j
public class LastSeenCommand extends BaseStatsCommand implements ICommand {

	private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;

	public LastSeenCommand() {
	}

	@Override
	public String getCommand() {
		return Commands.CMD_LAST_SEEN;
	}

	@Override
	public String getHelp() {
		return "returns the last time a user was seen as active on the server (ex: !lastseen moe)";
	}

	@Override
	public boolean isGuildCommand() {
		return false;
	}

	@Override
	public Pair<BotReply, String> handle(IMessage message) {

		final String lContent = message.getContent();
		final IGuild guild = message.getGuild();
		final String[] lChoppedContent = StringUtils.split(lContent, " ");
		String lLastSeenString = "Missing user name! Hint: " + getHelp();
		
		if (lChoppedContent != null && lChoppedContent.length > 1) {
			final String lUsername = lChoppedContent[1];
			lLastSeenString = "I don't know anything about " + lUsername;
			List<User> lUsers = searchForUser(lUsername);
			if (lUsers.size() == 1) {
				User lUser = lUsers.get(0);
				UserStats lStats = findUserStats(lUser);
				if (lStats != null) {
					log.info("Printing stats for user {}", lUsername);
					if (lStats.getPresence().getLastActivity() != null) {
						lLastSeenString = String.format("%s was last seen on %s", getDisplayNameForUser(lUser, guild),
								dateTimeFormatter.format(lStats.getPresence().getLastActivity()));
					}
				}
			}
			else if (lUsers.size() == 0) {
				lLastSeenString = String.format("Could not find any user that looked like %s.", lUsername);
			}
			else { // lUsers.size() > 1
				StringBuilder lMatchedUsers = new StringBuilder();
				for(User lUser: lUsers) {
					lMatchedUsers.append(getDisplayNameForUser(lUser, guild)).append(",");
				}
				lLastSeenString = String.format("I found multiple users that matched %s: %s", lUsername, lMatchedUsers.toString());
			}
		}
		
		return Pair.of(BotReply.CHANNEL, lLastSeenString);
	}

}
