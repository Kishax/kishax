# S3 Image Map Management System

## 概要

Minecraftサーバーで生成された画像マップをS3バケットに保存・取得する機能。ローカルストレージの容量制約を解消し、複数サーバー間でのマップ共有を可能にする。

## ✅ 実装ステータス

| コンポーネント | ステータス | ファイルパス |
|--------------|----------|------------|
| インターフェース定義 | ✅ 完了 | `apps/mc/spigot/svcore/src/main/java/net/kishax/mc/spigot/server/imagemap/ImageStorage.java` |
| ローカルストレージ実装 | ✅ 完了 | `apps/mc/spigot/svcore/src/main/java/net/kishax/mc/spigot/server/imagemap/LocalImageStorage.java` |
| S3ストレージ実装 | ✅ 完了 | `apps/mc/spigot/svcore/src/main/java/net/kishax/mc/spigot/server/imagemap/S3ImageStorage.java` |
| ストレージ管理クラス | ✅ 完了 | `apps/mc/spigot/svcore/src/main/java/net/kishax/mc/spigot/server/imagemap/ImageStorageManager.java` |
| ImageMap.java統合 | ✅ 完了 | `apps/mc/spigot/svcore/src/main/java/net/kishax/mc/spigot/server/ImageMap.java` |
| Settings.java更新 | ✅ 完了 | `apps/mc/common/src/main/java/net/kishax/mc/common/settings/Settings.java` |
| Gradle依存関係 | ✅ 完了 | `apps/mc/spigot/svcore/build.gradle` (AWS SDK v2追加) |
| MySQL設定SQL | ✅ 完了 | `.bak/db/mc/s3_image_storage_settings.sql` |
| IAM権限 | ✅ 完了 | `terraform/modules/iam/main.tf` (既に設定済み) |
| デプロイドキュメント | ✅ 完了 | `docs/infrastructure/ec2/deployment.md` (3-6節) |

## 要件

### 機能要件
- 画像マップの保存先をローカル/S3で切り替え可能
- S3フラグがtrueの場合、画像保存・取得をS3経由で実行
- 既存のローカルファイルシステム実装との互換性維持
- UUID-based ファイル管理（重複防止）

### 非機能要件
- 画像アップロード/ダウンロードの非同期処理
- エラー時のフォールバック（S3障害時はローカル保存）
- ログ出力による追跡可能性

## S3ディレクトリ構造

```
s3://kishax-production-image-maps/images/
├── 20241201/
│   ├── a1b2c3d4-e5f6-7890-abcd-ef1234567890.png
│   ├── b2c3d4e5-f6g7-8901-bcde-f12345678901.png
│   └── ...
├── 20241202/
│   ├── c3d4e5f6-g7h8-9012-cdef-123456789012.png
│   └── ...
└── 20241215/
    └── ...
```

### ディレクトリ命名規則
- **形式**: `YYYYMMDD/[UUID].png`
- **YYYYMMDD**: 画像生成日（JST）
- **UUID**: Minecraft内部のマップUUID
- **拡張子**: `.png` 固定

### メリット
- 日付ベースでバックアップ・削除が容易
- S3ライフサイクルポリシーで古い画像を自動削除可能
- UUIDで重複を防止

## 設定ファイル（config.yml）

### Spigotプラグイン設定
```yaml
# apps/mc/docker/templates/spigot/plugins/Kishax/config.yml

image_storage:
  # Storage mode: "local" or "s3"
  mode: "s3"
  
  # Local storage settings
  local:
    directory: "/mc/spigot/world/data/images"
  
  # S3 storage settings
  s3:
    enabled: true
    bucket: "kishax-production-image-maps"
    prefix: "images/"
    region: "ap-northeast-1"
    
    # IAMロール使用（EC2インスタンスプロファイル）
    use_instance_profile: true
    
    # 開発環境用（IAMロール使用しない場合）
    # access_key: "AKIAIOSFODNN7EXAMPLE"
    # secret_key: "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
    
    # アップロード設定
    async_upload: true
    upload_timeout_seconds: 30
    
    # キャッシュ設定
    local_cache_enabled: true
    local_cache_directory: "/mc/spigot/cache/images"
    cache_expiry_hours: 24
```

### Velocityプラグイン設定
```yaml
# apps/mc/docker/templates/velocity/plugins/kishax/config.yml

image_storage:
  # Velocityでは画像を直接扱わないが、設定を共有
  mode: "s3"
  s3:
    bucket: "kishax-production-image-maps"
    prefix: "images/"
    region: "ap-northeast-1"
```

## Java実装設計

### 1. 依存関係（Gradle）

#### Spigot プラグイン
```gradle
// apps/mc/spigot/sv1_21_11/build.gradle

dependencies {
    implementation 'org.spigotmc:spigot-api:1.21.11-R0.1-SNAPSHOT'
    
    // AWS SDK for Java v2
    implementation platform('software.amazon.awssdk:bom:2.21.0')
    implementation 'software.amazon.awssdk:s3:2.21.0'
    implementation 'software.amazon.awssdk:auth:2.21.0'
    
    // 非同期処理
    implementation 'com.google.guava:guava:32.1.3-jre'
}
```

### 2. パッケージ構造

```
src/main/java/net/kishax/plugin/
├── KishaxPlugin.java (メインクラス)
├── image/
│   ├── ImageStorageManager.java      (統括管理)
│   ├── ImageStorage.java             (インターフェース)
│   ├── LocalImageStorage.java        (ローカル実装)
│   ├── S3ImageStorage.java           (S3実装)
│   ├── S3Client.java                 (AWS SDK ラッパー)
│   └── ImageCache.java               (ローカルキャッシュ)
└── config/
    └── ImageStorageConfig.java       (設定読み込み)
```

### 3. インターフェース定義

#### ImageStorage.java
```java
package net.kishax.plugin.image;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ImageStorage {
    
    /**
     * 画像を保存
     * @param uuid マップUUID
     * @param imageData 画像データ
     * @return 保存成功時true
     */
    CompletableFuture<Boolean> saveImage(UUID uuid, byte[] imageData);
    
    /**
     * 画像を取得
     * @param uuid マップUUID
     * @return 画像データ（存在しない場合null）
     */
    CompletableFuture<byte[]> loadImage(UUID uuid);
    
    /**
     * 画像の存在確認
     * @param uuid マップUUID
     * @return 存在する場合true
     */
    CompletableFuture<Boolean> exists(UUID uuid);
    
    /**
     * 画像を削除
     * @param uuid マップUUID
     * @return 削除成功時true
     */
    CompletableFuture<Boolean> deleteImage(UUID uuid);
    
    /**
     * ストレージタイプを取得
     * @return "local" or "s3"
     */
    String getStorageType();
}
```

### 4. S3実装クラス

#### S3ImageStorage.java (概要)
```java
package net.kishax.plugin.image;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class S3ImageStorage implements ImageStorage {
    
    private final S3Client s3Client;
    private final String bucketName;
    private final String prefix;
    private final ImageCache cache;
    
    public S3ImageStorage(S3Client s3Client, String bucketName, 
                          String prefix, ImageCache cache) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.prefix = prefix;
        this.cache = cache;
    }
    
    @Override
    public CompletableFuture<Boolean> saveImage(UUID uuid, byte[] imageData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = buildS3Key(uuid);
                
                // S3にアップロード
                PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("image/png")
                    .build();
                
                s3Client.putObject(request, RequestBody.fromBytes(imageData));
                
                // ローカルキャッシュに保存
                cache.put(uuid, imageData);
                
                return true;
            } catch (Exception e) {
                // エラーログ出力
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<byte[]> loadImage(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            // キャッシュ確認
            byte[] cached = cache.get(uuid);
            if (cached != null) {
                return cached;
            }
            
            try {
                String key = buildS3Key(uuid);
                
                GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
                
                byte[] data = s3Client.getObjectAsBytes(request).asByteArray();
                
                // キャッシュに保存
                cache.put(uuid, data);
                
                return data;
            } catch (NoSuchKeyException e) {
                return null; // 画像が存在しない
            } catch (Exception e) {
                // エラーログ出力
                return null;
            }
        });
    }
    
    /**
     * S3キーを生成（YYYYMMDD/UUID.png）
     */
    private String buildS3Key(UUID uuid) {
        String datePrefix = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return prefix + datePrefix + "/" + uuid.toString() + ".png";
    }
    
    @Override
    public String getStorageType() {
        return "s3";
    }
}
```

### 5. ImageStorageManager (ファクトリー)

```java
package net.kishax.plugin.image;

import net.kishax.plugin.config.ImageStorageConfig;

public class ImageStorageManager {
    
    private final ImageStorage storage;
    
    public ImageStorageManager(ImageStorageConfig config) {
        String mode = config.getMode();
        
        if ("s3".equalsIgnoreCase(mode) && config.getS3Config().isEnabled()) {
            // S3ストレージを初期化
            S3Client s3Client = createS3Client(config);
            ImageCache cache = new ImageCache(
                config.getS3Config().getLocalCacheDirectory(),
                config.getS3Config().getCacheExpiryHours()
            );
            
            this.storage = new S3ImageStorage(
                s3Client,
                config.getS3Config().getBucket(),
                config.getS3Config().getPrefix(),
                cache
            );
        } else {
            // ローカルストレージを初期化
            this.storage = new LocalImageStorage(
                config.getLocalConfig().getDirectory()
            );
        }
    }
    
    public ImageStorage getStorage() {
        return storage;
    }
    
    private S3Client createS3Client(ImageStorageConfig config) {
        // AWS SDK v2 クライアント作成
        // IAMロール使用時は認証情報不要（インスタンスプロファイルから自動取得）
        // 開発環境ではアクセスキー/シークレットキー使用
        
        // 実装詳細は後述
    }
}
```

## 既存コードとの統合

### 画像保存箇所の修正例

#### Before (ローカル保存)
```java
// 既存のマップ画像保存処理
File imageFile = new File(worldFolder, "data/images/" + mapUuid + ".png");
ImageIO.write(bufferedImage, "png", imageFile);
```

#### After (S3/ローカル切り替え)
```java
// ImageStorageManager経由で保存
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ImageIO.write(bufferedImage, "png", baos);
byte[] imageData = baos.toByteArray();

imageStorageManager.getStorage()
    .saveImage(mapUuid, imageData)
    .thenAccept(success -> {
        if (success) {
            logger.info("Image saved: " + mapUuid);
        } else {
            logger.error("Failed to save image: " + mapUuid);
        }
    });
```

## IAM権限設定

### EC2インスタンスロールに必要な権限

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:HeadObject"
      ],
      "Resource": "arn:aws:s3:::kishax-production-image-maps/images/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::kishax-production-image-maps",
      "Condition": {
        "StringLike": {
          "s3:prefix": "images/*"
        }
      }
    }
  ]
}
```

## エラーハンドリング

### S3障害時のフォールバック
1. **アップロード失敗時**: ローカルに一時保存し、リトライキューに追加
2. **ダウンロード失敗時**: キャッシュに存在すれば使用、なければエラーログ
3. **タイムアウト**: 30秒でタイムアウト、非同期処理のため本体処理はブロックしない

### ログ出力
```
[INFO] [Kishax] Image storage initialized: S3 (bucket=kishax-production-image-maps)
[INFO] [Kishax] Image saved to S3: 20241215/a1b2c3d4-e5f6-7890-abcd-ef1234567890.png
[WARN] [Kishax] S3 upload failed for UUID a1b2c3d4, falling back to local storage
[ERROR] [Kishax] Failed to load image from S3: UUID b2c3d4e5 (NoSuchKey)
```

## テスト戦略

### 1. ユニットテスト
- モック S3Client を使用
- 各メソッドの正常系・異常系をテスト

### 2. 統合テスト
- LocalStack を使用したS3エミュレーション
- 実際のアップロード/ダウンロード動作確認

### 3. 本番前確認
- 開発環境で実際のS3バケットに接続
- 画像保存・取得・削除の動作確認

## 移行計画

### Phase 1: ローカル保存のまま実装準備
- ImageStorage インターフェース実装
- LocalImageStorage で既存機能を置き換え
- 動作確認

### Phase 2: S3実装追加
- AWS SDK依存追加
- S3ImageStorage 実装
- config.yml で mode="local" のまま

### Phase 3: S3切り替え
- 既存画像をS3にアップロード
- config.yml で mode="s3" に変更
- 本番デプロイ

### Phase 4: 最適化
- キャッシュ機能追加
- リトライロジック実装
- パフォーマンス最適化

## 運用

### バックアップ
```bash
# S3の画像を全てダウンロード
aws s3 sync s3://kishax-production-image-maps/images/ ./backup/images/

# 特定日付の画像のみダウンロード
aws s3 sync s3://kishax-production-image-maps/images/20241215/ ./backup/images/20241215/
```

### 古い画像の削除
```bash
# 30日以前の画像を削除
aws s3 rm s3://kishax-production-image-maps/images/202411 --recursive
```

### S3ライフサイクルポリシー（推奨）
```json
{
  "Rules": [
    {
      "Id": "DeleteOldImages",
      "Status": "Enabled",
      "Prefix": "images/",
      "Expiration": {
        "Days": 90
      }
    }
  ]
}
```

## コスト見積もり

### S3ストレージコスト
- **画像サイズ**: 平均 50KB/枚
- **月間生成数**: 1,000枚
- **保存期間**: 90日（ライフサイクルポリシー）
- **合計サイズ**: 50KB × 3,000枚 = 150MB

**月額コスト**: 約 $0.003 (negligible)

### データ転送コスト
- S3 → EC2 (同一リージョン): 無料
- EC2 → S3 (同一リージョン): 無料

**総コスト**: **ほぼ無料** (ストレージのみ)

## 次のステップ

1. **AWS SDK依存をGradleに追加**
2. **ImageStorageインターフェース実装**
3. **LocalImageStorage実装（既存機能置き換え）**
4. **S3ImageStorage実装**
5. **config.yml設定追加**
6. **IAM権限設定（Terraform）**
7. **統合テスト**
8. **本番デプロイ**






