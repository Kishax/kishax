package keyp.forev.fmc.spigot.server.menu;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum Type {
    GENERAL("fmc menu"),
    SERVER("server"),
    ONLINE_SERVER("online servers"),
    SERVER_TYPE("server type"),
    SERVER_EACH_TYPE("server each type"),
    IMAGE_MAP("image maps"),
    IMAGE_MAP_LIST("image maps list"),
    SETTING("settings"),
    TELEPORT("teleport"),
    PLAYER_TELEPORT("player teleport"),
    TELEPORT_REQUEST("teleport request"),
    TELEPORT_RESPONSE("teleport response"),
    TELEPORT_RESPONSE_HEAD("teleport response head"),
    CHOOSE_COLOR("choose color"),
    TELEPORT_POINT_TYPE("teleport point type"),
    TELEPORT_POINT("teleport point"),
    TELEPORT_NV_PLAYER("teleport navigate player"),
    DELETE("delete"),
    CHANGE_MATERIAL("material change"),
    TP_POINT_MANAGER("teleport point manager"),

    ;

    private final String name;
    Type(String name) {
        this.name = name;
    }

    public String get() {
        return name;
    }

    public static List<String> getTypes() {
        return Arrays.stream(Type.values())
            .map(Type::get)
            .collect(Collectors.toList());
    }

    public static Optional<Type> getType(String name) {
        return Arrays.stream(Type.values())
            .filter(type -> type.get().equalsIgnoreCase(name))
            .findFirst();
    }
}