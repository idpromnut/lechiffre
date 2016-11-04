package org.unecoverable.lechiffre.commands;

import org.unecoverable.lechiffre.entities.GuildStats;

import sx.blah.discord.handle.obj.IGuild;

public interface IStatsCommand {

	void enableCommands(final IGuild guild, final GuildStats stats);

}
