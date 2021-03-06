package org.unrecoverable.lechiffre.commands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.unrecoverable.lechiffre.entities.User;
import org.unrecoverable.lechiffre.entities.UserStats;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

@Slf4j
public class UserStatsCommand extends BaseStatsCommand implements ICommand {

	public UserStatsCommand() {
	}

	@Override
	public String getCommand() {
		return Commands.CMD_STATS;
	}

	@Override
	public String getHelp() {
		return "returns stats on a user (ex: !stats joe). PMing the command to me will result in extra info being PM'd back to you.";
	}

	@Override
	public boolean isGuildCommand() {
		return false;
	}

	@Override
	public Pair<BotReply, String> handle(IMessage message) {

		final String lContent = message.getContent();
		final String[] lChoppedContent = StringUtils.split(lContent, " ");

		// there is a user name present, return the stats for that user only
		if (lChoppedContent.length >= 2) {

			String searchString = StringUtils.join(ArrayUtils.subarray(lChoppedContent, 1, lChoppedContent.length), " ");
			List<User> lUsers = searchForUser(searchString);
			if (message.getChannel().isPrivate() && lUsers.size() == 1) {
				pushUserActivityChart(lUsers.get(0), findUserStats(lUsers.get(0)), message.getChannel());
			}
			return Pair.of(BotReply.PM, getStatsForUser(searchString));
		}

		return Pair.of(BotReply.NONE, null);
	}

	public String getStatsForUser(String username) {
		String lUserStatsString = "I don't know anything about " + username;
		List<User> lUsers = searchForUser(username);
		if (lUsers.size() == 1) {
			UserStats lStats = findUserStats(lUsers.get(0));
			if (lStats != null) {
				lUserStatsString = "__**" + lUsers.get(0).getName() + "**__:\n" + lStats.getFormattedStats() + "\n";
			}
		}
		return lUserStatsString;
	}

	protected void pushUserActivityChart(final User user, final UserStats userStats, final IChannel channel) {
		final DateTimeFormatter lActiveHourFormatter = new DateTimeFormatterBuilder()
				.appendPattern("h a")
				.toFormatter();
		LocalTime lTime;

		DefaultCategoryDataset lDataSet = new DefaultCategoryDataset();
		int[] lHourlyBins = userStats.getPresence().getOnline().getBins();
		for(int i = 0; i < userStats.getPresence().getOnline().getNumberOfBins(); ++i) {
			lTime = LocalTime.MIDNIGHT.plusHours(i);
			lDataSet.addValue(lHourlyBins[i], "Activity", lActiveHourFormatter.format(lTime));
		}

		JFreeChart lChart = ChartFactory.createBarChart(
				"User Activity Histogram for " + user.getName(),
				"Time of Day",
				"",
				lDataSet, PlotOrientation.VERTICAL, false, true, false);

		try {
			ByteArrayInputStream lChartData = new ByteArrayInputStream(ChartUtilities.encodeAsPNG(lChart.createBufferedImage(1600, 800)));
			channel.sendFile("activities-chart.png", lChartData, "activities-chart.png");
		} catch (IOException | MissingPermissionsException | RateLimitException | DiscordException e ) {
			log.error("could not generate chart", e);
		}
	}

}
