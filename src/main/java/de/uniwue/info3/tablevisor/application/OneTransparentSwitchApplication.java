package de.uniwue.info3.tablevisor.application;

import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.message.TVMessage;

import java.util.Set;

public class OneTransparentSwitchApplication extends BaseApplication {
	public OneTransparentSwitchApplication(IApplication controlPlaneConnector) {
		super(controlPlaneConnector);
	}

	@Override
	public void allToDataPlane(TVMessage tvMessage) {
		Set<Integer> allDps = TableVisor.getInstance().getLowerEndpointManager().getSockets().keySet();
		if (!allDps.isEmpty()) {
			int id = allDps.iterator().next();
			tvMessage.setDataplaneId(id);
		}
		super.allToDataPlane(tvMessage);
	}
}
