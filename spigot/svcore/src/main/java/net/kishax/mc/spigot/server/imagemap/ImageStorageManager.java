package net.kishax.mc.spigot.server.imagemap;

import org.slf4j.Logger;

import net.kishax.mc.common.settings.Settings;

/**
 * 画像ストレージの統括管理クラス
 * 設定に基づいてローカルまたはS3ストレージを初期化・提供
 */
public class ImageStorageManager {
    
    private final Logger logger;
    private final ImageStorage storage;
    private final S3ImageStorage s3Storage;
    
    public ImageStorageManager(Logger logger) {
        this.logger = logger;
        
        // 設定から画像ストレージモードを取得
        String storageMode = Settings.IMAGE_STORAGE_MODE.getValue(); // "local" or "s3"
        
        if ("s3".equalsIgnoreCase(storageMode)) {
            // S3設定を取得
            String bucketName = Settings.S3_BUCKET_NAME.getValue();
            String prefix = Settings.S3_PREFIX.getValue();
            String region = Settings.S3_REGION.getValue();
            boolean useInstanceProfile = Settings.S3_USE_INSTANCE_PROFILE.getBooleanValue();
            boolean cacheEnabled = Settings.S3_CACHE_ENABLED.getBooleanValue();
            String cacheDirectory = Settings.S3_CACHE_DIRECTORY.getValue();
            
            this.s3Storage = new S3ImageStorage(
                logger, 
                bucketName, 
                prefix, 
                region, 
                useInstanceProfile, 
                cacheEnabled, 
                cacheDirectory
            );
            this.storage = s3Storage;
            
            logger.info("ImageStorageManager initialized with S3 storage (bucket: {}, prefix: {})", 
                        bucketName, prefix);
        } else {
            // ローカルストレージ
            String localDirectory = Settings.IMAGE_FOLDER.getValue();
            this.storage = new LocalImageStorage(logger, localDirectory);
            this.s3Storage = null;
            
            logger.info("ImageStorageManager initialized with local storage (directory: {})", 
                        localDirectory);
        }
    }
    
    /**
     * 画像ストレージのインスタンスを取得
     * @return ImageStorageインスタンス
     */
    public ImageStorage getStorage() {
        return storage;
    }
    
    /**
     * 現在のストレージタイプを取得
     * @return "local" または "s3"
     */
    public String getStorageType() {
        return storage.getStorageType();
    }
    
    /**
     * リソースをクリーンアップ
     */
    public void shutdown() {
        if (s3Storage != null) {
            s3Storage.close();
            logger.info("ImageStorageManager shutdown: S3 client closed");
        }
    }
}




