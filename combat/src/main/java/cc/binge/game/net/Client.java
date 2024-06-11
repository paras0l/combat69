package cc.binge.game.net;


import cc.binge.game.net.message.ChatMessages;
import cc.binge.game.net.message.ChatMessages.ChatMessage;
import cc.binge.game.net.message.ChatMessages.ChatMessagesWrapper;
import cc.binge.game.net.message.ChatMessages.PrivateMessage;
import cc.binge.game.net.message.ChatMessages.Registration;
import com.almasb.fxgl.entity.Entity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Client
    extends SimpleChannelInboundHandler<ChatMessagesWrapper> {
  // ChannelHandler adds callbacks for state changes. This allows the user to hook in to state changes easily

  private String username;
  private String hostname;
  private int port;
  private ChatMessagesWrapper.Builder wrapperBuilder;

  public Entity getPlayer() {
    return player;
  }

  public void setPlayer(Entity player) {
    this.player = player;
  }

  private Entity player;

  public Entity getRival() {
    return rival;
  }

  public void setRival(Entity rival) {
    this.rival = rival;
  }

  private Entity rival;

  public String getUsername() {
    return username;
  }

  public String getHostname() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  private Channel serverChannel;

  public Client(String hostname, int port, String username) {
    this.hostname = hostname;
    this.port = port;
    this.username = username;
    this.wrapperBuilder = ChatMessagesWrapper.newBuilder();
  }



  public void connect() {
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    MessagePipeline pipeline = new MessagePipeline(this);

    Bootstrap bootstrap = new Bootstrap()
        .group(workerGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(pipeline);

    System.out.println("Connecting to " + hostname + ":" + port);
    ChannelFuture cf = bootstrap.connect(hostname, port);
    cf.syncUninterruptibly();
    serverChannel = cf.channel();

    // serverChannel : [id: 0x3ee2691f, L:/127.0.0.1:61888 - R:/127.0.0.1:3000]
    //                                      client address        server address
  }

  public void sendMessage(String message) {

    if(message.trim().length() == 0){
      System.out.println("Empty message inputted. Please retry...");
      return;
    }

    ChatMessage chatMsg = ChatMessage
        .newBuilder()
        .setUsername(username)
        .setMessageBody(message)
        .build();
    if(message.charAt(0) == '/') {
      String[] privateMsgArr = message.trim().split(" ");
      if(privateMsgArr.length == 1){
        String errMessage = "Incorect private message usage: " + System.lineSeparator() + "Syntax: /<destination_user> <message>";
        System.out.println(errMessage);
      }
      else {
        String destinationUser = message.split(" ")[0].substring(1);
        if(message.length() == destinationUser.length()+1){
          System.out.println("Message for destination user is required");
        }
        else if(destinationUser.equalsIgnoreCase(this.username)){
          System.out.println("You cannot send message to yourself");
        }
        else{
          sendPrivateMessage(chatMsg);
        }
      }
    }
    else
      sendChatMessage(chatMsg);
  }

  private void sendChatMessage(ChatMessage chatMsg) {

    /* Note: you could also do:
     * serverChannel.write(msgWrapper);
     * serverChannel.flush();
     * In this case there is no difference, but if you needed to do several
     * writes it would be more efficient to only do a single flush() after
     * the writes. */

    ChannelFuture write = serverChannel.writeAndFlush(
        wrapperBuilder
            .setChatMessage(chatMsg)
            .build()
    );
    write.syncUninterruptibly();
  }

  private void sendPrivateMessage(ChatMessage chatMsg) {
    PrivateMessage privateMsg
        = ChatMessages.PrivateMessage.newBuilder()
        .setDestinationPort(this.port)
        .setDestinationHost(this.hostname)
        .setMessageContents(chatMsg)
        .build();
    ChannelFuture write = serverChannel.writeAndFlush(
        wrapperBuilder
            .setPrivateMessage(privateMsg)
            .build()
    );
    write.syncUninterruptibly();
  }

  public void registerToServer(String username) {
    Registration registration = Registration.newBuilder()
        .setUsername(username)
        .build();

    ChannelFuture write = serverChannel
        .writeAndFlush(this.wrapperBuilder
            .setRegistration(registration)
            .build()
        );
    write.syncUninterruptibly();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    /* A connection has been established */
    InetSocketAddress addr
        = (InetSocketAddress) ctx.channel().remoteAddress();
    System.out.println("Connection established: " + addr);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    /* A channel has been disconnected */
    InetSocketAddress addr
        = (InetSocketAddress) ctx.channel().remoteAddress();
    System.out.println("Connection lost: " + addr);
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx)
      throws Exception {
    /* Writable status of the channel changed */
  }

  @Override
  public void channelRead0(
      ChannelHandlerContext ctx, ChatMessagesWrapper msg) {

    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
    String strDate = sdfDate.format(new Date());
    ChatMessage chatMessage = null;

    StringBuilder msgPrint = new StringBuilder();
    msgPrint.append(strDate);


    if (msg.hasAdminMessage()){
      msgPrint.append(" [Admin] : ");
      msgPrint.append(msg.getAdminMessage().getMessageBody());
    }
    else if (msg.hasPrivateMessage()) {
      PrivateMessage privateMessage = msg.getPrivateMessage();
      msgPrint.append(" [Private] ");
      chatMessage = privateMessage.getMessageContents();
    }
    // ChatMessagesWrapper msg is of type ChatMessage, if not admin or private
    if(chatMessage == null){
      chatMessage = msg.getChatMessage();
    }
    if (!msg.hasAdminMessage()){
      String user = chatMessage.getUsername();
      String messageBody = chatMessage.getMessageBody();
      msgPrint.append(" <" + user + "> : ");
      msgPrint.append(messageBody);
      if(messageBody.contains("向右移动")){
        rival.translateX(5);
      }
    }
    System.out.println(msgPrint.toString());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
  }

  public static class InputReader implements Runnable {
    private Client client;

    public InputReader (Client client) {
      this.client = client;
    }

    public void run() {
      try(BufferedReader reader =
          new BufferedReader(new InputStreamReader(System.in));) {

        while (true) {
          String line = "";
          try {
            line = reader.readLine();
          } catch (IOException e) {
            e.printStackTrace();
            break;
          }
          client.sendMessage(line);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
