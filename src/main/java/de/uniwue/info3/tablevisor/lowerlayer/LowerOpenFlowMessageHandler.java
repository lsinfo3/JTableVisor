package de.uniwue.info3.tablevisor.lowerlayer;

import de.uniwue.info3.tablevisor.config.SwitchConfig;
import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.message.TVMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.Optional;

public class LowerOpenFlowMessageHandler extends ChannelInboundHandlerAdapter implements ILowerLayerMessageHandler {
	private static final Logger logger = LogManager.getLogger();
	private int dataplaneId;
	private Channel channel;
	private boolean initialized;
	private LowerOpenFlowEndpoint endpoint;

	public LowerOpenFlowMessageHandler(LowerOpenFlowEndpoint endpoint, Channel channel) {
		this.endpoint = endpoint;
		this.channel = channel;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		TVMessage tvMessage = (TVMessage) msg;

		if (tvMessage.isOpenFlow()) {
			logger.trace("Received message from {}: {}", ctx.channel().remoteAddress(), tvMessage.getOFMessage().toString());

			switch (tvMessage.getOFMessage().getType()) {
				case HELLO:
					// use the receipt of hello from switch to initialize tablevisor socket
					sendFeaturesRequest();
					break;
				case ECHO_REQUEST:
					// answer direct to echo requests
					handleIncomingEchoRequest(tvMessage);
					break;
				case FEATURES_REPLY:
					if (initialized)
						handleIncomingDefault(tvMessage);
					else
						handleIncomingInitializationFeaturesReply(tvMessage);
					break;
				default:
					handleIncomingDefault(tvMessage);
					break;
			}
		}
		else if (initialized) {
			handleIncomingDefault(tvMessage);
		}
	}

	private void handleIncomingInitializationFeaturesReply(TVMessage tvMessage) {
		OFFeaturesReply featuresReply = tvMessage.getOFMessage();
		Optional<SwitchConfig> dataplane = endpoint.getLowerLayerEndpointConfig().switches.stream().filter(
				dp -> DatapathId.of(dp.datapathId).equals(featuresReply.getDatapathId())
		).findAny();
		if (dataplane.isPresent()) {
			synchronized (TableVisor.getInstance()) {
				// dataplane with given datapath id is present in config file -> continue initialization
				dataplaneId = dataplane.get().dataplaneId;
				endpoint.getLowerLayerEndpointManager().getSockets().put(dataplaneId, this);
				endpoint.getLowerLayerEndpointManager().getDatapathToDataplaneId().put(dataplane.get().datapathId, dataplaneId);
				endpoint.getLowerLayerEndpointManager().getDataplaneToDatapathId().put(dataplaneId, dataplane.get().datapathId);
				initialized = true;
				logger.info("Switch ID '{}' of type '{}' registered (datapath ID '{}')", dataplaneId, endpoint.getLowerLayerEndpointConfig().type, featuresReply.getDatapathId());

				TableVisor.getInstance().notify();
			}
		} else {
			// unknown dataplane
			logger.warn("Unknown dataplane with datapath id {}", featuresReply.getDatapathId());
		}
	}

	private void sendFeaturesRequest() {
		OFMessage ofReply = OFFactories
				.getFactory(OFVersion.OF_13)
				.buildFeaturesRequest()
				.build();
		TVMessage tvReply = new TVMessage(ofReply);
		send(tvReply);
	}

	void sendHello() {
		OFMessage ofReply = OFFactories
				.getFactory(OFVersion.OF_13)
				.buildHello()
				.build();
		TVMessage tvReply = new TVMessage(ofReply);
		send(tvReply);
	}

	private void handleIncomingEchoRequest(TVMessage tvMessage) {
		OFMessage ofReply = OFFactories
				.getFactory(tvMessage.getOFMessage().getVersion())
				.buildEchoReply()
				.setXid(tvMessage.getOFMessage().getXid())
				.setData(((OFEchoRequest) tvMessage.getOFMessage()).getData())
				.build();
		TVMessage tvReply = new TVMessage(ofReply);
		send(tvReply);
	}

	private void handleIncomingDefault(TVMessage tvMessage) {
		if (initialized) {
			tvMessage.setDataplaneId(dataplaneId);
			TableVisor.getInstance().getTvToDataLayer().allToControlPlane(tvMessage);
		}
	}

	@Override
	public void send(TVMessage tvMessage) {
		if (!tvMessage.isOpenFlow()) {
			logger.error("Cannot send to switch, message is of type '{}'", tvMessage.getTypeAsString());
			return;
		}
		logger.trace("Sent {} message to {}", tvMessage.getOFMessage().getType(), channel.remoteAddress());
		channel.writeAndFlush(tvMessage);
	}

	@Override
	public int getDataplanId() {
		return dataplaneId;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}
}