package org.unrecoverable.lechiffre.stats;

import java.time.ZonedDateTime;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.unecoverable.lechiffre.stats.HourlyBinnedStatistic;

public class HourlyBinnedStatisticTest {

	private HourlyBinnedStatistic stat;

	@Before
	public void setUp() throws Exception {
		stat = new HourlyBinnedStatistic();
	}

	@Test
	public void testMarkSingleHour() {
		Assert.assertNull(stat.getLastMark());
		stat.mark();
		stat.mark();
		ZonedDateTime lMark = stat.getLastMark();
		int lBin = stat.getBins()[lMark.getHour()];
		Assert.assertEquals(1, lBin);
	}

	@Test
	public void testMark() {
		Assert.assertNull(stat.getLastMark());
		ZonedDateTime l5AmMark = ZonedDateTime.now().withHour(5);
		ZonedDateTime l7AmMark = ZonedDateTime.now().withHour(7);
		ZonedDateTime lSecond7AmMark = ZonedDateTime.now().withHour(7).plusDays(1);
		stat.mark(l5AmMark);
		stat.mark(l7AmMark);
		Assert.assertEquals(1, stat.getBins()[5]);
		Assert.assertEquals(1, stat.getBins()[7]);
		stat.mark(lSecond7AmMark);
		Assert.assertEquals(1, stat.getBins()[5]);
		Assert.assertEquals(2, stat.getBins()[7]);
	}

	@Test
	public void testReset() {
		for(int i = 0; i < stat.getNumberOfBins(); i++) {
			stat.getBins()[i] = i;
		}
		stat.reset();
		for(int i = 0; i < stat.getNumberOfBins(); i++) {
			Assert.assertEquals(0, stat.getBins()[i]);
		}
	}

}
