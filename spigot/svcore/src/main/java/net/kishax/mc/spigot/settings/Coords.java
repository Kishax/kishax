package net.kishax.mc.spigot.settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import net.kishax.mc.common.database.Database;

public enum Coords {
  LOAD_POINT("load"),
  ROOM_POINT("room"),
  HUB_POINT("hub");

  private final Database db = Database.getInstance();
  private Map<String, Object> maps = new HashMap<>();

  Coords(String key) {
    Map<String, Object> rowMap = new HashMap<>();
    try (Connection connForCoords = db.getConnection()) {
      String query = "SELECT * FROM coords WHERE name = ?";
      PreparedStatement ps = connForCoords.prepareStatement(query);
      ps.setString(1, key);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        int columnCount = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
          String columnName = rs.getMetaData().getColumnName(i);
          rowMap.put(columnName, rs.getObject(columnName));
        }
      }
    } catch (SQLException | ClassNotFoundException e) {
      this.maps = null;
    }
    this.maps = rowMap;
  }

  public Location getLocation() {
    String worldName = getWorld();
    if (worldName == null || worldName.isEmpty()) {
      return null;
    }
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      return null;
    }
    return new Location(world, getX(), getY(), getZ(), getYaw(), getPitch());
  }

  public Map<String, Object> get() {
    return this.maps;
  }

  public Double getX() {
    return maps.get("x") instanceof Double ? (double) maps.get("x") : 0;
  }

  public Double getY() {
    return maps.get("y") instanceof Double ? (double) maps.get("y") : 0;
  }

  public Double getZ() {
    return maps.get("z") instanceof Double ? (double) maps.get("z") : 0;
  }

  public String getWorld() {
    return maps.get("world") instanceof String ? (String) maps.get("world") : "";
  }

  public Float getYaw() {
    return maps.get("yaw") instanceof Float ? (float) maps.get("yaw") : 0;
  }

  public Float getPitch() {
    return maps.get("pitch") instanceof Float ? (float) maps.get("pitch") : 0;
  }

  public String getServer() {
    return maps.get("server") instanceof String ? (String) maps.get("server") : "";
  }

  public void saveLocation(Location location) {
    try (Connection conn = db.getConnection()) {
      String query = "INSERT INTO coords (name, x, y, z, world, yaw, pitch, server) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE x=?, y=?, z=?, world=?, yaw=?, pitch=?, server=?";
      PreparedStatement ps = conn.prepareStatement(query);
      String name = this.name().toLowerCase().replace("_point", "");
      ps.setString(1, name);
      ps.setDouble(2, location.getX());
      ps.setDouble(3, location.getY());
      ps.setDouble(4, location.getZ());
      ps.setString(5, location.getWorld().getName());
      ps.setFloat(6, location.getYaw());
      ps.setFloat(7, location.getPitch());
      ps.setString(8, "spigot");
      ps.setDouble(9, location.getX());
      ps.setDouble(10, location.getY());
      ps.setDouble(11, location.getZ());
      ps.setString(12, location.getWorld().getName());
      ps.setFloat(13, location.getYaw());
      ps.setFloat(14, location.getPitch());
      ps.setString(15, "spigot");
      ps.executeUpdate();
    } catch (SQLException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}
