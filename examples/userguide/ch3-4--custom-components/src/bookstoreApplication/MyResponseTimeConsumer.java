/***************************************************************************
 * Copyright 2011 by
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

package bookstoreApplication;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

import kieker.analysis.plugin.AbstractAnalysisPlugin;
import kieker.analysis.plugin.ISingleInputPort;
import kieker.analysis.plugin.port.AbstractInputPort;
import kieker.analysis.plugin.port.InputPort;
import kieker.common.record.IMonitoringRecord;

public class MyResponseTimeConsumer extends AbstractAnalysisPlugin implements ISingleInputPort {
	
	private static final Collection<Class<?>> IN_CLASSES = Collections.unmodifiableCollection(new CopyOnWriteArrayList<Class<?>>(
			new Class<?>[] { IMonitoringRecord.class }));
	private final AbstractInputPort input = new InputPort("in", MyResponseTimeConsumer.IN_CLASSES, this);

	@Override
	public void newEvent(Object event) {
		if (event instanceof MyResponseTimeRecord) {
			/* Write the content to the standard output stream. */
			MyResponseTimeRecord myRecord = (MyResponseTimeRecord) event;
			System.out.println("[Consumer] " + myRecord.getLoggingTimestamp()
					+ ": " + myRecord.className + ", " + myRecord.methodName
					+ ", " + myRecord.responseTimeNanos);
		}
	}

	@Override
	public boolean execute() {
		return true;
	}

	@Override
	public void terminate(boolean error) {}
}
