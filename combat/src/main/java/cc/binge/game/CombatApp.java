package cc.binge.game;

import static com.almasb.fxgl.dsl.FXGL.*;

import cc.binge.game.enums.EntityType;
import cc.binge.game.net.Client;
import cc.binge.game.net.Client.InputReader;
import cc.binge.game.net.Server;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.CollisionHandler;
import java.io.IOException;
import java.util.Map;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class CombatApp extends GameApplication {

  private Entity player1;
  private Entity player2;

  static Client c = null;

  @Override
  protected void initSettings(GameSettings gameSettings) {  //第一个执行
    gameSettings.setWidth(600);
    gameSettings.setHeight(600);
    gameSettings.setManualResizeEnabled(true);
    gameSettings.setPreserveResizeRatio(true);
    gameSettings.setFullScreenAllowed(true);
    //gameSettings.setFullScreenFromStart(true);
    gameSettings.setTitle("战斗大王");
  }

  @Override
  protected void initGame() {  //第三个
    player1 = FXGL.entityBuilder()
        .type(EntityType.PLAYER)
        .at(300, 300)
        .viewWithBBox("img.png")
        .scaleOrigin(20, 20)
        .with(new CollidableComponent(true))
        .buildAndAttach();
    c.setPlayer(player1);
    player2 = FXGL.entityBuilder()
        .type(EntityType.PLAYER)
        .at(300, 400)
        .viewWithBBox("img.png")
        .scaleOrigin(20, 20)
        .with(new CollidableComponent(true))
        .buildAndAttach();
    c.setRival(player2);
    FXGL.entityBuilder()
        .type(EntityType.COIN)
        .at(500, 200)
        .viewWithBBox(new Circle(15, 15, 15, Color.YELLOW))
        .with(new CollidableComponent(true))
        .buildAndAttach();

    FXGL.entityBuilder()
        .type(EntityType.COIN)
        .at(500, 100)
        .viewWithBBox(new Circle(15, 15, 15, Color.YELLOW))
        .with(new CollidableComponent(true))
        .buildAndAttach();
    FXGL.entityBuilder()
        .type(EntityType.COIN)
        .at(500, 300)
        .viewWithBBox(new Circle(15, 15, 15, Color.YELLOW))
        .with(new CollidableComponent(true))
        .buildAndAttach();
  }

  @Override
  protected void initPhysics() {
    FXGL.getPhysicsWorld()
        .addCollisionHandler(new CollisionHandler(EntityType.PLAYER, EntityType.COIN) {

          // order of types is the same as passed into the constructor
          @Override
          protected void onCollisionBegin(Entity player, Entity coin) {
            coin.removeFromWorld();
          }
        });
  }

  @Override
  protected void initUI() {  //第四个
    Text textPixels = new Text();
    textPixels.setTranslateX(50); // x = 50
    textPixels.setTranslateY(100); // y = 100
    textPixels.textProperty().bind(getWorldProperties().intProperty("pixelsMoved").asString());
    getGameScene().addUINode(textPixels); // add to the scene graph
  }

  @Override
  protected void initInput() {   //第二个
    //绑定输出
    onKey(KeyCode.D, () -> {
      player1.translateX(5); // move right 5 pixels
      c.sendMessage(c.getUsername()+" 向右移动");
      inc("pixelsMoved", +5);
    });
    onKey(KeyCode.A, () -> {
      player2.translateX(-5); // move left 5 pixels 像素
      inc("pixelsMoved", -5);
    });

    onKey(KeyCode.W, () -> {
      player2.translateY(-5); // move up 5 pixels
      inc("pixelsMoved", +5);
    });

    onKey(KeyCode.S, () -> {
      player1.translateY(5); // move down 5 pixels
      inc("pixelsMoved", +5);
    });
    onKey(KeyCode.O, () -> {
      FXGL.play("select.wav");
    });
    onKey(KeyCode.P, () -> {
      FXGL.play("行动失败.wav");
    });
  }


  @Override
  protected void initGameVars(Map<String, Object> vars) {
    vars.put("pixelsMoved", 0);
  }

  public static void main(String[] args) {
    new Thread(() -> {
      if (args[0].equals("isServer")) {
        int port = 8088;
        Server s = new Server(port);
        try {
          s.start(port);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
//        while (true) {
//          System.out.println("Enter an admin message : ");
//          s.sendAdminMessage();
//        }

      if (args.length >= 4) {
        c = new Client(args[1], Integer.parseInt(args[2]), args[3]);
        System.out.println("Current Client Name : " + c.getUsername());
        c.connect();
      }

      if (c == null) {
        System.out.println("Usage: isServer <hostname> <port> <username>");
        System.exit(1);
      }

      c.registerToServer(c.getUsername());

      InputReader reader = new InputReader(c);
      Thread inputThread = new Thread(reader);
      inputThread.start();

    }).start();
//    var bounds = Screen.getPrimary().getVisualBounds(); //获取屏幕分辨率
//    config.height = (int) bounds.getHeight();
//    config.width = (int) bounds.getWidth();
    launch(args);
  }
}
