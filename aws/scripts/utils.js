const fs = require("fs");
const path = require("path");

/**
 * ファイルが存在するかチェック
 * @param {string} filePath - チェックするファイルパス
 * @returns {boolean} - ファイルが存在するかどうか
 */
function fileExists(filePath) {
  return fs.existsSync(filePath);
}

/**
 * ディレクトリが存在するかチェック
 * @param {string} dirPath - チェックするディレクトリパス
 * @returns {boolean} - ディレクトリが存在するかどうか
 */
function dirExists(dirPath) {
  return fs.existsSync(dirPath) && fs.statSync(dirPath).isDirectory();
}

/**
 * 環境変数が設定されているかチェック
 * @param {string} envName - チェックする環境変数名
 * @returns {boolean} - 環境変数が設定されているかどうか
 */
function hasEnvVar(envName) {
  return process.env[envName] !== undefined;
}

/**
 * 必要な環境変数がすべて設定されているかチェック
 * @param {string[]} requiredVars - 必要な環境変数名の配列
 * @returns {object} - {valid: boolean, missing: string[]}
 */
function validateRequiredEnvVars(requiredVars) {
  const missing = requiredVars.filter((varName) => !hasEnvVar(varName));
  return {
    valid: missing.length === 0,
    missing,
  };
}

/**
 * CloudFormation設定で使用される必須環境変数のリスト
 */
const REQUIRED_ENV_VARS = [
  "AWS_ACCOUNT_ID",
  "AWS_VPC_ID",
  "AWS_PRIVATE_SUBNET_IDS",
  "AWS_PUBLIC_SUBNET_IDS",
  "AWS_SSL_CERTIFICATE_ARN",
];

module.exports = {
  fileExists,
  dirExists,
  hasEnvVar,
  validateRequiredEnvVars,
  REQUIRED_ENV_VARS,
};

