package org.unrecoverable.lechiffre.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public class BaseCommand {

	protected List<IUser> searchForUser(String searchName, IGuild guild) {
		String lowercaseSearchName = searchName.toLowerCase();
		List<IUser> users = new ArrayList<>();
		for(IUser user: guild.getUsers()) {
			if (user.getDisplayName(guild).toLowerCase().contains(lowercaseSearchName)) {
				users.add(user);
			}
		}
		return users;
	}
	
	
	protected String createFoundMultipleMatchingUsersResponse(String searchString, List<IUser> users) {
		StringBuilder multipleUsersResponse = new StringBuilder("I found multiple users that matched \"").append(searchString).append("\":");
		for(IUser user: users) {
			multipleUsersResponse.append("\n - ").append(user.getName());
		}
		return multipleUsersResponse.toString();
	}
	
	protected String getNickname(IUser user, IGuild guild) {
		if (guild == null) {
			return user.getName();
		}
		else {
			String userName = user.getNicknameForGuild(guild);
			if (StringUtils.isBlank(userName)) userName = user.getDisplayName(guild);
			if (StringUtils.isBlank(userName)) userName = user.getName();
			return userName;
		}
	}
}
