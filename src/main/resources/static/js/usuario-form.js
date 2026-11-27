document.querySelectorAll("[data-mostrar-clave]").forEach((boton) => {
    const campo = document.getElementById(boton.dataset.mostrarClave);
    boton.addEventListener("click", () => {
        const mostrar = campo.type === "password";
        campo.type = mostrar ? "text" : "password";
        boton.setAttribute("aria-pressed", String(mostrar));
    });
});

const campoFoto = document.getElementById("fotoPerfil");
if (campoFoto) prepararRecorte(campoFoto);

const consultaDni = document.getElementById("usuarioDniConsulta");
if (consultaDni) prepararConsultaDni(consultaDni);

function prepararConsultaDni(panel) {
    const dni = document.getElementById("usuarioDni");
    const boton = document.getElementById("usuarioDniBoton");
    const autorizado = document.getElementById("usuarioDniAutorizado");
    const estado = document.getElementById("usuarioDniEstado");
    const nombres = document.getElementById("nombres");
    const apellidos = document.getElementById("apellidos");

    function informar(mensaje, error = false) {
        estado.textContent = mensaje;
        estado.classList.toggle("field-error", error);
    }

    panel.disabled = false;
    dni.addEventListener("input", () => {
        autorizado.checked = false;
        informar("");
    });
    dni.addEventListener("keydown", (evento) => {
        if (evento.key !== "Enter" || evento.isComposing) return;
        evento.preventDefault();
        boton.click();
    });

    boton.addEventListener("click", async () => {
        if (!/^[0-9]{8}$/.test(dni.value)) {
            informar("Ingresa exactamente 8 dígitos.", true);
            dni.focus();
            return;
        }
        if (!autorizado.checked) {
            informar("Confirma que tienes autorización para consultar este DNI.", true);
            autorizado.focus();
            return;
        }

        dni.readOnly = true;
        boton.disabled = true;
        autorizado.disabled = true;
        informar("Consultando DNI…");
        const errorConsulta = "No se pudo completar la consulta. Puedes ingresar los datos manualmente.";
        try {
            const respuesta = await fetch(panel.dataset.url, {
                method: "POST",
                headers: { Accept: "application/json", [panel.dataset.csrfHeader]: panel.dataset.csrfToken },
                body: new URLSearchParams({ dni: dni.value, consentimiento: "true" }),
                cache: "no-store"
            });
            if (respuesta.redirected || respuesta.status === 403) {
                informar("La consulta requiere una sesión activa de administrador con permiso de Usuarios.", true);
                return;
            }
            const resultado = await respuesta.json();
            if (!respuesta.ok || !resultado.nombres || !resultado.apellidos) {
                informar(resultado.error || errorConsulta, true);
                return;
            }
            nombres.value = resultado.nombres;
            apellidos.value = resultado.apellidos;
            if (nombres.value.length > 20 || apellidos.value.length > 20) {
                informar("Datos encontrados. Revisa el límite de 20 caracteres por campo antes de guardar.");
            } else {
                informar("Nombres y apellidos completados.");
            }
        } catch {
            informar(errorConsulta, true);
        } finally {
            dni.readOnly = false;
            boton.disabled = false;
            autorizado.disabled = false;
        }
    });
}

function prepararRecorte(campo) {
    const ventana = document.getElementById("recorteFoto");
    const lienzo = ventana.querySelector("canvas");
    const dibujo = lienzo.getContext("2d", { alpha: false });
    if (!dibujo || typeof ventana.showModal !== "function" || typeof FormDataEvent === "undefined") return;

    const formulario = campo.form;
    const guardar = formulario.querySelector('button[type="submit"]');
    const vista = document.getElementById("fotoPerfilVista");
    const quitar = document.getElementById("quitarFoto");
    const elegir = document.getElementById("elegirFoto");
    const quitarValor = document.getElementById("quitarFotoValor");
    const estado = document.getElementById("fotoEstado");
    const hayFotoGuardada = vista.hasAttribute("src");
    const zoom = document.getElementById("zoomFoto");
    const usar = document.getElementById("usarFoto");
    const estadoRecorte = document.getElementById("recorteEstado");
    const lado = lienzo.width;

    let seleccion = null;
    let pendiente = null;
    let urlVista;
    let turno = 0;
    let x = 0;
    let y = 0;
    let puntero;

    function informar(mensaje) {
        estado.hidden = false;
        estado.textContent = mensaje;
    }

    function mostrarFoto(recorte) {
        if (urlVista) URL.revokeObjectURL(urlVista);
        urlVista = recorte ? URL.createObjectURL(recorte) : null;
        if (urlVista) {
            vista.src = urlVista;
        } else {
            vista.removeAttribute("src");
        }
        vista.hidden = !urlVista;
        if (quitar) quitar.hidden = !seleccion;
        elegir.textContent = seleccion ? "Cambiar foto" : "Subir foto";
    }

    function dibujar() {
        const imagen = pendiente.imagen;
        const escala = lado / Math.min(imagen.naturalWidth, imagen.naturalHeight) * Number(zoom.value);
        const ancho = imagen.naturalWidth * escala;
        const alto = imagen.naturalHeight * escala;
        const limiteX = (ancho - lado) / 2;
        const limiteY = (alto - lado) / 2;
        x = Math.max(-limiteX, Math.min(limiteX, x));
        y = Math.max(-limiteY, Math.min(limiteY, y));

        dibujo.fillStyle = "#fff";
        dibujo.fillRect(0, 0, lado, lado);
        dibujo.drawImage(imagen, x - limiteX, y - limiteY, ancho, alto);
    }

    formulario.addEventListener("formdata", ({ formData }) => {
        if (!seleccion) return;
        formData.set("foto", seleccion.archivo);
        if (seleccion.recorte) formData.set("fotoRecortada", seleccion.recorte, "foto-perfil.jpg");
    });

    campo.addEventListener("change", async () => {
        const archivo = campo.files[0];
        if (!archivo) return;
        campo.value = "";

        const lectura = ++turno;
        guardar.disabled = true;
        const imagen = new Image();
        const url = URL.createObjectURL(archivo);
        try {
            imagen.src = url;
            await imagen.decode();
        } catch {
            if (lectura === turno) {
                seleccion = { archivo };
                if (quitarValor) quitarValor.value = "false";
                mostrarFoto(null);
                informar("Vista previa no disponible. El servidor revisará el archivo al guardar.");
            }
            return;
        } finally {
            URL.revokeObjectURL(url);
            if (lectura === turno) guardar.disabled = false;
        }
        if (lectura !== turno) return;

        pendiente = { archivo, imagen };
        x = 0;
        y = 0;
        zoom.value = "1";
        estadoRecorte.hidden = true;
        dibujar();
        ventana.showModal();
    });

    quitar?.addEventListener("click", () => {
        turno += 1;
        guardar.disabled = false;
        seleccion = null;
        campo.value = "";
        quitarValor.value = String(hayFotoGuardada);
        mostrarFoto(null);
        informar(hayFotoGuardada ? "La foto se quitará al guardar." : "Puedes seleccionar otra foto.");
        campo.focus();
    });

    zoom.addEventListener("input", dibujar);
    lienzo.addEventListener("pointerdown", (evento) => {
        if (puntero || evento.button !== 0) return;
        puntero = { id: evento.pointerId, x: evento.clientX, y: evento.clientY };
        lienzo.setPointerCapture(evento.pointerId);
    });
    lienzo.addEventListener("pointermove", (evento) => {
        if (puntero?.id !== evento.pointerId) return;
        const escala = lado / lienzo.clientWidth;
        x += (evento.clientX - puntero.x) * escala;
        y += (evento.clientY - puntero.y) * escala;
        puntero.x = evento.clientX;
        puntero.y = evento.clientY;
        dibujar();
    });
    lienzo.addEventListener("lostpointercapture", () => { puntero = null; });
    const flechas = { ArrowLeft: [-8, 0], ArrowRight: [8, 0], ArrowUp: [0, -8], ArrowDown: [0, 8] };
    lienzo.addEventListener("keydown", (evento) => {
        const paso = flechas[evento.key];
        if (!paso) return;
        evento.preventDefault();
        x += paso[0];
        y += paso[1];
        dibujar();
    });

    document.getElementById("cancelarFoto").addEventListener("click", () => ventana.close());
    ventana.addEventListener("close", () => {
        pendiente = null;
        puntero = null;
        campo.focus();
    });

    usar.addEventListener("click", () => {
        const imagenActual = pendiente;
        usar.disabled = true;
        lienzo.toBlob((recorte) => {
            usar.disabled = false;
            if (pendiente !== imagenActual || !ventana.open) return;
            if (!recorte) {
                estadoRecorte.hidden = false;
                estadoRecorte.textContent = "No se pudo generar el recorte. Vuelve a intentarlo.";
                return;
            }

            seleccion = { archivo: imagenActual.archivo, recorte };
            if (quitarValor) quitarValor.value = "false";
            mostrarFoto(recorte);
            informar("La foto se actualizará al guardar.");
            ventana.close();
        }, "image/jpeg", 0.9);
    });
}
