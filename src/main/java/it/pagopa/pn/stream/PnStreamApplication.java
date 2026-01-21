package it.pagopa.pn.stream;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static it.pagopa.pn.stream.utils.SpringApplicationUtils.buildSpringApplicationWithListener;

@SpringBootApplication
@EnableScheduling
public class PnStreamApplication {


    public static void main(String[] args) {
        buildSpringApplicationWithListener().run(args);
    }


    @RestController
    @RequestMapping("/")
    public static class RootController {

        @GetMapping("/")
        public String home() {
            return "";
        }
    }
}