package de.uniwue.info3.tablevisor.application;

import de.uniwue.info3.tablevisor.message.TVMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BaseApplication implements IApplication {
	private IApplication controlPlaneConnector;
	private IApplication dataPlaneConnector;

	private static Logger logger = LogManager.getLogger();

	/**
	 * This constructor automatically sets the dataPlaneConnector
	 * to whichever dataPlaneConnector was previously set
	 * in the provided controlPlaneConnector
	 *
	 * @param controlPlaneConnector The next layer towards the control plane.
	 */
	public BaseApplication(IApplication controlPlaneConnector) {
		if (controlPlaneConnector != null) {
			setSuccessingDataPlaneConnector(controlPlaneConnector.getSuccessingDataPlaneConnector());
		}
		setSuccessingControlPlaneConnector(controlPlaneConnector);
	}

	@Override
	public void allToControlPlane(TVMessage tvMessage) {
		if (tvMessage.getThisMsgType() != null) {
			switch (tvMessage.getThisMsgType()) {
				case PACKET_IN:
					packetInToControlPlane(tvMessage);
					break;
				case FLOW_MOD:
					flowModToControlPlane(tvMessage);
					break;
				case FLOW_REMOVED:
					flowModToControlPlane(tvMessage);
					break;
				case STATS_REPLY:
					switchStatsToControlPlane(tvMessage);
					break;
				case GET_CONFIG_REPLY:
					switchGetConfigToControlPlane(tvMessage);
					break;
				case SET_CONFIG:
					setConfigToControlPlane(tvMessage);
					break;
				case METER_MOD:
					meterModToControlPlane(tvMessage);
					break;
				case TABLE_MOD:
					tableModToControlPlane(tvMessage);
					break;
				case GROUP_MOD:
					groupModToControlPlane(tvMessage);
					break;
				case ROLE_REPLY:
					roleToControlPlane(tvMessage);
					break;
				case FEATURES_REPLY:
					switchFeaturesToControlPlane(tvMessage);
					break;
				case PORT_MOD:
					portModToControlPlane(tvMessage);
					break;
				case BARRIER_REPLY:
					barrierToControlPlane(tvMessage);
					break;
				default:
					miscToControlPlane(tvMessage);
					break;
			}
		}
		else if (tvMessage.isError()) {
			errorToControlPlane(tvMessage);
		}
		else if (tvMessage.isP4()) {
			p4ToControlPlane(tvMessage);
		}
		else {
			logger.warn("Discarded tvMessage due to unknown type: {}", tvMessage.getTypeAsString());
		}
	}

	@Override
	public void allToDataPlane(TVMessage tvMessage) {
		if (tvMessage.getThisMsgType() != null) {
			switch (tvMessage.getThisMsgType()) {
				case PACKET_OUT:
					packetOutToDataPlane(tvMessage);
					break;
				case FLOW_MOD:
					flowModToDataPlane(tvMessage);
					break;
				case STATS_REQUEST:
					switchStatsToDataPlane(tvMessage);
					break;
				case GET_CONFIG_REQUEST:
					switchGetConfigToDataPlane(tvMessage);
					break;
				case SET_CONFIG:
					setConfigToDataPlane(tvMessage);
					break;
				case METER_MOD:
					meterModToDataPlane(tvMessage);
					break;
				case TABLE_MOD:
					tableModToDataPlane(tvMessage);
					break;
				case GROUP_MOD:
					groupModToDataPlane(tvMessage);
					break;
				case ROLE_REQUEST:
					roleToDataPlane(tvMessage);
					break;
				case FEATURES_REQUEST:
					switchFeaturesToDataPlane(tvMessage);
					break;
				case PORT_MOD:
					portModToDataPlane(tvMessage);
					break;
				case BARRIER_REQUEST:
					barrierToDataPlane(tvMessage);
					break;
				default:
					miscToDataPlane(tvMessage);
					break;
			}
		}
		else if (tvMessage.isError()) {
			errorToDataPlane(tvMessage);
		}
		else if (tvMessage.isP4()) {
			p4ToDataPlane(tvMessage);
		}
		else {
			logger.warn("Discarded tvMessage due to unknown type: {}", tvMessage.getTypeAsString());
		}
	}

	@Override
	public void setSuccessingControlPlaneConnector(IApplication controlPlaneConnector) {
		this.controlPlaneConnector = controlPlaneConnector;
		if (controlPlaneConnector != null && controlPlaneConnector.getSuccessingDataPlaneConnector() != this) {
			controlPlaneConnector.setSuccessingDataPlaneConnector(this);
		}
	}

	@Override
	public IApplication getSuccessingControlPlaneConnector() {
		return controlPlaneConnector;
	}

	@Override
	public void setSuccessingDataPlaneConnector(IApplication dataPlaneConnector) {
		this.dataPlaneConnector = dataPlaneConnector;
		if (dataPlaneConnector != null && dataPlaneConnector.getSuccessingControlPlaneConnector() != this) {
			dataPlaneConnector.setSuccessingControlPlaneConnector(this);
		}
	}

	@Override
	public IApplication getSuccessingDataPlaneConnector() {
		return dataPlaneConnector;
	}

	@Override
	public void p4ToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void p4ToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void switchFeaturesToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void switchFeaturesToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void setConfigToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void setConfigToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void switchGetConfigToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void switchGetConfigToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void switchStatsToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void switchStatsToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void tableModToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void tableModToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void flowModToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void flowModToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void groupModToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void groupModToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void portModToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void portModToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void meterModToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void meterModToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void roleToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void roleToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void barrierToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void barrierToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void packetInToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void packetInToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void packetOutToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void packetOutToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void miscToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void miscToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}

	@Override
	public void errorToControlPlane(TVMessage tvMessage) {
		getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
	}

	@Override
	public void errorToDataPlane(TVMessage tvMessage) {
		getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
	}
}