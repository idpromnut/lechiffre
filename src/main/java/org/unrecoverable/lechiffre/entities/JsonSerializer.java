package org.unrecoverable.lechiffre.entities;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonSerializer {

	public JsonSerializer() {
	}

	public void saveStats(final GuildStats guildStats, final File statsFile) {

		ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
		Writer statsWriter = null;
		File lTempFile = null;
		try {
			lTempFile = new File(statsFile.getAbsolutePath() + ".tmp");
			statsWriter = new BufferedWriter(new FileWriter(lTempFile));
			mapper.writeValue(statsWriter, guildStats);
			if (!statsFile.delete()) {
				log.warn("could not delete old save file {}. New stats have been written to {}", statsFile.getAbsolutePath(), lTempFile.getAbsolutePath());
			}
			else {
				lTempFile.renameTo(statsFile);
			}
		} catch (IOException e) {
			log.error("could not write stats to {}", statsFile, e);
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
				log.error("could not load stats from {}", statsFile, e);
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
