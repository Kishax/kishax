# Kishax Infrastructure Makefile

include .env

# ECRリポジトリ
AWS_ECR_DISCORD_BOT := $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com/$(AWS_ECR_REPO_DISCORD_BOT_NAME)
AWS_ECR_GATHER_BOT := $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com/$(AWS_ECR_REPO_GATHER_BOT_NAME)
AWS_ECR_WEB := $(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com/$(AWS_ECR_REPO_WEB_BOT_NAME)

.PHONY: help
help: ## ヘルプを表示
	@echo "Infrastructure Makefile"
	@echo ""
	@echo "利用可能なコマンド:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

## =============================================================================
## 監視・ステータス確認
## =============================================================================

.PHONY: status-cloudformation
status-cloudformation: ## CloudFormationスタックステータスを確認
	@echo "📊 CloudFormationスタックステータス確認中..."
	aws cloudformation describe-stacks \
		--stack-name kishax-infrastructure \
		--profile $(AWS_PROFILE) \
		--query 'Stacks[0].{StackStatus:StackStatus,StackStatusReason:StackStatusReason,LastUpdatedTime:LastUpdatedTime}' \
		--output table
	@echo "📋 最新のスタックイベント:"
	aws cloudformation describe-stack-events \
		--stack-name kishax-infrastructure \
		--profile $(AWS_PROFILE) \
		--max-items 10 \
		--query 'StackEvents[].{Timestamp:Timestamp,LogicalResourceId:LogicalResourceId,ResourceStatus:ResourceStatus,ResourceStatusReason:ResourceStatusReason}' \
		--output table

.PHONY: status-services
status-services: ## ECSサービスステータスを確認
	@echo "🏃 ECSサービスステータス確認中..."
	aws ecs describe-services \
		--cluster kishax-infrastructure-cluster \
		--services kishax-discord-bot-service-v2 kishax-gather-bot-service-v2 kishax-web-service-v2 \
		--profile $(AWS_PROFILE) \
		--query 'services[].{ServiceName:serviceName,DesiredCount:desiredCount,RunningCount:runningCount,Status:status}' \
		--output table

# =============================================================================
# サービス再起動 (force-new-deployment)
# =============================================================================

.PHONY: restart-discord-bot
restart-discord-bot: ## Discord Botサービスを再起動 (force-new-deployment)
	@echo "🔄 Discord Botサービスを再起動中..."
	@aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-discord-bot-service-v2 --force-new-deployment --profile $(AWS_PROFILE) > /dev/null
	@echo "✅ Discord Botサービスの再起動を要求しました (新しいタスクで再開)"

.PHONY: restart-gather-bot
restart-gather-bot: ## Gather Botサービスを再起動 (force-new-deployment)
	@echo "🔄 Gather Botサービスを再起動中..."
	@aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-gather-bot-service-v2 --force-new-deployment --profile $(AWS_PROFILE) > /dev/null
	@echo "✅ Gather Botサービスの再起動を要求しました (新しいタスクで再開)"

.PHONY: restart-web
restart-web: ## Webサービスを再起動 (force-new-deployment)
	@echo "🔄 Webサービスを再起動中..."
	@aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-web-service-v2 --force-new-deployment --profile $(AWS_PROFILE) > /dev/null
	@echo "✅ Webサービスの再起動を要求しました (新しいタスクで再開)"

.PHONY: restart-all-services
restart-all-services: restart-discord-bot restart-gather-bot restart-web ## 全ECSサービスを再起動 (force-new-deployment)
	@echo "✅ 全サービスの再起動を要求しました"

# =============================================================================
# サービス有効/無効化 (desired-count操作)
# =============================================================================

.PHONY: enable-discord-bot
enable-discord-bot: ## Discord Botサービスを有効化 (desired-count=1)
	@echo "🟢 Discord Botサービスを有効化中..."
	@aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-discord-bot-service-v2 --desired-count 1 --profile $(AWS_PROFILE) > /dev/null
	@echo "✅ Discord Botサービスを有効化しました"

.PHONY: enable-gather-bot
enable-gather-bot: ## Gather Botサービスを有効化 (desired-count=1)
	@echo "🟢 Gather Botサービスを有効化中..."
	@aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-gather-bot-service-v2 --desired-count 1 --profile $(AWS_PROFILE) > /dev/null
	@echo "✅ Gather Botサービスを有効化しました"

.PHONY: enable-web
enable-web: ## Webサービスを有効化 (desired-count=1)
	@echo "🟢 Webサービスを有効化中..."
	@aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-web-service-v2 --desired-count 1 --profile $(AWS_PROFILE) > /dev/null
	@echo "✅ Webサービスを有効化しました"

.PHONY: enable-all-services
enable-all-services: enable-discord-bot enable-gather-bot enable-web ## 全ECSサービスを有効化
	@echo "✅ 全サービスの有効化を完了しました"

.PHONY: disable-discord-bot
disable-discord-bot: ## Discord Botサービスを無効化 (desired-count=0)
	@echo "🔴 Discord Botサービスを無効化中..."
	@aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-discord-bot-service-v2 --desired-count 0 --profile $(AWS_PROFILE) > /dev/null
	@echo "✅ Discord Botサービスを無効化しました"

.PHONY: disable-gather-bot
disable-gather-bot: ## Gather Botサービスを無効化 (desired-count=0)
	@echo "🔴 Gather Botサービスを無効化中..."
	@aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-gather-bot-service-v2 --desired-count 0 --profile $(AWS_PROFILE) > /dev/null
	@echo "✅ Gather Botサービスを無効化しました"

.PHONY: disable-web
disable-web: ## Webサービスを無効化 (desired-count=0)
	@echo "🔴 Webサービスを無効化中..."
	@aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-web-service-v2 --desired-count 0 --profile $(AWS_PROFILE) > /dev/null
	@echo "✅ Webサービスを無効化しました"

.PHONY: disable-all-services
disable-all-services: disable-discord-bot disable-gather-bot disable-web ## 全ECSサービスを無効化
	@echo "✅ 全サービスの無効化を完了しました"

# =============================================================================
# サービス開始/停止 (タスク操作)
# =============================================================================

.PHONY: start-discord-bot
start-discord-bot: ## Discord Bot停止中サービスを開始
	@echo "▶️ Discord Botサービスを開始中..."
	@CURRENT_COUNT=$$(aws ecs describe-services --cluster kishax-infrastructure-cluster --services kishax-discord-bot-service-v2 --profile $(AWS_PROFILE) --query "services[0].desiredCount" --output text); \
	if [ "$$CURRENT_COUNT" = "0" ]; then \
		aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-discord-bot-service-v2 --desired-count 1 --profile $(AWS_PROFILE) > /dev/null; \
		echo "✅ Discord Botサービスを開始しました"; \
	else \
		echo "ℹ️ Discord Botサービスは既に実行中です (desired-count=$$CURRENT_COUNT)"; \
	fi

.PHONY: start-gather-bot
start-gather-bot: ## Gather Bot停止中サービスを開始
	@echo "▶️ Gather Botサービスを開始中..."
	@CURRENT_COUNT=$$(aws ecs describe-services --cluster kishax-infrastructure-cluster --services kishax-gather-bot-service-v2 --profile $(AWS_PROFILE) --query "services[0].desiredCount" --output text); \
	if [ "$$CURRENT_COUNT" = "0" ]; then \
		aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-gather-bot-service-v2 --desired-count 1 --profile $(AWS_PROFILE) > /dev/null; \
		echo "✅ Gather Botサービスを開始しました"; \
	else \
		echo "ℹ️ Gather Botサービスは既に実行中です (desired-count=$$CURRENT_COUNT)"; \
	fi

.PHONY: start-web
start-web: ## Web停止中サービスを開始
	@echo "▶️ Webサービスを開始中..."
	@CURRENT_COUNT=$$(aws ecs describe-services --cluster kishax-infrastructure-cluster --services kishax-web-service-v2 --profile $(AWS_PROFILE) --query "services[0].desiredCount" --output text); \
	if [ "$$CURRENT_COUNT" = "0" ]; then \
		aws ecs update-service --cluster kishax-infrastructure-cluster --service kishax-web-service-v2 --desired-count 1 --profile $(AWS_PROFILE) > /dev/null; \
		echo "✅ Webサービスを開始しました"; \
	else \
		echo "ℹ️ Webサービスは既に実行中です (desired-count=$$CURRENT_COUNT)"; \
	fi

.PHONY: start-all-services
start-all-services: start-discord-bot start-gather-bot start-web ## 全停止中サービスを開始
	@echo "✅ 全サービスの開始チェックを完了しました"

.PHONY: stop-discord-bot
stop-discord-bot: ## Discord Bot実行中タスクを即座に停止
	@echo "⏹️ Discord Bot実行中タスクを即座停止中..."
	@TASK_ARNS=$$(aws ecs list-tasks --cluster kishax-infrastructure-cluster --service kishax-discord-bot-service-v2 --profile $(AWS_PROFILE) --query "taskArns" --output text); \
	if [ "$$TASK_ARNS" != "" ] && [ "$$TASK_ARNS" != "None" ]; then \
		aws ecs stop-task --cluster kishax-infrastructure-cluster --task $$TASK_ARNS --profile $(AWS_PROFILE) > /dev/null; \
		echo "✅ Discord Botタスクを停止しました"; \
	else \
		echo "ℹ️ Discord Botの実行中タスクはありません"; \
	fi

.PHONY: stop-gather-bot
stop-gather-bot: ## Gather Bot実行中タスクを即座に停止
	@echo "⏹️ Gather Bot実行中タスクを即座停止中..."
	@TASK_ARNS=$$(aws ecs list-tasks --cluster kishax-infrastructure-cluster --service kishax-gather-bot-service-v2 --profile $(AWS_PROFILE) --query "taskArns" --output text); \
	if [ "$$TASK_ARNS" != "" ] && [ "$$TASK_ARNS" != "None" ]; then \
		aws ecs stop-task --cluster kishax-infrastructure-cluster --task $$TASK_ARNS --profile $(AWS_PROFILE) > /dev/null; \
		echo "✅ Gather Botタスクを停止しました"; \
	else \
		echo "ℹ️ Gather Botの実行中タスクはありません"; \
	fi

.PHONY: stop-web
stop-web: ## Web実行中タスクを即座に停止
	@echo "⏹️ Web実行中タスクを即座停止中..."
	@TASK_ARNS=$$(aws ecs list-tasks --cluster kishax-infrastructure-cluster --service kishax-web-service-v2 --profile $(AWS_PROFILE) --query "taskArns" --output text); \
	if [ "$$TASK_ARNS" != "" ] && [ "$$TASK_ARNS" != "None" ]; then \
		aws ecs stop-task --cluster kishax-infrastructure-cluster --task $$TASK_ARNS --profile $(AWS_PROFILE) > /dev/null; \
		echo "✅ Webタスクを停止しました"; \
	else \
		echo "ℹ️ Webの実行中タスクはありません"; \
	fi

.PHONY: stop-all-services
stop-all-services: stop-discord-bot stop-gather-bot stop-web ## 全実行中タスクを即座に停止
	@echo "✅ 全タスクの停止を完了しました"

.PHONY: cancel-stack-update
cancel-stack-update: ## CloudFormationスタック更新をキャンセル
	@echo "❌ CloudFormationスタック更新をキャンセル中..."
	aws cloudformation cancel-update-stack \
		--stack-name kishax-infrastructure \
		--profile $(AWS_PROFILE)
	@echo "✅ スタック更新のキャンセルを要求しました"

## =============================================================================
## デプロイメント
## =============================================================================

.PHONY: deploy-all
deploy-all: deploy-lambda deploy-discord-bot deploy-gather-bot deploy-web ## 全サービスをデプロイ
	@echo "✅ 全サービスのデプロイが完了しました"

.PHONY: deploy-lambda
deploy-lambda: ## Lambda関数をデプロイ
	@echo "🚀 Lambda関数をデプロイ中..."
	cd aws/lambda/sqs-forwarder && \
	npm install && \
	rm -f deployment.zip && \
	zip -r deployment.zip index.js package.json package-lock.json node_modules/ && \
	aws lambda update-function-code \
		--function-name $(AWS_LAMBDA_FUNCTION_NAME) \
		--zip-file fileb://deployment.zip \
		--profile $(AWS_PROFILE)
	@echo "✅ Lambda関数のデプロイが完了しました"

.PHONY: deploy-discord-bot
deploy-discord-bot: ## Discord Botをデプロイ
	@echo "🚀 Discord Botをデプロイ中..."
	cd apps/discord-bot && \
	docker buildx build --platform linux/amd64 -t kishax-discord-bot . && \
	aws ecr get-login-password --region $(AWS_REGION) --profile $(AWS_PROFILE) | \
		docker login --username AWS --password-stdin $(AWS_ECR_DISCORD_BOT) && \
	docker tag kishax-discord-bot:latest $(AWS_ECR_DISCORD_BOT):latest && \
	docker push $(AWS_ECR_DISCORD_BOT):latest && \
	aws ecs update-service \
		--cluster kishax-infrastructure-cluster \
		--service kishax-discord-bot-service-v2 \
		--force-new-deployment \
		--profile $(AWS_PROFILE)
	@echo "✅ Discord Botのデプロイが完了しました"

.PHONY: deploy-gather-bot
deploy-gather-bot: ## Gather Botをデプロイ
	@echo "🚀 Gather Botをデプロイ中..."
	cd apps/gather-bot && \
	docker buildx build --platform linux/amd64 -t kishax-gather-bot . && \
	aws ecr get-login-password --region $(AWS_REGION) --profile $(AWS_PROFILE) | \
		docker login --username AWS --password-stdin $(AWS_ECR_GATHER_BOT) && \
	docker tag kishax-gather-bot:latest $(AWS_ECR_GATHER_BOT):latest && \
	docker push $(AWS_ECR_GATHER_BOT):latest && \
	aws ecs update-service \
		--cluster kishax-infrastructure-cluster \
		--service kishax-gather-bot-service-v2 \
		--force-new-deployment \
		--profile $(AWS_PROFILE)
	@echo "✅ Gather Botのデプロイが完了しました"

.PHONY: deploy-web
deploy-web: ## Web アプリケーションをデプロイ
	@echo "🚀 Web アプリケーションをデプロイ中..."
	cd apps/web && \
	npm install && \
	npx prisma generate && \
	docker buildx build --platform linux/amd64 -t kishax-web . && \
	aws ecr get-login-password --region $(AWS_REGION) --profile $(AWS_PROFILE) | \
		docker login --username AWS --password-stdin $(AWS_ECR_WEB) && \
	docker tag kishax-web:latest $(AWS_ECR_WEB):latest && \
	docker push $(AWS_ECR_WEB):latest && \
	aws ecs update-service \
		--cluster kishax-infrastructure-cluster \
		--service kishax-web-service-v2 \
		--force-new-deployment \
		--profile $(AWS_PROFILE)
	@echo "✅ Web アプリケーションのデプロイが完了しました"

## =============================================================================
## テスト・動作確認
## =============================================================================

.PHONY: test-integration
test-integration: ## 統合テスト実行（API Gateway → SQS → Discord Bot）
	@echo "🧪 Kishax 統合テスト実行中..."
	cd aws/integration-test && make test-integration

.PHONY: test-mc-plugins-integration
test-mc-plugins-integration: ## Minecraft Plugin統合テスト
	@echo "🎮 Minecraft Plugin 統合テスト実行中..."
	cd aws/integration-test && make test-mc-plugins

.PHONY: test-full-flow
test-full-flow: ## 完全フロー統合テスト（MC → API Gateway → Discord）
	@echo "🔄 完全統合フローテスト実行中..."
	cd aws/integration-test && make test-full-flow

.PHONY: test-lambda
test-lambda: ## Lambda関数をテスト
	@echo "🧪 Lambda関数をテスト中..."
	cd aws/lambda/sqs-forwarder && \
	aws lambda invoke \
		--function-name $(AWS_LAMBDA_FUNCTION_NAME) \
		--payload fileb://api-gateway-test.json \
		--profile $(AWS_PROFILE) \
		test-response.json && \
	cat test-response.json
	@echo "✅ Lambda関数のテストが完了しました"

.PHONY: test-api-gateway
test-api-gateway: ## API Gatewayをテスト
	@echo "🧪 API Gatewayをテスト中..."
	aws apigateway test-invoke-method \
		--rest-api-id $(API_GATEWAY_ID) \
		--resource-id $(API_GATEWAY_RESOURCE_ID) \
		--http-method POST \
		--body '{"type": "test_connection", "message": "Makefile test"}' \
		--profile $(AWS_PROFILE)
	@echo "✅ API Gatewayのテストが完了しました"

.PHONY: test-minecraft-discord
test-minecraft-discord: ## Minecraft→Discord連携をテスト
	@echo "🧪 Minecraft→Discord連携をテスト中..."
	@echo "Minecraftサーバーからプレイヤーのjoin/leaveイベントを発生させて、"
	@echo "Discordチャンネルにメッセージが表示されることを確認してください。"

.PHONY: test-player-leave
test-player-leave: ## Player Leave 統合テスト実行
	@echo "🚪 Player Leave 統合テスト実行中..."
	cd aws/integration-test && make test-player-leave

.PHONY: test-player-join
test-player-join: ## Player Join 統合テスト実行
	@echo "🎮 Player Join 統合テスト実行中..."
	cd aws/integration-test && make test-player-join

## =============================================================================
## 監視・デバッグ
## =============================================================================

.PHONY: logs-lambda
logs-lambda: ## Lambdaログを表示
	aws logs tail /aws/lambda/$(AWS_LAMBDA_FUNCTION_NAME) --follow --profile $(AWS_PROFILE)

.PHONY: logs-discord-bot
logs-discord-bot: ## Discord Botログを表示
	aws logs tail /ecs/kishax-discord-bot-v2 --follow --profile $(AWS_PROFILE)

.PHONY: logs-gather-bot
logs-gather-bot: ## Gather Botログを表示
	aws logs tail /ecs/kishax-gather-bot-v2 --follow --profile $(AWS_PROFILE)

.PHONY: logs-web
logs-web: ## Web アプリケーションログを表示
	aws logs tail /ecs/kishax-web-v2 --follow --profile $(AWS_PROFILE)

.PHONY: status-ecs
status-ecs: ## ECSサービス状態を確認
	@echo "📊 ECSサービス状態:"
	@echo "\n🤖 Discord Bot:"
	aws ecs describe-services \
		--cluster kishax-infrastructure-cluster \
		--services kishax-discord-bot-service-v2 \
		--query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount}' \
		--profile $(AWS_PROFILE)
	@echo "\n👥 Gather Bot:"
	aws ecs describe-services \
		--cluster kishax-infrastructure-cluster \
		--services kishax-gather-bot-service-v2 \
		--query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount}' \
		--profile $(AWS_PROFILE)
	@echo "\n🌐 Web Application:"
	aws ecs describe-services \
		--cluster kishax-infrastructure-cluster \
		--services kishax-web-service-v2 \
		--query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount}' \
		--profile $(AWS_PROFILE)

.PHONY: status-lambda
status-lambda: ## Lambda関数状態を確認
	aws lambda get-function --function-name $(AWS_LAMBDA_FUNCTION_NAME) --profile $(AWS_PROFILE) \
		--query '{FunctionName:Configuration.FunctionName,Runtime:Configuration.Runtime,LastModified:Configuration.LastModified,State:Configuration.State}'

## =============================================================================
## 開発ツール
## =============================================================================

.PHONY: ssm-backup
ssm-backup: ## SSMパラメータをバックアップ
	@echo "💾 SSMパラメータをバックアップ中..."
	aws ssm get-parameters-by-path \
		--path "/kishax" \
		--recursive \
		--with-decryption \
		--profile $(AWS_PROFILE) \
		--query "Parameters[*].{Name:Name,Value:Value}" \
		--output json | \
	jq -r '.[] | "# " + .Name + "\n" + (.Name | gsub("/kishax/"; "") | gsub("/"; "_") | ascii_upcase) + "=" + .Value + "\n"' > .env.backup.new
	@echo "✅ SSMパラメータが .env.backup.new に保存されました"

.PHONY: validate-ssm
validate-ssm: ## SSMパラメータ設定を確認
	@echo "🔍 SSMパラメータ確認中..."
	aws ssm get-parameters-by-path \
		--path "/kishax" \
		--recursive \
		--profile $(AWS_PROFILE) \
		--query "Parameters[*].{Name:Name,Type:Type}" \
		--output table

## =============================================================================
## 環境設定
## =============================================================================

.PHONY: setup-aws-auth
setup-aws-auth: ## AWS認証設定ガイド表示
	@echo "🔐 AWS認証設定ガイド:"
	@echo ""
	@echo "1. AWS SSO ログイン:"
	@echo "   aws sso login --profile $(AWS_PROFILE)"
	@echo ""
	@echo "2. 認証状態確認:"
	@echo "   aws sts get-caller-identity --profile $(AWS_PROFILE)"
	@echo ""
	@echo "3. Minecraft Plugin用 IAMユーザー:"
	@echo "   - ユーザー名: $(AWS_IAM_ROLE_NAME_FOR_API_GATEWAY)"
	@echo "   - アクセスキーID: $(AWS_IAM_ROLE_NAME_FOR_API_GATEWAY_ACCESS_KEY)"
	@echo "   - ポリシー: $(AWS_IAM_POLICY_NAME_FOR_API_GATEWAY)"

.PHONY: setup-prerequisites
setup-prerequisites: ## 前提条件チェック
	@echo "🔍 前提条件チェック中..."
	@command -v aws >/dev/null 2>&1 || { echo "❌ AWS CLI がインストールされていません"; exit 1; }
	@command -v docker >/dev/null 2>&1 || { echo "❌ Docker がインストールされていません"; exit 1; }
	@command -v node >/dev/null 2>&1 || { echo "❌ Node.js がインストールされていません"; exit 1; }
	@command -v java >/dev/null 2>&1 || { echo "❌ Java がインストールされていません"; exit 1; }
	@echo "✅ 全ての前提条件が満たされています"

## =============================================================================
## 初回セットアップ
## =============================================================================

.PHONY: setup-first-time
setup-first-time: setup-prerequisites setup-aws-auth ## 初回セットアップ
	@echo "🎉 初回セットアップを開始します"
	@echo ""
	@echo "次の手順を実行してください:"
	@echo "1. make setup-aws-auth の指示に従ってAWS認証を設定"
	@echo "2. DEPLOY.md を参考にSSMパラメータを設定"
	@echo "3. make deploy-all でサービスをデプロイ"
	@echo ""
	@echo "詳細な手順は DEPLOY.md を参照してください"


.PHONY: aws-install-deps
aws-install-deps: ## AWS設定生成ツールの依存関係をインストール
	@echo "📦 AWS設定生成ツールの依存関係をインストール中..."
	@cd aws/scripts && npm install
	@echo "✅ 依存関係のインストールが完了しました"

.PHONY: generate-prod-configs
generate-prod-configs: ## 本番用AWS設定ファイルを動的生成
	@echo "🔧 本番用AWS設定ファイルを生成中..."
	@if [ ! -d "aws/scripts/node_modules" ]; then \
		echo "⚠️  依存関係が見つかりません。インストールを実行します..."; \
		$(MAKE) aws-install-deps; \
	fi
	@cd aws/scripts && npm run generate
	@echo "✅ 本番用設定ファイルの生成が完了しました"

.PHONY: update-infra
update-infra: generate-prod-configs ## CloudFormationスタックを更新
	@echo "🚀 CloudFormationスタックを更新中..."
	aws cloudformation update-stack \
		--profile $(AWS_PROFILE) \
		--region $(AWS_REGION) \
		--stack-name kishax-infrastructure \
		--template-body file://aws/cloudformation-template.prod.yaml \
		--parameters file://aws/cloudformation-parameters.prod.json \
		--capabilities CAPABILITY_NAMED_IAM
	@echo "✅ CloudFormationスタックの更新を開始しました"


.PHONY: update-ssm
update-ssm: ## aws/ssm-parameters.json の内容をSSMに一括反映
	@echo "🚀 SSMパラメータを更新中..."
	@if ! command -v jq > /dev/null; then \
		echo "❌ 'jq' is not installed. Please install it to continue."; \
		exit 1; \
	fi
	@jq -c '.[]' aws/ssm-parameters.json | while read -r item; do \
		name=$$(echo $$item | jq -r '.Name'); \
		value=$$(echo $$item | jq -r '.Value'); \
		type=$$(echo $$item | jq -r '.Type'); \
		echo "Updating $$name..."; \
		aws ssm put-parameter \
			--name "$$name" \
			--value "$$value" \
			--type "$$type" \
			--profile $(AWS_PROFILE) \
			--overwrite > /dev/null; \
	done
	@echo "✅ SSMパラメータの更新が完了しました"

## =============================================================================
## Docker (Buildx)
## =============================================================================

.PHONY: buildx-and-push
buildx-and-push: ## 指定されたサービスのDockerイメージをビルドし、ECRにプッシュします (例: make buildx-and-push service=web)
	@if [ -z "$(service)" ]; then \
		echo "❌ 'service' arugment is required. (e.g., make buildx-and-push service=web)"; \
		exit 1; \
	fi
	@echo "🚀 Building and pushing $(service) image for linux/amd64..."
	@{ \
		service_upper=$$(echo $(service) | tr '[:lower:]' '[:upper:]' | tr '-' '_'); \
		ecr_repo_var=ECR_REPO_$${service_upper}; \
		ecr_repo=$$($$(ecr_repo_var)); \
		\
		cd apps/$(service) && \
		docker buildx build --platform linux/amd64 -t kishax-$(service):latest-amd64 . && \
		aws ecr get-login-password --region $(AWS_REGION) --profile $(AWS_PROFILE) | \
			docker login --username AWS --password-stdin $${ecr_repo} && \
		docker tag kishax-$(service):latest-amd64 $${ecr_repo}:latest-amd64 && \
		docker push $${ecr_repo}:latest-amd64; \
	}
	@echo "✅ Successfully pushed kishax-$(service):latest-amd64 to ECR."
	@echo "ℹ️ 注: このコマンドはECSサービスを自動で更新しません。"
	@echo "   'make deploy-$(service)' を実行するか、手動でサービスを更新してください。"
