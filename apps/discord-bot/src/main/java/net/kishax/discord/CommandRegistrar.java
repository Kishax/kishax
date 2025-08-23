package net.kishax.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * スラッシュコマンド登録クラス
 */
public class CommandRegistrar {
  private static final Logger logger = LoggerFactory.getLogger(CommandRegistrar.class);

  public static void registerCommands(JDA jda, Config config) {
    try {
      // kishaxコマンドの登録
      jda.upsertCommand(
          Commands.slash("kishax", "Kishax commands")
              .addSubcommands(
                  new SubcommandData("image_add_q", "画像マップをキューに追加するコマンド(urlか添付ファイルのどっちかを指定可能)")
                      .addOption(OptionType.STRING, "url", "画像リンクの設定項目", false)
                      .addOption(OptionType.ATTACHMENT, "image", "ファイルの添付項目", false)
                      .addOption(OptionType.STRING, "title", "画像マップのタイトル設定項目", false)
                      .addOption(OptionType.STRING, "comment", "画像マップのコメント設定項目", false)
              )
      ).queue(
          success -> logger.info("スラッシュコマンドを登録しました"),
          error -> logger.error("スラッシュコマンドの登録に失敗しました", error));

    } catch (Exception e) {
      logger.error("コマンド登録でエラーが発生しました", e);
    }
  }
}
