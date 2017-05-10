package org.unrecoverable.lechiffre.commands;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.unrecoverable.lechiffre.entities.ChannelStats;
import org.unrecoverable.lechiffre.entities.GuildStats;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

@Slf4j
public abstract class ChannelStatsCommand extends BaseStatsCommand implements ICommand {

	public ChannelStatsCommand() {
	}

	@Override
	public boolean isGuildCommand() {
		return true;
	}

	@Override
	public Pair<BotReply, String> handle(IMessage message) {

		final String lRequestedChannelName = message.getContent().replace(Commands.getCommandPrefix() + getCommand(), "");
		final IUser lAuthor = message.getAuthor();
		final IChannel lSourceChannel = message.getChannel();
		final IGuild lSourceGuild = lSourceChannel.getGuild();
		final IDiscordClient lClient = message.getClient();
		IPrivateChannel lDmChannel = null;
		BotReply lBotReply = BotReply.NONE;
		String lReplyMessage = null;
		
		try {
			lDmChannel = lClient.getOrCreatePMChannel(lAuthor);
		} catch (DiscordException | RateLimitException e) {
			log.error("Could not get DM channel for user {}", lAuthor);
		}

		if (lDmChannel != null) {
			// was a name of some sort provided?
			if (StringUtils.isNotBlank(lRequestedChannelName)) {
				
				log.trace("{} asks for channel stats for {} (if missing from your config", lAuthor.getName(), lRequestedChannelName);

				// prep message if we didn't find anything in our stats object
				lBotReply = BotReply.PM;
				lReplyMessage = "No channel found with the name '" + lRequestedChannelName + "'. RIP.";

				for(IChannel lChannel: getChannels(lClient)) {
					if (lChannel.getName().startsWith(lRequestedChannelName)) {
						ChannelStats lChannelStats = findChannelStats(lChannel.getID());
						if (lChannelStats != null) {
							publishChannelMessageActivityChart(lChannel, lChannelStats, lDmChannel);
						}
						else {
							log.debug("No stats for channel {} found for user {}'s request", lRequestedChannelName, lAuthor.getName());
						}
						lBotReply = BotReply.NONE;
						lReplyMessage = null;
						break;
					}
				}
			}
			// no name was provided, so try and return all channel stats for the guild that the message originated from
			else if (lSourceGuild != null) {
				Map<IChannel, ChannelStats> lChannelStatsMap = new HashMap<>();
				GuildStats lGuildStats = getGuildStatsMap().get(lSourceGuild);
				for(IChannel lChannel: getChannelsForGuild(lClient, lGuildStats)) {
					lChannelStatsMap.put(lChannel, lGuildStats.getChannelStats(lChannel.getID()));
				}
				publishChannelsMessageActivityChart(lChannelStatsMap, lSourceGuild.getName(), lDmChannel);
			}
			else {
				lBotReply = BotReply.PM;
				lReplyMessage = "I could not understand your request.";
			}
		}

		return Pair.of(lBotReply, lReplyMessage);
	}

	abstract protected List<IChannel> getChannels(final IDiscordClient client);

	protected List<IChannel> getChannelsForGuild(IDiscordClient client, GuildStats guildStats) {

		List<IChannel> lGuildChannels = new ArrayList<>();
		for(IChannel lChannel: getChannels(client)) {
			if (guildStats.isTrackedChannel(lChannel.getID())) {
				lGuildChannels.add(lChannel);
			}
		}
		return lGuildChannels;
	}
	
	protected void publishChannelMessageActivityChart(final IChannel channel, final ChannelStats channelStats, final IChannel targetReplyChannel) {
	
		Map<IChannel, ChannelStats> lSingleChannelStats = new HashMap<>();
		lSingleChannelStats.put(channel, channelStats);
		publishChannelsMessageActivityChart(lSingleChannelStats, channel.getName(), targetReplyChannel);
	}
	
	protected void publishChannelsMessageActivityChart(final Map<IChannel, ChannelStats> channels, final String name, final IChannel targetReplyChannel) {

		DefaultCategoryDataset lDataSet = new DefaultCategoryDataset();
		
		for(Map.Entry<IChannel, ChannelStats> lStats: channels.entrySet()) {
			addChannelMessageStatsToDataSet(lStats.getKey(), lStats.getValue(), lDataSet);
		}
		
		JFreeChart lChart = ChartFactory.createAreaChart(
				"Activity Histogram for " + name,
				"Time of Day",
				"",
				lDataSet, PlotOrientation.VERTICAL, (channels.size() > 1), true, false);

		
		lChart.setBackgroundPaint(Color.WHITE);
		lChart.getPlot().setBackgroundPaint(Color.WHITE);

		sendChartToChannel(lChart, targetReplyChannel, name + "-channel-messages-chart.png");
	}
	
	protected void addChannelMessageStatsToDataSet(final IChannel channel, final ChannelStats channelStats, final DefaultCategoryDataset dataSet) {
		
		final DateTimeFormatter lActiveHourFormatter = new DateTimeFormatterBuilder()
				.appendPattern("h a")
				.toFormatter();
		LocalTime lTime;
		int[] lHourlyBins = channelStats.getMessages().getBins();
		for(int i = 0; i < channelStats.getMessages().getNumberOfBins(); ++i) {
			lTime = LocalTime.MIDNIGHT.plusHours(i);
			dataSet.addValue(lHourlyBins[i], channel.getName(), lActiveHourFormatter.format(lTime));
		}
		
	}

	protected void sendChartToChannel(final JFreeChart chart, final IChannel channel, final String filename) {
		try {
			ByteArrayInputStream lChartData = new ByteArrayInputStream(ChartUtilities.encodeAsPNG(chart.createBufferedImage(1600, 800)));
			channel.sendFile(lChartData, filename);
		} catch (IOException | MissingPermissionsException | RateLimitException | DiscordException e ) {
			log.error("could not generate chart", e);
		}
	}
}
