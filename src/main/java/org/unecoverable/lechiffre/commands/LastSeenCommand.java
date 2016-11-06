package org.unecoverable.lechiffre.commands;

import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.unecoverable.lechiffre.entities.User;
import org.unecoverable.lechiffre.entities.UserStats;

import lombok.extern.slf4j.Slf4j;
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
	public Pair<Boolean, String> handle(IMessage message) {

		final String lContent = message.getContent();
		final String[] lChoppedContent = StringUtils.split(lContent, " ");
		final String lUsername = lChoppedContent[1];
		String lLastSeenString = "I don't know anything about " + lUsername;
		User lUser = searchForUser(lUsername);
		if (lUser != null) {
			UserStats lStats = findUserStats(lUser);
			if (lStats != null) {
				log.info("Printing stats for user {}", lUsername);
				if (lStats.getPresence().getLastActivity() != null) {
					lLastSeenString = String.format("%s was last seen on %s", lUser.getName(),
							dateTimeFormatter.format(lStats.getPresence().getLastActivity()));
				}
			}
		}
		return Pair.of(Boolean.TRUE, lLastSeenString);
	}

}
