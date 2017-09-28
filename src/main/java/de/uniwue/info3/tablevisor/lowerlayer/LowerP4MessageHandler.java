package de.uniwue.info3.tablevisor.lowerlayer;

import de.uniwue.info3.tablevisor.config.SwitchConfig;
import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.message.TVMessage;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;

public class LowerP4MessageHandler implements ILowerLayerMessageHandler {
	private static final Logger logger = LogManager.getLogger();
	private int dataplaneId;
	private LowerP4Endpoint endpoint;

	public LowerP4MessageHandler(LowerP4Endpoint endpoint, int dataplaneId) {
		this.endpoint = endpoint;
		this.dataplaneId = dataplaneId;

		logger.info("Switch ID '{}' of type '{}' registered", dataplaneId, endpoint.getLowerLayerEndpointConfig().type);

		synchronized (TableVisor.getInstance()) {
			TableVisor.getInstance().notify();
		}
	}

	@Override
	public int getDataplanId() {
		return dataplaneId;
	}

	@Override
	public void send(TVMessage tvMessage) {
		if (!tvMessage.isP4()) {
			logger.error("Cannot send to switch, message is of type '{}'", tvMessage.getTypeAsString());
			return;
		}

		LinkedList<String> cliParams = tvMessage.getCmdLine();

		SwitchConfig swc = TableVisor.getInstance().getConfig().getSwitchConfigById(tvMessage.getDataplaneId());
		cliParams.addFirst("--rte-host " + swc.rteIp);
		cliParams.addFirst("--rte-port " + swc.rtePort);

		CommandLine commandline = new CommandLine(endpoint.getLowerLayerEndpointConfig().rtecliPath);
		for (String p : tvMessage.getCmdLine()) {
			if (p.startsWith("--")) {
				// Split argument into 2 to comply with what RTECLI expects
				String[] parts = p.split(" ", 2);
				commandline.addArgument(parts[0], false);
				commandline.addArgument(parts[1], false);
			}
			else {
				commandline.addArgument(p, false);
			}
		}

		DefaultExecutor exec = new DefaultExecutor();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		exec.setStreamHandler(streamHandler);

		try {
			exec.execute(commandline);
			logger.trace(commandline.toString().trim());
		}
		catch (IOException e) {
			logger.warn("Error during RTECLI execution: {}", e.getMessage(), e);
			String cmdLine = commandline.toString().trim();
			if (!cmdLine.isEmpty()) logger.debug(cmdLine);
			String outStr = outputStream.toString().trim();
			if (!outStr.isEmpty()) logger.debug(outStr);
			return;
		}

		String reply = outputStream.toString().trim();
		logger.debug("RTECLI - {}", reply);

		TVMessage replyMsg = new TVMessage(reply, tvMessage.getOriginalRequest(), getDataplanId());
		TableVisor.getInstance().getTvToDataLayer().allToControlPlane(replyMsg);
	}

	@Override
	public boolean isInitialized() {
		return true;
	}
}
