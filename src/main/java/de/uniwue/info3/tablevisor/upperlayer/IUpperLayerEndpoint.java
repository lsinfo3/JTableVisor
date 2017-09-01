package de.uniwue.info3.tablevisor.upperlayer;

import de.uniwue.info3.tablevisor.config.UpperLayerEndpointConfig;
import de.uniwue.info3.tablevisor.message.TVMessage;

public interface IUpperLayerEndpoint {
	void initialize(UpperLayerEndpointManager endpointManager, UpperLayerEndpointConfig endpointConfig);
	void send(TVMessage msg);
	UpperLayerEndpointManager getUpperLayerEndpointManager();
	UpperLayerEndpointConfig getUpperLayerEndpointConfig();
}
