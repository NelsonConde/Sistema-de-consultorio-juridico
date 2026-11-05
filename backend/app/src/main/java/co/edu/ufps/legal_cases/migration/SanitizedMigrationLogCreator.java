package co.edu.ufps.legal_cases.migration;

import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogCreator;

/** The deployment command emits its own allowlisted report, never SQL or connection messages. */
public final class SanitizedMigrationLogCreator implements LogCreator {
    private static final Log SILENT = new Log() {
        public boolean isDebugEnabled() { return false; }
        public void debug(String message) { }
        public void info(String message) { }
        public void warn(String message) { }
        public void notice(String message) { }
        public void error(String message) { }
        public void error(String message, Exception exception) { }
    };
    @Override public Log createLogger(Class<?> type) { return SILENT; }
}
