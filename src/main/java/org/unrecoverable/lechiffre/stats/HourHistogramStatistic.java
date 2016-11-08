package org.unrecoverable.lechiffre.stats;

import java.time.ZonedDateTime;

public class HourHistogramStatistic extends BinnedStatistic {

	public HourHistogramStatistic() {
		super(24);
	}

	public void mark(final ZonedDateTime mark) {
		int lHour = mark.getHour();
		mark(mark, lHour);
	}
}
