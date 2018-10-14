package org.unrecoverable.lechiffre;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.unrecoverable.lechiffre.commands.UserStatsCommand;
import org.unrecoverable.lechiffre.commands.VoiceChannelStatsCommand;
import org.unrecoverable.lechiffre.commands.TextChannelStatsCommand;
import org.unrecoverable.lechiffre.commands.AdminCommand;
import org.unrecoverable.lechiffre.commands.Commands;
import org.unrecoverable.lechiffre.commands.DiceCommand;
import org.unrecoverable.lechiffre.commands.GuildStatsCommand;
import org.unrecoverable.lechiffre.commands.HelpCommand;
import org.unrecoverable.lechiffre.commands.ICommand;
import org.unrecoverable.lechiffre.commands.IStatsCommand;
import org.unrecoverable.lechiffre.commands.LastSeenCommand;
import org.unrecoverable.lechiffre.commands.LogoutCommand;
import org.unrecoverable.lechiffre.commands.SaveStatsCommand;
import org.unrecoverable.lechiffre.commands.SelfStatsCommand;
import org.unrecoverable.lechiffre.commands.TauntCommand;
import org.unrecoverable.lechiffre.entities.GuildStats;
import org.unrecoverable.lechiffre.entities.JsonSerializer;
import org.unrecoverable.lechiffre.modules.CommandModule;
import org.unrecoverable.lechiffre.modules.GreetingModule;
import org.unrecoverable.lechiffre.modules.StatsModule;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Slf4jReporter.LoggingLevel;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelCreateEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.modules.Configuration;
import sx.blah.discord.modules.IModule;
import sx.blah.discord.modules.ModuleLoader;
import sx.blah.discord.util.DiscordException;

@Slf4j
public class Bot {

	public static final String HOME_DIRECTORY = "home.dir";
	
	public static final String METRIC_REGISTRY_NAME = "bot";

	private static final JsonSerializer JSON_SERIALIZER = new JsonSerializer();
	private static final MetricRegistry metricRegistry = new MetricRegistry();

	private GreetingModule greetsModule = new GreetingModule();
	private StatsModule statsModule = new StatsModule();
	private CommandModule commandModule = new CommandModule();
	private List<ICommand> commands = new LinkedList<>();
	private SaveStatsCommand saveStatsCommand;
	
	@Getter @Setter
	private File homeDirectory = new File(".");
	
	private File configurationDirectory = new File("etc");

	@Setter
	private org.unrecoverable.lechiffre.entities.Configuration configuration;


	public Bot() {

	}

	public static final void main(String[] args) {
		
		// Check if the bot token was passed 
		String lBotToken = null;
		if (args.length > 0) {
			lBotToken = args[0];
			log.info("Using token configured on command line: {}", lBotToken);
		}
		
		Bot lBot = new Bot();
		try {
			if (System.getProperties().containsKey(HOME_DIRECTORY)) {
				File homeDir = new File(System.getProperty(HOME_DIRECTORY));
				lBot.setHomeDirectory(homeDir);
				log.info("Using properties configured home directory for configuration: {}", homeDir.getCanonicalPath());
			}

			lBot.startBot(lBotToken);
		}
		catch (Exception e) {
			log.error("Could not start bot", e);
		}
	}
	
	public void startBot(final String botToken) throws DiscordException {	
		// This functionality is dependent on these options being true
		if (!Configuration.AUTOMATICALLY_ENABLE_MODULES || !Configuration.LOAD_EXTERNAL_MODULES)
			throw new RuntimeException("Invalid configuration!");

		configurationDirectory = new File(homeDirectory, "etc");
		
		// load config
		configuration = loadConfiguration(configurationDirectory, "config.yml");
		String foundBotToken = StringUtils.isNotBlank(botToken) ? botToken : configuration.getBotToken();

		if (StringUtils.isBlank(foundBotToken)) {
			throw new IllegalStateException("no botToken provided (either on the command line or in the configuration file); exiting");
		}
		
		// set data directory to home directory if not set
		if (StringUtils.isBlank(configuration.getDataDirectoryPath())) {
			configuration.setDataDirectoryPath(homeDirectory.getAbsolutePath());
			log.info("Data directory not set in configuration file, setting to home directory: {}", homeDirectory.getAbsolutePath());
		}
		
		// configure command prefix
		if (StringUtils.isNotBlank(configuration.getCommandPrefix())) {
			Commands.setCommandPrefix(configuration.getCommandPrefix());
		}

		configureMetrics(configuration);
		
		// create command list
		saveStatsCommand = new SaveStatsCommand();
		LogoutCommand logoutCommand = new LogoutCommand();
		logoutCommand.getPreLogoutCommands().add(saveStatsCommand);
		commands.add(saveStatsCommand);
		commands.add(logoutCommand);
		commands.add(new UserStatsCommand());
		commands.add(new LastSeenCommand());
		commands.add(new GuildStatsCommand());
		commands.add(new SelfStatsCommand());
		commands.add(new TextChannelStatsCommand());
		commands.add(new VoiceChannelStatsCommand());
		commands.add(new AdminCommand());
		commands.add(new DiceCommand());
		
		// create Taunt command
		TauntCommand tauntCommand = new TauntCommand();
		tauntCommand.load(homeDirectory);
		commands.add(tauntCommand);
		
		// create the help command using all the previously defined commands
		HelpCommand helpCommand = new HelpCommand(commands);
		commands.add(helpCommand);
		commandModule.getCommandChain().addAll(commands);

		greetsModule.configure(configuration);
		greetsModule.setTauntCommand(tauntCommand);
		
		// configure all modules that require it
		commandModule.configure(configuration);
		
		// set up periodic stats saving thread
		StatsSaveWorker statsSaveWorker = new StatsSaveWorker();
		statsSaveWorker.start();

		if (StringUtils.isBlank(foundBotToken)) {
			foundBotToken = configuration.getBotToken();
			if (StringUtils.isBlank(foundBotToken)) {
				log.error("No App bot user token found. Set botToken in the config file or provide the token as the first command line parameter");
				System.exit(1);
			}
			else {
				log.info("Using token configured from config file: {}", foundBotToken);
			}
		}

		ClientBuilder builder = new ClientBuilder();
		final IDiscordClient client = builder.withToken(foundBotToken).build();

		// register modules
		ModuleLoader moduleLoader = new ModuleLoader(client);
		moduleLoader.loadModule(statsModule);
		moduleLoader.loadModule(commandModule);
		moduleLoader.loadModule(greetsModule);
		
		for(IModule lModule: moduleLoader.getLoadedModules()) {
			log.info("Loaded module {} by {}, version {}", lModule.getName(), lModule.getAuthor(), lModule.getVersion());
		}

		client.getDispatcher().registerListener((IListener<ReadyEvent>) (ReadyEvent e) -> {
			IUser bot = e.getClient().getOurUser();
			log.info("Logged in as {}", bot.getName());

			// add any missing guilds from our stats
			for(IGuild guild: e.getClient().getGuilds()) {

				// filter by guild white list
				// if the white list is empty, allow everything
				if (isWhitelistedGuild(guild)) {
					
					log.info("Found guild {}({}) in whitelist (or whitelist empty)", guild.getName(), guild.getStringID());
					GuildStats stats;

					stats = JSON_SERIALIZER.loadStats(new File(configuration.getDataDirectoryPath(), JsonSerializer.escapeStringAsFilename(guild.getName()) + "-stats.json"));

					if (stats != null) {
						log.info("Guild stats for {} have been loaded from disk", guild.getName());
						log.debug("Guild stats: {}", stats);
					}
					else {
						stats = new GuildStats();
						log.info("No previous stats found for {}", guild.getName());
					}

					statsModule.trackGuild(guild, stats);
					for(ICommand command: commands) {
						if (command instanceof IStatsCommand) {
							((IStatsCommand) command).enableCommands(guild, stats);
						}
					}
					log.info("Member of guild: {}, tracking stats for guild", guild.getName());
				}
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
	}
	
	private boolean isWhitelistedGuild(final IGuild guild) {
		boolean allowed = false;
		for(String value: configuration.getGuildWhitelist()) {
			if (value != null && (
				value.equalsIgnoreCase(guild.getStringID()) ||
				value.equalsIgnoreCase(guild.getName()) ) ) {

				allowed = true;
				break;
			}
		}
		return allowed || configuration.getGuildWhitelist().isEmpty();
	}

	private org.unrecoverable.lechiffre.entities.Configuration loadConfiguration(final File directory, final String configurationFileName) {
		
		// load in configuration
		Reader configReader = null;
		org.unrecoverable.lechiffre.entities.Configuration configuration = null;
		Yaml readerYaml = new Yaml(new Constructor(org.unrecoverable.lechiffre.entities.Configuration.class));
		File configFile = new File(directory, configurationFileName);
		try {
			configReader = new FileReader(configFile);
			configuration = (org.unrecoverable.lechiffre.entities.Configuration) readerYaml.load(configReader);
			log.info("Loaded configuration from {}: {}", configFile, configuration);
		} catch (FileNotFoundException e1) {
			log.error("could not load configuration from {}", configFile, e1);
			configuration = new org.unrecoverable.lechiffre.entities.Configuration();
			log.info("Using default configuration: {}", configuration);
		}
		finally {
			IOUtils.closeQuietly(configReader);
		}

		return configuration;
	}
	
	private void configureMetrics(final org.unrecoverable.lechiffre.entities.Configuration configuration) {

		// set up bot metrics, if configured
		SharedMetricRegistries.add(METRIC_REGISTRY_NAME, metricRegistry);
		MBeanServer mBeanServer = MBeanServerFactory.createMBeanServer();
		final JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).registerWith(mBeanServer).inDomain("lechiffre").build();
		jmxReporter.start();
		
		// if a graphite host is configured, setup graphite reporter
		if (StringUtils.isNotBlank(configuration.getGraphiteHost())) {
			startGraphiteReporter(
					configuration.getMetricPrefix(), 
					configuration.getGraphiteHost(), 
					configuration.getGraphitePort(),
					configuration.getGraphiteRate(),
					configuration.getGraphiteRateUnit());
		}
		else {
			startSlf4jReporter(metricRegistry);
			log.info("Reporting of metrics to graphite is not configured; logging metrics to log file.");
		}
		
		// Add JVM Metrics
		if (configuration.isJvmMetricsEnabled()) {
			metricRegistry.registerAll(new MemoryUsageGaugeSet());
			metricRegistry.registerAll(new GarbageCollectorMetricSet());
			metricRegistry.registerAll(new JvmAttributeGaugeSet());
			metricRegistry.registerAll(new ThreadStatesGaugeSet());
			metricRegistry.registerAll(new ClassLoadingGaugeSet());
			metricRegistry.registerAll(new BufferPoolMetricSet(mBeanServer));
			log.info("JVM statistics enabled");
		}
	}
	
	private void startSlf4jReporter(final MetricRegistry metricRegistry) {

		final Slf4jReporter slf4jReporter = Slf4jReporter.forRegistry(metricRegistry)
                .outputTo(log)
                .filter(MetricFilter.ALL)
                .withLoggingLevel(LoggingLevel.INFO)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.SECONDS)
                .build();
		slf4jReporter.start(10, TimeUnit.SECONDS);
	}
	
	private void startGraphiteReporter(final String metricPrefix, final String graphiteHost, final int graphitePort, final int rate, final TimeUnit rateUnit) {

		String foundMetricPrefix = metricPrefix;
		// if there was no prefix configured, use the hostname of this machine.
		if (StringUtils.isBlank(metricPrefix)) {
			try {
				InetAddress localHost = InetAddress.getLocalHost();
				foundMetricPrefix = localHost.getHostName();
			} catch (UnknownHostException e) {
				log.warn("cannot discover local host name; using localhost");
				foundMetricPrefix = "localhost";
			}
		}
		
		log.info("Reporting metrics to {}:{} with the prefix {} at {} {} rate", 
				graphiteHost, 
				graphitePort,
				foundMetricPrefix,
				rate, rateUnit.name());

		final Graphite graphite = new Graphite(
				new InetSocketAddress(
						graphiteHost, 
						graphitePort));
		final GraphiteReporter graphiteReporter = GraphiteReporter.forRegistry(metricRegistry)
		                                                  .prefixedWith(foundMetricPrefix)
		                                                  .convertRatesTo(TimeUnit.SECONDS)
		                                                  .convertDurationsTo(TimeUnit.SECONDS)
		                                                  .filter(MetricFilter.ALL)
		                                                  .build(graphite);
		graphiteReporter.start(rate, rateUnit);
	}

	class StatsSaveWorker implements Runnable {

		private Thread worker;

		public void start() {
			worker = new Thread(this);
			worker.setName("PeriodicStatsSavingThread");
			worker.setDaemon(true);
			worker.start();
		}

		@Override
		public void run() {
			while(true) {
				try {
					saveStatsCommand.handle(null);
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
