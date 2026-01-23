package it.pagopa.pn.stream.utils;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import it.pagopa.pn.stream.PnStreamApplication;
import org.springframework.boot.SpringApplication;

public class SpringApplicationUtils {

    public static SpringApplication buildSpringApplicationWithListener() {
        SpringApplication app = new SpringApplication(PnStreamApplication.class);
        app.addListeners(new TaskIdApplicationListener());
        return app;
    }
}
