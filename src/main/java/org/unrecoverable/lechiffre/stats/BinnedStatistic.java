package org.unrecoverable.lechiffre.stats;

import java.time.ZonedDateTime;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * This statistic forms the base of a binned statistic. The number of bins is configurable, and it will
 * record the last mark time. The context for the bins is completely up to the user.
 *
 * @author Chris Matthews
 */
public class BinnedStatistic {

	@Getter @Setter
	private ZonedDateTime lastMark = null;

	@Getter @Setter
	private int numberOfBins;

	@Getter @Setter
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

	@JsonIgnore
	public long getBinSum() {
		long lSum = 0;
		for(int i = 0; i < getNumberOfBins(); i++)
			lSum += getBins()[i];
		return lSum;
	}

	@Override
	public String toString() {
		return "BinnedStatistic [lastMark=" + lastMark + ", numberOfBins=" + numberOfBins + ", bins="
				+ Arrays.toString(bins) + "]";
	}
}
