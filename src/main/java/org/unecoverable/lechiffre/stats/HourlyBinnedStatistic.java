package org.unecoverable.lechiffre.stats;

import java.time.ZonedDateTime;

import lombok.Getter;

public class HourlyBinnedStatistic {

	@Getter
	private ZonedDateTime lastMark = null;

	@Getter
	private int numberOfBins = 24;

	@Getter
	private int[] bins;

	public HourlyBinnedStatistic() {
		bins = new int[numberOfBins];
	}

	public void mark() {
		mark(ZonedDateTime.now());
	}

	public void mark(final ZonedDateTime iMark) {
		int hour = iMark.getHour();
		if (lastMark != null) {
			if (iMark.isAfter(lastMark.plusHours(1))) {
				bins[hour] = ++(bins[hour]);
			}
			// otherwise the last mark did not occur at least an hour before now, so we have already incremented "this" hour counter
		}
		else {
			lastMark = iMark;
			bins[hour] = ++(bins[hour]);
		}
	}

	public void reset() {
		for(int i = 0; i < numberOfBins; i++) {
			bins[i] = 0;
		}
	}


}
