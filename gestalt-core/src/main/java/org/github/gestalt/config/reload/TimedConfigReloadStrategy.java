package org.github.gestalt.config.reload;

import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.source.ConfigSource;

import java.time.Duration;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Reloads a sources every specified duration.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
public final class TimedConfigReloadStrategy extends ConfigReloadStrategy {
    private static final System.Logger logger = System.getLogger(TimedConfigReloadStrategy.class.getName());

    private final Timer timer = new Timer();
    private final Duration reloadDelay;

    /**
     * Constructor for TimedConfigReloadStrategy.
     *
     * @param reloadDelay how often to reload the config source
     */
    public TimedConfigReloadStrategy(Duration reloadDelay) {
        Objects.requireNonNull(reloadDelay, "Reload Delay must be set for a TimedConfigReloadStrategy");
        this.reloadDelay = reloadDelay;
        startTimer();
    }

    /**
     * Constructor for TimedConfigReloadStrategy.
     *
     * @param source      the config source to reload
     * @param reloadDelay how often to reload the config source
     */
    public TimedConfigReloadStrategy(ConfigSource source, Duration reloadDelay) {
        super(source);
        Objects.requireNonNull(reloadDelay, "Reload Delay must be set for a TimedConfigReloadStrategy");
        this.reloadDelay = reloadDelay;
        startTimer();
    }

    private void startTimer() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    reload();
                } catch (GestaltException e) {
                    logger.log(System.Logger.Level.ERROR, "Exception reloading source " + source.name() + ", exception " + e, e);
                }
            }
        }, reloadDelay.toMillis(), reloadDelay.toMillis());
    }
}
