package de.uniwue.info3.tablevisor.config;

import de.uniwue.info3.tablevisor.lowerlayer.LowerLayerType;

import java.util.List;

public class LowerLayerEndpointConfig {
	public String name;
	public String ip;
	public int port;
	public LowerLayerType type;
	public List<SwitchConfig> switches;
}
