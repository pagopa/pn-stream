package it.pagopa.pn.stream.config.springbootcfg;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;

@Configuration
public class RestTemplateFactory {

    private final it.pagopa.pn.commons.pnclients.RestTemplateFactory templateFactory;

    public RestTemplateFactory(it.pagopa.pn.commons.pnclients.RestTemplateFactory restTemplateFactory) {
        this.templateFactory = restTemplateFactory;
    }


    @Bean
    @Primary
    @Qualifier("withOffsetDateTimeFormatter")
    public RestTemplate restTemplateWithOffsetDateTimeFormatter(@Value("${pn.commons.retry.max-attempts}") int retryMaxAttempts, @Value("${pn.commons.connection-timeout-millis}") int connectionTimeout , @Value("${pn.commons.read-timeout-millis}") int readTimeout) {
        // Override del comportamento di serializzazione delle date
        // per ovviare al problema del numero di cifre nella frazione di secondo
        RestTemplate template = templateFactory.restTemplateWithTracing(retryMaxAttempts, connectionTimeout, readTimeout);
        template.getMessageConverters().stream()
                .filter(AbstractJackson2HttpMessageConverter.class::isInstance)
                .map(AbstractJackson2HttpMessageConverter.class::cast)
                .forEach(converter -> converter.getObjectMapper()
                        .configOverride(OffsetDateTime.class)
                        .setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"))
                );
        return template;
    }

}
