package de.uniwue.info3.tablevisor.upperlayer;

import de.uniwue.info3.tablevisor.config.UpperLayerEndpointConfig;
import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.message.TVMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;

@ChannelHandler.Sharable
public class UpperOpenFlowMessageHandler extends ChannelInboundHandlerAdapter implements IUpperLayerMessageHandler {
	private static final Logger logger = LogManager.getLogger();

	private UpperLayerEndpointConfig cfg;
	private UpperOpenFlowEndpoint parent;

	public UpperOpenFlowMessageHandler(UpperLayerEndpointConfig cfg, UpperOpenFlowEndpoint parent) {
		this.cfg = cfg;
		this.parent = parent;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		TVMessage tvMsg = (TVMessage) msg;

		// Directly handle HELLO and ECHO reqeusts
		if (tvMsg.getOFMessage().getType() == OFType.HELLO) {
			return;
		}
		if (tvMsg.getOFMessage().getType() == OFType.ECHO_REQUEST) {
			sendEchoReply(tvMsg);
			return;
		}

		logger.trace("OFMessage read from control channel: {}", tvMsg.getOFMessage().toString());

		TableVisor.getInstance().getTvToControllerLayer().allToDataPlane(tvMsg);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.error("{} - Exception caught by socket", cfg.name, cause);
		//ctx.close();
	}

	@Override
	public void send(TVMessage msg) {
		parent.getChannel().writeAndFlush(msg);
	}

	private void sendEchoReply(TVMessage msg) {
		OFEchoRequest orig = msg.getOFMessage();
		OFEchoReply rep = OFFactories.getFactory(OFVersion.OF_13).buildEchoReply()
				.setXid(orig.getXid())
				.setData(orig.getData())
				.build();
		send(new TVMessage(rep));
	}
}
