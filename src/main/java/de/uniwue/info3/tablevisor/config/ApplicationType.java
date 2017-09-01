package de.uniwue.info3.tablevisor.config;

import de.uniwue.info3.tablevisor.application.*;

public enum ApplicationType {
	// Note: the order of this Enum decides the order in which the Apps are called.
	// TOP: TVtoControllerLayer
	ControllerLogApplication(LogApplication.ControllerLogApplication.class),

	OneTransparentSwitchApplication(OneTransparentSwitchApplication.class),
	MultiSwitchApplication(MultiSwitchApplication.class),
	P4ControlApplication(P4ControlApplication.class),

	SwitchLogApplication(LogApplication.SwitchLogApplication.class);
	// BOTTOM: TVtoDataLayer

	// ---

	public final Class c;

	ApplicationType(Class c) {
		this.c = c;
	}
}
