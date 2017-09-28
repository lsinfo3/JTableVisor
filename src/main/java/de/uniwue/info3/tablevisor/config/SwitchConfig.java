package de.uniwue.info3.tablevisor.config;

import java.util.Map;

public class SwitchConfig {
	public int dataplaneId;
	public String ip;
	public String datapathId;

	/**
	 * Our TV Table IDs (Contoller sees them) -> Switch Table IDs
	 */
	public Map<Integer, Integer> tableMap;
	/**
	 * Our TV Dataplane IDs (Contoller sees them) -> Port ID towards that Device
	 */
	public Map<Integer, Integer> portMap;

	// P4 stuff only:
	public String[] tableSpecs;
	public int numberOfPorts;
	public String rteIp;
	public int rtePort;

	private P4Dict p4Dict;

	public P4Dict getP4Dict() {
		if (p4Dict == null) {
			p4Dict = new P4Dict();
			for (String path : tableSpecs) {
				p4Dict.parseP4File(path);
			}
		}
		return p4Dict;
	}
}
