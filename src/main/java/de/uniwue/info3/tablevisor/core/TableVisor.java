package de.uniwue.info3.tablevisor.core;

import de.uniwue.info3.tablevisor.application.*;
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
	private HashMap<IdPair, Integer> switchIdsToOurTableIds;

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

		logger.info("Using configuration file {}", configFile.toAbsolutePath().toString());
		config = ConfigurationParser.parseYamlFile(configFile);
		populateIdMaps();

		INSTANCE = this;
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

	public IdPair ourTableIdToSwitchId(int tableId) {
		IdPair ret = ourTableIdsToSwitchIds.get(tableId);
		if (ret == null) {
			// TODO: Maybe remove this case and let Java throw an NPE later, so we have a stack trace?
			return new IdPair(-1, 0);
		}
		return ourTableIdsToSwitchIds.get(tableId);
	}

	public IdPair ourTableIdToSwitchId(TableId tableId) {
		return ourTableIdToSwitchId(tableId.getValue());
	}

	public Integer switchIdToOurTableId(IdPair pair) {
		return switchIdsToOurTableIds.get(pair);
	}

	public Integer switchIdToOurTableId(int dataplaneId, int tableId) {
		return switchIdToOurTableId(new IdPair(dataplaneId, tableId));
	}

	public Integer switchIdToOurTableId(int dataplaneId, TableId tableId) {
		return switchIdToOurTableId(new IdPair(dataplaneId, tableId));
	}

	private void populateIdMaps() {
		ourTableIdsToSwitchIds = new HashMap<>();
		switchIdsToOurTableIds = new HashMap<>();

		for (LowerLayerEndpointConfig lec : config.lowerLayerEndpoints) {
			for (SwitchConfig swc : lec.switches) {
				for (Map.Entry<Integer, Integer> e : swc.tableMap.entrySet()) {
					IdPair pair = new IdPair(swc.dataplaneId, e.getValue());

					if (ourTableIdsToSwitchIds.containsKey(e.getKey())) {
						throw new IllegalStateException("Table ID " + e.getKey() + " used twice");
					}
					ourTableIdsToSwitchIds.put(e.getKey(), pair);

					if (switchIdsToOurTableIds.containsKey(pair)) {
						throw new IllegalStateException("Dataplane Table ID " + pair.tableId + " used twice");
					}
					switchIdsToOurTableIds.put(pair, e.getKey());
				}
			}
		}
	}
}
