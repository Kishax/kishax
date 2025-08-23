const fs = require("fs");
const path = require("path");

// プロジェクトルートの.envファイルを読み込み
require("dotenv").config({ path: path.resolve(__dirname, "../../.env") });

/**
 * $(環境変数名) 形式の文字列を実際の環境変数値に置換
 * @param {string} content - 置換対象の文字列
 * @returns {string} - 置換後の文字列
 */
function replaceEnvVariables(content) {
  return content.replace(/\$\(([^)]+)\)/g, (match, envName) => {
    const value = process.env[envName];
    if (value === undefined) {
      throw new Error(
        `❌ 環境変数 ${envName} が設定されていません。.envファイルを確認してください。`,
      );
    }
    console.log(`✓ ${envName} = ${value}`);
    return value;
  });
}

/**
 * 本番用設定ファイルを動的生成
 */
function generateProdFiles() {
  console.log("🔧 本番用AWS設定ファイルを生成中...");
  console.log("📁 環境変数ファイル: " + path.resolve(__dirname, "../../.env"));

  const files = [
    "cloudformation-template.yaml",
    "cloudformation-parameters.json",
  ];

  files.forEach((file) => {
    try {
      const templatePath = path.join(__dirname, "..", file);
      const prodPath = path.join(
        __dirname,
        "..",
        file.replace(/\.([^.]+)$/, ".prod.$1"),
      );

      console.log(`\n📄 処理中: ${file}`);

      // テンプレートファイル読み込み
      if (!fs.existsSync(templatePath)) {
        throw new Error(
          `テンプレートファイルが見つかりません: ${templatePath}`,
        );
      }

      const content = fs.readFileSync(templatePath, "utf8");

      // 環境変数置換
      const prodContent = replaceEnvVariables(content);

      // 本番用ファイル書き込み
      fs.writeFileSync(prodPath, prodContent);

      console.log(
        `✅ 生成完了: ${file} -> ${file.replace(/\.([^.]+)$/, ".prod.$1")}`,
      );
    } catch (error) {
      console.error(`❌ エラー: ${file} の処理中に問題が発生しました`);
      console.error(error.message);
      process.exit(1);
    }
  });

  console.log("\n🎉 全ての本番用設定ファイルの生成が完了しました!");
  console.log("📋 生成されたファイル:");
  console.log("  - aws/cloudformation-template.prod.yaml");
  console.log("  - aws/cloudformation-parameters.prod.json");
}

// メイン処理実行
if (require.main === module) {
  generateProdFiles();
}

module.exports = { generateProdFiles, replaceEnvVariables };

