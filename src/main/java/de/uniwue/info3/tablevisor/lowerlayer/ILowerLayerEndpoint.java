package de.uniwue.info3.tablevisor.lowerlayer;

import de.uniwue.info3.tablevisor.config.LowerLayerEndpointConfig;

public interface ILowerLayerEndpoint {
	void initialize(LowerLayerEndpointManager endpointManager, LowerLayerEndpointConfig endpointConfig);
	LowerLayerEndpointManager getLowerLayerEndpointManager();
	LowerLayerEndpointConfig getLowerLayerEndpointConfig();
}
