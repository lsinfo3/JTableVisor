package de.uniwue.info3.tablevisor.upperlayer;

import de.uniwue.info3.tablevisor.message.TVMessage;
import io.netty.channel.ChannelInboundHandler;

public interface IUpperLayerMessageHandler extends ChannelInboundHandler {
    void send(TVMessage msg);
}
