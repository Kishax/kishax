package common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public enum FMCSettings {
	IMAGE_LIMIT_TIMES("imageuploadlimittimes"),
    IMAGE_FOLDER("image_folder"),
    RULEBOOK_CONTENT("rulebook"),
    CONFIRM_URL("confirm_url"),
    ;

    private final Database db = Database.getInstance();
    private final String columnKey;
	private final String value;
	
	FMCSettings(String key) {
        this.columnKey = key;
        String dbValue;
        try (Connection connForSettings = db.getConnection()) {
            String query = "SELECT value FROM settings WHERE name = ?";
            PreparedStatement ps = connForSettings.prepareStatement(query);
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) { 
                dbValue = rs.getString("value");
            } else {
                dbValue = null;
            }
        } catch (SQLException | ClassNotFoundException e) {
            dbValue = null;
        }
        this.value = dbValue;
    }
	
    public String getValue() {
    	return this.value;
    }

    public int getIntValue() {
        return this.value != null ? Integer.parseInt(this.value) : 0;
    }

    public String getColumnKey() {
        return this.columnKey;
    }
}
