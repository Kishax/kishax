package common;

public enum PermSettings {
	NEW_FMC_USER("group.new-fmc-user"),
    SUB_ADMIN("group.sub-admin"),
    SUPER_ADMIN("group.super-admin"),
    ;

	private final String value;
	
	PermSettings(String key) {
        this.value = key;
    }
	
    public String get() {
        return this.value;
    }
}
