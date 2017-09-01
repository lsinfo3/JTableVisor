package de.uniwue.info3.tablevisor.application;

import de.uniwue.info3.tablevisor.message.TVMessage;

public interface IApplication {
	IApplication getSuccessingControlPlaneConnector();
	IApplication getSuccessingDataPlaneConnector();
	void setSuccessingControlPlaneConnector(IApplication app);
	void setSuccessingDataPlaneConnector(IApplication app);

	void allToControlPlane(TVMessage tvMessage);
	void allToDataPlane(TVMessage tvMessage);

	void p4ToControlPlane(TVMessage tvMessage);
	void p4ToDataPlane(TVMessage tvMessage);

	void switchFeaturesToControlPlane(TVMessage tvMessage);
	void switchFeaturesToDataPlane(TVMessage tvMessage);
	void switchGetConfigToControlPlane(TVMessage tvMessage);
	void switchGetConfigToDataPlane(TVMessage tvMessage);
	void setConfigToControlPlane(TVMessage tvMessage);
	void setConfigToDataPlane(TVMessage tvMessage);
	void switchStatsToControlPlane(TVMessage tvMessage);
	void switchStatsToDataPlane(TVMessage tvMessage);
	void tableModToControlPlane(TVMessage tvMessage);
	void tableModToDataPlane(TVMessage tvMessage);
	void flowModToControlPlane(TVMessage tvMessage);
	void flowModToDataPlane(TVMessage tvMessage);
	void groupModToControlPlane(TVMessage tvMessage);
	void groupModToDataPlane(TVMessage tvMessage);
	void portModToControlPlane(TVMessage tvMessage);
	void portModToDataPlane(TVMessage tvMessage);
	void barrierToControlPlane(TVMessage tvMessage);
	void barrierToDataPlane(TVMessage tvMessage);
	void meterModToControlPlane(TVMessage tvMessage);
	void meterModToDataPlane(TVMessage tvMessage);
	void roleToControlPlane(TVMessage tvMessage);
	void roleToDataPlane(TVMessage tvMessage);
	void packetInToControlPlane(TVMessage tvMessage);
	void packetInToDataPlane(TVMessage tvMessage);
	void packetOutToControlPlane(TVMessage tvMessage);
	void packetOutToDataPlane(TVMessage tvMessage);
	void miscToControlPlane(TVMessage tvMessage);
	void miscToDataPlane(TVMessage tvMessage);
	void errorToControlPlane(TVMessage tvMessage);
	void errorToDataPlane(TVMessage tvMessage);
}
