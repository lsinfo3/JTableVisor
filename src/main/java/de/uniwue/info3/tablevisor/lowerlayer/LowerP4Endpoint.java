package de.uniwue.info3.tablevisor.lowerlayer;

import de.uniwue.info3.tablevisor.config.LowerLayerEndpointConfig;
import de.uniwue.info3.tablevisor.config.SwitchConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LowerP4Endpoint implements ILowerLayerEndpoint {
	private static final Logger logger = LogManager.getLogger();
	private LowerLayerEndpointConfig endpointConfig;
	private LowerLayerEndpointManager endpointManager;

	@Override
	public void initialize(LowerLayerEndpointManager endpointManager, LowerLayerEndpointConfig endpointConfig) {
		this.endpointManager = endpointManager;
		this.endpointConfig = endpointConfig;

		for (SwitchConfig endp : endpointConfig.switches) {
			endpointManager.getDataplaneToDatapathId().put(endp.dataplaneId, endp.datapathId);
			endpointManager.getDatapathToDataplaneId().put(endp.datapathId, endp.dataplaneId);
			endpointManager.getSockets().put(endp.dataplaneId, new LowerP4MessageHandler(this, endp.dataplaneId));
		}
	}

	@Override
	public LowerLayerEndpointManager getLowerLayerEndpointManager() {
		return endpointManager;
	}

	@Override
	public LowerLayerEndpointConfig getLowerLayerEndpointConfig() {
		return endpointConfig;
	}
}
