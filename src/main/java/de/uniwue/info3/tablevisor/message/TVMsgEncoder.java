package de.uniwue.info3.tablevisor.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TVMsgEncoder extends MessageToByteEncoder<TVMessage> {
	private static final Logger logger = LogManager.getLogger();

	@Override
	protected void encode(ChannelHandlerContext channelHandlerContext, TVMessage tvMessage, ByteBuf byteBuf) {
		logger.trace("Sent data to {}, message: {}", channelHandlerContext.name(), tvMessage.getOFMessage());
		tvMessage.getOFMessage().writeTo(byteBuf);
	}
}
