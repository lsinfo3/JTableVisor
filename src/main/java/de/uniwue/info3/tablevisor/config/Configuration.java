package de.uniwue.info3.tablevisor.config;

import de.uniwue.info3.tablevisor.lowerlayer.ILowerLayerEndpoint;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.*;

public class Configuration {
	public String ourDatapathId;
	public List<UpperLayerEndpointConfig> upperLayerEndpoints;
	public List<LowerLayerEndpointConfig> lowerLayerEndpoints;
	public Set<ApplicationType> applications;

	private int totalNumberOfSwitches = -1;
	private short totalNumberOfTables = -1;
	private List<SwitchConfig> allSwitches;
	private HashMap<Integer, SwitchConfig> switchMap;

	public int getTotalNumberOfSwitches() {
		if (totalNumberOfSwitches == -1) {
			totalNumberOfSwitches = lowerLayerEndpoints.stream().mapToInt(e -> e.switches.size()).sum();
		}
		return totalNumberOfSwitches;
	}

	public int getTotalNumberOfTables() {
		if (totalNumberOfTables == -1) {
			totalNumberOfTables = (short) lowerLayerEndpoints.stream()
					.flatMap(c -> c.switches.stream())
					.mapToInt(s -> s.tableMap.size())
					.sum();
		}
		return totalNumberOfTables;
	}

	public DatapathId getOurDatapathId() {
		return DatapathId.of(ourDatapathId);
	}

	public List<SwitchConfig> getAllSwitches() {
		if (allSwitches == null) {
			allSwitches = new ArrayList<>();
			for (LowerLayerEndpointConfig endp : lowerLayerEndpoints) {
				allSwitches.addAll(endp.switches);
			}
		}
		return allSwitches;
	}

	public SwitchConfig getSwitchConfigById(int dataplaneId) {
		if (switchMap == null) {
			switchMap = new HashMap<>();
			for (LowerLayerEndpointConfig epc : lowerLayerEndpoints) {
				for (SwitchConfig sw : epc.switches) {
					switchMap.put(sw.dataplaneId, sw);
				}
			}
		}
		return switchMap.get(dataplaneId);
	}

	public Integer getDatapleIdOfOutputPort(int fromDataplaneId, int port) {
		SwitchConfig sw = getSwitchConfigById(fromDataplaneId);
		for (Map.Entry<Integer, Integer> e : sw.portMap.entrySet()) {
			if (e.getValue().equals(port)) {
				return e.getKey();
			}
		}
		return null;
	}
}