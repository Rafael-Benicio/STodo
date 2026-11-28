const TrackerRepository = require('../db/trackerRepository');

const TRACKER_RULES = {
    MIN_EPISODE: 0,
    DEFAULT_PROTOCOL: 'https://',
    ALLOWED_PROTOCOLS: new Set(['http:', 'https:'])
};

/**
 * Validates and normalizes an episode counter value to a non-negative integer.
 * @param {number|string} rawEpisode - Input episode number.
 * @returns {number} Sanitized non-negative integer.
 * @example const ep = normalizeEpisodeNumber('12');
 */
function normalizeEpisodeNumber(rawEpisode) {
    const parsed = Number(rawEpisode ?? TRACKER_RULES.MIN_EPISODE);
    if (!Number.isInteger(parsed) || parsed < TRACKER_RULES.MIN_EPISODE) {
        throw new Error(`Invalid episode value "${rawEpisode}": expected integer >= ${TRACKER_RULES.MIN_EPISODE}`);
    }
    return parsed;
}

/**
 * Normalizes and validates an external URL ensuring HTTP or HTTPS protocol.
 * @param {string} rawUrl - Raw URL string supplied by user.
 * @returns {string} Validated URL string.
 * @example const url = validateExternalUrl('example.com/ep1');
 */
function validateExternalUrl(rawUrl) {
    const trimmed = typeof rawUrl === 'string' ? rawUrl.trim() : '';
    if (!trimmed) {
        throw new Error(`Invalid URL "${rawUrl}": expected non-empty URL string`);
    }
    const candidate = /^[a-zA-Z][a-zA-Z\d+\-.]*:(?!\d)/.test(trimmed)
        ? trimmed
        : `${TRACKER_RULES.DEFAULT_PROTOCOL}${trimmed}`;
    const parsed = new URL(candidate);
    if (!TRACKER_RULES.ALLOWED_PROTOCOLS.has(parsed.protocol)) {
        throw new Error(`Unsupported URL protocol "${parsed.protocol}": expected http: or https:`);
    }
    return parsed.href;
}

/**
 * Normalizes an array of link entries into validated { label, url } objects.
 * @param {Array<Object|string>} rawLinks - Input links array.
 * @returns {Array<Object>} Sanitized link objects.
 * @example const list = normalizeLinksList([{ label: 'Watch', url: 'https://example.com' }]);
 */
function normalizeLinksList(rawLinks) {
    if (!Array.isArray(rawLinks)) return [];
    return rawLinks
        .filter((entry) => entry && (typeof entry === 'string' || typeof entry.url === 'string'))
        .map((entry) => {
            const url = validateExternalUrl(typeof entry === 'string' ? entry : entry.url);
            const rawLabel = typeof entry === 'object' && entry.label ? entry.label.trim() : '';
            return { label: rawLabel || url, url };
        });
}

/**
 * TrackerService encapsulates business validation and persistence for episode trackers.
 */
const TrackerService = {
    /**
     * Fetches all episode trackers ordered by display position.
     * @returns {Promise<Array<Object>>} List of trackers.
     * @example const trackers = await TrackerService.getAllTrackers();
     */
    getAllTrackers: async () => {
        return await TrackerRepository.getAll();
    },

    /**
     * Validates and creates a new episode tracker entry.
     * @param {Object} tracker - Tracker creation payload.
     * @returns {Promise<void>}
     * @example await TrackerService.addTracker({ title: 'Frieren', currentEpisode: 1 });
     */
    addTracker: async (tracker) => {
        const title = typeof tracker?.title === 'string' ? tracker.title.trim() : '';
        if (!title) {
            throw new Error(`Invalid tracker title "${tracker?.title}": expected non-empty string`);
        }
        const maxPos = await TrackerRepository.getMaxPosition();
        const sanitized = {
            ...tracker,
            title,
            currentEpisode: normalizeEpisodeNumber(tracker.currentEpisode),
            links: normalizeLinksList(tracker.links),
            position: maxPos + 1,
            createdAt: Date.now(),
            updatedAt: Date.now()
        };
        return await TrackerRepository.insert(sanitized);
    },

    /**
     * Validates and updates an existing episode tracker record.
     * @param {Object} tracker - Tracker update payload.
     * @returns {Promise<Object>} Sanitized updated tracker.
     * @example await TrackerService.updateTracker({ id: '1', title: 'Show', currentEpisode: 5, links: [] });
     */
    updateTracker: async (tracker) => {
        const title = typeof tracker?.title === 'string' ? tracker.title.trim() : '';
        if (!tracker?.id || !title) {
            throw new Error(`Invalid tracker update payload (id="${tracker?.id}", title="${tracker?.title}"): expected valid id and non-empty title`);
        }
        const sanitized = {
            ...tracker,
            title,
            currentEpisode: normalizeEpisodeNumber(tracker.currentEpisode),
            links: normalizeLinksList(tracker.links),
            updatedAt: Date.now()
        };
        await TrackerRepository.update(sanitized);
        return sanitized;
    },

    /**
     * Deletes an episode tracker by its identifier.
     * @param {string} id - Tracker UUID.
     * @returns {Promise<void>}
     * @example await TrackerService.deleteTracker('uuid-1');
     */
    deleteTracker: async (id) => {
        return await TrackerRepository.delete(id);
    },

    validateExternalUrl
};

module.exports = TrackerService;
