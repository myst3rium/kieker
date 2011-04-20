package kieker.monitoring.writer.filesystem;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;

import kieker.common.record.IMonitoringRecord;
import kieker.monitoring.core.configuration.Configuration;
import kieker.monitoring.core.controller.ITimeSourceController;
import kieker.monitoring.writer.AbstractAsyncThread;
import kieker.monitoring.writer.AbstractAsyncWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * ==================LICENCE=========================
 * Copyright 2006-2011 Kieker Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==================================================
 */
/**
 * @author Matthias Rohr, Andre van Hoorn, Jan Waller, Robert von Massow
 */
public final class AsyncFsWriter extends AbstractAsyncWriter {
	private static final Log log = LogFactory.getLog(AsyncFsWriter.class);

	private static final String PREFIX = AsyncFsWriter.class.getName() + ".";
	private static final String PATH = AsyncFsWriter.PREFIX
			+ "customStoragePath";
	private static final String TEMP = AsyncFsWriter.PREFIX
			+ "storeInJavaIoTmpdir";

	public AsyncFsWriter(final Configuration configuration) {
		super(configuration);
	}

	@Override
	protected void init() {
		String path;
		if (this.configuration.getBooleanProperty(AsyncFsWriter.TEMP)) {
			path = System.getProperty("java.io.tmpdir");
		} else {
			path = this.configuration.getStringProperty(AsyncFsWriter.PATH);
		}
		File f = new File(path);
		if (!f.isDirectory()) {
			AsyncFsWriter.log.error("'" + path + "' is not a directory.");
			throw new IllegalArgumentException("'" + path
					+ "' is not a directory.");
		}
		final String ctrlName =
				this.getController().getControllerConfig().getHostName() + "-"
						+ this.getController().getControllerConfig().getName();

		final DateFormat m_ISO8601UTC =
				new SimpleDateFormat("yyyyMMdd'-'HHmmssSS");
		m_ISO8601UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
		final String dateStr = m_ISO8601UTC.format(new java.util.Date());
		path =
				path + File.separatorChar + "kieker-" + dateStr + "-UTC-"
						+ ctrlName + File.separatorChar;
		f = new File(path);
		if (!f.mkdir()) {
			AsyncFsWriter.log
					.error("Failed to create directory '" + path + "'");
			throw new IllegalArgumentException("Failed to create directory '"
					+ path + "'");
		}

		final String mappingFileFn = path + File.separatorChar + "kieker.map";
		final MappingFileWriter mappingFileWriter;
		try {
			mappingFileWriter = new MappingFileWriter(mappingFileFn);
		} catch (final Exception ex) {
			AsyncFsWriter.log.error("Failed to create mapping file '"
					+ mappingFileFn + "'", ex);
			throw new IllegalArgumentException(
					"Failed to create mapping file '" + mappingFileFn + "'", ex);
		}
		this.addWorker(new FsWriterThread(this.getController(),
				this.blockingQueue, mappingFileWriter, path));
	}

}

/**
 * @author Matthias Rohr, Andre van Hoorn, Jan Waller
 */
final class FsWriterThread extends AbstractAsyncThread {

	// configuration parameters
	private static final int maxEntriesInFile = 25000;

	// internal variables
	private final String filenamePrefix;
	private final MappingFileWriter mappingFileWriter;
	private PrintWriter pos = null;
	private int entriesInCurrentFileCounter =
			FsWriterThread.maxEntriesInFile + 1; // Force to initialize first
													// file!

	// to get that info later
	private final String path;

	public FsWriterThread(final ITimeSourceController ctrl,
			final BlockingQueue<IMonitoringRecord> writeQueue,
			final MappingFileWriter mappingFileWriter, final String path) {
		super(ctrl, writeQueue);
		this.path = new File(path).getAbsolutePath();
		this.filenamePrefix = path + File.separatorChar + "kieker";
		this.mappingFileWriter = mappingFileWriter;
	}

	// TODO: keep track of record type ID mapping!
	/**
	 * Note that it's not necessary to synchronize this method since a file is
	 * written at most by one thread.
	 * 
	 * @throws java.io.IOException
	 */
	@Override
	protected final void consume(final IMonitoringRecord monitoringRecord)
			throws IOException {
		final Object[] recordFields = monitoringRecord.toArray();
		final int LAST_FIELD_INDEX = recordFields.length - 1;
		// check if file exists and is not full
		this.prepareFile(); // may throw FileNotFoundException

		this.pos.write("$");
		this.pos.write(Integer.toString((this.mappingFileWriter
				.idForRecordTypeClass(monitoringRecord.getClass()))));
		this.pos.write(';');
		this.pos.write(Long.toString(monitoringRecord.getLoggingTimestamp()));
		if (LAST_FIELD_INDEX > 0) {
			this.pos.write(';');
		}
		for (int i = 0; i <= LAST_FIELD_INDEX; i++) {
			final Object val = recordFields[i];
			// TODO: assert that val!=null and provide suitable log msg if null
			this.pos.write(val.toString());
			if (i < LAST_FIELD_INDEX) {
				this.pos.write(';');
			}
		}
		this.pos.println();
		this.pos.flush();
	}

	/**
	 * Determines and sets a new Filename
	 */
	private final void prepareFile() throws FileNotFoundException {
		if (this.entriesInCurrentFileCounter++ > FsWriterThread.maxEntriesInFile) {
			if (this.pos != null) {
				this.pos.close();
			}
			this.entriesInCurrentFileCounter = 0;

			final DateFormat m_ISO8601UTC =
					new SimpleDateFormat("yyyyMMdd'-'HHmmssSS");
			m_ISO8601UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
			final String dateStr = m_ISO8601UTC.format(new java.util.Date());
			// TODO: where does this number come from?
			// final int random = (new Random()).nextInt(100);
			final String filename =
					this.filenamePrefix + "-" + dateStr + "-UTC-"
							+ this.getName() + ".dat";
			// log.debug("** " +
			// java.util.Calendar.getInstance().currentTimeNanos().toString() +
			// " new filename: " + filename);
			this.pos =
					new PrintWriter(new DataOutputStream(
							new BufferedOutputStream(new FileOutputStream(
									filename))));
			this.pos.flush();
		}
	}

	@Override
	protected void cleanup() {
		if (this.pos != null) {
			this.pos.close();
		}
	}

	@Override
	public final String getInfoString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(super.getInfoString());
		sb.append("; Writing to Directory: '");
		sb.append(this.path);
		sb.append("'");
		return sb.toString();
	}
}
