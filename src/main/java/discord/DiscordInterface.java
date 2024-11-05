package discord;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;

public interface DiscordInterface {
	CompletableFuture<JDA> loginDiscordBotAsync();
	CompletableFuture<Void> logoutDiscordBot();
	CompletableFuture<String> getMessageContent();
	void sendRequestButtonWithMessage(String buttonMessage);
	void sendWebhookMessage(WebhookMessageBuilder builder);
	CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription, boolean isChat);
	CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription);
	void getBotMessage(String messageId, Consumer<MessageEmbed> embedConsumer, boolean isChat);
	MessageEmbed addDescriptionToEmbed(MessageEmbed embed, String additionalDescription);
	void editBotEmbedReplacedAll(String messageId, MessageEmbed newEmbed);
	CompletableFuture<String> sendBotMessageAndgetMessageId(String content, MessageEmbed embed, boolean isChat);
	CompletableFuture<String> sendBotMessageAndgetMessageId(String content);
	CompletableFuture<String> sendBotMessageAndgetMessageId(MessageEmbed embed);
	CompletableFuture<String> sendBotMessageAndgetMessageId(String content, boolean isChat);
	CompletableFuture<String> sendBotMessageAndgetMessageId(MessageEmbed embed, boolean isChat);
	MessageEmbed createEmbed(String description, int color);
	void sendBotMessage(String content, MessageEmbed embed);
	void sendBotMessage(String content);
	void sendBotMessage(MessageEmbed embed);
}
