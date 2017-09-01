package de.uniwue.info3.tablevisor.config;

import java.util.Map;

public class SwitchConfig {
	public int dataplaneId;
	public String datapathId;
	/**
	 * Our TV Table IDs (Contoller sees them) -> Switch Table IDs
	 */
	public Map<Integer, Integer> tableMap;
	/**
	 * Our TV Dataplane IDs (Contoller sees them) -> Port ID towards that Device
	 */
	public Map<Integer, Integer> portMap;

}
