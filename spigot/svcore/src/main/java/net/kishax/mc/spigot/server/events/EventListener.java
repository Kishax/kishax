package net.kishax.mc.spigot.server.events;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.settings.Settings;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.Message;
import java.security.SecureRandom;
import net.kishax.mc.common.server.ServerStatusCache;
import net.kishax.mc.spigot.server.InvSaver;
import net.kishax.mc.spigot.server.InventoryCheck;
import net.kishax.mc.spigot.server.ItemFrames;
import net.kishax.mc.spigot.server.cmd.sub.Confirm;
import net.kishax.mc.spigot.server.menu.Menu;
import net.kishax.mc.spigot.server.menu.Type;
import net.kishax.mc.spigot.settings.Coords;
import net.kishax.mc.spigot.util.RunnableTaskUtil;
import net.kishax.mc.spigot.util.config.PortalsConfig;
import net.kishax.mc.spigot.util.interfaces.MessageRunnable;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class EventListener implements Listener {
  public static Map<Player, Map<RunnableTaskUtil.Key, MessageRunnable>> playerInputerMap = new HashMap<>();
  public static Map<Player, Map<RunnableTaskUtil.Key, BukkitTask>> playerTaskMap = new HashMap<>();
  public static final Map<Player, Location> playerBeforeLocationMap = new HashMap<>();
  public static final AtomicBoolean isHub = new AtomicBoolean(false);
  private final JavaPlugin plugin;
  private final BukkitAudiences audiences;
  private final Logger logger;
  private final Database db;
  private final PortalsConfig psConfig;
  private final Menu menu;
  private final ServerStatusCache ssc;
  private final Luckperms lp;
  private final InventoryCheck inv;
  private final ItemFrames fif;
  private final Provider<SocketSwitch> sswProvider;
  private final Set<Player> playersInPortal = new HashSet<>();
  
  // QRコード右クリック制限用マップ（プレイヤー -> 最後のクリック時間）
  private final Map<Player, Long> qrClickCooldowns = new HashMap<>();
  private static final long QR_CLICK_COOLDOWN_MS = 30000; // 30秒のクールダウン

  @Inject
  public EventListener(JavaPlugin plugin, BukkitAudiences audiences, Logger logger, Database db, PortalsConfig psConfig,
      Menu menu, ServerStatusCache ssc, Luckperms lp, InventoryCheck inv, ItemFrames fif, Provider<SocketSwitch> sswProvider) {
    this.plugin = plugin;
    this.audiences = audiences;
    this.logger = logger;
    this.db = db;
    this.psConfig = psConfig;
    this.menu = menu;
    this.ssc = ssc;
    this.lp = lp;
    this.inv = inv;
    this.fif = fif;
    this.sswProvider = sswProvider;
  }

  @EventHandler
  public void onBowShoot(EntityShootBowEvent event) {
    if (event.getEntity() instanceof Player && event.getProjectile() instanceof Arrow) {
      Player player = (Player) event.getEntity();
      Arrow arrowTile = (Arrow) event.getProjectile();
      if (arrowTile != null) {
        if (arrowTile.getPersistentDataContainer()
            .has(new NamespacedKey(plugin, Type.TELEPORT_REQUEST.getPersistantKey()), PersistentDataType.STRING)) {
          event.setCancelled(true);
          Component message = Component.text("ショートカットメニュー属性の矢を放つことはできません！")
              .color(NamedTextColor.RED)
              .decorate(TextDecoration.BOLD);

          audiences.player(player).sendMessage(message);
          return;
        }
      }
    }
  }

  @EventHandler
  public void onInventoryMoveItem(InventoryMoveItemEvent event) {
    ItemStack item = event.getItem();
    if (item != null && item.hasItemMeta()) {
      ItemMeta meta = item.getItemMeta();
      Set<Material> materials = Type.getMaterials();
      if (materials.contains(item.getType())) {
        menu.getShortCutMap().forEach((key, value) -> {
          NamespacedKey keyKey = new NamespacedKey(plugin, key.getPersistantKey());
          if (meta.getPersistentDataContainer().has(keyKey, PersistentDataType.STRING)) {
            event.setCancelled(true); // 移動をキャンセル
          }
        });
      }
    }
  }

  @EventHandler
  public void onPrepareItemCraft(PrepareItemCraftEvent event) {
    if (event.getView().getPlayer() instanceof Player player) {
      ItemStack[] items = event.getInventory().getMatrix();
      for (ItemStack item : items) {
        if (item != null) {
          Material material = item.getType();
          Set<Material> materials = Type.getMaterials();
          if (materials.contains(material)) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
              menu.getShortCutMap().forEach((key, value) -> {
                if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, key.getPersistantKey()),
                    PersistentDataType.STRING)) {
                  player.closeInventory();
                  Component message = Component.text("ショートカットメニュー属性のアイテムをクラフトエリアにセットすることはできません！")
                      .color(NamedTextColor.RED)
                      .decorate(TextDecoration.BOLD);
                  audiences.player(player).sendMessage(message);
                  return;
                }
              });
            }
          }
        }
      }
    }
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    Action action = event.getAction();
    ItemStack item = player.getInventory().getItemInMainHand();

    if (item != null) {
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
          Material material = item.getType();

          // QRコード右クリック処理
          if (material == Material.FILLED_MAP && meta instanceof MapMeta) {
            MapMeta mapMeta = (MapMeta) meta;
            if (isConfirmMap(mapMeta)) {
              event.setCancelled(true);
              handleConfirmMapRightClick(player);
              return;
            }
          }

          Set<Material> materials = Type.getMaterials();
          if (materials.contains(material)) {
            menu.getShortCutMap().forEach((key, value) -> {
              if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, key.getPersistantKey()),
                  PersistentDataType.STRING)) {
                event.setCancelled(true);
                player.closeInventory();
                value.run(player);
                return;
              }
            });
          }
        }
      }
    }
  }

  @Deprecated
  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) throws SQLException {
    if (event.getWhoClicked() instanceof Player player) {
      ClickType clickType = event.getClick();
      int slot = event.getRawSlot();
      String title = event.getView().getOriginalTitle();
      Inventory inv = event.getInventory();
      InventoryType invType = inv.getType();

      ItemStack item = event.getCurrentItem();
      if (item != null) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
          // 自動作業台を利用する間はイベントをキャンセル
          if (invType.equals(InventoryType.CRAFTER)) {
            Material material = item.getType();
            Set<Material> materials = Type.getMaterials();
            if (materials.contains(material)) {
              menu.getShortCutMap().forEach((key, value) -> {
                if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, key.getPersistantKey()),
                    PersistentDataType.STRING)) {
                  player.closeInventory();
                  Component message = Component.text("ショートカットメニュー属性のアイテムをクラフトエリアにセットすることはできません！")
                      .color(NamedTextColor.RED)
                      .decorate(TextDecoration.BOLD);

                  audiences.player(player).sendMessage(message);
                  return;
                }
              });
            }
          } else if (clickType.isRightClick() || clickType == ClickType.CREATIVE) {
            Material material = item.getType();
            Set<Material> materials = Type.getMaterials();
            if (materials.contains(material)) {
              menu.getShortCutMap().forEach((key, value) -> {
                if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, key.getPersistantKey()),
                    PersistentDataType.STRING)) {
                  event.setCancelled(true);
                  player.closeInventory();
                  value.run(player);
                  return;
                }
              });
            }
          }
        }
      }

      Optional<Type> type = Type.search(title);
      if (type.isPresent()) {
        Type menuType = type.get();
        if (!menuType.equals(Type.CHANGE_MATERIAL)) {
          event.setCancelled(true);
        }
        menu.runMenuEventAction(player, menuType, slot, event);
      } else if (title.endsWith(" server")) {
        event.setCancelled(true);
        Map<String, Map<String, Map<String, Object>>> serverStatusMap = ssc.getStatusMap();
        String serverName = title.split(" ")[0];
        boolean iskey = serverStatusMap.entrySet().stream()
            .anyMatch(e -> e.getValue().entrySet().stream()
                .anyMatch(e2 -> {
                  if (e2.getKey() instanceof String) {
                    String statusServerName = (String) e2.getKey();
                    return statusServerName.equals(serverName);
                  }
                  return false;
                }));
        if (iskey) {
          menu.runMenuEventAction(player, Type.SERVER, slot, event);
        }
      } else if (title.endsWith(" servers")) {
        event.setCancelled(true);
        menu.runMenuEventAction(player, Type.SERVER_EACH_TYPE, slot, event);
      }
    }
  }

  @Deprecated
  @EventHandler
  public void onInventoryOpen(InventoryOpenEvent event) {
    if (event.getPlayer() instanceof Player player) {
      String title = event.getView().getOriginalTitle();

      Optional<Type> type = Type.search(title);
      if (type.isPresent()) {
        Type menuType = type.get();
        if (menuType == Type.CHANGE_MATERIAL) {
          InvSaver.save(player);
          logger.info("Inventory snapshot saved for " + player.getName());
        }
      }
    }
  }

  @Deprecated
  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (event.getPlayer() instanceof Player player) {
      String title = event.getView().getOriginalTitle();
      Optional<Type> type = Type.search(title);
      if (type.isPresent()) {
        Type menuType = type.get();
        if (menuType == Type.CHANGE_MATERIAL) {
          AtomicBoolean isDone = Menu.menuEventFlags.get(player).get(Type.CHANGE_MATERIAL);
          if (isDone.compareAndSet(false, true)) {
            InvSaver.getDiffItems(player).forEach(item -> {
              player.getInventory().addItem(item);
              TextComponent message = Component.text()
                  .append(
                      Component.text("アイテムセット中にインベントリから離れました。").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                  .appendNewline()
                  .append(Component.text("アイテム名: " + item.getType()).color(NamedTextColor.GRAY)
                      .decorate(TextDecoration.ITALIC))
                  .appendNewline()
                  .append(
                      Component.text("かしこみかしこみ、謹んでお返し申す。").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                  .appendNewline()
                  .append(Component.text("※アイテムを返却しました。").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                  .build();

              audiences.player(player).sendMessage(message);
            });
          }

          InvSaver.playerInventorySnapshot.remove(player);
        }
      }
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    Confirm.confirmMap.remove(player);
    playerInputerMap.remove(player);
    playerTaskMap.remove(player);
    EventListener.playerBeforeLocationMap.remove(player);
  }

  @Deprecated
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    event.setJoinMessage(null);
    Player player = event.getPlayer();
    if (EventListener.isHub.get()) {
      int permLevel = lp.getPermLevel(player.getName());
      if (permLevel < 1) {
        Location loadLocation = Coords.LOAD_POINT.getLocation();
        if (loadLocation != null) {
          player.teleport(loadLocation);
        } else {
          Location currentLocation = player.getLocation();
          Coords.LOAD_POINT.saveLocation(currentLocation);
          player.sendMessage(ChatColor.YELLOW + "初回参加のため、現在の座標をロードポイントとして設定しました。");
        }
      } else {
        Location hubLocation = Coords.HUB_POINT.getLocation();
        if (hubLocation != null) {
          player.teleport(hubLocation);
        } else {
          Location currentLocation = player.getLocation();
          Coords.HUB_POINT.saveLocation(currentLocation);
          player.sendMessage(ChatColor.YELLOW + "初回参加のため、現在の座標をハブポイントとして設定しました。");
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
          player.setGameMode(GameMode.CREATIVE);
          player.sendMessage(ChatColor.GREEN + "クリエイティブモードに変更しました。");
        }
      }
    }
    inv.updatePlayerInventory(player);

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      fif.loadWorldsItemFrames();
    }, 20L);
  }

  @Deprecated
  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    Chunk chunk = event.getChunk();
    try (Connection conn = db.getConnection()) {
      for (Entity entity : chunk.getEntities()) {
        if (entity instanceof ItemFrame) {
          ItemFrame itemFrame = (ItemFrame) entity;
          fif.replaceImageMap(conn, itemFrame);
        }
      }
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("Error updating item frames: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }

  @Deprecated
  @EventHandler
  public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();
    String message = event.getMessage();
    if (EventListener.playerInputerMap.containsKey(player)) {
      event.setCancelled(true);
      Map<RunnableTaskUtil.Key, MessageRunnable> map = EventListener.playerInputerMap.get(player);
      map.entrySet().forEach(action -> {
        RunnableTaskUtil.Key key = action.getKey();

        List<RunnableTaskUtil.Key> keys = Arrays.stream(RunnableTaskUtil.Key.values())
            .collect(Collectors.toList());

        if (keys.contains(key)) {
          MessageRunnable runnable = action.getValue();
          runnable.run(message);
        }
      });
    }
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (plugin.getConfig().getBoolean("Portals.Move", false)) {
      Player player = event.getPlayer();
      Location loc = player.getLocation();
      if (loc != null) {
        Block block = loc.getBlock();
        if (WandListener.isMakePortal) {
          WandListener.isMakePortal = false;
        }
        List<Map<?, ?>> portals = psConfig.getListMap("portals");
        boolean isInAnyPortal = false;
        if (portals != null) {
          for (Map<?, ?> portal : portals) {
            String name = (String) portal.get("name");
            List<?> corner1List = (List<?>) portal.get("corner1");
            List<?> corner2List = (List<?>) portal.get("corner2");
            if (corner1List != null && corner2List != null) {
              Location corner1 = new Location(player.getWorld(),
                  ((Number) corner1List.get(0)).doubleValue(),
                  ((Number) corner1List.get(1)).doubleValue(),
                  ((Number) corner1List.get(2)).doubleValue());
              Location corner2 = new Location(player.getWorld(),
                  ((Number) corner2List.get(0)).doubleValue(),
                  ((Number) corner2List.get(1)).doubleValue(),
                  ((Number) corner2List.get(2)).doubleValue());
              if (isWithinBounds(loc, corner1, corner2)) {
                isInAnyPortal = true;
                if (!playersInPortal.contains(player)) {
                  playersInPortal.add(player);
                  logger.info("Player {} entered the gate: {}!", new Object[] { player.getName(), name });
                  switch (name) {
                    case "survival", "minigame", "mod", "others", "online", "dev" -> {
                      event.setCancelled(true);
                      player.performCommand("kishax menu server " + name);
                    }
                    case "waterGate" -> {
                      if (block.getType() == Material.WATER) {
                        // 水面に入ったときの音を流す
                        player.playSound(player, Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 1.0f);
                        player.performCommand("kishax check");
                      }
                    }
                    case "confirm" -> {
                      player.performCommand("kishax confirm");
                    }
                  }
                }
                break;
              }
            }
          }
        }

        if (!isInAnyPortal) {
          playersInPortal.remove(player);
        }
      }
    }
  }

  @EventHandler
  public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
    Player player = e.getPlayer();
    if (playersInPortal.contains(player)) {
      playersInPortal.remove(player);
    }
  }

  private boolean isWithinBounds(Location loc, Location corner1, Location corner2) {
    double x1 = Math.min(corner1.getX(), corner2.getX());
    double x2 = Math.max(corner1.getX(), corner2.getX());
    double y1 = Math.min(corner1.getY(), corner2.getY());
    double y2 = Math.max(corner1.getY(), corner2.getY());
    double z1 = Math.min(corner1.getZ(), corner2.getZ());
    double z2 = Math.max(corner1.getZ(), corner2.getZ());

    return loc.getX() >= x1 && loc.getX() < x2 + 1 &&
        loc.getY() >= y1 && loc.getY() < y2 + 1 &&
        loc.getZ() >= z1 && loc.getZ() < z2 + 1;
  }

  /**
   * QRコードマップかどうかを判定
   */
  private boolean isConfirmMap(MapMeta mapMeta) {
    List<String> lore = mapMeta.getLore();
    return lore != null && lore.stream().anyMatch(line -> line.contains("<QRコード>"));
  }

  /**
   * QRコード右クリック時の処理
   */
  private void handleConfirmMapRightClick(Player player) {
    // クールダウンチェック
    long currentTime = System.currentTimeMillis();
    Long lastClickTime = qrClickCooldowns.get(player);
    
    if (lastClickTime != null && (currentTime - lastClickTime) < QR_CLICK_COOLDOWN_MS) {
      long remainingSeconds = (QR_CLICK_COOLDOWN_MS - (currentTime - lastClickTime)) / 1000;
      Component cooldownMessage = Component.text("QRコードは" + remainingSeconds + "秒後に再度右クリックできます。")
          .color(NamedTextColor.YELLOW);
      audiences.player(player).sendMessage(cooldownMessage);
      return;
    }
    
    // クリック時間を記録
    qrClickCooldowns.put(player, currentTime);
    
    try (Connection conn = db.getConnection()) {
      // プレイヤーの認証情報を取得
      Map<String, Object> memberMap = db.getMemberMap(conn, player.getName());
      if (memberMap.isEmpty()) {
        Component errorMessage = Component.text("プレイヤー情報が見つかりません。")
            .color(NamedTextColor.RED);
        audiences.player(player).sendMessage(errorMessage);
        return;
      }

      if (!(memberMap.get("id") instanceof Integer id)) {
        Component errorMessage = Component.text("プレイヤーIDの取得に失敗しました。")
            .color(NamedTextColor.RED);
        audiences.player(player).sendMessage(errorMessage);
        return;
      }

      String playerUUID = player.getUniqueId().toString();
      String authToken;
      String action;
      long expiresAt;
      
      // 既存のトークンを確認
      Map<String, Object> tokenInfo = db.getAuthTokenInfo(conn, playerUUID);
      if (!tokenInfo.isEmpty() && tokenInfo.get("token") != null) {
        String existingToken = (String) tokenInfo.get("token");
        java.sql.Timestamp expires = (java.sql.Timestamp) tokenInfo.get("expires");
        
        // トークンが有効かチェック
        if (expires != null && expires.getTime() > System.currentTimeMillis()) {
          // 既存の有効なトークンを使用
          authToken = existingToken;
          expiresAt = expires.getTime();
          action = "refresh";
        } else {
          // トークンが無効なので新規生成
          authToken = generateAuthToken(player);
          expiresAt = System.currentTimeMillis() + (10 * 60 * 1000);
          db.updateAuthToken(conn, playerUUID, authToken, expiresAt);
          action = "update";
        }
      } else {
        // トークンが存在しないので新規生成
        authToken = generateAuthToken(player);
        expiresAt = System.currentTimeMillis() + (10 * 60 * 1000);
        db.updateAuthToken(conn, playerUUID, authToken, expiresAt);
        action = "create";
      }

      // 認証URLを生成
      String authUrl = Settings.CONFIRM_URL.getValue() + "?t=" + authToken;

      // Velocity経由でWeb側にプレイヤー情報とトークンを送信  
      sendAuthTokenToVelocity(conn, player, authToken, expiresAt, action);

      // 認証URLをプレイヤーに送信
      sendAuthenticationInfo(player, authUrl);

    } catch (SQLException | ClassNotFoundException e) {
      Component errorMessage = Component.text("認証情報の生成に失敗しました。")
          .color(NamedTextColor.RED);
      audiences.player(player).sendMessage(errorMessage);
      logger.error("Error generating authentication info: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }

  /**
   * 認証トークンを生成
   */
  private String generateAuthToken(Player player) {
    return generateOTP(32) + "_" + System.currentTimeMillis();
  }

  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final SecureRandom random = new SecureRandom();

  private static String generateOTP(int length) {
    StringBuilder otp = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      int index = random.nextInt(CHARACTERS.length());
      otp.append(CHARACTERS.charAt(index));
    }
    return otp.toString();
  }



  /**
   * 認証情報をプレイヤーに送信
   */
  private void sendAuthenticationInfo(Player player, String authUrl) {
    Component titleMessage = Component.text("━━━━━ MC認証情報 ━━━━━")
        .color(NamedTextColor.GOLD)
        .decorate(TextDecoration.BOLD);

    Component urlLabel = Component.text("認証URL: ")
        .color(NamedTextColor.YELLOW);

    Component urlComponent = Component.text(authUrl)
        .color(NamedTextColor.AQUA)
        .decorate(TextDecoration.UNDERLINED)
        .clickEvent(ClickEvent.openUrl(authUrl))
        .hoverEvent(HoverEvent.showText(Component.text("クリックして認証ページを開く")));

    Component instructionMessage = Component.text("右クリックで認証URLを取得できます。アクセスすることで、Kishaxサーバでの主要機能をアンロックできます。")
        .color(NamedTextColor.GRAY)
        .decorate(TextDecoration.ITALIC);

    Component separatorMessage = Component.text("━━━━━━━━━━━━━━━━━━━")
        .color(NamedTextColor.GOLD)
        .decorate(TextDecoration.BOLD);

    audiences.player(player).sendMessage(titleMessage);
    audiences.player(player).sendMessage(Component.empty());
    audiences.player(player).sendMessage(urlLabel.append(urlComponent));
    audiences.player(player).sendMessage(Component.empty());
    audiences.player(player).sendMessage(instructionMessage);
    audiences.player(player).sendMessage(separatorMessage);
  }

  /**
   * Velocity経由でWeb側に認証トークン情報を送信
   */
  private void sendAuthTokenToVelocity(Connection conn, Player player, String token, long expiresAt, String action) {
    try {
      Message msg = new Message();
      msg.web = new Message.Web();
      msg.web.authToken = new Message.Web.AuthToken();
      msg.web.authToken.who = new Message.Minecraft.Who();
      msg.web.authToken.who.name = player.getName();
      msg.web.authToken.who.uuid = player.getUniqueId().toString();
      msg.web.authToken.token = token;
      msg.web.authToken.expiresAt = expiresAt;
      msg.web.authToken.action = action;

      SocketSwitch ssw = sswProvider.get();
      ssw.sendVelocityServer(conn, msg);
      
      logger.info("Sent auth token info to Velocity for player: {} (action: {})", player.getName(), action);
    } catch (Exception e) {
      logger.error("Failed to send auth token info to Velocity: {}", e.getMessage());
    }
  }

  /**
   * プレイヤーの認証状態をチェック
   */
  public boolean isPlayerAuthenticated(Player player) {
    try (Connection conn = db.getConnection()) {
      return checkPlayerAuthStatus(conn, player.getName());
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("Error checking player auth status: {}", e.getMessage());
      return false;
    }
  }

  /**
   * データベースでプレイヤーの認証状態をチェック
   */
  private boolean checkPlayerAuthStatus(Connection conn, String playerName) throws SQLException {
    String query = "SELECT perm_level FROM members WHERE name = ? LIMIT 1";
    try (PreparedStatement ps = conn.prepareStatement(query)) {
      ps.setString(1, playerName);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          int permLevel = rs.getInt("perm_level");
          return permLevel >= 1; // 権限レベル1以上で認証済みと判定
        }
      }
    }
    return false;
  }

  /**
   * プレイヤーの認証状態を更新（認証完了時）
   */
  public void updatePlayerAuthStatus(Connection conn, String playerUUID, boolean authenticated) throws SQLException {
    int permLevel = authenticated ? 1 : 0;
    String query = "UPDATE members SET perm_level = ? WHERE uuid = ?";
    try (PreparedStatement ps = conn.prepareStatement(query)) {
      ps.setInt(1, permLevel);
      ps.setString(2, playerUUID);
      ps.executeUpdate();
    }
  }
}
