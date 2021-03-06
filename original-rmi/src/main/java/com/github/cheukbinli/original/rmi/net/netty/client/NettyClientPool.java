package com.github.cheukbinli.original.rmi.net.netty.client;

import com.github.cheukbinli.original.common.rmi.LoadBalanceFactory;
import com.github.cheukbinli.original.common.rmi.RmiClient;
import com.github.cheukbinli.original.common.rmi.model.ConsumerValueModel;
import com.github.cheukbinli.original.common.rmi.model.RegisterLoadBalanceModel;
import com.github.cheukbinli.original.common.rmi.model.RegisterLoadBalanceModel.ServiceType;
import com.github.cheukbinli.original.common.rmi.net.NetworkClient;
import com.github.cheukbinli.original.common.util.pool.AbstractObjectPool;
import com.github.cheukbinli.original.rmi.config.RmiConfig.RmiConfigGroup;
import com.github.cheukbinli.original.rmi.config.RmiConfigArg;
import com.github.cheukbinli.original.rmi.net.netty.SimpleChannelFutureListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NettyClientPool extends AbstractObjectPool<NettyClientHandle, InetSocketAddress> implements RmiClient<String, Void, NetworkClient<Bootstrap, NettyClientHandle, InetSocketAddress, String, Void, Channel, RmiConfigGroup>, RmiConfigArg> {

	private static final Logger LOG = LoggerFactory.getLogger(NettyClientPool.class);

	private LoadBalanceFactory<String, Void> loadBalanceFactory;

	private NetworkClient<Bootstrap, NettyClientHandle, InetSocketAddress, String, Void, Channel, RmiConfigGroup> nettyClient;

	private RmiConfigGroup rmiConfigGroup;

	private ExecutorService executorService = Executors.newCachedThreadPool();

	private int tryAgainInterval = 1000;// 重连间隔

	private volatile boolean isInit;

	/***
	 * 默认连接处理数,默认CPU*2
	 */
	private int availableProcessors;

	public NettyClientPool() {
	}

	public void cancleRegistration(String clientName) throws Throwable {
		if (null != clientName) {
			RegisterLoadBalanceModel registerLoadBalanceModel = new RegisterLoadBalanceModel();
			registerLoadBalanceModel.setType(ServiceType.client);
			registerLoadBalanceModel.setServerName(clientName);
			loadBalanceFactory.cancleRegistration(registerLoadBalanceModel);
		}
	}

	public void start() throws NumberFormatException, IllegalStateException, UnsupportedOperationException, InterruptedException, Exception {
		if (isInit) {
			return;
		}
		isInit = true;
		if (null == loadBalanceFactory)
			loadBalanceFactory = nettyClient.getLoadBalanceFactory();
		// 线程平均分配
		availableProcessors = rmiConfigGroup.getProtocolModel().getNetWorkThreads();
		availableProcessors = availableProcessors > 0 ? availableProcessors : Runtime.getRuntime().availableProcessors();
		// 连接生成数
		for (int i = 0; i < availableProcessors; i++) {
			connect(new ConsumerValueModel(rmiConfigGroup.getProtocolModel().getLocalName() + "_" + getPoolName() + "_" + i + "_" + System.currentTimeMillis(), rmiConfigGroup.getProtocolModel().getLocalAddress(), getPoolName()));
			Thread.sleep(100);
		}
	}

	public void addConnectionByServerName(String serviceName, String clientName) {
		addConnectionByServerName(new ConsumerValueModel(clientName, serviceName));
	}
	public void addConnectionByServerName(ConsumerValueModel consumerValueModel) {
		connect(consumerValueModel);
	}

	private void connect(final ConsumerValueModel consumerValueModel) {
		executorService.execute(new Runnable() {
			public void run() {
				try {
					RegisterLoadBalanceModel registerLoadBalanceModel = new RegisterLoadBalanceModel();
					// 拥有的服务
					registerLoadBalanceModel.setServiceName(consumerValueModel.getServiceName());
					// 服务器主机名
					registerLoadBalanceModel.setServerName(consumerValueModel.getConsumerName());
					registerLoadBalanceModel.setType(ServiceType.client);
					// 健康
					// registerLoadBalanceModel.setHealthCheck(rmiConfigArg.getProtocolModel().getLocalAddress() + ":" + rmiConfigArg.getProtocolModel().getPort());

					// String address = loadBalanceFactory.getResourceAndUseRegistration(registerLoadBalanceModel);
					List<String> address = loadBalanceFactory.getResource(registerLoadBalanceModel);
					if (LOG.isDebugEnabled())
						LOG.debug("资源服务器地址：{}", address);
					if (null == address || address.isEmpty())
						throw new Throwable("not service resource online.");
					String[] tempServerInfo = address.get(0).split("@");
					String[] addresses = tempServerInfo[1].split(":");
					consumerValueModel.setServerName(tempServerInfo[0]).setServerUrl(tempServerInfo[1]);

					InetSocketAddress inetSocketAddress = new InetSocketAddress(addresses[0], Integer.valueOf(addresses[1]));
					try {
						Socket ping = new Socket(inetSocketAddress.getAddress(), inetSocketAddress.getPort());
						ping.close();
					} catch (Exception e) {
						if (consumerValueModel.getTryAgain() < 0)
							return;
						Thread.sleep(tryAgainInterval);
						addConnectionByServerName(consumerValueModel.addTryAgain(-1));
						return;
					}
					nettyClient.getClient().connect(inetSocketAddress).addListener(new SimpleChannelFutureListener(nettyClient, consumerValueModel)).sync().channel();
				} catch (Throwable e) {
					LOG.error(null, e);
				}

			}
		});
	}

	@Override
	public void invalidateReBuildObject(int count) throws Exception {
		// 一共过期N个对象
		if (LOG.isDebugEnabled())
			LOG.debug("连接池{}:一共过期{}个对象。", getPoolName(), count);
	}

	public void invalidateObject(NettyClientHandle t) throws Exception {
		destroyObject(t);
	}

	public void destroyObject(NettyClientHandle t) throws Exception {
		ChannelHandlerContext ctx = t.getObject().getChannelHandlerContext();
		ctx.close();
		ctx.channel().close();
	}

	@Override
	public synchronized void removeObject(NettyClientHandle t) throws Exception {
		super.removeObject(t);
	}

	public RmiConfigGroup getRmiConfigGroup() {
		return rmiConfigGroup;
	}

	public NettyClientPool setRmiConfigGroup(RmiConfigGroup rmiConfigGroup) {
		this.rmiConfigGroup = rmiConfigGroup;
		return this;
	}

	public LoadBalanceFactory<String, Void> getLoadBalanceFactory() {
		return loadBalanceFactory;
	}

	public NettyClientPool setLoadBalanceFactory(LoadBalanceFactory<String, Void> loadBalanceFactory) {
		this.loadBalanceFactory = loadBalanceFactory;
		return this;
	}

	public NettyClientPool setNettyClient(NetworkClient<Bootstrap, NettyClientHandle, InetSocketAddress, String, Void, Channel, RmiConfigGroup> client) {
		this.nettyClient = client;
		return this;
	}

	public NettyClientPool(int poolSize, String serviceName) {
		super(poolSize, serviceName);
	}

	public NettyClientPool(String serviceName) {
		super(serviceName);
	}

	public RmiClient<String, Void, NetworkClient<Bootstrap, NettyClientHandle, InetSocketAddress, String, Void, Channel, RmiConfigGroup>, RmiConfigArg> setServiceName(String serviceName) {
		setPoolName(serviceName);
		return this;
	}

	public boolean isFailure(NettyClientHandle t) throws Exception {
		return !t.getChannelHandlerContext().channel().isActive();
	}

	public NettyClientPool(final NetworkClient<Bootstrap, NettyClientHandle, InetSocketAddress, String, Void, Channel, RmiConfigGroup> nettyClient, String serviceName) {
		super();
		this.nettyClient = nettyClient;
		this.rmiConfigGroup = nettyClient.getRmiConfigGroup();
		this.loadBalanceFactory = nettyClient.getLoadBalanceFactory();
		setPoolName(serviceName);
	}

}