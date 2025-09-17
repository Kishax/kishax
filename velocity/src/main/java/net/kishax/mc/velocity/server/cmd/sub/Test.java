package net.kishax.mc.velocity.server.cmd.sub;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kishax.api.auth.AuthLevel;
import net.kishax.mc.common.database.Database;
import net.kishax.mc.velocity.auth.McAuthService;
import net.kishax.mc.velocity.server.cmd.sub.interfaces.Request;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Singleton
public class Test implements Request {
  private static final Logger logger = LoggerFactory.getLogger(Test.class);

  public static final List<String> args1 = new ArrayList<>(List.of("getPlayerLevel"));

  private final McAuthService mcAuthService;
  private final Database database;

  @Inject
  public Test(McAuthService mcAuthService, Database database) {
    this.mcAuthService = mcAuthService;
    this.database = database;
  }

  @Override
  public void execute(CommandSource source, String[] args) {
    if (args.length < 2) {
      source.sendMessage(Component.text("Usage: /kishax test <subcommand> [args...]", NamedTextColor.RED));
      source.sendMessage(Component.text("Available subcommands: getPlayerLevel", NamedTextColor.YELLOW));
      return;
    }

    String subCommand = args[1].toLowerCase();

    switch (subCommand) {
      case "getplayerlevel" -> handleGetPlayerLevel(source, args);
      default -> source.sendMessage(Component.text("Unknown test subcommand: " + subCommand, NamedTextColor.RED));
    }
  }

  private void handleGetPlayerLevel(CommandSource source, String[] args) {
    if (args.length < 3) {
      source.sendMessage(Component.text("Usage: /kishax test getPlayerLevel <mcid>", NamedTextColor.RED));
      return;
    }

    String mcid = args[2];

    // Get UUID from database
    getPlayerUuid(mcid).thenCompose(uuid -> {
      if (uuid == null) {
        source.sendMessage(Component.text("Player not found in members table: " + mcid, NamedTextColor.RED));
        return CompletableFuture.completedFuture(null);
      }

      source.sendMessage(
          Component.text("Checking auth level for player: " + mcid + " (UUID: " + uuid + ")", NamedTextColor.YELLOW));

      // Check permission level using McAuthService
      return mcAuthService.checkPermissionAsync(mcid, uuid);

    }).thenAccept(response -> {
      if (response == null) {
        return;
      }

      AuthLevel authLevel = response.getAuthLevel();
      List<String> activeProducts = response.getActiveProducts();
      String kishaxUserId = response.getKishaxUserId();

      source.sendMessage(Component.text("=== Auth Level Check Result ===", NamedTextColor.GREEN));
      source.sendMessage(Component.text("Player: " + mcid, NamedTextColor.AQUA));
      source.sendMessage(Component.text("Auth Level: " + authLevel.name() + " (" + authLevel.getDescription() + ")",
          NamedTextColor.AQUA));
      source.sendMessage(
          Component.text("Active Products: " + (activeProducts.isEmpty() ? "None" : String.join(", ", activeProducts)),
              NamedTextColor.AQUA));
      source.sendMessage(Component.text("Kishax User ID: " + (kishaxUserId != null ? kishaxUserId : "Not linked"),
          NamedTextColor.AQUA));
      source.sendMessage(Component.text("===============================", NamedTextColor.GREEN));

    }).exceptionally(throwable -> {
      logger.error("Error checking auth level for player: " + mcid, throwable);
      source.sendMessage(Component.text("Error checking auth level: " + throwable.getMessage(), NamedTextColor.RED));
      return null;
    });
  }

  private CompletableFuture<String> getPlayerUuid(String mcid) {
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = database.getConnection()) {
        String sql = "SELECT uuid FROM members WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
          stmt.setString(1, mcid);
          try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
              return rs.getString("uuid");
            }
          }
        }
      } catch (SQLException | ClassNotFoundException e) {
        logger.error("Error getting UUID for player: " + mcid, e);
      }
      return null;
    });
  }

  /**
   * Get list of MCIDs from members table for tab completion
   */
  public CompletableFuture<List<String>> getMcidList() {
    return CompletableFuture.supplyAsync(() -> {
      List<String> mcids = new ArrayList<>();
      try (Connection conn = database.getConnection()) {
        String sql = "SELECT name FROM members ORDER BY name";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
          try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
              mcids.add(rs.getString("name"));
            }
          }
        }
      } catch (SQLException | ClassNotFoundException e) {
        logger.error("Error getting MCID list from members table", e);
      }
      return mcids;
    });
  }

  // Required by Request interface - not used for test commands
  @Override
  public void execute2(Player player, String targetServerName) {
    // Not implemented for test commands
  }

  @Override
  public String getExecPath(String serverName) {
    // Not implemented for test commands
    return "";
  }

  @Override
  public Map<String, String> paternFinderMapForReq(String buttonMessage) {
    // Not implemented for test commands
    return Collections.emptyMap();
  }
}
