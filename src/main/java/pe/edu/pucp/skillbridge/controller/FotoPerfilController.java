package pe.edu.pucp.skillbridge.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import pe.edu.pucp.skillbridge.entity.Colaborador;
import pe.edu.pucp.skillbridge.entity.Usuario;
import pe.edu.pucp.skillbridge.repository.ColaboradorRepository;
import pe.edu.pucp.skillbridge.service.FotoPerfilService;

@Controller
public class FotoPerfilController {
    private final FotoPerfilService fotos;
    private final ColaboradorRepository colaboradores;

    public FotoPerfilController(FotoPerfilService fotos, ColaboradorRepository colaboradores) {
        this.fotos = fotos;
        this.colaboradores = colaboradores;
    }

    @GetMapping("/colaborador/perfil/foto")
    public ResponseEntity<Resource> fotoPropia(@RequestAttribute("usuarioVerificado") Usuario cuenta) {
        return responder(cuenta.getFotoUrl());
    }

    @GetMapping({"/pm/colaboradores/fotos/{id}", "/resource/colaboradores/fotos/{id}"})
    public ResponseEntity<Resource> fotoDirectorio(@PathVariable("id") Integer idColaborador) {
        String url = colaboradores.findById(idColaborador).map(Colaborador::getUsuario)
                .map(Usuario::getFotoUrl).orElse(null);
        return responder(url);
    }

    private ResponseEntity<Resource> responder(String url) {
        Resource foto = fotos.buscarPorUrl(url);
        if (foto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_JPEG)
                .body(foto);
    }
}
