package pe.edu.pucp.skillbridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RecuperacionConfig {

    @Bean(defaultCandidate = false)
    public ThreadPoolTaskExecutor recuperacionExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("recuperacion-");
        return executor;
    }
}
