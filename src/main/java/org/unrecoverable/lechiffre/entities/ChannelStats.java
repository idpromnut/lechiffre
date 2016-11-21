package org.unrecoverable.lechiffre.entities;

import org.unrecoverable.lechiffre.stats.HourlyBinnedStatistic;

import lombok.Getter;

public class ChannelStats {

	@Getter
	private HourlyBinnedStatistic messages = new HourlyBinnedStatistic();

	public ChannelStats() {
	}

	@Override
	public String toString() {
		return "ChannelStats [messages=" + messages + "]";
	}
	
}
