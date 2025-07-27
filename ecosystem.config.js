module.exports = {
	apps: [
		{
			// アプリ基本設定
			name: "gather-bot",
			script: "index.js",
			cwd: "./",

			// インスタンス設定
			instances: 1,
			exec_mode: "fork",

			// 自動再起動設定
			autorestart: true,
			watch: false,
			max_memory_restart: "500M",
			restart_delay: 5000,

			// 環境変数
			env: {
				NODE_ENV: "production",
				PORT: 3000,
			},
			env_development: {
				NODE_ENV: "development",
				PORT: 3000,
			},

			// ログ設定
			log_file: "./logs/combined.log",
			out_file: "./logs/out.log",
			error_file: "./logs/error.log",
			time: true,
			log_date_format: "YYYY-MM-DD HH:mm:ss Z",
			merge_logs: true,

			// プロセス設定
			min_uptime: "10s",
			max_restarts: 10,

			// その他設定
			source_map_support: true,
			instance_var: "INSTANCE_ID",

			// 高度な設定
			kill_timeout: 5000,
			listen_timeout: 8000,

			// PM2+監視設定
			pmx: true,

			// ログローテーション設定（pm2-logrotateが必要）
			log_type: "json",
		},
	],

	// デプロイ設定（オプション）
	deploy: {
		production: {
			user: "ubuntu",
			host: ["your-server.com"],
			ref: "origin/main",
			repo: "git@github.com:username/gather-slack-bot.git",
			path: "/var/www/gather-slack-bot",
			"pre-deploy-local": "",
			"post-deploy":
				"npm install && pm2 reload ecosystem.config.js --env production",
			"pre-setup": "",
		},
	},
};
