package de.uniwue.info3.tablevisor.application;

import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.lowerlayer.LowerLayerType;
import de.uniwue.info3.tablevisor.message.TVMessage;

/**
 * Such Apps that change the DataplaneID of Messages should probably inherit from BaseApplication instead!
 */
public class BaseOFApplication extends BaseApplication {
	public BaseOFApplication(IApplication controlPlaneConnector) {
		super(controlPlaneConnector);
	}

	@Override
	public void allToControlPlane(TVMessage tvMessage) {
		if (TableVisor.getInstance().getLowerEndpointTypeById(tvMessage.getDataplaneId()) == LowerLayerType.OPENFLOW) {
			super.allToControlPlane(tvMessage);
		}
		else {
			getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
		}
	}

	@Override
	public void allToDataPlane(TVMessage tvMessage) {
		if (TableVisor.getInstance().getLowerEndpointTypeById(tvMessage.getDataplaneId()) == LowerLayerType.OPENFLOW) {
			super.allToDataPlane(tvMessage);
		}
		else {
			getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
		}
	}
}
