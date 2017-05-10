package org.unrecoverable.lechiffre;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
import org.unrecoverable.lechiffre.commands.Commands;
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

import lombok.Setter;
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
import sx.blah.discord.modules.IModule;
import sx.blah.discord.modules.ModuleLoader;
import sx.blah.discord.util.DiscordException;

@Slf4j
public class Bot {

	public static final String METRIC_REGISTRY_NAME = "bot";

	private static final JsonSerializer JSON_SERIALIZER = new JsonSerializer();
	private static final MetricRegistry metricRegistry = new MetricRegistry();

	private GreetingModule greetsModule = new GreetingModule();
	private StatsModule statsModule = new StatsModule();
	private CommandModule commandModule = new CommandModule();
	private List<ICommand> commands = new LinkedList<>();

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

		// load config
		configuration = loadConfiguration("etc/config.yml");
		String lBotToken = StringUtils.isNotBlank(botToken) ? botToken : configuration.getBotToken();

		if (StringUtils.isBlank(lBotToken)) {
			throw new IllegalStateException("no botToken provided (either on the command line or in the configuration file); exiting");
		}
		
		// configure command prefix
		if (StringUtils.isNotBlank(configuration.getCommandPrefix())) {
			Commands.setCommandPrefix(configuration.getCommandPrefix());
		}

		configureMetrics(configuration);
		
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

		// Load and configure Greeting module
		File lGreetingsFile = new File("etc/welcome.txt");
		BufferedReader lGreetingMessageReader = null;
		try {
			lGreetingMessageReader = new BufferedReader(new FileReader(lGreetingsFile));
			List<String> lGreetings = new ArrayList<>();
			while(lGreetingMessageReader.ready()) {
				lGreetings.add(lGreetingMessageReader.readLine());
			}
			greetsModule.setNewUserGreetMessages(lGreetings);
			log.info("Loaded greetings from {}", lGreetingsFile);
		} catch (IOException e1) {
			log.error("could not load greetings from {}", lGreetingsFile, e1);
		}
		finally {
			IOUtils.closeQuietly(lGreetingMessageReader);
		}
		
		
		// configure all modules that require it
		commandModule.configure(configuration);
		
		// set up periodic stats saving thread
		StatsSaveWorker lStatsSaveWorker = new Bot.StatsSaveWorker(configuration);
		lStatsSaveWorker.start();

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
		lModuleLoader.loadModule(greetsModule);
		
		for(IModule lModule: lModuleLoader.getLoadedModules()) {
			log.info("Loaded module {} by {}, version {}", lModule.getName(), lModule.getAuthor(), lModule.getVersion());
		}

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
	}
	
	private org.unrecoverable.lechiffre.entities.Configuration loadConfiguration(final String configurationFileName) {
		
		// load in configuration
		Reader lConfigReader = null;
		org.unrecoverable.lechiffre.entities.Configuration lConfiguration = null;
		Yaml readerYaml = new Yaml(new Constructor(org.unrecoverable.lechiffre.entities.Configuration.class));
		File lConfigFile = new File(configurationFileName);
		try {
			lConfigReader = new FileReader(lConfigFile);
			lConfiguration = (org.unrecoverable.lechiffre.entities.Configuration) readerYaml.load(lConfigReader);
			log.info("Loaded configuration from {}: {}", lConfigFile, lConfiguration);
		} catch (FileNotFoundException e1) {
			log.error("could not load configuration from {}", lConfigFile, e1);
			lConfiguration = new org.unrecoverable.lechiffre.entities.Configuration();
		}
		finally {
			IOUtils.closeQuietly(lConfigReader);
		}

		return lConfiguration;
	}
	
	private void configureMetrics(final org.unrecoverable.lechiffre.entities.Configuration configuration) {

		// set up bot metrics, if configured
		SharedMetricRegistries.add(METRIC_REGISTRY_NAME, metricRegistry);
		MBeanServer lMBeanServer = MBeanServerFactory.createMBeanServer();
		final JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).registerWith(lMBeanServer).inDomain("lechiffre").build();
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
			metricRegistry.registerAll(new BufferPoolMetricSet(lMBeanServer));
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

		String lMetricPrefix = metricPrefix;
		// if there was no prefix configured, use the hostname of this machine.
		if (StringUtils.isBlank(metricPrefix)) {
			try {
				InetAddress lLocalHost = InetAddress.getLocalHost();
				lMetricPrefix = lLocalHost.getHostName();
			} catch (UnknownHostException e) {
				log.warn("cannot discover local host name; using localhost");
				lMetricPrefix = "localhost";
			}
		}
		
		log.info("Reporting metrics to {}:{} with the prefix {} at {} {} rate", 
				graphiteHost, 
				graphitePort,
				lMetricPrefix,
				rate, rateUnit.name());

		final Graphite graphite = new Graphite(
				new InetSocketAddress(
						graphiteHost, 
						graphitePort));
		final GraphiteReporter graphiteReporter = GraphiteReporter.forRegistry(metricRegistry)
		                                                  .prefixedWith(lMetricPrefix)
		                                                  .convertRatesTo(TimeUnit.SECONDS)
		                                                  .convertDurationsTo(TimeUnit.SECONDS)
		                                                  .filter(MetricFilter.ALL)
		                                                  .build(graphite);
		graphiteReporter.start(rate, rateUnit);
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
