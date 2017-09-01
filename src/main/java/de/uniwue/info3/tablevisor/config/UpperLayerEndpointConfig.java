package de.uniwue.info3.tablevisor.config;

import de.uniwue.info3.tablevisor.upperlayer.UpperLayerType;

public class UpperLayerEndpointConfig {
	public String name;
	public UpperLayerType type;
	public String ip;
	public int port;
	public long reconnectInterval = 10000L;
}
