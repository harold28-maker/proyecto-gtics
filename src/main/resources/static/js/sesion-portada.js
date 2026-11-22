(() => {
    const {sesionUrl, logoutUrl} = document.currentScript.dataset;

    window.addEventListener('pageshow', async () => {
        sessionStorage.setItem('skillbridge:portada', 'true');

        try {
            const session = await fetch(sesionUrl, {cache: 'no-store'});
            if (session.status === 204) {
                return;
            }
            if (!session.ok) {
                throw new Error('No se pudo consultar la sesión.');
            }

            const csrf = await session.json();
            const logout = await fetch(logoutUrl, {
                method: 'POST',
                headers: {'X-Requested-With': 'XMLHttpRequest'},
                body: new URLSearchParams({[csrf.parameterName]: csrf.token}),
                keepalive: true
            });
            if (!logout.ok) {
                throw new Error('No se pudo cerrar la sesión.');
            }
        } catch (error) {
            console.warn(error.message);
        }
    });
})();
