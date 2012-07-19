/***************************************************************************
 * Copyright 2012 by
 *  + Christian-Albrechts-University of Kiel
 *    + Department of Computer Science
 *      + Software Engineering Group 
 *  and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package kieker.test.common.util.namedRecordPipe;

import java.util.concurrent.atomic.AtomicInteger;

import kieker.common.namedRecordPipe.Broker;
import kieker.common.namedRecordPipe.IPipeWriter;
import kieker.common.namedRecordPipe.Pipe;
import kieker.common.record.IMonitoringRecord;

/**
 * @author Andre van Hoorn
 */
public final class NamedPipeFactory {
	private static final AtomicInteger NEXT_PIPE_ID = new AtomicInteger(0);
	private static final String PIPE_NAME_PREFIX = "pipeName_" + NamedPipeFactory.class.getName() + "_";

	private NamedPipeFactory() {}

	/**
	 * This method should be used in tests to generate unique names for {@link kieker.monitoring.core.configuration.Configuration}s with
	 * {@link kieker.monitoring.writer.namedRecordPipe.PipeWriter}s and {@link java.io.PipedReader}s
	 * in order to avoid naming conflicts.
	 * 
	 * @return a unique name
	 */
	public static final String createPipeName() {
		return NamedPipeFactory.PIPE_NAME_PREFIX + NamedPipeFactory.NEXT_PIPE_ID.getAndIncrement();
	}

	/**
	 * Creates an {@link kieker.common.record.IMonitoringRecordReceiver} that writes records
	 * to a {@link Pipe} with the given name.
	 * 
	 * @param pipeName
	 * @return the {@link kieker.common.record.IMonitoringRecordReceiver}
	 */
	public static final IPipeWriter createAndRegisterNamedPipeRecordWriter(final String pipeName) {
		final Pipe namedPipe = Broker.INSTANCE.acquirePipe(pipeName);
		final IPipeWriter writer = new IPipeWriter() {

			public boolean newMonitoringRecord(final IMonitoringRecord record) {
				return namedPipe.writeMonitoringRecord(record);
			}
		};
		return writer;
	}
}