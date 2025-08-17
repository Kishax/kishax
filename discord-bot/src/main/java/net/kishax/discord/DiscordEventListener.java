package net.kishax.discord;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discord イベントリスナー
 * リフレクションを使わないシンプルな実装
 */
public class DiscordEventListener extends ListenerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(DiscordEventListener.class);

  private final Config config;

  public DiscordEventListener(Config config) {
    this.config = config;
  }

  @Override
  public void onReady(ReadyEvent event) {
    logger.info("Discord Bot が準備完了: {}", event.getJDA().getSelfUser().getAsTag());

    // プレゼンス設定
    event.getJDA().getPresence().setActivity(
        Activity.playing(config.getDiscordPresenceActivity()));
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    String commandName = event.getName();
    String subcommandName = event.getSubcommandName();

    logger.info("スラッシュコマンド実行: {} - {}", commandName, subcommandName);

    if ("kishax".equals(commandName)) {
      handleKishaxCommand(event, subcommandName);
    }
  }

  @Override
  public void onButtonInteraction(ButtonInteractionEvent event) {
    String buttonId = event.getComponentId();

    logger.info("ボタン押下: {}", buttonId);

    switch (buttonId) {
      case "reqOK" -> handleRequestApproval(event);
      case "reqCancel" -> handleRequestReject(event);
      default -> event.reply("不明なボタンです").setEphemeral(true).queue();
    }
  }

  private void handleKishaxCommand(SlashCommandInteractionEvent event, String subcommand) {
    switch (subcommand) {
      case "image_add_q" -> handleImageAddQueue(event);
      default -> event.reply("不明なサブコマンドです").setEphemeral(true).queue();
    }
  }

  private void handleImageAddQueue(SlashCommandInteractionEvent event) {
    // 画像マップキューに追加の処理
    String url = event.getOption("url") != null ? event.getOption("url").getAsString() : null;
    String title = event.getOption("title") != null ? event.getOption("title").getAsString() : "無題";
    String comment = event.getOption("comment") != null ? event.getOption("comment").getAsString() : "";

    // 添付ファイルの処理
    if (event.getOption("image") != null) {
      var attachment = event.getOption("image").getAsAttachment();
      url = attachment.getUrl();
    }

    if (url == null) {
      event.reply("URLまたは画像ファイルを指定してください").setEphemeral(true).queue();
      return;
    }

    // TODO: 実際の画像マップキューに追加する処理を実装
    event.reply("画像マップをキューに追加しました\\nURL: " + url + "\\nタイトル: " + title)
        .setEphemeral(true)
        .queue();

    logger.info("画像マップキュー追加: URL={}, Title={}, Comment={}", url, title, comment);
  }


  private void handleRequestApproval(ButtonInteractionEvent event) {
    // リクエスト承認の処理
    event.reply("リクエストを承認しました").setEphemeral(true).queue();
    logger.info("リクエスト承認: ユーザー={}", event.getUser().getAsTag());

    // TODO: 実際の承認処理を実装
  }

  private void handleRequestReject(ButtonInteractionEvent event) {
    // リクエスト拒否の処理
    event.reply("リクエストを拒否しました").setEphemeral(true).queue();
    logger.info("リクエスト拒否: ユーザー={}", event.getUser().getAsTag());

    // TODO: 実際の拒否処理を実装
  }
}
