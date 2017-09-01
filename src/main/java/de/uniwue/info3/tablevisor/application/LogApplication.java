package de.uniwue.info3.tablevisor.application;

import de.uniwue.info3.tablevisor.message.TVMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;

public class LogApplication {
	private static final Logger logger = LogManager.getLogger();

	public static class ControllerLogApplication extends BaseApplication {
		public ControllerLogApplication(IApplication controlPlaneConnector) {
			super(controlPlaneConnector);
		}

		public final void allToControlPlane(TVMessage tvMessage) {
			String additional = getStatsType(tvMessage);
			logger.info("[... -> Control]:     {}{}", tvMessage.getTypeAsString(), additional);
			super.allToControlPlane(tvMessage);
		}

		public final void allToDataPlane(TVMessage tvMessage) {
			String additional = getStatsType(tvMessage);
			logger.info("[Control -> ...]: {}{}", tvMessage.getTypeAsString(), additional);
			super.allToDataPlane(tvMessage);
		}

		@Override
		public void miscToDataPlane(TVMessage tvMessage) {
			if (tvMessage.isOpenFlow()) {
				logger.warn("OFMessage type '{}' not recognized (fowarded as 'MISC')", tvMessage.getTypeAsString());
			}
			else {
				logger.warn("Unrecognized message (protocol = '{}')", tvMessage.getProtocol());
			}
			super.miscToDataPlane(tvMessage);
		}
	}

	public static class SwitchLogApplication extends BaseApplication {
		public SwitchLogApplication(IApplication controlPlaneConnector) {
			super(controlPlaneConnector);
		}

		public final void allToControlPlane(TVMessage tvMessage) {
			String additional = getStatsType(tvMessage);
			logger.info("[Data("+tvMessage.getDataplaneId()+") -> ...]: {}{}", tvMessage.getTypeAsString(), additional);
			super.allToControlPlane(tvMessage);
		}

		public final void allToDataPlane(TVMessage tvMessage) {
			String additional = getStatsType(tvMessage);
			logger.info("[... -> Data("+tvMessage.getDataplaneId()+")]:     {}{}", tvMessage.getTypeAsString(), additional);
			super.allToDataPlane(tvMessage);
		}

		@Override
		public void miscToControlPlane(TVMessage tvMessage) {
			if (tvMessage.isOpenFlow()) {
				logger.warn("OFMessage type '{}' not recognized (fowarded as 'MISC')", tvMessage.getTypeAsString());
			}
			else {
				logger.warn("Unrecognized message (protocol = '{}')", tvMessage.getProtocol());
			}
			super.miscToControlPlane(tvMessage);
		}
	}

	private static String getStatsType(TVMessage tvMessage) {
		String add = "";
		if (tvMessage.isOpenFlow()) {
			OFMessage msg = tvMessage.getOFMessage();
			if (msg instanceof OFStatsRequest) {
				add += ((OFStatsRequest) msg).getStatsType();
			}
			else if (msg instanceof OFStatsReply) {
				add += ((OFStatsReply) msg).getStatsType();
			}
		}
		if (!add.isEmpty()) {
			add = " (" + add + ")";
		}
		return add;
	}
}
