package com.proxy.client.handler;

import com.proxy.client.service.ClientBeanManager;
import com.proxy.common.protobuf.ProxyMessageProtos;
import com.proxy.common.protocol.CommonConstant;
import com.proxy.common.util.ProxyMessageUtil;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 登录安全认证请求 handler
 */
public class LoginAuthReqHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(LoginAuthReqHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("客户端发送登录信息");
        String clientKey= ClientBeanManager.getConfigService().readConfig().get("key");
        ctx.writeAndFlush(ProxyMessageUtil.buildLoginReq(null,clientKey.getBytes()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ProxyMessageProtos.ProxyMessage message = (ProxyMessageProtos.ProxyMessage) msg;
        // 如果是握手应答消息，需要判断是否认证成功
        byte type = message.getType().toByteArray()[0];

        if (type == CommonConstant.Login.TYPE_LOGIN_RESP) {
            byte command=message.getCommand().byteAt(0);
            String loginMesg=new String(message.getData().toByteArray());

            if (command== CommonConstant.Login.LOGIN_SUCCESS){
                logger.info("客户端认证登录成功");
                // 记录客户端和代理服务器的channel
                ClientBeanManager.getProxyService().setChannel(ctx.channel());
            }else if(command== CommonConstant.Login.LOGIN_FAIL){
                logger.info("客户端认证登录失败:{}",loginMesg);
                ctx.channel().close();
            }
        } else {
            //向上传递消息，由其他handler 继续处理
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
    }

}