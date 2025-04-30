package net.kishax.mc.spigot.server.menu;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Material;

public enum Type {
  GENERAL("Kishaxメニュー", "kishax_menu", Material.ENCHANTED_BOOK),
  SERVER("サーバーメニュー", "server"),
  ONLINE_SERVER("オンラインサーバーメニュー", "online_servers", Material.GREEN_WOOL),
  SERVER_TYPE("サーバータイプメニュー", "server_type", Material.COMPASS),
  SERVER_EACH_TYPE("各サーバータイプメニュー", "server_each_type"),
  IMAGE_MAP("イメージマップメニュー", "image_maps"),
  IMAGE_MAP_LIST("イメージマップリストメニュー", "image_maps_list", Material.MAP),
  SETTING("設定メニュー", "settings", Material.ENCHANTING_TABLE),
  TELEPORT("テレポートメニュー", "teleport", Material.ENDER_PEARL),
  PLAYER_TELEPORT("プレイヤーテレポートメニュー", "player_teleport", Material.PLAYER_HEAD),
  TELEPORT_REQUEST("テレポートリクエストメニュー", "teleport_request", Material.ARROW),
  TELEPORT_REQUEST_ME("逆テレポートリクエストメニュー", "teleport_me_request", Material.TARGET),
  TELEPORT_RESPONSE("テレポートレスポンスメニュー", "teleport_response", Material.SHIELD),
  TELEPORT_RESPONSE_HEAD("テレポートレスポンスヘッダーメニュー", "teleport_response_header"),
  CHOOSE_COLOR("色変更", "choose_color"),
  TELEPORT_POINT_TYPE("テレポートポイントタイプメニュー", "teleport_point_type", Material.LIGHTNING_ROD),
  TELEPORT_POINT_PUBLIC("テレポートポイント<パブリック>", "teleport_point_public", Material.DISPENSER),
  TELEPORT_POINT_PRIVATE("テレポートポイント<プライベート>", "teleport_point_private", Material.OBSERVER),
  TELEPORT_NV_PLAYER("プレイヤーナビ", "teleport_navigate_player", Material.ENDER_EYE),
  DELETE("削除画面", "delete"),
  CHANGE_MATERIAL("表示アイテム変更", "material_change"),
  TP_POINT_MANAGER("テレポートポイント管理画面", "teleport_point_manager"),
  SHORTCUT("ショートカットメニュー", "shortcut")
  ;
  private final String title;
  private final String persistantKey;
  private final Material material;
  Type(String title, String persistantKey, Material... material) {
    this.title = title;
    this.persistantKey = persistantKey;
    if (material.length > 0) {
      this.material = material[0];
    } else {
      this.material = null;
    }
  }

  public String get() {
    return title;
  }

  public String getPersistantKey() {
    return persistantKey;
  }

  public Material getMaterial() {
    return material;
  }

  public static Set<Material> getMaterials() {
    return Arrays.stream(Type.values())
      .map(Type::getMaterial)
      .filter(entry -> entry != null)
      .collect(Collectors.toSet());
  }

  public static List<String> gets() {
    return Arrays.stream(Type.values())
      .map(Type::get)
      .collect(Collectors.toList());
  }

  public static Optional<Type> search(String titleName) {
    return Arrays.stream(Type.values())
      .filter(type -> type.get().equalsIgnoreCase(titleName))
      .findFirst();
  }

  public static Optional<Type> searchPersistantKeys(String key) {
    return Arrays.stream(Type.values())
      .filter(type -> type.getPersistantKey().equalsIgnoreCase(key))
      .findFirst();
  }
}
