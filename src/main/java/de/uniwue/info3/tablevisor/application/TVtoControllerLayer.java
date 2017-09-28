package de.uniwue.info3.tablevisor.application;

import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.message.TVMessage;
import de.uniwue.info3.tablevisor.upperlayer.IUpperLayerEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

public class TVtoControllerLayer extends BaseApplication {
	private static Logger logger = LogManager.getLogger();

	public TVtoControllerLayer() {
		super(null);
	}

	@Override
	public void allToControlPlane(TVMessage tvMessage) {
		Collection<IUpperLayerEndpoint> endpoints = TableVisor.getInstance().getUpperEndpointManager().getEndpoints();
		if (endpoints.isEmpty()) {
			logger.warn("No UpperLayerEndpoint available, cannot forward {} to control plane", tvMessage.getOFMessage().getType());
		}
		else {
			IUpperLayerEndpoint endp = endpoints.iterator().next();
			endp.send(tvMessage);
		}
	}
}