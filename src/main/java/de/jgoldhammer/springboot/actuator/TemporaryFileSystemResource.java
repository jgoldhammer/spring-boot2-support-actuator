package de.jgoldhammer.springboot.actuator;

import java.io.Closeable;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;

/**
 * copied from HeapDumpWebEndpoint because this class was private.
 * 
 * @author jgoldhammer
 *
 */
public final class TemporaryFileSystemResource extends FileSystemResource {

	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

	private final Log logger = LogFactory.getLog(getClass());

	TemporaryFileSystemResource(File file) {
		super(file);
	}

	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		ReadableByteChannel readableChannel = super.readableChannel();
		return new ReadableByteChannel() {

			@Override
			public boolean isOpen() {
				return readableChannel.isOpen();
			}

			@Override
			public void close() throws IOException {
				closeThenDeleteFile(readableChannel);
			}

			@Override
			public int read(ByteBuffer dst) throws IOException {
				return readableChannel.read(dst);
			}

		};
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new FilterInputStream(super.getInputStream()) {

			@Override
			public void close() throws IOException {
				closeThenDeleteFile(this.in);
			}

		};
	}

	private void closeThenDeleteFile(Closeable closeable) throws IOException {
		try {
			closeable.close();
		} finally {
			deleteFile();
		}
	}

	private void deleteFile() {
		try {
			Files.delete(getFile().toPath());
		} catch (IOException ex) {
			TemporaryFileSystemResource.this.logger
					.warn("Failed to delete temporary heap dump file '" + getFile() + "'", ex);
		}
	}

	@Override
	public boolean isFile() {
		// Prevent zero-copy so we can delete the file on close
		return false;
	}

}