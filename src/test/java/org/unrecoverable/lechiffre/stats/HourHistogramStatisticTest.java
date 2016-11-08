package org.unrecoverable.lechiffre.stats;

import static org.junit.Assert.*;

import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;

public class HourHistogramStatisticTest {

	private HourHistogramStatistic stat;

	@Before
	public void setUp() throws Exception {
		stat = new HourHistogramStatistic();
	}

	@Test
	public void testMarkZonedDateTime() {
		assertNull(stat.getLastMark());
		stat.mark(ZonedDateTime.now());
		assertEquals(1, stat.getBins()[stat.getLastMark().getHour()]);
	}

}
