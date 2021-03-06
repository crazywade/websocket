package com.hcnay.websocket.service;

import org.springframework.stereotype.Service;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/ws")
@Service
public class WebSocketServiceImpl {
    //静态变量 用来记录当前在线连接数 应设计为线程安全的
    private static int onlineCount = 0;
    //concurrent包的线程安全set 用来存放每个客户端的WebSocketServiceImpldioxide
    private static CopyOnWriteArraySet<WebSocketServiceImpl> webSocketSet = new CopyOnWriteArraySet<>();
    //与某个客户端的连接会话 需要通过它来给客户端发送数据
    private Session session;

    /**
     * 连接建立成功调用
     * @param session
     */
    @OnOpen
    public void onOpen(Session session){
        this.session = session;
        webSocketSet.add(this);//加入set中
        addOnlineCount();//在线数+1
        System.out.println("有新连接加入！当前在线人数为"+getOnlineCount());
        try {
            sendMessage("有新的连接加入了！！");
        } catch (IOException e) {
            System.out.println("IO异常");
            }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(){
        webSocketSet.remove(this);
        subOnlineCount();
        System.out.println("有一连接关闭！当前在线人数为"+getOnlineCount());
    }

    /**
     * 收到客户端后调用
     * @param message 接受的消息
     * @param session
     */
    @OnMessage
    public void onMessage(String message,Session session){
        System.out.println("来自客户端的消息："+message);

        //群发消息
        for (WebSocketServiceImpl item:webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 异常处理
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session,Throwable error){
        System.out.println("发送错误");
        error.printStackTrace();
    }
    /**
     * 发送消息
     * @param message
     * @throws IOException
     */
    private void sendMessage(String message)throws IOException{
        this.session.getBasicRemote().sendText(message);
    }

    //返回在线数
    private static synchronized int getOnlineCount(){
        return onlineCount;
    }

    //连接人数增加
    private static synchronized void addOnlineCount(){
        WebSocketServiceImpl.onlineCount++;
    }

    //连接人数减少
    private static synchronized void subOnlineCount(){
        WebSocketServiceImpl.onlineCount--;
    }
}
