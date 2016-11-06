package org.unrecoverable.lechiffre.stats;

import static org.junit.Assert.*;

import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;
import org.unrecoverable.lechiffre.stats.BinnedStatistic;

public class BinnedStatisticTest {

	private static final int BINS = 5;

	private BinnedStatistic stat;

	@Before
	public void setUp() throws Exception {
		stat = new BinnedStatistic(BINS);
	}

	@Test
	public void testMark() {
		for(int i = 0; i < BINS; i++)
			assertEquals(0, stat.getBins()[i]);
		stat.mark(ZonedDateTime.now(), 0);
		assertEquals(1, stat.getBins()[0]);
	}

	@Test
	public void testReset() {
		for(int i = 0; i < BINS; i++)
			stat.getBins()[i] = i;
		stat.reset();
		for(int i = 0; i < BINS; i++)
			assertEquals(0, stat.getBins()[i]);
	}

	@Test
	public void testGetLastMark() {
		ZonedDateTime lTestMark = ZonedDateTime.now().plusHours(2);
		assertNull(stat.getLastMark());
		stat.mark(lTestMark, 0);
		assertEquals(lTestMark, stat.getLastMark());
	}

	@Test
	public void testGetNumberOfBins() {
		assertEquals(BINS, stat.getNumberOfBins());
	}

	@Test
	public void testGetBins() {
		assertNotNull(stat.getBins());
		assertEquals(BINS, stat.getBins().length);
	}

}
