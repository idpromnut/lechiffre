package org.unecoverable.lechiffre.entities;

import java.util.concurrent.atomic.AtomicInteger;

import org.unecoverable.lechiffre.stats.ActivityStatistic;
import org.unecoverable.lechiffre.stats.HourlyBinnedStatistic;

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
		lBuilder.append("presence: ").append(lTopHour).append(",");
		lBuilder.append("messages: ").append(messagesAuthored.get()).append(",");
		lBuilder.append("mentions by others: ").append(mentions.get());
		return lBuilder.toString();
	}
}
