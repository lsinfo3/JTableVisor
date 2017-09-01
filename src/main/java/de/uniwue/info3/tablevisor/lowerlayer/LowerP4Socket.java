package de.uniwue.info3.tablevisor.lowerlayer;

import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.message.TVMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LowerP4Socket implements ILowerLayerSocket {
	private static final Logger logger = LogManager.getLogger();
	private int dataplaneId;
	private LowerP4Endpoint endpoint;

	public LowerP4Socket(LowerP4Endpoint endpoint, int dataplaneId) {
		this.endpoint = endpoint;
		this.dataplaneId = dataplaneId;

		logger.info("Switch ID '{}' of type '{}' registered", dataplaneId, endpoint.getLowerLayerEndpointConfig().type);

		synchronized (TableVisor.getInstance()) {
			TableVisor.getInstance().notify();
		}
	}

	@Override
	public int getDataplanId() {
		return dataplaneId;
	}

	@Override
	public void send(TVMessage tvMessage) {
		// TODO: JSon File...
		logger.warn("Cannot send '{}' to data plane '{}' - method not implemented", tvMessage.getTypeAsString(), dataplaneId);
	}

	@Override
	public boolean isInitialized() {
		return true;
	}
}
