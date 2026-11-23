(() => {
    const {loginUrl} = document.currentScript.dataset;
    const portada = 'skillbridge:portada';
    const navigation = performance.getEntriesByType('navigation')[0];

    if (navigation?.type === 'back_forward' && sessionStorage.getItem(portada)) {
        window.location.replace(loginUrl);
        return;
    }
    sessionStorage.removeItem(portada);

    window.addEventListener('pageshow', (event) => {
        if (!event.persisted) {
            return;
        }
        if (sessionStorage.getItem(portada)) {
            window.location.replace(loginUrl);
        } else {
            window.location.reload();
        }
    });
})();
