const { Bonjour } = require('bonjour-service');

let bonjourInstance = null;
const peers = new Map();

/**
 * Checks if a given IP address is a local loopback address.
 * @param {string} ip - The IP address to check.
 */
function isLocalIp(ip) {
    return ip === '127.0.0.1' || ip === '::1';
}

/**
 * Returns the array of current active discovered peers.
 * Example: const activePeers = getPeers();
 */
function getPeers() {
    return Array.from(peers.values());
}

/**
 * Starts advertising the local desktop hub and browsing for mobile peers.
 * Example: startMdnsService(8080, (peers) => console.log(peers));
 */
function startMdnsService(port = 8080, onPeerChange) {
    const bonjour = new Bonjour();
    bonjourInstance = bonjour;

    const service = bonjour.publish({
        name: 'STodo Desktop Hub',
        type: 'stodo',
        protocol: 'tcp',
        port: port,
        txt: { version: '1.0.0' }
    });

    console.log(`Serviço mDNS anunciado: _stodo._tcp. local na porta ${port}`);

    const browser = bonjour.find({ type: 'stodo' });
    setupBrowserListeners(browser, port, onPeerChange);

    return service;
}

/**
 * Sets up listeners for the mDNS browser events.
 */
function setupBrowserListeners(browser, localPort, onPeerChange) {
    browser.on('up', (peer) => {
        const isSelf = peer.name === 'STodo Desktop Hub' || 
                       (peer.port === localPort && isLocalIp(peer.referer.address));
        if (isSelf) return;

        console.log(`[mDNS] Peer found: ${peer.name} at ${peer.referer.address}:${peer.port}`);
        peers.set(peer.name, {
            name: peer.name,
            host: peer.referer.address,
            port: peer.port
        });
        if (onPeerChange) onPeerChange(getPeers());
    });

    browser.on('down', (peer) => {
        console.log(`[mDNS] Peer lost: ${peer.name}`);
        peers.delete(peer.name);
        if (onPeerChange) onPeerChange(getPeers());
    });
}

module.exports = {
    startMdnsService,
    getPeers
};
