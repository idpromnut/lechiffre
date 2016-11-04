package org.unecoverable.lechiffre.stats;

import java.time.ZonedDateTime;

import lombok.Data;

@Data
public class ActivityStatistic {

	private ZonedDateTime firstSeen;
	private ZonedDateTime lastActivity;
	private HourlyBinnedStatistic online = new HourlyBinnedStatistic();

	public void activity() {
		setLastActivity(ZonedDateTime.now());
		online.mark();
	}
}
