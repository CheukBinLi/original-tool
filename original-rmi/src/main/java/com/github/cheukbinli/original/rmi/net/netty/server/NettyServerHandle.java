package com.github.cheukbinli.original.rmi.net.netty.server;

import com.github.cheukbinli.original.common.rmi.model.TransmissionModel;
import com.github.cheukbinli.original.common.rmi.net.MessageHandleFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class NettyServerHandle extends SimpleChannelInboundHandler<TransmissionModel> {

	private final static Logger LOG = LoggerFactory.getLogger(NettyServerHandle.class);

	private final MessageHandleFactory<Object, Object, Object> messageHandle;

	private NettyServer nettyServer;

	private AtomicInteger counter = new AtomicInteger();

	public NettyServerHandle(NettyServer nettyServer, MessageHandleFactory<Object, Object, Object> messageHandle) {
		this.nettyServer = nettyServer;
		this.messageHandle = messageHandle;
		nettyServer.modifyConnectionCount(1);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TransmissionModel msg) throws Exception {
		counter.set(0);
		messageHandle.messageHandle(ctx, msg, msg.getServiceType());
		ReferenceCountUtil.release(msg);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		nettyServer.modifyConnectionCount(-1);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// super.exceptionCaught(ctx, cause);
		ctx.close();
		ctx.channel().close();
	}
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			// 空闲6s之后触发 (心跳包丢失)
			if (counter.getAndAdd(1) >= 3) {
				// 连续丢失3个心跳包 (断开连接)
				if (LOG.isDebugEnabled())
					LOG.debug("将与{}客户端断开连接", ctx.channel().remoteAddress().toString());
				ctx.close();
				ctx.channel().close();
			} else {
				if (LOG.isDebugEnabled())
					LOG.debug("丢失了第 " + counter + " 个心跳包");
			}
		} else
			super.userEventTriggered(ctx, evt);
	}

}
