package de.uniwue.info3.tablevisor.lowerlayer;

import de.uniwue.info3.tablevisor.config.LowerLayerEndpointConfig;
import de.uniwue.info3.tablevisor.core.TableVisor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LowerLayerEndpointManager {
	private Map<Integer, ILowerLayerSocket> sockets = new HashMap<>();
	private Map<Integer, String> dataplaneToDatapathId = new HashMap<>();
	private Map<String, Integer> datapathToDataplaneId = new HashMap<>();
	private List<ILowerLayerEndpoint> allEndpoints = new LinkedList<>();

	public void initialize(List<LowerLayerEndpointConfig> endpoints) {
		for (LowerLayerEndpointConfig endpointConfig : endpoints) {
			ILowerLayerEndpoint endpoint = null;
			switch (endpointConfig.type) {
				case OPENFLOW:
					endpoint = new LowerOpenFlowEndpoint();
					break;
				case P4_NETRONOME:
					endpoint = new LowerP4Endpoint();
					break;
			}
			allEndpoints.add(endpoint);
			endpoint.initialize(this, endpointConfig);
		}
	}

	public Map<Integer, ILowerLayerSocket> getSockets() {
		return sockets;
	}

	public Map<Integer, String> getDataplaneToDatapathId() {
		return dataplaneToDatapathId;
	}

	public Map<String, Integer> getDatapathToDataplaneId() {
		return datapathToDataplaneId;
	}

	public List<ILowerLayerEndpoint> getAllEndpoints() {
		return allEndpoints;
	}

	public boolean allInitialized() {
		if (sockets.size() != TableVisor.getInstance().getConfig().getTotalNumberOfSwitches()) {
			return false;
		}
		for (ILowerLayerSocket socket : sockets.values()) {
			if (!socket.isInitialized()) {
				return false;
			}
		}
		return true;
	}
}