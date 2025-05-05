package it.pagopa.pn.stream.config.springbootcfg;

import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.services.ssm.SsmClient;

class ActivatorTest {
    @Mock
    private AwsConfigs awsConfigs;
    @Mock
    private ExceptionHelper exceptionHelper;

    @Test
    void activatorTest(){
        Assertions.assertDoesNotThrow( ()  -> {
            new AwsConfigsActivation();
            new AwsServicesClientsConfigActivation(awsConfigs);
            new ClockConfigActivation();
            new PnErrorWebExceptionHandlerActivation(exceptionHelper);
            new PnResponseEntityExceptionHandlerActivation(exceptionHelper);
        });
    }
}
