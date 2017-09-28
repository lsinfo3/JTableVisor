package de.uniwue.info3.tablevisor.config;

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
		return sw.portMap.get(port);
	}

	public int getOutPortOfDataplaneId(int fromDataplaneId, int toDataplaneId) {
		SwitchConfig sw = getSwitchConfigById(fromDataplaneId);
		LinkedList<Integer> treffer = new LinkedList<>();
		for (Map.Entry<Integer, Integer> e : sw.portMap.entrySet()) {
			if (e.getValue().equals(toDataplaneId)) {
				treffer.add(e.getKey());
			}
		}
		return treffer.stream().mapToInt(Integer::intValue).min().orElse(-1);
	}

	public int getInPortOfDataplaneId(int fromDataplaneId, int toDataplaneId) {
		SwitchConfig sw = getSwitchConfigById(fromDataplaneId);
		LinkedList<Integer> treffer = new LinkedList<>();
		for (Map.Entry<Integer, Integer> e : sw.portMap.entrySet()) {
			if (e.getValue().equals(toDataplaneId)) {
				treffer.add(e.getKey());
			}
		}
		return treffer.stream().mapToInt(Integer::intValue).max().orElse(-1);
	}

	public int smallestDataplaneId() {
		return getAllSwitches().stream().mapToInt(swc -> swc.dataplaneId).min().orElse(-1);
	}

	public int biggestDataplaneId() {
		return getAllSwitches().stream().mapToInt(swc -> swc.dataplaneId).max().orElse(-1);
	}

	public int nextSmallerDataplaneId(int currentId) {
		return getAllSwitches().stream().mapToInt(swc -> swc.dataplaneId).filter(i -> i < currentId).max().orElse(-1);
	}

	public int nextBiggerDataplaneId(int currentId) {
		return getAllSwitches().stream().mapToInt(swc -> swc.dataplaneId).filter(i -> i > currentId).min().orElse(-1);
	}
}