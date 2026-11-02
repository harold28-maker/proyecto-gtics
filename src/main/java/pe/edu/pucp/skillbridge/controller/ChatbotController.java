package pe.edu.pucp.skillbridge.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.skillbridge.dto.ChatbotMessageResponse;
import pe.edu.pucp.skillbridge.dto.ChatbotRequest;
import pe.edu.pucp.skillbridge.dto.ChatbotResponse;
import pe.edu.pucp.skillbridge.service.ChatbotService;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ResponseEntity<ChatbotResponse> responder(@Valid @RequestBody ChatbotRequest request,
                                                      Authentication authentication) {
        try {
            ChatbotService.Respuesta respuesta = chatbotService.responder(
                    authentication.getName(), request.mensaje());
            return ResponseEntity.ok(ChatbotResponse.exitosa(respuesta.contenido(), respuesta.fecha()));
        } catch (Exception ex) {
            log.warn("El chatbot no pudo generar una respuesta ({})", ex.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ChatbotResponse.fallida(
                            "Google AI Studio no pudo procesar la solicitud. Verifica GEMINI_API_KEY, la cuota y el acceso al modelo, e inténtalo nuevamente."));
        }
    }

    @GetMapping("/historial")
    public List<ChatbotMessageResponse> historial(Authentication authentication) {
        return chatbotService.historial(authentication.getName());
    }

    @DeleteMapping("/historial")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void limpiarHistorial(Authentication authentication) {
        chatbotService.limpiarHistorial(authentication.getName());
    }
}
