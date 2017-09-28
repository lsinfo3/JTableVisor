package de.uniwue.info3.tablevisor.lowerlayer;

import de.uniwue.info3.tablevisor.message.TVMessage;

public interface ILowerLayerMessageHandler {
	int getDataplanId();
	void send(TVMessage tvMessage);
	boolean isInitialized();
}
