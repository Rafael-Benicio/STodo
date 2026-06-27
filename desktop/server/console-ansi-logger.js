/**
 * ANSI color code configurations for different log levels to avoid magic values.
 * @type {Object<string, string>}
 */
const COLORS = {
    INFO: '\x1b[36m',  // Cyan
    WARN: '\x1b[33m',  // Yellow
    ERROR: '\x1b[31m', // Red
    FATAL: '\x1b[35m'  // Magenta
};

/**
 * Reset ANSI code to restore default terminal colors.
 * @type {string}
 */
const COLOR_RESET = '\x1b[0m';

/**
 * Logs an informational message to the console with ANSI styling.
 * @param {string} context - The context or category of the log.
 * @param {string} message - The message content.
 * @example info('Sync', 'Server started');
 */
function info(context, message) {
    const formatted = `${COLORS.INFO}[INFO][${context}] ${message}${COLOR_RESET}`;
    console.log(formatted);
}

/**
 * Logs a warning message to the console with ANSI styling.
 * @param {string} context - The context or category of the log.
 * @param {string} message - The message content.
 * @example warn('Sync', 'Peer disconnected');
 */
function warn(context, message) {
    const formatted = `${COLORS.WARN}[WARN][${context}] ${message}${COLOR_RESET}`;
    console.warn(formatted);
}

/**
 * Logs an error message to the console with ANSI styling.
 * @param {string} context - The context or category of the log.
 * @param {string} message - The message content.
 * @example error('Sync', 'Database locked');
 */
function error(context, message) {
    const formatted = `${COLORS.ERROR}[ERROR][${context}] ${message}${COLOR_RESET}`;
    console.error(formatted);
}

/**
 * Logs a fatal message to the console with ANSI styling.
 * @param {string} context - The context or category of the log.
 * @param {string} message - The message content.
 * @example fatal('Sync', 'Server port conflict');
 */
function fatal(context, message) {
    const formatted = `${COLORS.FATAL}[FATAL][${context}] ${message}${COLOR_RESET}`;
    console.error(formatted);
}

module.exports = {
    info,
    warn,
    error,
    fatal
};
