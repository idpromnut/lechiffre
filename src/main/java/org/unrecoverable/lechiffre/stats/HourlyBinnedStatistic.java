package org.unrecoverable.lechiffre.stats;

import java.time.ZonedDateTime;

/**
 * This statistic will record at most 1 unique mark per hour per 24 hour period.
 *
 * @author Chris Matthews
 */
public class HourlyBinnedStatistic extends BinnedStatistic {

	public HourlyBinnedStatistic() {
		super(24);
	}

	public void mark() {
		mark(ZonedDateTime.now());
	}

	public void mark(final ZonedDateTime mark) {
		int lHour = mark.getHour();
		if (getLastMark() != null) {
			if (mark.isAfter(getLastMark().plusHours(1))) {
				super.mark(mark, lHour);
			}
			// otherwise the last mark did not occur at least an hour before now, so we have already incremented "this" hour counter
		}
		else {
			super.mark(mark, lHour);
		}
	}

}
