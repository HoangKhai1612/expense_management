package com.finai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Budget alert thresholds.
 *
 * These live in configuration rather than in code so that changing a threshold is
 * a deployment setting, not a code change scattered across modules.
 */
@ConfigurationProperties(prefix = "app.budget-alerts")
public class BudgetAlertProperties {

    /** Used / budget ratio at which a WARNING is raised. */
    private BigDecimal warningThreshold = new BigDecimal("80");

    /** Used / budget ratio at which the budget counts as EXCEEDED and a CRITICAL alert is raised. */
    private BigDecimal exceededThreshold = new BigDecimal("100");

    public BigDecimal getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(BigDecimal warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public BigDecimal getExceededThreshold() {
        return exceededThreshold;
    }

    public void setExceededThreshold(BigDecimal exceededThreshold) {
        this.exceededThreshold = exceededThreshold;
    }
}
