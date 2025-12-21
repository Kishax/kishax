package net.kishax.mc.common.socket.message;

public class Message {
  public Web web;
  public Minecraft mc;
  public Minecraft minecraft; // ソケット通信用の別名フィールド
  public Discord discord;

  public static class Web {
    public MinecraftConfirm confirm;
    public AuthToken authToken;
    public AuthTokenSaved authTokenSaved;

    public static class MinecraftConfirm {
      public Message.Minecraft.Who who;
    }

    public static class AuthToken {
      public Message.Minecraft.Who who;
      public String token;
      public Long expiresAt;
      public String action; // "create", "update", "refresh"
    }

    public static class AuthTokenSaved {
      public Message.Minecraft.Who who;
      public String token;
    }
  }

  public static class Discord {
    public RuleBook rulebook;

    public static class RuleBook {
      public Boolean sync;
    }
  }

  public static class Minecraft {
    public Server server;
    public Sync sync;
    public Command cmd;
    public Otp otp;

    public static class Who {
      public String name;
      public String uuid;
      public Boolean system;
    }

    public static class Server {
      public String action;
      public String name;
    }

    public static class Sync {
      public String content;
    }

    public static class Command {
      public Teleport teleport;
      public Forward forward;
      public ImageMap imagemap;
      public Input input;

      public static class Teleport {
        public Point point;
        public Player player;

        public static class Point {
          public Who who;
          public String name;
          public Boolean back;
          public Boolean register;
        }

        public static class Player {
          public Who who;
          public String target;
          public Boolean reverse;
        }
      }

      public static class Forward {
        public Who who;
        public String target;
        public String cmd;
      }

      public static class ImageMap {
        public Who who;
        public String type;
      }

      public static class Input {
        public Who who;
        public Boolean mode;
      }
    }

    public static class Otp {
      public String mcid;
      public String uuid;
      public String otp;
      public String action; // "send_otp"
    }
  }
}
