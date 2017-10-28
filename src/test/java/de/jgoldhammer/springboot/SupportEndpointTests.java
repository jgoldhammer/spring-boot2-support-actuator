package de.jgoldhammer.springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.zeroturnaround.zip.ZipUtil;

import de.jgoldhammer.springboot.actuator.SupportEndpoint;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SupportEndpointTests {

	@Autowired
	SupportEndpoint supportEndpoint;
	
	@Test
	public void shouldReturnZipFile() throws IOException {
		WebEndpointResponse<Resource> supportEndpointResponse = supportEndpoint.support();
		Resource body = supportEndpointResponse.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getFile()).isNotNull();
		
		File tempDirectory = Files.createTempDirectory("support").toFile();
		ZipUtil.unpack(body.getFile(), tempDirectory);
		
		File[] zipEntries = tempDirectory.listFiles();
		boolean containsSupportEndpoint =false;
		for (File zipEntry : zipEntries) {
			boolean isBeansFile = zipEntry.getName().startsWith("beans");
			if(isBeansFile) {
				List<String> contents = Files.readAllLines(zipEntry.toPath());
				for (String content : contents) {
					if(content.contains("SupportEndpoint")) {
						containsSupportEndpoint = true;
					};
				}
			}
		}
		
		assertThat(containsSupportEndpoint).isTrue();
		
	}

}
