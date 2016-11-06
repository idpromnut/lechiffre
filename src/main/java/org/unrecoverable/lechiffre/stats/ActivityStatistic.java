package org.unrecoverable.lechiffre.stats;

import java.time.ZonedDateTime;

import lombok.Data;

@Data
public class ActivityStatistic {

	private ZonedDateTime firstSeen;
	private ZonedDateTime lastActivity;
	private HourlyBinnedStatistic online = new HourlyBinnedStatistic();

	public void activity() {
		ZonedDateTime lActivityMark = ZonedDateTime.now();
		if (getFirstSeen() == null) setFirstSeen(lActivityMark);
		setLastActivity(lActivityMark);
		getOnline().mark(lActivityMark);
	}
}
