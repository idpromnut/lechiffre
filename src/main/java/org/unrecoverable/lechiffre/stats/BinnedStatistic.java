package org.unrecoverable.lechiffre.stats;

import java.time.ZonedDateTime;

import lombok.Getter;

/**
 * This statistic forms the base of a binned statistic. The number of bins is configurable, and it will
 * record the last mark time. The context for the bins is completely up to the user.
 *
 * @author Chris Matthews
 */
public class BinnedStatistic {

	@Getter
	private ZonedDateTime lastMark = null;

	@Getter
	private int numberOfBins;

	@Getter
	private int[] bins;

	public BinnedStatistic(int bins) {
		numberOfBins = bins;
		this.bins = new int[bins];
	}

	public void mark(ZonedDateTime markDateTime, int binNumber) {
		lastMark = markDateTime;
		bins[binNumber] += 1;
	}

	public void reset() {
		for(int i = 0; i < numberOfBins; i++) {
			bins[i] = 0;
		}
	}
}
