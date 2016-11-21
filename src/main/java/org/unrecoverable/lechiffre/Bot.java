package org.unrecoverable.lechiffre;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.unrecoverable.lechiffre.commands.UserStatsCommand;
import org.unrecoverable.lechiffre.commands.VoiceChannelStatsCommand;
import org.unrecoverable.lechiffre.commands.TextChannelStatsCommand;
import org.unrecoverable.lechiffre.commands.GuildStatsCommand;
import org.unrecoverable.lechiffre.commands.HelpCommand;
import org.unrecoverable.lechiffre.commands.ICommand;
import org.unrecoverable.lechiffre.commands.IStatsCommand;
import org.unrecoverable.lechiffre.commands.LastSeenCommand;
import org.unrecoverable.lechiffre.commands.LogoutCommand;
import org.unrecoverable.lechiffre.commands.SaveStatsCommand;
import org.unrecoverable.lechiffre.commands.SelfStatsCommand;
import org.unrecoverable.lechiffre.entities.GuildStats;
import org.unrecoverable.lechiffre.entities.JsonSerializer;
import org.unrecoverable.lechiffre.modules.CommandModule;
import org.unrecoverable.lechiffre.modules.StatsModule;
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
	private static org.unrecoverable.lechiffre.entities.Configuration configuration;

	public Bot() {

	}

	public static final void main(String[] args) {
		// This functionality is dependent on these options being true
		if (!Configuration.AUTOMATICALLY_ENABLE_MODULES || !Configuration.LOAD_EXTERNAL_MODULES)
			throw new RuntimeException("Invalid configuration!");

		// load in configuration
		Reader lConfigReader;
		Yaml readerYaml = new Yaml(new Constructor(org.unrecoverable.lechiffre.entities.Configuration.class));
		File lConfigFile = new File("etc/config.yml");
		try {
			lConfigReader = new FileReader(lConfigFile);
			configuration = (org.unrecoverable.lechiffre.entities.Configuration) readerYaml.load(lConfigReader);
			log.info("Loaded configuration from {}: {}", lConfigFile, configuration);
		} catch (FileNotFoundException e1) {
			log.error("could not load configuration from {}", lConfigFile, e1);
			configuration = new org.unrecoverable.lechiffre.entities.Configuration();
		}

		// create command list
		final SaveStatsCommand lSaveStatsCommand = new SaveStatsCommand();
		LogoutCommand lLogoutCommand = new LogoutCommand();
		lLogoutCommand.getPreLogoutCommands().add(lSaveStatsCommand);
		commands.add(lSaveStatsCommand);
		commands.add(lLogoutCommand);
		commands.add(new UserStatsCommand());
		commands.add(new LastSeenCommand());
		commands.add(new GuildStatsCommand());
		commands.add(new SelfStatsCommand());
		commands.add(new TextChannelStatsCommand());
		commands.add(new VoiceChannelStatsCommand());

		// create the help command using all the previously defined commands
		HelpCommand lHelpCommand = new HelpCommand(commands);
		commands.add(lHelpCommand);

		commandModule.getCommandChain().addAll(commands);
		commandModule.configure(configuration);

		// set up periodic stats saving thread
		StatsSaveWorker lStatsSaveWorker = new Bot.StatsSaveWorker(configuration);
		lStatsSaveWorker.start();

		try {
			// try and get token from configuration file
			String lBotToken = null;

			if (args.length > 0) {
				lBotToken = args[0];
				log.info("Using token configured on command line: {}", lBotToken);
			}

			if (StringUtils.isBlank(lBotToken)) {
				lBotToken = configuration.getBotToken();
				if (StringUtils.isBlank(lBotToken)) {
					log.error("No App bot user token found. Set botToken in the config file or provide the token as the first command line parameter");
					System.exit(1);
				}
				else {
					log.info("Using token configured from config file: {}", lBotToken);
				}
			}

			ClientBuilder builder = new ClientBuilder();
			final IDiscordClient client = builder.withToken(lBotToken).build();

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
						log.debug("Guild stats: {}", lStats);
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

	public static class StatsSaveWorker implements Runnable {

		private org.unrecoverable.lechiffre.entities.Configuration configuration;
		private Thread worker;

		public StatsSaveWorker(org.unrecoverable.lechiffre.entities.Configuration configuration) {
			this.configuration = configuration;
		}

		public void start() {
			worker = new Thread(this);
			worker.setName("PeriodicStatsSavingThread");
			worker.setDaemon(true);
			worker.start();
		}

		@Override
		public void run() {
			SaveStatsCommand lSaveStatsCommand = new SaveStatsCommand();
			lSaveStatsCommand.configure(configuration);
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
	}
}
