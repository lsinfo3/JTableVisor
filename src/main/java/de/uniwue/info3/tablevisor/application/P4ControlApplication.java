package de.uniwue.info3.tablevisor.application;

import de.uniwue.info3.tablevisor.config.SwitchConfig;
import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.message.TVMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFAuxId;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class P4ControlApplication extends BaseP4Application {
	private static OFFactory fac = OFFactories.getFactory(OFVersion.OF_13);
	private static final Logger logger = LogManager.getLogger();

	public P4ControlApplication(IApplication controlPlaneConnector) {
		super(controlPlaneConnector);
	}

	@Override
	public void switchFeaturesToDataPlane(TVMessage tvMessage) {
		OFFeaturesReply rep = fac.buildFeaturesReply()
				.setDatapathId(DatapathId.of(TableVisor.getInstance().getDatapathFromDataplaneId(tvMessage.getDataplaneId())))
				.setNBuffers(1)
				.setNTables((short) 1)
				.setAuxiliaryId(OFAuxId.MAIN)
				.setCapabilities(Collections.emptySet())
				.setReserved(0)
				.setXid(tvMessage.getOFMessage().getXid())
				.build();
		TVMessage msg = new TVMessage(rep, tvMessage.getDataplaneId());
		getSuccessingControlPlaneConnector().allToControlPlane(msg);
	}

	@Override
	public void switchStatsToDataPlane(TVMessage tvMessage) {
		OFStatsRequest req = tvMessage.getOFMessage();
		OFMessage resp = null;

		switch (req.getStatsType()) {
			case PORT_DESC:
				SwitchConfig swc = TableVisor.getInstance().getConfig().getSwitchConfigById(tvMessage.getDataplaneId());
				ArrayList<OFPortDesc> ports = new ArrayList<>();
				HashSet<OFPortFeatures> features = new HashSet<>();
				features.add(OFPortFeatures.PF_10GB_FD);
				features.add(OFPortFeatures.PF_COPPER);

				// TODO: swc.ports??
				/*for (int i = 0; i < swc.ports; i++) {
					ports.add(fac.buildPortDesc()
							.setPortNo(OFPort.of(i))
							.setName(tvMessage.getDataplaneId() + "_" + i)
							.setHwAddr(MacAddress.of(String.format("%02X:00:00:00:00:%02X", tvMessage.getDataplaneId(), i)))
							.setCurrSpeed(10000000)
							.setMaxSpeed(0)
							.setCurr(features)
							.build());
				}*/
				resp = fac.buildPortDescStatsReply()
						.setEntries(ports)
						.setXid(tvMessage.getOFMessage().getXid())
						.build();
				break;

			case METER_FEATURES:
				OFMeterFeatures meterFeatures = fac.buildMeterFeatures()
						.build();
				resp = fac.buildMeterFeaturesStatsReply()
						.setFeatures(meterFeatures)
						.setXid(tvMessage.getOFMessage().getXid())
						.build();
				break;

			case DESC:
				resp = fac.buildDescStatsReply()
						.setHwDesc(TableVisor.getInstance().getLowerEndpointById(tvMessage.getDataplaneId()).getLowerLayerEndpointConfig().type.toString())
						.setMfrDesc("TableVisor")
						.setSwDesc(TableVisor.VERSION)
						.setXid(tvMessage.getOFMessage().getXid())
						.build();
				break;
		}

		if (resp != null) {
			TVMessage msg = new TVMessage(resp, tvMessage.getDataplaneId());
			getSuccessingControlPlaneConnector().allToControlPlane(msg);
		}
		else {
			logger.warn("No reply constructed for STATS_REQUEST '{}'", req.getStatsType());
		}
	}

	@Override
	public void switchGetConfigToDataPlane(TVMessage tvMessage) {
		OFMessage resp = fac.buildGetConfigReply()
				.setMissSendLen(128)
				.setXid(tvMessage.getOFMessage().getXid())
				.build();
		getSuccessingControlPlaneConnector().allToControlPlane(new TVMessage(resp, tvMessage.getDataplaneId()));
	}

	@Override
	public void setConfigToDataPlane(TVMessage tvMessage) {
		// (ignore this message for now)
	}

	@Override
	public void roleToDataPlane(TVMessage tvMessage) {
		OFMessage resp = fac.buildRoleReply()
				.setRole(OFControllerRole.ROLE_MASTER)
				.setXid(tvMessage.getOFMessage().getXid())
				.build();
		getSuccessingControlPlaneConnector().allToControlPlane(new TVMessage(resp, tvMessage.getDataplaneId()));
	}
}
