package f5.si.kishax.mc.spigot.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InvSaver {
  public static final Map<Player, List<ItemStack>> playerInventorySnapshot = new HashMap<>();

  public static void save(Player player) {
    List<ItemStack> snapshot = new ArrayList<>();
    for (ItemStack item : player.getInventory().getContents()) {
      snapshot.add(item == null ? null : item.clone());
    }
    playerInventorySnapshot.put(player, snapshot);
  }

  public static List<ItemStack> getDiffItems(Player player) {
    List<ItemStack> oldSnapshot = playerInventorySnapshot.get(player);
    List<ItemStack> currentInventory = Arrays.asList(player.getInventory().getContents());

    if (oldSnapshot != null) {
      return calculateDifference(oldSnapshot, currentInventory);
    }

    return Collections.emptyList();
  }

  // 差分を計算するメソッド
  private static List<ItemStack> calculateDifference(List<ItemStack> oldInventory, List<ItemStack> newInventory) {
    Map<ItemStack, Integer> oldCount = countItems(oldInventory);
    Map<ItemStack, Integer> newCount = countItems(newInventory);

    List<ItemStack> removedItems = new ArrayList<>();

    for (Map.Entry<ItemStack, Integer> entry : oldCount.entrySet()) {
      ItemStack item = entry.getKey();
      int oldAmount = entry.getValue();
      int newAmount = newCount.getOrDefault(item, 0);

      if (newAmount < oldAmount) {
        ItemStack removed = item.clone();
        removed.setAmount(oldAmount - newAmount);
        removedItems.add(removed);
      }
    }

    return removedItems;
  }

  // アイテムの種類と数をカウント
  private static Map<ItemStack, Integer> countItems(List<ItemStack> inventory) {
    Map<ItemStack, Integer> itemCount = new HashMap<>();
    for (ItemStack item : inventory) {
      if (item != null) {
        itemCount.put(item, itemCount.getOrDefault(item, 0) + item.getAmount());
      }
    }

    return itemCount;
  }
}
