package keyp.forev.fmc.common.socket.message;

public class Message {
    public Web web;
    public Minecraft mc;
    public Discord discord;

    public static class Web {
        public MinecraftConfirm confirm;

        public static class MinecraftConfirm {
            public Message.Minecraft.Who who;
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
    }
}

