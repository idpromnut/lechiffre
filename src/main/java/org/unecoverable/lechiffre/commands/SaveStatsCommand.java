package org.unecoverable.lechiffre.commands;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.unecoverable.lechiffre.entities.GuildStats;
import org.unecoverable.lechiffre.entities.JsonSerializer;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

@Slf4j
public class SaveStatsCommand extends BaseStatsCommand implements ICommand {

	private JsonSerializer serializer = new JsonSerializer();

	public SaveStatsCommand() {
	}

	@Override
	public String getCommand() {
		return Commands.CMD_SAVE;
	}

	@Override
	public String getHelp() {
		return "saves all stats to a sekret place where it will be safe. Oh so safe... (ex: !save)";
	}

	@Override
	public Pair<Boolean, String> handle(IMessage message) {
		log.info("Saving stats for all known guilds");
		for(Map.Entry<IGuild, GuildStats> lEntry: getGuildStatsMap().entrySet()) {
			serializer.saveStats(lEntry.getValue(), JsonSerializer.escapeStringAsFilename(lEntry.getKey().getName()) + "-stats.json");
			log.info("Guild stats for {} have been written to disk ({} users)", lEntry.getKey().getName(), lEntry.getValue().getUsers().size());
		}
		return Pair.of(Boolean.TRUE, null);
	}

}
