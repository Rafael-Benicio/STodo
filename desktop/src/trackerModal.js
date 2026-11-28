/**
 * Tracker modal module: manages episode number editing, link management, and deletion.
 */
let activeModalTracker = null;

/**
 * Closes the episode tracker configuration modal.
 * @example closeTrackerModal();
 */
function closeTrackerModal() {
    const modal = document.getElementById('trackerModal');
    if (!modal) return;
    modal.style.display = 'none';
    activeModalTracker = null;
}

/**
 * Creates a clickable button element for opening a link from the tracker modal.
 * @param {Object} link - Link object containing label and url.
 * @returns {HTMLButtonElement} Clickable link anchor button.
 * @example const btn = createModalLinkAnchor({ label: 'Ep', url: 'https://a.com' });
 */
function createModalLinkAnchor(link) {
    const anchor = document.createElement('button');
    anchor.type = 'button';
    anchor.className = 'tracker-link-anchor';
    anchor.innerText = link.label || link.url;
    anchor.title = link.url;
    anchor.onclick = () => window.api.openExternalLink(link.url);
    return anchor;
}

/**
 * Builds a single link entry row inside the tracker modal with open and remove actions.
 * @param {Object} link - Link object containing label and url.
 * @param {number} index - Index of the link within the tracker's links array.
 * @returns {HTMLElement} Configured modal link row element.
 * @example const row = buildTrackerModalLinkItem({ label: 'Ep', url: 'https://a.com' }, 0);
 */
function buildTrackerModalLinkItem(link, index) {
    const row = document.createElement('div');
    row.className = 'tracker-modal-link-item';

    const removeBtn = document.createElement('button');
    removeBtn.type = 'button';
    removeBtn.className = 'color-reset-btn';
    removeBtn.innerText = '✕';
    removeBtn.title = 'Remover link';
    removeBtn.onclick = () => removeTrackerModalLink(index);

    row.append(createModalLinkAnchor(link), removeBtn);
    return row;
}

/**
 * Removes a link at the given index from the active modal tracker and persists it.
 * @param {number} index - Zero-based index to remove.
 * @example await removeTrackerModalLink(0);
 */
async function removeTrackerModalLink(index) {
    if (!activeModalTracker) return;
    activeModalTracker.links = activeModalTracker.links.filter((_, i) => i !== index);
    activeModalTracker = await window.api.updateTracker(activeModalTracker);
    renderTrackerModalLinks(activeModalTracker);
    await loadTrackers();
}

/**
 * Renders the list of links attached to the active tracker inside the modal.
 * @param {Object} tracker - Target tracker item.
 * @example renderTrackerModalLinks(tracker);
 */
function renderTrackerModalLinks(tracker) {
    const container = document.getElementById('trackerModalLinksList');
    container.innerHTML = '';
    const links = tracker.links || [];
    if (links.length === 0) {
        container.innerHTML = '<span class="tracker-empty-links">Nenhum link adicionado ainda.</span>';
        return;
    }
    links.forEach((link, idx) => container.appendChild(buildTrackerModalLinkItem(link, idx)));
}

/**
 * Saves the directly typed episode number from the modal input.
 * @example await handleSaveTrackerValue();
 */
async function handleSaveTrackerValue() {
    if (!activeModalTracker) return;
    const input = document.getElementById('trackerModalEpisodeInput');
    const parsed = parseInt(input.value, 10);
    if (Number.isNaN(parsed) || parsed < TRACKER_UI_CONFIG.MIN_EPISODE) return;

    activeModalTracker.currentEpisode = parsed;
    await window.api.updateTracker(activeModalTracker);
    await loadTrackers();
    closeTrackerModal();
}

/**
 * Adds a new link from modal inputs to the active tracker and persists changes.
 * @example await handleAddTrackerModalLink();
 */
async function handleAddTrackerModalLink() {
    if (!activeModalTracker) return;
    const urlInput = document.getElementById('trackerModalLinkUrl');
    const labelInput = document.getElementById('trackerModalLinkLabel');
    const rawUrl = urlInput.value.trim();
    if (!rawUrl) return;

    const label = labelInput.value.trim();
    const nextLinks = [...(activeModalTracker.links || []), { label, url: rawUrl }];
    activeModalTracker = await window.api.updateTracker({ ...activeModalTracker, links: nextLinks });
    urlInput.value = '';
    labelInput.value = '';
    renderTrackerModalLinks(activeModalTracker);
    await loadTrackers();
}

/**
 * Deletes the currently opened tracker record and refreshes the tracker list.
 * @example await handleDeleteModalTracker();
 */
async function handleDeleteModalTracker() {
    if (!activeModalTracker) return;
    const targetId = activeModalTracker.id;
    closeTrackerModal();
    await window.api.deleteTracker(targetId);
    await loadTrackers();
}

/**
 * Opens the tracker configuration modal populated with the selected item's state.
 * @param {Object} tracker - Tracker item entity to inspect or edit.
 * @example openTrackerModal(tracker);
 */
function openTrackerModal(tracker) {
    activeModalTracker = { ...tracker, links: [...(tracker.links || [])] };
    document.getElementById('trackerModalTitle').innerText = tracker.title;
    document.getElementById('trackerModalEpisodeInput').value = tracker.currentEpisode;
    document.getElementById('trackerModalLinkUrl').value = '';
    document.getElementById('trackerModalLinkLabel').value = '';
    renderTrackerModalLinks(activeModalTracker);
    document.getElementById('trackerModal').style.display = 'flex';
}

/**
 * Attaches click and keyboard listeners for tracker modal controls.
 * @example initTrackerModal();
 */
function initTrackerModal() {
    const modal = document.getElementById('trackerModal');
    if (!modal) return;
    document.getElementById('trackerModalCloseBtn').onclick = closeTrackerModal;
    document.getElementById('trackerModalSaveBtn').onclick = handleSaveTrackerValue;
    document.getElementById('trackerModalAddLinkBtn').onclick = handleAddTrackerModalLink;
    document.getElementById('trackerModalDeleteBtn').onclick = handleDeleteModalTracker;
    modal.onclick = (e) => { if (e.target === modal) closeTrackerModal(); };
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeTrackerModal(); });
}
