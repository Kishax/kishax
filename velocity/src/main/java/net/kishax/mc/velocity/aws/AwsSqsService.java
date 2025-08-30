package net.kishax.mc.velocity.aws;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.inject.Inject;

import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.velocity.util.config.VelocityConfig;

public class AwsSqsService {
  private final Logger logger;
  private final VelocityConfig config;
  private final HttpClient httpClient;
  private final Gson gson;

  @Inject
  public AwsSqsService(Logger logger, VelocityConfig config) {
    this.logger = logger;
    this.config = config;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    this.gson = new Gson();
  }

  /**
   * 認証トークン情報をWeb側(SQS)に送信
   */
  public void sendAuthTokenToWeb(Message.Web.AuthToken authToken) {
    try {
      String apiGatewayEndpoint = config.getString("AWS.ApiGateway.Endpoint", "");
      
      if (apiGatewayEndpoint.isEmpty()) {
        logger.warn("AWS API Gateway endpoint is not configured. Skipping auth token send.");
        return;
      }

      // メッセージを構築
      Message message = new Message();
      message.web = new Message.Web();
      message.web.authToken = authToken;

      String jsonMessage = gson.toJson(message);
      
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(apiGatewayEndpoint))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(jsonMessage))
          .timeout(Duration.ofSeconds(30))
          .build();

      HttpResponse<String> response = httpClient.send(request, 
          HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        logger.info("Successfully sent auth token to Web via API Gateway for player: {}", 
            authToken.who.name);
      } else {
        logger.warn("Failed to send auth token to Web. Status code: {}, Response: {}", 
            response.statusCode(), response.body());
      }

    } catch (Exception e) {
      logger.error("Error sending auth token to Web via API Gateway: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }
}