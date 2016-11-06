package org.unecoverable.lechiffre.commands;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.unecoverable.lechiffre.entities.Configuration;
import org.unecoverable.lechiffre.entities.GuildStats;
import org.unecoverable.lechiffre.entities.IConfigurable;
import org.unecoverable.lechiffre.entities.JsonSerializer;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

@Slf4j
public class SaveStatsCommand extends BaseStatsCommand implements ICommand, IConfigurable {

	private JsonSerializer serializer = new JsonSerializer();
	private Configuration configuration = null;

	public SaveStatsCommand() {
	}

	@Override
	public void configure(Configuration configuration) {
		this.configuration = configuration;
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
	public boolean isGuildCommand() {
		return false;
	}

	@Override
	public Pair<BotReply, String> handle(IMessage message) {
		if (configuration != null) {
			log.info("Saving stats for all known guilds");
			for(Map.Entry<IGuild, GuildStats> lEntry: getGuildStatsMap().entrySet()) {
				File lStatsFile = new File(configuration.getDataDirectoryPath(), JsonSerializer.escapeStringAsFilename(lEntry.getKey().getName()) + "-stats.json");
				serializer.saveStats(lEntry.getValue(), lStatsFile);
				log.info("Guild stats for {} have been written to {} ({} users)", lEntry.getKey().getName(), lStatsFile.getAbsolutePath(), lEntry.getValue().getUsers().size());
			}
		}
		else {
			log.warn("no configuration set, cannot save stats");
		}
		return Pair.of(BotReply.NONE, null);
	}
}
