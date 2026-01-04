package net.kishax.mc.spigot.server.imagemap;

import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * 画像ストレージの抽象インターフェース
 * ローカルファイルシステムまたはS3への画像保存・取得を提供
 */
public interface ImageStorage {
    
    /**
     * 画像を保存
     * @param image 保存する画像
     * @param imageUUID マップUUID
     * @param ext 拡張子 (png, jpg等)
     * @param date 画像生成日
     * @return 非同期処理の結果
     */
    CompletableFuture<Void> saveImage(BufferedImage image, String imageUUID, String ext, LocalDate date);
    
    /**
     * 画像を取得
     * @param imageUUID マップUUID
     * @param ext 拡張子
     * @param date 画像生成日
     * @return 画像データ、存在しない場合はnull
     */
    CompletableFuture<BufferedImage> loadImage(String imageUUID, String ext, LocalDate date);
    
    /**
     * 画像が存在するか確認
     * @param imageUUID マップUUID
     * @param ext 拡張子
     * @param date 画像生成日
     * @return 存在する場合はtrue
     */
    CompletableFuture<Boolean> exists(String imageUUID, String ext, LocalDate date);
    
    /**
     * 画像を削除
     * @param imageUUID マップUUID
     * @param ext 拡張子
     * @param date 画像生成日
     * @return 削除に成功した場合はtrue
     */
    CompletableFuture<Boolean> deleteImage(String imageUUID, String ext, LocalDate date);
    
    /**
     * ストレージタイプを取得
     * @return "local" または "s3"
     */
    String getStorageType();
}







