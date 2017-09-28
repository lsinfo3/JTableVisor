package de.uniwue.info3.tablevisor.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMessageReader;

public class TVMsgDecoder extends LengthFieldBasedFrameDecoder {
	private static final Logger logger = LogManager.getLogger();

	private final OFMessageReader<OFMessage> reader;

	public TVMsgDecoder(OFMessageReader<OFMessage> reader) {
		super(65536, 2, 2, -4, 0);
		this.reader = reader;
	}

	@Override
	protected Object decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
		logger.trace("Received data from {}, buffer status: {} Bytes", channelHandlerContext.name(), byteBuf.readableBytes());

		try {
			ByteBuf frame = (ByteBuf) super.decode(channelHandlerContext, byteBuf);
			if (frame == null) {
				return null;
			}
			OFMessage ofMsg = reader.readFrom(frame);
			if (ofMsg == null) {
				return null;
			}

			return new TVMessage(ofMsg);
		}
		catch (OFParseError e) {
			logger.warn("{} - Error reading OFMessage: '{}'", "TVMsgDecoder", e.getMessage());
			return new TVMessage(e, null);
		}
		catch (RuntimeException e) {
			logger.error("{} - Error reading OFMessage", "TVMsgDecoder", e);
		}
		return null;
	}
}
