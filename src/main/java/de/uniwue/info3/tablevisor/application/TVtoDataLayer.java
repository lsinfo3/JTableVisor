package de.uniwue.info3.tablevisor.application;

import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.lowerlayer.ILowerLayerSocket;
import de.uniwue.info3.tablevisor.message.TVMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TVtoDataLayer extends BaseApplication {
	private static Logger logger = LogManager.getLogger();

	public TVtoDataLayer(IApplication controlPlaneConnector) {
		super(controlPlaneConnector);
	}

	private void sendToDataPlane(TVMessage tvMessage) {
		ILowerLayerSocket socket = TableVisor.getInstance().getLowerEndpointManager().getSockets().get(tvMessage.getDataplaneId());
		if (socket != null) {
			socket.send(tvMessage);
		}
		else {
			logger.warn("Try to send message {} to unknown dataplane {}. Message discarded", tvMessage.getOFMessage().getType(), tvMessage.getDataplaneId());
		}
	}

	@Override
	public void allToDataPlane(TVMessage tvMessage) {
		sendToDataPlane(tvMessage);
	}
}