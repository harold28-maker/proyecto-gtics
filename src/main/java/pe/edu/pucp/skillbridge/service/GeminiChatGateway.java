package pe.edu.pucp.skillbridge.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class GeminiChatGateway implements AiChatGateway {

    private final ChatClient chatClient;

    public GeminiChatGateway(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String generar(String promptSistema, String promptUsuario) {
        return chatClient
                .prompt()
                .system(promptSistema)
                .user(promptUsuario)
                .call()
                .content();
    }
}
