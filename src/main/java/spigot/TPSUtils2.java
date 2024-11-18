package spigot;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;

public class TPSUtils2 {
    @SuppressWarnings("UseSpecificCatch")
    public static double getTPS() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            Field tpsField = server.getClass().getField("recentTps");
            double[] tps = (double[]) tpsField.get(server);
            return tps[0]; // 1分間の平均TPSを返す
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
