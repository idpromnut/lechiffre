package org.unrecoverable.lechiffre.entities;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.unrecoverable.lechiffre.stats.ActivityStatistic;
import org.unrecoverable.lechiffre.stats.HourlyBinnedStatistic;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

public class UserStats {

	/** Activity statistics for this user. */
	@Getter
	private ActivityStatistic presence = new ActivityStatistic();

	/** The number of messages sent by this user to all channels within a guild. */
	@Getter
	private AtomicInteger messagesAuthored = new AtomicInteger(0);

	/** The number of mentions of this user by other users. */
	@Getter
	private AtomicInteger mentions = new AtomicInteger(0);

	/** A map of IChannel snowflake IDs to hourly binned statistics for the tracked user. */
	@Getter
	private Map<Channel, HourlyBinnedStatistic> messageHistogramByChannel = new HashMap<>();


	public UserStats() {
	}

	@JsonIgnore
	public HourlyBinnedStatistic getChannelHourlyBinnedStatById(String snowflakeId) {
		HourlyBinnedStatistic lStat = null;
		for(Channel lChannel: messageHistogramByChannel.keySet()) {
			if (snowflakeId.equals(lChannel.getId())) {
				lStat = messageHistogramByChannel.get(lChannel);
			}
		}
		return lStat;
	}

	@Override
	public String toString() {
		final DateTimeFormatter lActiveHourFormatter = new DateTimeFormatterBuilder()
				.appendPattern("h a")
				.toFormatter();
		StringBuilder lBuilder = new StringBuilder();
		HourlyBinnedStatistic lOnline = presence.getOnline();
		int lTopHour = 0;
		int lTopHourHits = 0;
		for(int i = 0; i < lOnline.getNumberOfBins(); i++) {
			if (lOnline.getBins()[i] > lTopHourHits) {
				lTopHourHits = lOnline.getBins()[i];
				lTopHour = i;
			}
		}

		LocalTime lTime = LocalTime.MIDNIGHT.plusHours(lTopHour);
		LocalTime lEndTime = LocalTime.MIDNIGHT.plusHours(lTopHour + 1);
		lBuilder.append("most active hour in the day:  **").append(lActiveHourFormatter.format(lTime)).append(" to ").append(lActiveHourFormatter.format(lEndTime)).append("**\n");
		lBuilder.append("messages sent:                         **").append(messagesAuthored.get()).append("**\n");
		lBuilder.append("mentions by others:                 **").append(mentions.get()).append("**\n");
		return lBuilder.toString();
	}
}
