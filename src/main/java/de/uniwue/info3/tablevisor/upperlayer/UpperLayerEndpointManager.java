package de.uniwue.info3.tablevisor.upperlayer;

import de.uniwue.info3.tablevisor.config.UpperLayerEndpointConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class UpperLayerEndpointManager {
	private static final Logger logger = LogManager.getLogger();
	private Collection<IUpperLayerEndpoint> endpoints = new LinkedList<>();

	public void initialize(List<UpperLayerEndpointConfig> endpoints) {
		if (endpoints == null || endpoints.isEmpty()) {
			logger.warn("No UpperLayerEndpoint provided; cannot initialize connection towards the controller");
			return;
		}

		for (UpperLayerEndpointConfig endpointConfig : endpoints) {
			switch (endpointConfig.type) {
				case OPENFLOW:
					UpperLayerOpenFlowEndpoint endpoint = new UpperLayerOpenFlowEndpoint();
					this.endpoints.add(endpoint);
					endpoint.initialize(this, endpointConfig);
					break;
				default:
					logger.warn("Unknown ControlPlane Endpoint type '{}'", endpointConfig.type);
			}
		}
	}

	public Collection<IUpperLayerEndpoint> getEndpoints() {
		return Collections.unmodifiableCollection(endpoints);
	}
}