package de.jgoldhammer.springboot.actuator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.Deflater;

import org.apache.commons.io.FileUtils;
import org.springframework.boot.actuate.audit.AuditEventsEndpoint;
import org.springframework.boot.actuate.beans.BeansEndpoint;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.endpoint.DefaultEnablement;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.zeroturnaround.zip.NameMapper;
import org.zeroturnaround.zip.ZipUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MeterRegistry.Search;

@Endpoint(id = "support", defaultEnablement = DefaultEnablement.ENABLED)
public class SupportEndpoint {

	private BeansEndpoint beansEndpoint;
	private InfoEndpoint infoEndpoint;
	private HealthEndpoint healthEndpoint;
	private ThreadDumpEndpoint threadDumpEndpoint;
	private AuditEventsEndpoint auditEventsEndpoint;
	private ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint;
	private EnvironmentEndpoint envEndpoint;
	private FlywayEndpoint flywayEndpoint;
	private LiquibaseEndpoint liquibaseEndpoint;
	private LoggersEndpoint loggersEndpoint;
	private LogFileWebEndpoint logFileWebEndpoint;
	@SuppressWarnings("unused")
	private HeapDumpWebEndpoint heapDumpWebEndpoint;
	private MeterRegistry meterRegistry;
	private TraceEndpoint traceEndpoint;

	private long timeout = TimeUnit.SECONDS.toMillis(2000);

	private Lock lock = new ReentrantLock();
	private ObjectMapper objectMapper;

	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

	public SupportEndpoint(BeansEndpoint beansEndpoint, InfoEndpoint infoEndpoint, HealthEndpoint healthEndpoint,
			ThreadDumpEndpoint threadDumpEndpoint, AuditEventsEndpoint auditEventsEndpoint,
			ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint,
			EnvironmentEndpoint envEndpoint, FlywayEndpoint flywayEndpoint, LiquibaseEndpoint liquibaseEndpoint,
			LoggersEndpoint loggersEndpoint, LogFileWebEndpoint logFileWebEndpoint,
			HeapDumpWebEndpoint headDumpEndpoint, MeterRegistry meterRegistry, TraceEndpoint traceEndpoint,
			ObjectMapper objectMapper) {
		this.beansEndpoint = beansEndpoint;
		this.infoEndpoint = infoEndpoint;
		this.healthEndpoint = healthEndpoint;
		this.threadDumpEndpoint = threadDumpEndpoint;
		this.auditEventsEndpoint = auditEventsEndpoint;
		this.configurationPropertiesReportEndpoint = configurationPropertiesReportEndpoint;
		this.envEndpoint = envEndpoint;
		this.flywayEndpoint = flywayEndpoint;
		this.liquibaseEndpoint = liquibaseEndpoint;
		this.loggersEndpoint = loggersEndpoint;
		this.logFileWebEndpoint = logFileWebEndpoint;
		this.heapDumpWebEndpoint = headDumpEndpoint;
		this.meterRegistry = meterRegistry;
		this.traceEndpoint = traceEndpoint;
		this.objectMapper = objectMapper;

	}

	@ReadOperation
	public WebEndpointResponse<Resource> support() throws IOException {

		List<File> createdFiles = new ArrayList<File>();
		try {
			if (this.lock.tryLock(this.timeout, TimeUnit.MILLISECONDS)) {
				try {
					try {
						createSupportInformation(createdFiles);
						File destZipFile = createZipFile(createdFiles);
						return new WebEndpointResponse<Resource>(new TemporaryFileSystemResource(destZipFile));

					} finally {
						cleanupTemporaryFiles(createdFiles);
					}
				} finally {
					this.lock.unlock();
				}
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return new WebEndpointResponse<>(HttpStatus.TOO_MANY_REQUESTS.value());

	}

	/**
	 * cleanup the files
	 * 
	 * @param createdFiles
	 */
	private void cleanupTemporaryFiles(List<File> createdFiles) {
		for (File createdFile : createdFiles) {
			createdFile.delete();
		}

	}

	private File createZipFile(List<File> createdFiles) throws IOException {
		File destZipFile = Files.createTempFile("support", ".zip").toFile();
		ZipUtil.packEntries((File[]) createdFiles.toArray(new File[createdFiles.size()]), destZipFile,
				new NameMapper() {

					@Override
					public String map(String name) {
						return name;
					}
				}, Deflater.BEST_COMPRESSION);
		return destZipFile;
	}

	/**
	 * @param withHeapDump
	 * @param createdFiles
	 * @throws JsonProcessingException
	 */
	private void createSupportInformation(List<File> createdFiles) throws JsonProcessingException {
		if (beansEndpoint != null) {
			createFileWithInformation("beans",
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(beansEndpoint.beans()),
					createdFiles);
		}

		if (infoEndpoint != null) {
			createFileWithInformation("info",
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(infoEndpoint.info()),
					createdFiles);
		}

		if (healthEndpoint != null) {
			createFileWithInformation("health",
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(healthEndpoint.health()),
					createdFiles);
		}

		if (threadDumpEndpoint != null) {
			createFileWithInformation("threaddump",
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(threadDumpEndpoint.threadDump()),
					createdFiles);
		}

		if (auditEventsEndpoint != null) {
			createFileWithInformation("auditEvents", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
					auditEventsEndpoint.eventsWithPrincipalDateAfterAndType(null, null, null)), createdFiles);
		}

		if (configurationPropertiesReportEndpoint != null) {
			createFileWithInformation("configurationProperties", objectMapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(configurationPropertiesReportEndpoint.configurationProperties()), createdFiles);
		}

		if (envEndpoint != null) {
			createFileWithInformation("env",
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(envEndpoint.environment(null)),
					createdFiles);
		}

		if (flywayEndpoint != null) {
			createFileWithInformation("flyway",
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(flywayEndpoint.flywayReports()),
					createdFiles);
		}

		if (liquibaseEndpoint != null) {
			createFileWithInformation("liquibaseEndpoint", objectMapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(liquibaseEndpoint.liquibaseReports()), createdFiles);
		}

		if (loggersEndpoint != null) {
			createFileWithInformation("loggers",
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(loggersEndpoint.loggers()),
					createdFiles);
		}

		if (logFileWebEndpoint != null) {
			Resource logFile = logFileWebEndpoint.logFile();
			try {
				createdFiles.add(logFile.getFile());
			} catch (IOException e) {
				throw new RuntimeException("Cannot return a file for the logfile resource", e);
			}
		}

		/*
		 * if (heapDumpWebEndpoint != null && withHeapDump != null && withHeapDump ==
		 * Boolean.TRUE) { WebEndpointResponse<Resource> heapDump =
		 * heapDumpWebEndpoint.heapDump(Boolean.TRUE); if (heapDump.getBody() != null)
		 * try { createdFiles.add(heapDump.getBody().getFile()); } catch (IOException e)
		 * { throw new RuntimeException("Cannot return a file for the heapdump created",
		 * e); } }
		 */

		// we cannot use the metricsendpoint because the ListNamesResponse etc. are not
		// visible here...
		/*
		 * if (metricsEndpoint != null) { String metricNamesOutput =
		 * objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
		 * metricsEndpoint.listNames()); Set<String> metricNames =
		 * metricsEndpoint.listNames().getNames(); StringBuilder metricValues = new
		 * StringBuilder(); for (String metricName : metricNames) {
		 * metricValues.append(objectMapper.writerWithDefaultPrettyPrinter().
		 * writeValueAsString(metricsEndpoint.metric(metricName))); }
		 * createFileWithInformation("metrics", metricNamesOutput +
		 * metricValues.toString(), createdFiles); }
		 */
		

		if (meterRegistry != null) {
			Collection<Meter> meters = meterRegistry.getMeters();
			StringBuilder meterValues = new StringBuilder();
			for (Meter meter : meters) {
				Search searchResult = meterRegistry.find(meter.getId().getName());
				meterValues.append(objectMapper.writerWithDefaultPrettyPrinter().without(SerializationFeature.FAIL_ON_EMPTY_BEANS).writeValueAsString(searchResult));
			}

			createFileWithInformation("metrics",
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(meters) + meterValues.toString(), createdFiles);
		}

		if (traceEndpoint != null) {
			createFileWithInformation("trace",
					objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(traceEndpoint.traces()),
					createdFiles);

		}
	}

	private Path createFileWithInformation(String fileName, CharSequence data, List<File> createdFiles) {

		Path beansFile;
		try {
			beansFile = Files.createTempFile(fileName + "-" + dateFormat.format(new Date()) + "-support-", ".txt");
		} catch (IOException e) {
			throw new RuntimeException("Cannot create a temporary file with filename " + fileName, e);
		}

		try {
			FileUtils.write(beansFile.toFile(), data, StandardCharsets.UTF_8);
			createdFiles.add(beansFile.toFile());
		} catch (IOException e) {
			throw new RuntimeException(
					"Cannot write the data to temporary file  " + beansFile.toAbsolutePath().toString(), e);
		}
		return beansFile;

	}
}
