package de.uniwue.info3.tablevisor.application;

import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.lowerlayer.LowerLayerType;
import de.uniwue.info3.tablevisor.message.TVMessage;

/**
 * Such Apps that change the DataplaneID of Messages should probably inherit from BaseApplication instead!
 */
public class BaseP4Application extends BaseApplication {
	public BaseP4Application(IApplication controlPlaneConnector) {
		super(controlPlaneConnector);
	}

	@Override
	public void allToControlPlane(TVMessage tvMessage) {
		if (TableVisor.getInstance().getLowerEndpointTypeById(tvMessage.getDataplaneId()) == LowerLayerType.P4_NETRONOME) {
			super.allToControlPlane(tvMessage);
		}
		else {
			getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
		}
	}

	@Override
	public void allToDataPlane(TVMessage tvMessage) {
		if (TableVisor.getInstance().getLowerEndpointTypeById(tvMessage.getDataplaneId()) == LowerLayerType.P4_NETRONOME) {
			super.allToDataPlane(tvMessage);
		}
		else {
			getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
		}
	}
}
