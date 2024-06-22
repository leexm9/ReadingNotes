## 第 3 章 Netty 实现

在上一章节中使用 Java 原生的 nio 实现 Reactor 线程模型，在实现中需要我们考虑较多的细节问题，诸如多线程、并发等。Netty 框架是 nio reactor 线程模型的实现，帮助我们屏蔽了诸多烦人和易出错的细节，帮助我们构建高性能的网络客户端和服务端程序。

服务端实现：

```java
public class NettyServer {

    private static final int PORT = 8088;

    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup workers = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(boss, workers)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new ServerHandler());
                    }
                });

        serverBootstrap.bind(PORT).addListener(future -> {
            System.out.println("服务器启动，等待连接中......");
        });
    }

}
```

客户端实现：

```java
public class NettyClient {

    private static final int PORT = 8088;

    private static final String HOST = "127.0.0.1";

    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress(HOST, PORT)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ClientHandler());
                    }
                });

        try {
            for (int i = 0; i < 10; i++) {
                // Start the client.
                ChannelFuture future = bootstrap.connect().sync();
                // Wait until the connection is closed.
                future.channel().closeFuture().sync();
            }
        } finally {
            group.shutdownGracefully();
        }
    }

}
```