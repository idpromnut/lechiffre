package org.unrecoverable.lechiffre.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unrecoverable.lechiffre.commands.Commands;
import org.unrecoverable.lechiffre.commands.ICommand;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NonNull;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;

@JsonInclude
@Data
public class Configuration {

	/**
	 * Bot token that is generated in the Discord App Bot User settings
	 */
	private String botToken;
	
	/**
	 * Set this to get error reports when a problem occurs with lechiffre
	 */
	private String ownerId;

	/**
	 * Channel to publish informational messages to (greetings for example).
	 */
	private String infoChannel;
	
	/**
	 * Map of roles to lists of commands
	 */
	@NonNull
	private Map<String, List<String>> roleToCommandPermissions = new HashMap<>();

	@NonNull
	private String dataDirectoryPath = ".";

	private int statsSavePeriodMinutes = 10;

	private List<String> channelReplyWhitelist = new ArrayList<>();

	public Configuration() {

		// by default @everyone has access to all commands
		roleToCommandPermissions.put("@everyone", Commands.getCommands());
	}

	public boolean userHasPermission(final IUser user, final IGuild guild, final ICommand command) {
		boolean lHasPermission = false;

		if (roleToCommandPermissions != null) {
			List<String> lAllowedCommands;
			for (IRole lRole : user.getRolesForGuild(guild)) {
				if (roleToCommandPermissions.containsKey(lRole.getName())) {
					lAllowedCommands = roleToCommandPermissions.get(lRole.getName());
					if (lAllowedCommands.contains(command.getCommand())) {
						lHasPermission = true;
						break;
					}
				}
			}
		}
		return lHasPermission;
	}
}
