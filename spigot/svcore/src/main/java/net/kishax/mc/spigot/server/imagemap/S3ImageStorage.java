package net.kishax.mc.spigot.server.imagemap;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * S3への画像保存・取得実装
 */
public class S3ImageStorage implements ImageStorage {

    private final Logger logger;
    private final S3Client s3Client;
    private final String bucketName;
    private final String prefix;
    private final LocalImageStorage localCache;
    private final boolean cacheEnabled;

    public S3ImageStorage(Logger logger, String bucketName, String prefix, String region,
            boolean useInstanceProfile, boolean cacheEnabled, String cacheDirectory) {
        this.logger = logger;
        this.bucketName = bucketName;
        this.prefix = prefix;
        this.cacheEnabled = cacheEnabled;

        // S3クライアントの初期化
        if (useInstanceProfile) {
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .credentialsProvider(InstanceProfileCredentialsProvider.create())
                    .build();
            logger.info("S3Client initialized with Instance Profile (region: {})", region);
        } else {
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .build();
            logger.info("S3Client initialized with default credentials (region: {})", region);
        }

        // ローカルキャッシュの初期化
        if (cacheEnabled) {
            this.localCache = new LocalImageStorage(logger, cacheDirectory);
            logger.info("Local cache enabled: {}", cacheDirectory);
        } else {
            this.localCache = null;
        }
    }

    @Override
    public CompletableFuture<Void> saveImage(BufferedImage image, String imageUUID, String ext, LocalDate date) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 画像をバイト配列に変換
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, ext, baos);
                baos.flush();
                byte[] imageBytes = baos.toByteArray();

                // S3キーの生成
                String s3Key = buildS3Key(imageUUID, ext, date);

                // S3にアップロード
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType("image/" + ext)
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromBytes(imageBytes));
                logger.info("Saved image to S3: s3://{}/{}", bucketName, s3Key);

                // ローカルキャッシュにも保存
                if (cacheEnabled && localCache != null) {
                    localCache.saveImage(image, imageUUID, ext, date).join();
                }

            } catch (Throwable e) {
                logger.error("Failed to save image to S3: {}", e.getMessage());
                throw new RuntimeException("Failed to save image to S3", e);
            }
        });
    }

    @Override
    public CompletableFuture<BufferedImage> loadImage(String imageUUID, String ext, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // キャッシュから取得を試みる
                if (cacheEnabled && localCache != null) {
                    BufferedImage cachedImage = localCache.loadImage(imageUUID, ext, date).join();
                    if (cachedImage != null) {
                        logger.info("Loaded image from local cache: {}", imageUUID);
                        return cachedImage;
                    }
                }

                // S3から取得
                String s3Key = buildS3Key(imageUUID, ext, date);

                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build();

                byte[] imageBytes = s3Client.getObjectAsBytes(getRequest).asByteArray();
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

                logger.info("Loaded image from S3: s3://{}/{}", bucketName, s3Key);

                // キャッシュに保存
                if (cacheEnabled && localCache != null && image != null) {
                    localCache.saveImage(image, imageUUID, ext, date);
                }

                return image;

            } catch (NoSuchKeyException e) {
                logger.warn("Image not found in S3: {}", buildS3Key(imageUUID, ext, date));
                return null;
            } catch (Throwable e) {
                logger.error("Failed to load image from S3: {}", e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(String imageUUID, String ext, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String s3Key = buildS3Key(imageUUID, ext, date);

                HeadObjectRequest headRequest = HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build();

                s3Client.headObject(headRequest);
                return true;

            } catch (NoSuchKeyException e) {
                return false;
            } catch (Throwable e) {
                logger.error("Failed to check existence in S3: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteImage(String imageUUID, String ext, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String s3Key = buildS3Key(imageUUID, ext, date);

                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build();

                s3Client.deleteObject(deleteRequest);
                logger.info("Deleted image from S3: s3://{}/{}", bucketName, s3Key);

                // キャッシュからも削除
                if (cacheEnabled && localCache != null) {
                    localCache.deleteImage(imageUUID, ext, date).join();
                }

                return true;

            } catch (Throwable e) {
                logger.error("Failed to delete image from S3: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public String getStorageType() {
        return "s3";
    }

    /**
     * S3キーを生成
     * 
     * @param imageUUID マップUUID
     * @param ext       拡張子
     * @param date      画像生成日
     * @return S3キー (例: images/20241215/uuid.png)
     */
    private String buildS3Key(String imageUUID, String ext, LocalDate date) {
        String dateStr = date.toString().replace("-", "");
        return prefix + dateStr + "/" + imageUUID + "." + ext;
    }

    /**
     * S3クライアントをクローズ
     */
    public void close() {
        if (s3Client != null) {
            s3Client.close();
            logger.info("S3Client closed");
        }
    }
}
