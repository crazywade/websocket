# SpringBoot的WebSocket实现

## 1. pom文件配置

首先引入依赖

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

再引入Jsp支持依赖

```xml
		<dependency>
			<groupId>org.apache.tomcat.embed</groupId>
			<artifactId>tomcat-embed-jasper</artifactId>
			<version>8.5.12</version>
		</dependency>
		<!-- https://mvnrepository.com/artifact/javax.servlet/javax.servlet-api -->
		<dependency>
			<groupId>javax.servlet</groupId>
			<artifactId>javax.servlet-api</artifactId>
			<version>3.1.0</version>
		</dependency>
		<!-- jstl标签支持 -->
		<dependency>
			<groupId>jstl</groupId>
			<artifactId>jstl</artifactId>
			<version>1.2</version>
		</dependency>
		<!--JSP -->
		<dependency>
			<groupId>javax.servlet.jsp</groupId>
			<artifactId>jsp-api</artifactId>
			<version>2.1</version>
			<scope>provided</scope>
		</dependency>
		<!-- https://mvnrepository.com/artifact/javax.servlet/jstl -->
		<dependency>
			<groupId>javax.servlet</groupId>
			<artifactId>jstl</artifactId>
			<version>1.2</version>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
	</dependencies>
```

## 2. 代码

### 2.1 WebSocket服务端点配置

新建一个配置类，这个类用于定义WebSocket服务器的端点，这样客户端就可以请求服务器的端点。此类还可以定义WebSocket的打开、关闭、错误和发送消息的方法。（2.0.0版本更新）

```java
package com.hcnay.websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig {
    //创建服务器断点
    @Bean
    public ServerEndpointExporter serverEndpointExporter(){
        return new ServerEndpointExporter();
    }
}

```

### 2.2 实现WebSocket的服务端

```java
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

```

### 2.3 开发控制器打开前端页面

```java
package com.hcnay.websocket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/websocket")
public class WebSocketController {
    //跳转websocket页面
    @GetMapping("index")
    public String websocket(){
        return "websocket";
    }
}

```

### 2.4 前端

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>My WebSocket</title>
<script type="text/javascript" src="https://code.jquery.com/jquery-3.2.1.min.js"></script>
<script type="text/javascript" src="/js/websocket.js"></script>
</head>
<body>
    测试一下WebSocket站点吧
    <br />
    <input id="message" type="text" />
    <button onclick="sendMessage();">发送消息</button>
    <button onclick="closeWebSocket();">关闭WebSocket连接</button>
    <div id="context"></div>
</body>
</html>
```



```javascript
var websocket = null;
// 判断当前浏览器是否支持WebSocket
if ('WebSocket' in window) {
	// 创建WebSocket对象,连接服务器端点
	websocket = new WebSocket("ws://localhost:8080/ws");
} else {
	alert('Not support websocket')
}

// 连接发生错误的回调方法
websocket.onerror = function() {
	appendMessage("error");
};

// 连接成功建立的回调方法
websocket.onopen = function(event) {
	appendMessage("open");
};

// 接收到消息的回调方法
websocket.onmessage = function(event) {
	appendMessage(event.data);
};

// 连接关闭的回调方法
websocket.onclose = function() {
	appendMessage("close");
};

// 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，
// 防止连接还没断开就关闭窗口，server端会抛异常。
window.onbeforeunload = function() {
	websocket.close();
};

// 将消息显示在网页上
appendMessage = function (message) {
	var context = $("#context").html() +"<br/>" + message;
	$("#context").html(context);
};

// 关闭连接
closeWebSocket = function () {
	websocket.close();
};

// 发送消息
sendMessage = function () {
	var message = $("#message").val();
	websocket.send(message);
};
```



## 3. 遇到的坑

- SpringBoot的静态文件如JS等，默认在resources的static文件夹下，在引入的时候直接写`/js/websocket.js`即可, 写`./../websocket.js`会导致jsp无法引入js。

- js的写法错误

  ```javascript
  //错误写法
  function add(){}
  //正确写法
  add = function(){}
  ```


