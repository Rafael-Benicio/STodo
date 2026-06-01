const { Bonjour } = require('bonjour-service');

function startMdnsService(port = 8080) {
    const bonjour = new Bonjour();

    // Anunciar o serviço STodo na rede local
    const service = bonjour.publish({
        name: 'STodo Desktop Hub',
        type: 'stodo',
        protocol: 'tcp',
        port: port,
        txt: { version: '1.0.0' }
    });

    console.log(`Serviço mDNS anunciado: _stodo._tcp. local na porta ${port}`);

    return service;
}

module.exports = startMdnsService;
