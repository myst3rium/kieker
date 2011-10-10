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

package kieker.test.monitoring.junit.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;
import kieker.monitoring.core.configuration.Configuration;
import kieker.monitoring.core.controller.IMonitoringController;
import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.core.sampler.ISampler;
import kieker.monitoring.core.sampler.ScheduledSamplerJob;
import kieker.test.monitoring.junit.util.DefaultConfigurationFactory;

import org.junit.Test;

/**
 * 
 * @author Andre van Hoorn
 * 
 */
public class TestPeriodicSampling extends TestCase { // NOCS

	@Test
	public void testPeriodicSampler() throws InterruptedException {
		final Configuration configuration = DefaultConfigurationFactory.createDefaultConfigurationWithDummyWriter();
		final IMonitoringController monitoringController = MonitoringController.createInstance(configuration);

		final AtomicInteger numTriggers = new AtomicInteger(0);
		final ISampler samplingCounter = new ISampler() {

			@Override
			public void sample(final IMonitoringController monitoringController) {
				numTriggers.incrementAndGet();
			}
		};

		final long period = 3000; // 3000 ms
		final long offset = 300; // i.e., 1st event after 300 ms

		final ScheduledSamplerJob samplerJob = monitoringController.schedulePeriodicSampler(samplingCounter, offset, period, TimeUnit.MILLISECONDS);

		Thread.sleep(6600); // sleep 6.6 seconds // NOCS (MagicNumberCheck)

		// Expecting sampling trigger events at milliseconds 300, 3300, 6300
		final int numEventsBeforeRemoval = numTriggers.get();

		monitoringController.removeScheduledSampler(samplerJob);

		Thread.sleep(10000); // sleep another 10 seconds // NOCS (MagicNumberCheck)

		/* There should be no new trigger events */

		final int numEventsAfterRemoval = numTriggers.get();

		Assert.assertEquals("Unexpected number of triggering events before removal", 3, numEventsBeforeRemoval); // NOCS (MagicNumberCheck)
		Assert.assertEquals("Unexpected number of triggering events before removal", 3, numEventsAfterRemoval); // NOCS (MagicNumberCheck)

		monitoringController.terminateMonitoring();
	}
}
