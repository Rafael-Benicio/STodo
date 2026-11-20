/**
 * Auto-uncheck UI module: manages renewal presets and badges.
 */
const RENEWAL_CONFIG = {
    PRESETS: [
        { label: 'Sem renovação', minutes: 0 },
        { label: 'Diário', minutes: 1440 },
        { label: '2 Dias', minutes: 2880 },
        { label: 'Semanal (7d)', minutes: 10080 }
    ],
    MS_PER_MINUTE: 60 * 1000,
    MS_PER_HOUR: 60 * 60 * 1000,
    HOURS_PER_DAY: 24
};

/**
 * Formats the remaining time until auto-uncheck into a concise readable string.
 * @param {number} uncheckTimestamp - Target expiration epoch milliseconds.
 * @returns {string} Formatted remaining time.
 * @example const text = formatRemainingDuration(1700000000000);
 */
function formatRemainingDuration(uncheckTimestamp) {
    const remainingMs = uncheckTimestamp - Date.now();
    if (remainingMs <= 0) return 'expirando...';
    const hours = Math.floor(remainingMs / RENEWAL_CONFIG.MS_PER_HOUR);
    if (hours >= RENEWAL_CONFIG.HOURS_PER_DAY) {
        const days = Math.ceil(hours / RENEWAL_CONFIG.HOURS_PER_DAY);
        return `${days}d rest`;
    }
    if (hours > 0) return `${hours}h rest`;
    const mins = Math.max(1, Math.floor(remainingMs / RENEWAL_CONFIG.MS_PER_MINUTE));
    return `${mins}m rest`;
}

/**
 * Creates visual badge indicating task auto-renewal status or remaining time.
 * @param {Object} task - Target task entity.
 * @returns {HTMLSpanElement|null} Created badge element or null.
 * @example const badge = createRenewalBadge(task);
 */
function createRenewalBadge(task) {
    const minutes = task.autoUncheckMinutes || 0;
    if (minutes <= 0) return null;

    const badge = document.createElement('span');
    badge.className = 'badge-renewal';
    if (task.completed && task.uncheckTimestamp > 0) {
        badge.innerText = `⏱️ ${formatRemainingDuration(task.uncheckTimestamp)}`;
        badge.title = 'Tempo restante até desmarcar automaticamente';
        return badge;
    }
    const preset = RENEWAL_CONFIG.PRESETS.find(p => p.minutes === minutes);
    const label = preset ? preset.label : `${minutes}m`;
    badge.innerText = `🔄 ${label}`;
    badge.title = 'Renovação automática ativa';
    return badge;
}

/**
 * Creates dropdown select element allowing user to configure task renewal interval.
 * @param {Object} task - Target task entity.
 * @returns {HTMLSelectElement} Dropdown selector element.
 * @example const select = createTaskRenewalSelect(task);
 */
function createTaskRenewalSelect(task) {
    const select = document.createElement('select');
    select.className = 'renewal-select';
    select.title = 'Configurar renovação automática';

    RENEWAL_CONFIG.PRESETS.forEach(preset => {
        const option = document.createElement('option');
        option.value = preset.minutes;
        option.innerText = preset.label;
        option.selected = (task.autoUncheckMinutes || 0) === preset.minutes;
        select.appendChild(option);
    });

    select.onchange = async (e) => {
        task.autoUncheckMinutes = parseInt(e.target.value, 10) || 0;
        await window.api.updateTask(task);
        await loadTasks();
    };
    return select;
}
