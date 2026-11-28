/**
 * Episode tracker UI module: handles rendering and interaction for the Series tab.
 */
const TRACKER_UI_CONFIG = {
    MIN_EPISODE: 0,
    INCREMENT_STEP: 1,
    DECREMENT_STEP: -1
};

let allTrackers = [];
const expandedTrackerIds = new Set();

/**
 * Loads all episode trackers from the database and refreshes view if active.
 * @example await loadTrackers();
 */
async function loadTrackers() {
    allTrackers = await window.api.getTrackers();
    if (currentTab === UI_CONFIG.TABS.TRACKER) {
        renderTrackers();
    }
}

/**
 * Increments or decrements a tracker's episode count without going below zero.
 * @param {Object} tracker - Target tracker item.
 * @param {number} delta - Step value (+1 or -1).
 * @example await adjustTrackerEpisode(tracker, 1);
 */
async function adjustTrackerEpisode(tracker, delta) {
    const nextValue = Math.max(TRACKER_UI_CONFIG.MIN_EPISODE, (tracker.currentEpisode || 0) + delta);
    if (nextValue === tracker.currentEpisode) return;
    tracker.currentEpisode = nextValue;
    await window.api.updateTracker(tracker);
    await loadTrackers();
}

/**
 * Creates a reusable step button (+ or -) for adjusting episode count.
 * @param {string} symbol - Button label ('+' or '-').
 * @param {string} title - Tooltip title.
 * @param {Function} onClick - Click callback.
 * @returns {HTMLButtonElement} Configured button element.
 * @example const btn = createTrackerStepButton('+', 'Incrementar', fn);
 */
function createTrackerStepButton(symbol, title, onClick) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'tracker-step-btn';
    btn.innerText = symbol;
    btn.title = title;
    btn.onclick = onClick;
    return btn;
}

/**
 * Creates the episode counter group (-, number, +) positioned on the left side.
 * @param {Object} tracker - Target tracker item.
 * @returns {HTMLElement} Counter controls container.
 * @example const controls = createTrackerCounterControls(tracker);
 */
function createTrackerCounterControls(tracker) {
    const group = document.createElement('div');
    group.className = 'tracker-counter-group';

    const decBtn = createTrackerStepButton('-', 'Diminuir episódio', () => {
        adjustTrackerEpisode(tracker, TRACKER_UI_CONFIG.DECREMENT_STEP);
    });
    const valueSpan = document.createElement('span');
    valueSpan.className = 'tracker-episode-value';
    valueSpan.innerText = tracker.currentEpisode ?? TRACKER_UI_CONFIG.MIN_EPISODE;

    const incBtn = createTrackerStepButton('+', 'Aumentar episódio', () => {
        adjustTrackerEpisode(tracker, TRACKER_UI_CONFIG.INCREMENT_STEP);
    });

    group.append(decBtn, valueSpan, incBtn);
    return group;
}

/**
 * Creates the leftmost options button to open the tracker modal.
 * @param {Object} tracker - Target tracker item.
 * @returns {HTMLButtonElement} Leftmost modal trigger button.
 * @example const btn = createTrackerOptionsButton(tracker);
 */
function createTrackerOptionsButton(tracker) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'options-btn tracker-options-btn';
    btn.innerText = '⚙️';
    btn.title = 'Editar valor, links e exclusão';
    btn.onclick = (e) => {
        e.stopPropagation();
        openTrackerModal(tracker);
    };
    return btn;
}

/**
 * Creates a clickable browser link element inside the expanded row drawer.
 * @param {Object} link - Link object containing label and url.
 * @returns {HTMLButtonElement} Clickable link button.
 * @example const el = createTrackerLinkButton({ label: 'Site', url: 'https://a.com' });
 */
function createTrackerLinkButton(link) {
    const linkBtn = document.createElement('button');
    linkBtn.type = 'button';
    linkBtn.className = 'tracker-external-link-btn';
    linkBtn.innerText = `🌐 ${link.label || link.url}`;
    linkBtn.title = link.url;
    linkBtn.onclick = (e) => {
        e.stopPropagation();
        window.api.openExternalLink(link.url);
    };
    return linkBtn;
}

/**
 * Creates the expandable links drawer shown when the tracker's name is clicked.
 * @param {Object} tracker - Target tracker item.
 * @returns {HTMLElement} Links container element.
 * @example const drawer = createTrackerLinksDrawer(tracker);
 */
function createTrackerLinksDrawer(tracker) {
    const drawer = document.createElement('div');
    drawer.className = 'tracker-links-drawer';
    drawer.style.display = expandedTrackerIds.has(tracker.id) ? 'flex' : 'none';

    const links = tracker.links || [];
    if (links.length === 0) {
        drawer.innerHTML = '<span class="tracker-empty-links">Nenhum link cadastrado (use ⚙️ para adicionar).</span>';
        return drawer;
    }
    links.forEach((link) => drawer.appendChild(createTrackerLinkButton(link)));
    return drawer;
}

/**
 * Creates the clickable title container that toggles the item's links drawer.
 * @param {Object} tracker - Target tracker item.
 * @param {HTMLElement} linksDrawer - Associated drawer element to toggle.
 * @returns {HTMLElement} Title container element.
 * @example const titleEl = createTrackerTitleElement(tracker, drawer);
 */
function createTrackerTitleElement(tracker, linksDrawer) {
    const wrapper = document.createElement('div');
    wrapper.className = 'tracker-title-wrapper';
    wrapper.title = 'Clique para exibir/ocultar os links';
    wrapper.onclick = () => toggleTrackerDrawer(tracker.id, linksDrawer);

    const titleSpan = document.createElement('span');
    titleSpan.className = 'tracker-title';
    titleSpan.innerText = tracker.title;
    wrapper.appendChild(titleSpan);

    const linkCount = (tracker.links || []).length;
    if (linkCount > 0) {
        const badge = document.createElement('span');
        badge.className = 'badge-count tracker-links-badge';
        badge.innerText = `🔗 ${linkCount}`;
        wrapper.appendChild(badge);
    }
    return wrapper;
}

/**
 * Toggles visibility and remembered expansion state of a tracker links drawer.
 * @param {string} trackerId - Tracker identifier.
 * @param {HTMLElement} linksDrawer - Drawer element.
 * @example toggleTrackerDrawer('uuid-1', drawer);
 */
function toggleTrackerDrawer(trackerId, linksDrawer) {
    const isExpanded = expandedTrackerIds.has(trackerId);
    if (isExpanded) {
        expandedTrackerIds.delete(trackerId);
        linksDrawer.style.display = 'none';
        return;
    }
    expandedTrackerIds.add(trackerId);
    linksDrawer.style.display = 'flex';
}

/**
 * Creates a complete tracker list item with leftmost modal button, counter, title, and links drawer.
 * @param {Object} tracker - Tracker item entity.
 * @returns {HTMLElement} List item element.
 * @example const li = createTrackerElement(tracker);
 */
function createTrackerElement(tracker) {
    const li = document.createElement('li');
    li.className = 'tracker-item';

    const mainRow = document.createElement('div');
    mainRow.className = 'tracker-main-row';

    const linksDrawer = createTrackerLinksDrawer(tracker);
    const optionsBtn = createTrackerOptionsButton(tracker);
    const counterControls = createTrackerCounterControls(tracker);
    const titleEl = createTrackerTitleElement(tracker, linksDrawer);

    mainRow.append(optionsBtn, counterControls, titleEl);
    li.append(mainRow, linksDrawer);
    return li;
}

/**
 * Renders all episode tracker entries into the tracker list container.
 * @example renderTrackers();
 */
function renderTrackers() {
    const list = document.getElementById('trackerList');
    list.innerHTML = '';
    if (allTrackers.length === 0) {
        list.innerHTML = '<li class="empty-state">Nenhuma série cadastrada. Adicione uma acima!</li>';
        return;
    }
    allTrackers.forEach((tracker) => list.appendChild(createTrackerElement(tracker)));
}

/**
 * Creates a new episode tracker entry from the tracker input controls.
 * @example await handleAddTracker();
 */
async function handleAddTracker() {
    const titleInput = document.getElementById('trackerTitleInput');
    const episodeInput = document.getElementById('trackerEpisodeInput');
    const title = titleInput.value.trim();
    if (!title) return;

    const episode = Math.max(TRACKER_UI_CONFIG.MIN_EPISODE, parseInt(episodeInput.value, 10) || 0);
    await window.api.addTracker({ title, currentEpisode: episode, links: [] });
    titleInput.value = '';
    episodeInput.value = TRACKER_UI_CONFIG.MIN_EPISODE;
    await loadTrackers();
}
