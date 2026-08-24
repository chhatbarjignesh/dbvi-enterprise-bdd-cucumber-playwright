package com.dbvi.automation.framework.perfecto;

import com.dbvi.automation.framework.factory.DriverFactory;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventHandler;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;

/**
 * PerfectoReportingPlugin is a custom Cucumber plugin/listener that listens to test step lifecycles
 * and automatically dispatches stepStart and stepEnd commands to Perfecto Smart Reporting.
 */
public class PerfectoReportingPlugin implements ConcurrentEventListener {

    private final EventHandler<TestStepStarted> stepStartedHandler = this::handleTestStepStarted;
    private final EventHandler<TestStepFinished> stepFinishedHandler = this::handleTestStepFinished;

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestStepStarted.class, stepStartedHandler);
        publisher.registerHandlerFor(TestStepFinished.class, stepFinishedHandler);
    }

    private void handleTestStepStarted(TestStepStarted event) {
        // We only care about BDD steps (PickleStepTestStep), not hooks
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
            String stepText = step.getStep().getKeyword() + " " + step.getStep().getText();

            PerfectoReporter reporter = DriverFactory.getPerfectoReporter();
            if (reporter != null) {
                reporter.stepStart(stepText);
            }
        }
    }

    private void handleTestStepFinished(TestStepFinished event) {
        // We only care about BDD steps (PickleStepTestStep), not hooks
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PerfectoReporter reporter = DriverFactory.getPerfectoReporter();
            if (reporter != null) {
                String status = event.getResult().getStatus().name();
                reporter.stepEnd("Step finished with status: " + status);
            }
        }
    }
}
