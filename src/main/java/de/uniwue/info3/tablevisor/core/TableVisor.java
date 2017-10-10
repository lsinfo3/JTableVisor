package de.uniwue.info3.tablevisor.core;

import de.uniwue.info3.tablevisor.application.IApplication;
import de.uniwue.info3.tablevisor.application.TVtoControllerLayer;
import de.uniwue.info3.tablevisor.application.TVtoDataLayer;
import de.uniwue.info3.tablevisor.config.*;
import de.uniwue.info3.tablevisor.lowerlayer.ILowerLayerEndpoint;
import de.uniwue.info3.tablevisor.lowerlayer.LowerLayerEndpointManager;
import de.uniwue.info3.tablevisor.lowerlayer.LowerLayerType;
import de.uniwue.info3.tablevisor.upperlayer.UpperLayerEndpointManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.types.TableId;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TableVisor {
	public static final String VERSION = "3.0.0";
	private static TableVisor INSTANCE;
	private static final Logger logger = LogManager.getLogger();

	private final Configuration config;
	private boolean shutdown = false;
	private UpperLayerEndpointManager upperEndpointManager;
	private LowerLayerEndpointManager lowerEndpointManager;
	private TVtoControllerLayer tvToControllerLayer;
	private TVtoDataLayer tvToDataLayer;

	private HashMap<Integer, IdPair> ourTableIdsToSwitchIds;
	private HashMap<IdPair, Integer[]> switchIdsToOurTableIds;
	private int maxTableId = -1;
	private Path configFile;

	public static TableVisor getInstance() {
		return INSTANCE;
	}

	public boolean isShutdown() {
		return shutdown;
	}

	public TableVisor(Path configFile) throws IOException {
		if (INSTANCE != null) {
			throw new IllegalStateException("Duplicate initialization.");
		}
		INSTANCE = this;

		logger.info("Using configuration file {}", configFile.toAbsolutePath().toString());
		this.configFile = configFile;
		config = ConfigurationParser.parseYamlFile(configFile);
		populateIdMaps();
	}

	public Path getConfigFile() {
		return configFile;
	}

	public void start() {
		logger.info("Starting TableVisor");

		if (config == null) {
			logger.error("No configuration file set");
			return;
		}

		tvToControllerLayer = new TVtoControllerLayer();
		tvToDataLayer = new TVtoDataLayer(tvToControllerLayer);

		if (config.applications == null || config.applications.isEmpty()) {
			logger.warn("No applications enabled, only core modules instantiated");
		}
		else {
			IApplication previous = tvToControllerLayer;
			for (ApplicationType t : ApplicationType.values()) {
				if (config.applications.contains(t)) {
					logger.info("Initializing {}...", t);

					try {
						Constructor<IApplication> constr = t.c.getConstructor(IApplication.class);
						previous = constr.newInstance(previous);
					}
					catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
						logger.error("Could not initialize {}", t, e);
					}
				}
			}
		}

		lowerEndpointManager = new LowerLayerEndpointManager();
		upperEndpointManager = new UpperLayerEndpointManager();

		logger.info("Waiting for all ({}) switches to connect...", config.getTotalNumberOfSwitches());
		lowerEndpointManager.initialize(config.lowerLayerEndpoints);
		synchronized (this) {
			while (!lowerEndpointManager.allInitialized()) {
				try {
					this.wait();
				}
				catch (InterruptedException e) {
					// Nothing.
				}
			}
		}
		logger.info("All switches connected.");
		upperEndpointManager.initialize(config.upperLayerEndpoints);

		synchronized (this) {
			while (!shutdown) {
				try {
					this.wait();
				}
				catch (InterruptedException e) {
					// Nothing. Exit?
				}
			}
		}

		logger.info("Exiting TableVisor");
	}

	public UpperLayerEndpointManager getUpperEndpointManager() {
		return upperEndpointManager;
	}

	public LowerLayerEndpointManager getLowerEndpointManager() {
		return lowerEndpointManager;
	}

	public TVtoControllerLayer getTvToControllerLayer() {
		return tvToControllerLayer;
	}

	public TVtoDataLayer getTvToDataLayer() {
		return tvToDataLayer;
	}

	public Configuration getConfig() {
		return config;
	}

	public LowerLayerType getLowerEndpointTypeById(int dataplaneId) {
		ILowerLayerEndpoint endp = getLowerEndpointById(dataplaneId);
		return (endp == null ? null : getLowerEndpointById(dataplaneId).getLowerLayerEndpointConfig().type);
	}

	public ILowerLayerEndpoint getLowerEndpointById(int dataplaneId) {
		for (ILowerLayerEndpoint ep : lowerEndpointManager.getAllEndpoints()) {
			for (SwitchConfig sw : ep.getLowerLayerEndpointConfig().switches) {
				if (sw.dataplaneId == dataplaneId) {
					return ep;
				}
			}
		}
		return null;
	}

	public String getDatapathFromDataplaneId(int dataplaneId) {
		return getLowerEndpointManager().getDataplaneToDatapathId().get(dataplaneId);
	}

	public int getDataplaneIdFromDatapathId(String datapathId) {
		return getLowerEndpointManager().getDatapathToDataplaneId().get(datapathId);
	}

	public IdPair ourTableIdToSwitchId(int tableId) {
		IdPair ret = ourTableIdsToSwitchIds.get(tableId);
		if (ret == null) {
			return new IdPair(-1, 0);
		}
		return ourTableIdsToSwitchIds.get(tableId);
	}

	public IdPair ourTableIdToSwitchId(TableId tableId) {
		return ourTableIdToSwitchId(tableId.getValue());
	}

	public Integer[] switchIdToOurTableId(IdPair pair) {
		return switchIdsToOurTableIds.get(pair);
	}

	public Integer[] switchIdToOurTableId(int dataplaneId, int tableId) {
		return switchIdToOurTableId(new IdPair(dataplaneId, tableId));
	}

	public Integer[] switchIdToOurTableId(int dataplaneId, TableId tableId) {
		return switchIdToOurTableId(new IdPair(dataplaneId, tableId));
	}

	private void populateIdMaps() {
		ourTableIdsToSwitchIds = new HashMap<>();
		switchIdsToOurTableIds = new HashMap<>();
		int maxTable = maxTableId();

		for (LowerLayerEndpointConfig lec : config.lowerLayerEndpoints) {
			for (SwitchConfig swc : lec.switches) {
				if (lec.type == LowerLayerType.P4_NETRONOME) {
					swc.getP4Dict();
				}

				for (Map.Entry<Integer, Integer> e : swc.tableMap.entrySet()) {
					IdPair pair = new IdPair(swc.dataplaneId, e.getValue());

					if (ourTableIdsToSwitchIds.containsKey(e.getKey())) {
						throw new IllegalStateException("Table ID " + e.getKey() + " used twice");
					}
					ourTableIdsToSwitchIds.put(e.getKey(), pair);

					Integer[] ourTableIds = switchIdsToOurTableIds.get(pair);
					if (ourTableIds == null) {
						ourTableIds = new Integer[]{e.getKey()};
					}
					else {
						if (pair.dataplaneId != getConfig().smallestDataplaneId()){
							throw new IllegalStateException("Dataplane Table ID " + pair.tableId + " used twice in Switch: "+pair.dataplaneId);
						}
						if (pair.tableId != 0) {
							throw new IllegalStateException("Only dataplane table ID 0 may be used twice, but was: " + pair.tableId);
						}
						if (e.getKey() != maxTable && e.getKey() != 0) {
							throw new IllegalStateException("Duplicate control plane pointer towards the same table: only allowed for lowest (0) and highest ("+maxTable+"), but was used with: " + e.getKey());
						}
						ourTableIds = Arrays.copyOf(ourTableIds, ourTableIds.length + 1);
						ourTableIds[ourTableIds.length-1] = e.getKey();

					}
					switchIdsToOurTableIds.put(pair, ourTableIds);
				}
			}
		}
	}

	public int maxTableId() {
		if (maxTableId == -1) {
			maxTableId = config.getAllSwitches().stream()
					.flatMap(s -> s.tableMap.keySet().stream())
					.mapToInt(Integer::intValue)
					.max().orElse(-1);
		}
		return maxTableId;
	}
}