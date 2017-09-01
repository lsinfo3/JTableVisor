package de.uniwue.info3.tablevisor.upperlayer;

import de.uniwue.info3.tablevisor.config.UpperLayerEndpointConfig;
import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.message.TVMessage;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;

@ChannelHandler.Sharable
public class ULGenericOpenFlowHandler extends ChannelInboundHandlerAdapter implements IUpperLayerSocket {
	private static final Logger logger = LogManager.getLogger();

	private UpperLayerEndpointConfig cfg;
	private UpperLayerOpenFlowEndpoint parent;

	public ULGenericOpenFlowHandler(UpperLayerEndpointConfig cfg, UpperLayerOpenFlowEndpoint parent) {
		this.cfg = cfg;
		this.parent = parent;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		TVMessage tvMsg = (TVMessage) msg;

		if (tvMsg != null && tvMsg.getOFMessage() != null) {
			logger.debug("{} - Read OFMessage (ver {}): {}", cfg.name, tvMsg.getOFMessage().getVersion(), tvMsg.getOFMessage().getType());
		}
		else {
			logger.trace("{} - Read null-OFMessage", cfg.name);
		}

		// Directly handle HELLO and ECHO reqeusts
		if (tvMsg.getOFMessage().getType() == OFType.HELLO) {
			return;
		}
		if (tvMsg.getOFMessage().getType() == OFType.ECHO_REQUEST) {
			sendEchoReply(tvMsg);
			return;
		}

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
