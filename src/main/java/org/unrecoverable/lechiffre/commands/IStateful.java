package org.unrecoverable.lechiffre.commands;

import java.io.File;

public interface IStateful {

	boolean load(File stateDirectory);
	
	boolean save(File stateDirectory);
}
