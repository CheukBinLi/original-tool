package com.github.cheukbinli.original.rmi.net.netty.server;

import com.github.cheukbinli.original.common.cache.CacheSerialize;
import com.github.cheukbinli.original.common.rmi.LoadBalanceFactory;
import com.github.cheukbinli.original.common.rmi.RmiBeanFactory;
import com.github.cheukbinli.original.common.rmi.RmiContant;
import com.github.cheukbinli.original.common.rmi.model.RegisterLoadBalanceModel;
import com.github.cheukbinli.original.common.rmi.model.RegisterLoadBalanceModel.ServiceType;
import com.github.cheukbinli.original.common.rmi.net.MessageHandleFactory;
import com.github.cheukbinli.original.rmi.config.RmiConfig.RmiConfigGroup;
import com.github.cheukbinli.original.rmi.config.ServiceGroupConfig.ServiceGroupModel;
import com.github.cheukbinli.original.rmi.net.netty.NettyMessageDecoder;
import com.github.cheukbinli.original.rmi.net.netty.NettyMessageEncoder;
import com.github.cheukbinli.original.rmi.net.netty.message.NettyHearBeatServiceHandle;
import com.github.cheukbinli.original.rmi.net.netty.message.RmiServiceHandle;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"rawtypes", "unchecked", "static-access"})
public class NettyServer implements RmiContant {

	private static final Logger LOG = LoggerFactory.getLogger(NettyServer.class);
	private RmiConfigGroup rmiConfigGroup;
	private CacheSerialize cacheSerialize;
	private RmiBeanFactory rmiBeanFactory;
	private MessageHandleFactory messageHandleFactory;
	private LoadBalanceFactory<String, Void> loadBalanceFactory;
	private AtomicInteger connectionCount = new AtomicInteger(0);
	private RegisterLoadBalanceModel registerLoadBalanceModel;
	private Thread uploadLoadInfotask;
	private Thread work;

	public synchronized void start() throws InterruptedException, NullPointerException {

		try {
			if (rmiConfigGroup.getProtocolModel().getNetWorkThreads() < 0) {
				rmiConfigGroup.getProtocolModel().setNetWorkThreads(Runtime.getRuntime().availableProcessors() * 2);
			}
			if (rmiConfigGroup.getProtocolModel().getHandleThreads() < 0) {
				rmiConfigGroup.getProtocolModel().setHandleThreads(Runtime.getRuntime().availableProcessors() * 2);
			}
			initInfo();
			init(rmiConfigGroup.getProtocolModel().getNetWorkThreads());
		} catch (Throwable e) {
			LOG.error("NettyServer.class -method: run()", e);
		}
	}

	private void init(final int poolSize) throws Throwable {
		if (null == work || work.interrupted()) {
			if (LOG.isDebugEnabled())
				LOG.info("server is start.");
			if (null == rmiBeanFactory) {
				throw new NullPointerException("rmiBeanFactory is null");
			}
			// 参数
			if (null == cacheSerialize)
				throw new NullPointerException("cacheSerialize is null");
			if (null == messageHandleFactory) {
				messageHandleFactory = new HandleService();
				messageHandleFactory.start(rmiConfigGroup.getProtocolModel().getHandleThreads());
			}
			// messageHandleFactory = NettyHandleServiceFactory.newInstance(handleThreads);
			// throw new NullPointerException("messageHandle is
			// null");
			// 心跳
			messageHandleFactory.registrationMessageHandle(RMI_SERVICE_TYPE_HEAR_BEAT, new NettyHearBeatServiceHandle());
			if (!messageHandleFactory.serviceTypeContains(RMI_SERVICE_TYPE_REQUEST)) {
				// RMI服务
				messageHandleFactory.registrationMessageHandle(RMI_SERVICE_TYPE_REQUEST, new RmiServiceHandle(rmiBeanFactory));
			}
			// 注解目录
			if (null == loadBalanceFactory) {
				throw new NullPointerException("loadBalanceFactory is null");
			}
			loadBalanceFactory.init();
			/***
			 * 注册服务
			 */
			for (Entry<String, ServiceGroupModel> en : rmiConfigGroup.getServiceGroup().getServiceGroupConfig().entrySet()) {
				registerLoadBalanceModel = new RegisterLoadBalanceModel();
				// 拥有的服务
				registerLoadBalanceModel.setServiceName(en.getKey()).setType(ServiceType.server);
				// 服务器主机名
				registerLoadBalanceModel.setServerName(rmiConfigGroup.getProtocolModel().getLocalName());
				// 服务地址
				registerLoadBalanceModel.setUrl(rmiConfigGroup.getProtocolModel().getLocalAddress() + ":" + rmiConfigGroup.getProtocolModel().getPort());
				// 健康
				registerLoadBalanceModel.setHealthCheck(rmiConfigGroup.getProtocolModel().getLocalAddress() + ":" + rmiConfigGroup.getProtocolModel().getPort());
				loadBalanceFactory.registration(registerLoadBalanceModel);
			}
			work = new Thread(new Runnable() {

				public void run() {
					try {
						final EventLoopGroup bossGroup = new NioEventLoopGroup(poolSize);
						final EventLoopGroup workerGroup = new NioEventLoopGroup(poolSize);
						try {
							ServerBootstrap server = new ServerBootstrap();
							server.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_BACKLOG, 1024).childHandler(new ChannelInitializer<SocketChannel>() {
								@Override
								public void initChannel(SocketChannel ch) throws Exception {
									// 注册handler
									ch.pipeline().addLast(new NettyMessageDecoder(rmiConfigGroup.getProtocolModel().getFrameLength(), 4, 4, cacheSerialize));
									ch.pipeline().addLast(new NettyMessageEncoder(cacheSerialize));
									ch.pipeline().addLast(new IdleStateHandler(rmiConfigGroup.getProtocolModel().getHeartbeat(), 0, 0));
									ch.pipeline().addLast(new NettyServerHandle(NettyServer.this, messageHandleFactory));
									// ch.pipeline().addLast("logging", new LoggingHandler(LogLevel.DEBUG));
								}

							});
							server.bind(rmiConfigGroup.getProtocolModel().getPort()).sync().channel().closeFuture().sync();
							LOG.warn("service is close.");

						} finally {
							workerGroup.shutdownGracefully();
							bossGroup.shutdownGracefully();
						}
					} catch (Throwable e) {
						LOG.error(null, e);
					}
				}
			});
			work.start();
			// 更新任务
			uploadLoadInfotask = new Thread(new Runnable() {
				public void run() {
					RegisterLoadBalanceModel loadBalanceModel = null;
					try {
						while (true) {
							// if (connectionCount.get() % 2 > 0) {
							if (null == loadBalanceModel) {
								loadBalanceModel = (RegisterLoadBalanceModel) registerLoadBalanceModel.clone();
							}
							loadBalanceModel.setType(ServiceType.load).setValue(Integer.toString(connectionCount.get()));
							loadBalanceFactory.registration(loadBalanceModel);
							// }
							synchronized (uploadLoadInfotask) {
								uploadLoadInfotask.wait(60000);
							}
						}
					} catch (Throwable e) {
						LOG.error(null, e);
					}
				}
			});
			uploadLoadInfotask.start();
		}
	}

	/***
	 * 
	 * @param value
	 *            基数加/减 value
	 */
	public void modifyConnectionCount(int value) {
		connectionCount.addAndGet(value);
		synchronized (uploadLoadInfotask) {
			uploadLoadInfotask.notify();
		}
	}

	void initInfo() {
		LOG.info("rmi server info.");
		LOG.info("registration center address:{}", rmiConfigGroup.getRegistryModel().getServerAddress());
		LOG.info("registration name:{}", rmiConfigGroup.getProtocolModel().getLocalName());
		LOG.info("network address:{}", rmiConfigGroup.getProtocolModel().getLocalAddress());
		LOG.info("network frame max size:{}", (rmiConfigGroup.getProtocolModel().getFrameLength() / 1024) + "KB");
		LOG.info("network access thread:{} , message handle thread:{}", rmiConfigGroup.getProtocolModel().getNetWorkThreads(), rmiConfigGroup.getProtocolModel().getHandleThreads());
	}

	public NettyServer() {
		super();
	}

	public NettyServer(RmiConfigGroup rmiConfigGroup, RmiBeanFactory rmiBeanFactory, MessageHandleFactory<Object, Object, Object> messageHandleFactory) {
		super();
		this.rmiConfigGroup = rmiConfigGroup;
		this.rmiBeanFactory = rmiBeanFactory;
		this.messageHandleFactory = messageHandleFactory;
	}

	public CacheSerialize getCacheSerialize() {
		return cacheSerialize;
	}

	public NettyServer setCacheSerialize(CacheSerialize cacheSerialize) {
		this.cacheSerialize = cacheSerialize;
		return this;
	}

	public RmiConfigGroup getRmiConfigGroup() {
		return this.rmiConfigGroup;
	}

	public NettyServer setRmiConfigGroup(RmiConfigGroup rmiConfigGroup) {
		this.rmiConfigGroup = rmiConfigGroup;
		return this;
	}

	public RmiBeanFactory getRmiBeanFactory() {
		return rmiBeanFactory;
	}

	public NettyServer setRmiBeanFactory(RmiBeanFactory rmiBeanFactory) {
		this.rmiBeanFactory = rmiBeanFactory;
		return this;
	}

	public MessageHandleFactory getMessageHandleFactory() {
		return messageHandleFactory;
	}

	public NettyServer setMessageHandleFactory(MessageHandleFactory messageHandleFactory) {
		this.messageHandleFactory = messageHandleFactory;
		return this;
	}

	public LoadBalanceFactory<String, Void> getLoadBalanceFactory() {
		return loadBalanceFactory;
	}

	public NettyServer setLoadBalanceFactory(LoadBalanceFactory<String, Void> loadBalanceFactory) {
		this.loadBalanceFactory = loadBalanceFactory;
		return this;
	}

}
