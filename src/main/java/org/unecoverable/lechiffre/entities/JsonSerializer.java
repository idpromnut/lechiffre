package org.unecoverable.lechiffre.entities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonSerializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonSerializer.class);

	public JsonSerializer() {
	}

	public void saveStats(final GuildStats guildStats, final String statsFilename) {

		ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
//		mapper.registerModule(new LeChiffreStatsModule());
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
		Writer statsWriter = null;
		File statsFile = new File(statsFilename);
		File lTempFile = null;
		try {
			lTempFile = File.createTempFile(statsFilename, "tmp", statsFile.getParentFile());
			statsWriter = new BufferedWriter(new FileWriter(lTempFile));
			mapper.writeValue(statsWriter, guildStats);
			lTempFile.renameTo(statsFile);
		} catch (IOException e) {
			LOGGER.error("could not write stats to {}", statsFilename, e);
			if (lTempFile != null) lTempFile.delete();
		} finally {
			IOUtils.closeQuietly(statsWriter);
		}
	}

	public GuildStats loadStats(final File statsFile) {

		GuildStats guildStats = null;
		if (statsFile.exists() && !statsFile.isDirectory()) {
			Reader statsReader = null;
			try {
				ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
				statsReader = new BufferedReader(new FileReader(statsFile));
				guildStats = mapper.readValue(statsReader, GuildStats.class);
			} catch (IOException e) {
				LOGGER.error("could not load stats from {}", statsFile, e);
			} finally {
				IOUtils.closeQuietly(statsReader);
			}
		}
		return guildStats;
	}

	private static final Pattern PATTERN = Pattern.compile("[^A-Za-z0-9_\\-]");

	private static final int MAX_LENGTH = 127;

	public static String escapeStringAsFilename(String in) {

		StringBuffer sb = new StringBuffer();

		// Apply the regex.
		java.util.regex.Matcher m = PATTERN.matcher(in);

		while (m.find()) {

			// Convert matched character to percent-encoded.
			String replacement = "%" + Integer.toHexString(m.group().charAt(0)).toUpperCase();

			m.appendReplacement(sb, replacement);
		}
		m.appendTail(sb);

		String encoded = sb.toString();

		// Truncate the string.
		int end = Math.min(encoded.length(), MAX_LENGTH);
		return encoded.substring(0, end);
	}

}
