package org.unrecoverable.lechiffre.entities;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.concurrent.atomic.AtomicInteger;

import org.unrecoverable.lechiffre.stats.ActivityStatistic;
import org.unrecoverable.lechiffre.stats.HourlyBinnedStatistic;

import lombok.Getter;

public class UserStats {

	@Getter
	private ActivityStatistic presence = new ActivityStatistic();

	@Getter
	private AtomicInteger messagesAuthored = new AtomicInteger(0);

	@Getter
	private AtomicInteger mentions = new AtomicInteger(0);

	public UserStats() {
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
