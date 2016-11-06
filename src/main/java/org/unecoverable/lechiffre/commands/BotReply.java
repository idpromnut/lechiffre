package org.unecoverable.lechiffre.commands;

public enum BotReply {
	NONE,		// no reply
	PM,			// force the response to be PM'd back to the origin user
	CHANNEL		// use the same channel as the user did, if allowed
}
