package de.jgoldhammer.springboot.actuator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventsEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.beans.BeansEndpoint;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.actuate.flyway.FlywayEndpoint;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.liquibase.LiquibaseEndpoint;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.actuate.management.HeapDumpWebEndpoint;
import org.springframework.boot.actuate.management.ThreadDumpEndpoint;
import org.springframework.boot.actuate.trace.TraceEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class SupportEndpointConfiguration {

	@Bean
	@ConditionalOnEnabledEndpoint
	public SupportEndpoint supportEndpoint(@Autowired(required = false) BeansEndpoint beansEndpoint,
			@Autowired(required = false) InfoEndpoint infoEndpoint,
			@Autowired(required = false) HealthEndpoint healthEndpoint,
			@Autowired(required = false) ThreadDumpEndpoint threadDumpEndpoint,
			@Autowired(required = false) AuditEventsEndpoint auditEventsEndpoint,
			@Autowired(required = false) ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint,
			@Autowired(required = false) EnvironmentEndpoint envEndpoint,
			@Autowired(required = false) FlywayEndpoint flywayEndpoint,
			@Autowired(required = false) LiquibaseEndpoint liquibaseEndpoint,
			@Autowired(required = false) LoggersEndpoint loggersEndpoint,
			@Autowired(required = false) LogFileWebEndpoint logFileWebEndpoint,
			@Autowired(required = false) HeapDumpWebEndpoint headDumpEndpoint,
			@Autowired(required = false) MeterRegistry meterRegistry,
			@Autowired(required = false) TraceEndpoint traceEndpoint,
			@Autowired ObjectMapper objectMapper) {

		return new SupportEndpoint(beansEndpoint, infoEndpoint, healthEndpoint, threadDumpEndpoint, auditEventsEndpoint,
				configurationPropertiesReportEndpoint, envEndpoint, flywayEndpoint, liquibaseEndpoint, loggersEndpoint,
				logFileWebEndpoint, headDumpEndpoint, meterRegistry, traceEndpoint, objectMapper);
	}
}
