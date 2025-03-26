package keyp.forev.fmc.common.database;

import com.google.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

public class CreateTable {
  private final Logger logger;

  @Inject
  public CreateTable(Logger logger) {
    this.logger = logger;
  }

  public CompletableFuture<List<CompletableFuture<Boolean>>> createTables(Connection conn) throws SQLException {
    return CompletableFuture.supplyAsync(() -> {
      List<CompletableFuture<Boolean>> futuresList = new ArrayList<>();
      Arrays.stream(Table.values())
          .collect(Collectors.toList()).stream()
          .forEach(entry -> futuresList.add(createTable(conn, entry)));
      return futuresList;
    });
  }

  public CompletableFuture<Boolean> createTable(Connection conn, Table table) {
    return CompletableFuture.supplyAsync(() -> {
      try (PreparedStatement ps = conn.prepareStatement(table.get())) {
        int success = ps.executeUpdate();
        if (success > 0) {
          return true;
        }
        return true; // case: table already exists
      } catch (SQLException e) {
        String tableName = table.getTableName();
        if (tableName != null) {
          logger.error("Cannot create {} Tables: {}", tableName, e);
        } else {
          logger.error("Cannot create something SQL Tables: {}", e);
        }
      }
      return false;
    });
  }

  public enum Table {
    COORDS("""
          CREATE TABLE IF NOT EXISTS `coords` (
            `id` int NOT NULL AUTO_INCREMENT,
            `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
            `player` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
            `world` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
            `x` double DEFAULT NULL,
            `y` double DEFAULT NULL,
            `z` double DEFAULT NULL,
            `yaw` float DEFAULT NULL,
            `pitch` float DEFAULT NULL,
            `server` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
            `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`)
          ) ENGINE = InnoDB AUTO_INCREMENT = 4 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
        """),
    IMAGE_TILES("""
          CREATE TABLE IF NOT EXISTS `image_tiles` (
            `id` int NOT NULL AUTO_INCREMENT,
            `server` varchar(255) NOT NULL,
            `mapid` int NOT NULL,
            `x` int NOT NULL,
            `y` int NOT NULL,
            `image` longblob NOT NULL,
            PRIMARY KEY (`id`)
          ) ENGINE = InnoDB AUTO_INCREMENT = 489 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
        """),
    IMAGES("""
          CREATE TABLE IF NOT EXISTS `images` (
            `id` int NOT NULL AUTO_INCREMENT,
            `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
            `uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
            `mapid` int DEFAULT NULL,
            `server` varchar(255) DEFAULT NULL,
            `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
            `imuuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
            `ext` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
            `url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
            `comment` text,
            `isqr` tinyint(1) DEFAULT '0',
            `otp` varchar(255) DEFAULT NULL,
            `d` tinyint unsigned DEFAULT '0',
            `dname` varchar(255) DEFAULT NULL,
            `did` varchar(255) DEFAULT NULL,
            `menu` tinyint(1) DEFAULT '0',
            `menuer` varchar(255) DEFAULT NULL,
            `confirm` tinyint(1) DEFAULT '0',
            `date` date NOT NULL,
            `large` tinyint(1) DEFAULT '0',
            `locked` tinyint(1) DEFAULT '0',
            `locked_action` tinyint(1) DEFAULT '1',
            PRIMARY KEY (`id`)
          ) ENGINE = InnoDB AUTO_INCREMENT = 185 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
        """),
    INVALID_LOGIN("""
          CREATE TABLE IF NOT EXISTS `invalid_login` (
            `ids` int NOT NULL AUTO_INCREMENT,
            `id` int DEFAULT NULL,
            `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `server` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `ban` tinyint(1) DEFAULT '0',
            `emid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `confirm` tinyint(1) DEFAULT '0',
            `member_id` int DEFAULT NULL,
            `admin` tinyint(1) DEFAULT '0',
            `secret2` int DEFAULT NULL,
            `req` timestamp NULL DEFAULT NULL,
            `sst` timestamp NULL DEFAULT NULL,
            `st` timestamp NULL DEFAULT NULL,
            `old_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            PRIMARY KEY (`ids`)
          ) ENGINE = InnoDB AUTO_INCREMENT = 2 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
        """),
    LOG("""
          CREATE TABLE IF NOT EXISTS `log` (
            `id` int NOT NULL AUTO_INCREMENT,
            `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `server` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `playtime` int DEFAULT NULL,
            `first` tinyint(1) DEFAULT '0',
            `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `ban` tinyint(1) DEFAULT '0',
            `req` tinyint(1) DEFAULT '0',
            `reqserver` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `ssserver` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `sss` tinyint(1) DEFAULT '0',
            `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `join` tinyint(1) DEFAULT '0',
            `register` tinyint(1) DEFAULT '0',
            `cname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `quit` tinyint(1) DEFAULT '0',
            `reqsul` tinyint(1) DEFAULT '0',
            `reqsulstatus` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `stop` tinyint(1) unsigned zerofill DEFAULT '0',
            PRIMARY KEY (`id`)
          ) ENGINE = InnoDB AUTO_INCREMENT = 7237 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
        """),
    MEMBER("""
          CREATE TABLE IF NOT EXISTS `members` (
            `ids` int DEFAULT NULL,
            `id` int NOT NULL AUTO_INCREMENT,
            `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `server` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `ban` tinyint(1) DEFAULT '0',
            `emid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `confirm` tinyint(1) DEFAULT '0',
            `member_id` int DEFAULT NULL,
            `secret2` int DEFAULT NULL,
            `req` timestamp NULL DEFAULT NULL,
            `st` timestamp NULL DEFAULT NULL,
            `old_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `msgId` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `mententer` tinyint(1) unsigned zerofill DEFAULT '0',
            `hubinv` tinyint(1) DEFAULT '1',
            `tptype` tinyint(1) DEFAULT '1',
            `silent` tinyint(1) DEFAULT '0',
            PRIMARY KEY (`id`)
          ) ENGINE = InnoDB AUTO_INCREMENT = 215 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
        """),
    SETTINGS("""
          CREATE TABLE IF NOT EXISTS `settings` (
            `id` int NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
            `name` varchar(255) NOT NULL,
            `value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
            PRIMARY KEY (`id`)
          ) ENGINE = InnoDB AUTO_INCREMENT = 14 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
        """),
    STATUS("""
          CREATE TABLE IF NOT EXISTS `status` (
            `id` int NOT NULL AUTO_INCREMENT,
            `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `port` int DEFAULT NULL,
            `online` tinyint(1) DEFAULT '0',
            `player_list` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `current_players` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
            `exception` tinyint(1) DEFAULT '0',
            `exception2` tinyint(1) DEFAULT '0',
            `type` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
            `socketport` int DEFAULT '0',
            `platform` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
            `entry` tinyint(1) DEFAULT '0',
            `modded_mode` tinyint(1) DEFAULT '0',
            `memory` int DEFAULT '0',
            `modded_listUrl` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
            `distributed_url` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
            `distributed_mode` tinyint(1) DEFAULT '0',
            `modded_loaderType` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
            `modded_loaderUrl` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
            `exec` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
            `hub` tinyint(1) DEFAULT '0',
            PRIMARY KEY (`id`)
          ) ENGINE = InnoDB AUTO_INCREMENT = 62 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
        """),
    TP_POINTS("""
          CREATE TABLE IF NOT EXISTS `tp_points` (
            `id` int NOT NULL AUTO_INCREMENT,
            `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
            `uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
            `world` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
            `x` double DEFAULT NULL,
            `y` double DEFAULT NULL,
            `z` double DEFAULT NULL,
            `yaw` float DEFAULT NULL,
            `pitch` float DEFAULT NULL,
            `server` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
            `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `title` varchar(255) NOT NULL,
            `type` varchar(255) NOT NULL,
            `comment` text,
            PRIMARY KEY (`id`)
          ) ENGINE = InnoDB AUTO_INCREMENT = 10 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
        """),
    ;

    private final String query;

    Table(String query) {
      this.query = query;
    }

    public String get() {
      return this.query;
    }

    public String getTableName() {
      Pattern tablePattern = Pattern.compile("CREATE TABLE `(\\w+)`");
      Matcher tableMatcher = tablePattern.matcher(query);
      if (tableMatcher.find()) {
        return tableMatcher.group(1);
      }

      return null;
    }

    public static List<String> getAll() {
      return Arrays.stream(Table.values())
          .map(Table::get)
          .collect(Collectors.toList());
    }
  }
}
