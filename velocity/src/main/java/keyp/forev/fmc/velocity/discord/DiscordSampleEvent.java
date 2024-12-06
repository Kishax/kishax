package keyp.forev.fmc.velocity.discord;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

public class DiscordSampleEvent extends ListenerAdapter {
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        event.getMessage();
        if (event.getAuthor().isBot()) {
            return;
        }
        if (event.getMessage().getContentRaw().equals("!ping")) {
            event.getChannel().sendMessage("Pong!").queue();
        }
    }
}
