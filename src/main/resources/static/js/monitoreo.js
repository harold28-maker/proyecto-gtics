(() => {
    const monitoreo = document.getElementById('monitoreo');
    const actualizarAhora = document.getElementById('monitoreo-actualizar');
    const aviso = document.getElementById('monitoreo-aviso');
    const ultima = document.getElementById('monitoreo-ultima');
    const hora = document.getElementById('monitoreo-hora');
    let datos = document.getElementById('monitoreo-datos');
    let actualizando = false;
    let accesoSuspendido = false;

    function mostrarAviso(texto, error = false) {
        aviso.textContent = texto;
        aviso.hidden = !texto;
        aviso.classList.toggle('monitoreo-aviso-error', error);
    }

    function suspenderActualizacion(texto) {
        accesoSuspendido = true;
        datos.replaceChildren();
        ultima.hidden = true;
        mostrarAviso(texto, true);
    }

    async function actualizar() {
        if (actualizando || accesoSuspendido || document.hidden) return;

        actualizando = true;
        const solicitud = new AbortController();
        const plazo = window.setTimeout(() => solicitud.abort(), 8000);
        try {
            const respuesta = await fetch(monitoreo.dataset.url, {cache: 'no-store', signal: solicitud.signal});
            if (respuesta.redirected || respuesta.status === 401) {
                suspenderActualizacion('Vuelve a iniciar sesión para consultar Monitoreo.');
                return;
            }
            if (respuesta.status === 403) {
                suspenderActualizacion('El administrador deshabilitó tu acceso a Monitoreo. Contacta a Administración.');
                return;
            }
            if (!respuesta.ok) throw new Error('Error al consultar la actividad.');

            const documento = new DOMParser().parseFromString(await respuesta.text(), 'text/html');
            const nuevosDatos = documento.getElementById('monitoreo-datos');
            if (!nuevosDatos) throw new Error('No se recibió la actividad.');

            const anteriores = new Set((datos.dataset.accesos || '').split(',').filter(Boolean));
            const recientes = (nuevosDatos.dataset.accesos || '').split(',');
            const nuevos = datos.dataset.hora ? recientes.filter(id => id && !anteriores.has(id)) : [];
            nuevosDatos.querySelectorAll('[data-acceso]').forEach(fila => {
                const esNuevo = nuevos.includes(fila.dataset.acceso);
                fila.classList.toggle('monitoreo-nuevo', esNuevo);
                fila.querySelector('.monitoreo-etiqueta').hidden = !esNuevo;
            });

            const foco = datos.contains(document.activeElement) ? document.activeElement.id : '';
            datos.replaceWith(nuevosDatos);
            datos = nuevosDatos;
            if (foco) document.getElementById(foco)?.focus({preventScroll: true});
            hora.textContent = datos.dataset.hora || '';
            ultima.hidden = !datos.dataset.hora;

            if (datos.dataset.vista === 'eventos' && nuevos.length > 0) {
                mostrarAviso(nuevos.length === 1 ? 'Se registró un nuevo inicio de sesión.'
                    : `Se registraron ${nuevos.length} inicios de sesión nuevos.`);
            } else {
                mostrarAviso('');
            }
        } catch {
            const fecha = datos.dataset.fecha;
            mostrarAviso(fecha ? `No se pudo actualizar. Últimos datos del ${fecha}.`
                : 'No se pudo actualizar la actividad. Intenta nuevamente.', true);
        } finally {
            window.clearTimeout(plazo);
            actualizando = false;
        }
    }

    actualizarAhora.addEventListener('click', evento => {
        if (accesoSuspendido || evento.ctrlKey || evento.metaKey || evento.shiftKey || evento.altKey) return;
        evento.preventDefault();
        actualizar();
    });
    window.setInterval(actualizar, 15000);
    document.addEventListener('visibilitychange', actualizar);
})();
