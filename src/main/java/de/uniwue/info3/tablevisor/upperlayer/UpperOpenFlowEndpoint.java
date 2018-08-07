package de.uniwue.info3.tablevisor.upperlayer;

import de.uniwue.info3.tablevisor.config.UpperLayerEndpointConfig;
import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.message.TVMessage;
import de.uniwue.info3.tablevisor.message.TVMsgDecoder;
import de.uniwue.info3.tablevisor.message.TVMsgEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFHello;
import org.projectfloodlight.openflow.protocol.OFVersion;

public class UpperOpenFlowEndpoint implements IUpperLayerEndpoint {
	private static final Logger logger = LogManager.getLogger();

	private IUpperLayerMessageHandler openFlowHandler = null;
	private UpperLayerEndpointConfig cfg = null;
	private UpperLayerEndpointManager manager = null;
	private Channel ch = null;

	private long lastReconnect = -1;

	public void initialize(UpperLayerEndpointManager manager, UpperLayerEndpointConfig cfg) {
		if (this.cfg != null) {
			logger.warn("{} - Prevented duplicate initialization of UpperOpenFlowEndpoint", cfg.name);
			return;
		}

		this.manager = manager;
		this.cfg = cfg;
		this.openFlowHandler = new UpperOpenFlowMessageHandler(cfg, this);

		new Thread(() -> {
			// Do reconnects in a while-loop instead of Event-hooks to prevent the creation of new threads for this purpose
			while (!TableVisor.getInstance().isShutdown()) {
				logger.info("{} - Attempting connection to control plane {}:{}", cfg.name, cfg.ip, cfg.port);

				NioEventLoopGroup workerGroup = new NioEventLoopGroup();
				Bootstrap b = new Bootstrap();
				b.group(workerGroup);
				b.channel(NioSocketChannel.class);
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);

				b.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(
								new TVMsgEncoder(),
								new TVMsgDecoder(OFFactories.getFactory(OFVersion.OF_13).getReader()),
								openFlowHandler
						);
					}
				});

				ChannelFuture future = b.connect(cfg.ip, cfg.port);
				ch = future.channel();
				future.addListener((ChannelFutureListener) f -> {
					if (f.isSuccess()) {
						logger.info("{} - Connection to {}:{} established, sending HELLO...", cfg.name, cfg.ip, cfg.port);
						sendHello();
					}
				});

				// Reconnect on failure... otherwise, just block this thread forever
				future.channel().closeFuture().awaitUninterruptibly();

				if (TableVisor.getInstance().isShutdown()) {
					logger.info("{} - Connection to {}:{} closed", cfg.name);
					return;
				}

				if (cfg.reconnectInterval > 0 && System.currentTimeMillis() - lastReconnect < cfg.reconnectInterval) {
					logger.info("{} - Connection to {}:{} failed; reconnecting in {} seconds", cfg.name, cfg.ip, cfg.port, (double) cfg.reconnectInterval / 1000);
					try {
						Thread.sleep(cfg.reconnectInterval);
					}
					catch (InterruptedException e) {
						// Do nothing (why even wake up this thread?)
					}
					lastReconnect = System.currentTimeMillis();
				}
				else {
					logger.info("{} - Connection to {}:{} failed; reconnecting", cfg.name, cfg.ip, cfg.port, (double) cfg.reconnectInterval / 1000);
					lastReconnect = System.currentTimeMillis();
				}
			}
		}).start();
	}

	private void sendHello() {
		OFHello hi = OFFactories.getFactory(OFVersion.OF_13).buildHello()
				.setXid(System.currentTimeMillis())
				.build();
		TVMessage msg = new TVMessage(hi);
		send(msg);
	}

	public Channel getChannel() {
		return ch;
	}

	public void send(TVMessage msg) {
		if (ch == null) {
			logger.error("{} - Cannot send, channel {}:{} has not been initialized", cfg.name, cfg.ip, cfg.port);
		}
		else if (!ch.isActive()) {
			logger.error("{} - Cannot send, channel {}:{} inactive", cfg.name, cfg.ip, cfg.port);
		}
		else if (!msg.isOpenFlow()) {
			logger.error("{} - Cannot send to controller, message is of type '{}'", cfg.name, msg.getTypeAsString());
		}
		else {
			openFlowHandler.send(msg);
		}
	}

	@Override
	public UpperLayerEndpointManager getUpperLayerEndpointManager() {
		return manager;
	}

	@Override
	public UpperLayerEndpointConfig getUpperLayerEndpointConfig() {
		return cfg;
	}
}