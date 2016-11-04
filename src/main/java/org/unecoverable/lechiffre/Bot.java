package org.unecoverable.lechiffre;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.unecoverable.lechiffre.commands.GetStatsCommand;
import org.unecoverable.lechiffre.commands.HelpCommand;
import org.unecoverable.lechiffre.commands.ICommand;
import org.unecoverable.lechiffre.commands.IStatsCommand;
import org.unecoverable.lechiffre.commands.LastSeenCommand;
import org.unecoverable.lechiffre.commands.LogoutCommand;
import org.unecoverable.lechiffre.commands.SaveStatsCommand;
import org.unecoverable.lechiffre.entities.GuildStats;
import org.unecoverable.lechiffre.entities.JsonSerializer;
import org.unecoverable.lechiffre.modules.CommandModule;
import org.unecoverable.lechiffre.modules.StatsModule;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ChannelCreateEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.modules.Configuration;
import sx.blah.discord.modules.ModuleLoader;
import sx.blah.discord.util.DiscordException;

@Slf4j
public class Bot {

//	private static final String OWNER_CLIENT_ID = "189577713026072576";  // idpromnut#0111

	private static final JsonSerializer JSON_SERIALIZER = new JsonSerializer();

	private static final StatsModule statsModule = new StatsModule();
	private static final CommandModule commandModule = new CommandModule();
	private static final List<ICommand> commands = new LinkedList<>();
	private static org.unecoverable.lechiffre.entities.Configuration configuration;

	public Bot() {

	}

	public static final void main(String[] args) {
		// This functionality is dependent on these options being true
		if (!Configuration.AUTOMATICALLY_ENABLE_MODULES || !Configuration.LOAD_EXTERNAL_MODULES)
			throw new RuntimeException("Invalid configuration!");

		// There needs to be at least 1 argument
		if (args.length != 1)
			throw new IllegalArgumentException("At least 1 argument required (discord token)!");

		// load in configuration
		Reader lConfigReader;
		Yaml readerYaml = new Yaml(new Constructor(org.unecoverable.lechiffre.entities.Configuration.class));
		File lConfigFile = new File("etc/config.yml");
		try {
			lConfigReader = new FileReader(lConfigFile);
			configuration = (org.unecoverable.lechiffre.entities.Configuration) readerYaml.load(lConfigReader);
			log.info("Loaded configuration from {}: {}", lConfigFile, configuration);
		} catch (FileNotFoundException e1) {
			log.error("could not load configuration from {}", lConfigFile, e1);
		}

		// create command list
		commands.add(new GetStatsCommand());
		final SaveStatsCommand lSaveStatsCommand = new SaveStatsCommand();
		commands.add(lSaveStatsCommand);
		commands.add(new LastSeenCommand());
		LogoutCommand lLogoutCommand = new LogoutCommand();
		lLogoutCommand.getPreLogoutCommands().add(lSaveStatsCommand);
		commands.add(lLogoutCommand);
		HelpCommand lHelpCommand = new HelpCommand(commands);
		commands.add(lHelpCommand);
		commandModule.getCommandChain().addAll(commands);
		commandModule.configure(configuration);

		// set up periodic stats saving thread
		final Thread lPeriodicSaveStatsWorker = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						lSaveStatsCommand.handle(null);
						Thread.sleep(TimeUnit.MILLISECONDS.convert(configuration.getStatsSavePeriodMinutes(), TimeUnit.MINUTES));
					} catch (InterruptedException e) {
						// exit
						log.warn("periodic stats saving thread was interrupted and will not exit");
						return;
					} catch (Exception e) {
						log.warn("could not save stats");
					}
				}
			}
		});
		lPeriodicSaveStatsWorker.setName("PeriodicStatsSavingThread");
		lPeriodicSaveStatsWorker.setDaemon(true);
		lPeriodicSaveStatsWorker.start();


		try {
			ClientBuilder builder = new ClientBuilder();
			final IDiscordClient client = builder.withToken("MjQxNjkzNjg0NzkyMjk1NDI0.CvVv2w.fRG9mwTjg7GFImE5oEFYKSos6rg").build();

			// register modules
			ModuleLoader lModuleLoader = new ModuleLoader(client);
			lModuleLoader.loadModule(statsModule);
			lModuleLoader.loadModule(commandModule);

			client.getDispatcher().registerListener((IListener<ReadyEvent>) (ReadyEvent e) -> {
				IUser lBot = e.getClient().getOurUser();
				log.info("Logged in as {}", lBot.getName());

				// add any missing guilds from our stats
				for(IGuild lGuild: e.getClient().getGuilds()) {

					GuildStats lStats;

					lStats = JSON_SERIALIZER.loadStats(new File(configuration.getDataDirectoryPath(), JsonSerializer.escapeStringAsFilename(lGuild.getName()) + "-stats.json"));

					if (lStats != null) {
						log.info("Guild stats for {} have been loaded from disk", lGuild.getName());
					}
					else {
						lStats = new GuildStats();
						log.info("No previous stats found for {}", lGuild.getName());
					}

					statsModule.trackGuild(lGuild, lStats);
					for(ICommand lCommand: commands) {
						if (lCommand instanceof IStatsCommand) {
							((IStatsCommand) lCommand).enableCommands(lGuild, lStats);
						}
					}
					log.info("Member of guild: {}, tracking stats for guild", lGuild.getName());
				}
			});

			client.getDispatcher().registerListener((IListener<ChannelCreateEvent>) (ChannelCreateEvent e) -> {
				if (e.getChannel().isPrivate()) {
					IPrivateChannel lPrivateChannel = (IPrivateChannel) e.getChannel();
					log.info("New DM channel {} with {}", lPrivateChannel.getName(), lPrivateChannel.getRecipient().getName());
				}
				else {
					log.info("New channel {} in {}", e.getChannel().getName(), e.getChannel().getGuild().getName());
				}
			});

			client.login();

			// The modules should handle the rest
		} catch (DiscordException e) {
			log.error("There was an error initializing the client", e);
		}
	}
}
