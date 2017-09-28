package de.uniwue.info3.tablevisor.lowerlayer;

import de.uniwue.info3.tablevisor.config.LowerLayerEndpointConfig;
import de.uniwue.info3.tablevisor.message.TVMsgDecoder;
import de.uniwue.info3.tablevisor.message.TVMsgEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFVersion;

public class LowerOpenFlowEndpoint implements ILowerLayerEndpoint {
	private static final Logger logger = LogManager.getLogger();
	private LowerLayerEndpointConfig endpointConfig;
	private LowerLayerEndpointManager endpointManager;

	@Override
	public void initialize(LowerLayerEndpointManager endpointManager, LowerLayerEndpointConfig endpointConfig) {
		this.endpointManager = endpointManager;
		this.endpointConfig = endpointConfig;

		final LowerOpenFlowEndpoint endpoint = this;

		new Thread(() -> {
			EventLoopGroup bossGroup = new NioEventLoopGroup(1);
			EventLoopGroup workerGroup = new NioEventLoopGroup(16);

			try {
				ServerBootstrap bootstrap = new ServerBootstrap()
						.group(bossGroup, workerGroup)
						.channel(NioServerSocketChannel.class)
						.option(ChannelOption.SO_REUSEADDR, true)
						.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
						.option(ChannelOption.SO_BACKLOG, 1000)
						.childHandler(new ChannelInitializer<Channel>() {
							@Override
							protected void initChannel(Channel channel) throws Exception {
								logger.info("{} - Connection from {}", endpointConfig.name, channel.remoteAddress());
								LowerOpenFlowMessageHandler socket = new LowerOpenFlowMessageHandler(endpoint, channel);
								channel.pipeline().addLast(
										new TVMsgEncoder(),
										new TVMsgDecoder(OFFactories.getFactory(OFVersion.OF_13).getReader()),
										socket
								);
								socket.sendHello();
							}
						});

				logger.info("{} - Listening for OpenFlow connections on port {}", endpointConfig.name, endpointConfig.port);
				ChannelFuture f = bootstrap.bind(endpointConfig.port).sync();

				f.channel().closeFuture().sync();

			} catch (InterruptedException e) {
				logger.info("{} - Error listening on Port {}", endpointConfig.name, endpointConfig.port, e);
			} finally {
				workerGroup.shutdownGracefully();
				bossGroup.shutdownGracefully();
			}
		}).start();
	}

	@Override
	public LowerLayerEndpointManager getLowerLayerEndpointManager() {
		return endpointManager;
	}

	@Override
	public LowerLayerEndpointConfig getLowerLayerEndpointConfig() {
		return endpointConfig;
	}
}