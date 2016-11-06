package org.unrecoverable.lechiffre.stats;

import static org.junit.Assert.*;

import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;
import org.unrecoverable.lechiffre.stats.HourlyBinnedStatistic;

public class HourlyBinnedStatisticTest {

	private HourlyBinnedStatistic stat;

	@Before
	public void setUp() throws Exception {
		stat = new HourlyBinnedStatistic();
	}

	@Test
	public void testMarkSingleHour() {
		assertNull(stat.getLastMark());
		stat.mark();
		stat.mark();
		ZonedDateTime lMark = stat.getLastMark();
		int lBin = stat.getBins()[lMark.getHour()];
		assertEquals(1, lBin);
	}

	@Test
	public void testMark() {
		assertNull(stat.getLastMark());
		ZonedDateTime l5AmMark = ZonedDateTime.now().withHour(5);
		ZonedDateTime l7AmMark = ZonedDateTime.now().withHour(7);
		ZonedDateTime lSecond7AmMark = ZonedDateTime.now().withHour(7).plusDays(1);
		stat.mark(l5AmMark);
		assertEquals(l5AmMark, stat.getLastMark());
		stat.mark(l7AmMark);
		assertEquals(l7AmMark, stat.getLastMark());
		assertEquals(1, stat.getBins()[5]);
		assertEquals(1, stat.getBins()[7]);
		stat.mark(lSecond7AmMark);
		assertEquals(lSecond7AmMark, stat.getLastMark());
		assertEquals(1, stat.getBins()[5]);
		assertEquals(2, stat.getBins()[7]);
	}
}
