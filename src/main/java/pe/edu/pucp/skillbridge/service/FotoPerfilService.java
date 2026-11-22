package pe.edu.pucp.skillbridge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FotoPerfilService {
    private static final Logger LOG = LoggerFactory.getLogger(FotoPerfilService.class);
    private static final String PREFIJO_URL = "/admin/usuarios/fotos/";
    private static final Pattern NOMBRE_FOTO = Pattern.compile("[0-9a-f-]{36}\\.jpg");
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELES = 25_000_000;
    private static final int LADO_FINAL = 384;

    private final Path directorio;

    public FotoPerfilService(@Value("${skillbridge.upload-dir:uploads}") String uploadDir) {
        this.directorio = Path.of(uploadDir, "fotos-perfil").toAbsolutePath().normalize();
    }

    /**
     * Comprueba la imagen original y, si se recibió, el recorte de la vista previa.
     * Guarda una copia cuadrada en JPEG.
     */
    public String guardar(MultipartFile original, MultipartFile recorte) throws IOException {
        BufferedImage imagen = leerImagen(original);
        if (recorte != null && !recorte.isEmpty()) {
            imagen = leerImagen(recorte);
        }

        int lado = Math.min(LADO_FINAL, imagen.getWidth());
        BufferedImage foto = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
        Graphics2D dibujo = foto.createGraphics();
        try {
            dibujo.setColor(Color.WHITE);
            dibujo.fillRect(0, 0, lado, lado);
            dibujo.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            dibujo.drawImage(imagen, 0, 0, lado, lado, null);
        } finally {
            dibujo.dispose();
        }

        Files.createDirectories(directorio);
        String nombre = UUID.randomUUID() + ".jpg";
        Path temporal = Files.createTempFile(directorio, "foto-", ".tmp");
        try {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                if (!ImageIO.write(foto, "jpeg", salida)) {
                    throw new IOException("El servidor no pudo generar el JPEG de perfil.");
                }
            }
            Files.move(temporal, directorio.resolve(nombre));
            return PREFIJO_URL + nombre;
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private BufferedImage leerImagen(MultipartFile archivo) {
        if (archivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("La foto de perfil no puede superar los 5 MB.");
        }

        try (InputStream bytes = archivo.getInputStream();
             MemoryCacheImageInputStream datos = new MemoryCacheImageInputStream(bytes)) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(datos);
            if (!lectores.hasNext()) {
                throw new IllegalArgumentException("Selecciona una imagen JPG o PNG válida.");
            }

            ImageReader lector = lectores.next();
            try {
                String formato = lector.getFormatName();
                if (!"jpeg".equalsIgnoreCase(formato) && !"png".equalsIgnoreCase(formato)) {
                    throw new IllegalArgumentException("Solo se permiten fotos en formato JPG o PNG.");
                }

                lector.setInput(datos, true, true);
                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);
                if (ancho < 1 || alto < 1 || (long) ancho * alto > MAX_PIXELES) {
                    throw new IllegalArgumentException("Selecciona una foto de hasta 25 megapíxeles.");
                }

                int lado = Math.min(ancho, alto);
                ImageReadParam lectura = lector.getDefaultReadParam();
                lectura.setSourceRegion(new Rectangle((ancho - lado) / 2, (alto - lado) / 2, lado, lado));
                int salto = Math.max(1, lado / LADO_FINAL);
                lectura.setSourceSubsampling(salto, salto, 0, 0);
                return lector.read(0, lectura);
            } finally {
                lector.dispose();
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer la foto. Selecciona un JPG o PNG válido.", ex);
        }
    }

    public Resource buscar(String nombre) {
        Path archivo = resolver(nombre);
        if (archivo == null || !Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        return new FileSystemResource(archivo);
    }

    public void eliminar(String url) {
        if (url == null || !url.startsWith(PREFIJO_URL)) {
            return;
        }

        Path archivo = resolver(url.substring(PREFIJO_URL.length()));
        if (archivo != null) {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException ex) {
                LOG.warn("No se pudo retirar una foto de perfil anterior.", ex);
            }
        }
    }

    private Path resolver(String nombre) {
        return NOMBRE_FOTO.matcher(nombre).matches() ? directorio.resolve(nombre) : null;
    }
}
