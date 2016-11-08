package org.unrecoverable.lechiffre.stats;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ActivityStatisticTest {

	private ActivityStatistic stat;

	@Before
	public void setUp() throws Exception {
		stat = new ActivityStatistic();
	}

	@Test
	public void testActivity() {
		assertNull(stat.getFirstSeen());
		assertNull(stat.getLastActivity());
		stat.activity();
		assertNotNull(stat.getFirstSeen());
		assertNotNull(stat.getLastActivity());
		assertNotNull(stat.getOnline().getLastMark());
		assertEquals(stat.getFirstSeen(), stat.getLastActivity());
		assertEquals(stat.getFirstSeen(), stat.getOnline().getLastMark());
	}

}
