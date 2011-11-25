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

package kieker.test.tools.junit.traceAnalysis.plugins;

import junit.framework.Assert;
import junit.framework.TestCase;
import kieker.analysis.plugin.port.AbstractInputPort;
import kieker.common.logging.Log;
import kieker.common.logging.LogFactory;
import kieker.test.tools.junit.traceAnalysis.util.ExecutionFactory;
import kieker.tools.traceAnalysis.plugins.traceFilter.TraceEquivalenceClassFilter;
import kieker.tools.traceAnalysis.plugins.traceReconstruction.InvalidTraceException;
import kieker.tools.traceAnalysis.systemModel.Execution;
import kieker.tools.traceAnalysis.systemModel.ExecutionTrace;
import kieker.tools.traceAnalysis.systemModel.repository.SystemModelRepository;

import org.junit.Test;

/**
 * 
 * @author Andre van Hoorn
 */
public class TestTraceEquivalenceFilterAssemblyEquivalence extends TestCase { // NOCS
	private static final Log LOG = LogFactory.getLog(TestTraceEquivalenceFilterAssemblyEquivalence.class);

	private final SystemModelRepository systemEntityFactory = new SystemModelRepository();
	private final ExecutionFactory executionFactory = new ExecutionFactory(this.systemEntityFactory);

	@Test
	public void testEqualTrace() {
		final ExecutionTrace trace0;
		final ExecutionTrace trace1;

		try {
			trace0 = this.genValidBookstoreTrace(45653l, 17); // NOCS (MagicNumberCheck)
			trace1 = this.genValidBookstoreTrace(45653l, 17); // NOCS (MagicNumberCheck)
		} catch (final InvalidTraceException ex) {
			TestTraceEquivalenceFilterAssemblyEquivalence.LOG.error("InvalidTraceException", ex);
			Assert.fail("InvalidTraceException" + ex);
			return;
		}
		Assert.assertEquals(trace0, trace1);

		final TraceEquivalenceClassFilter filter = new TraceEquivalenceClassFilter("TraceEquivalenceClassFilter", this.systemEntityFactory,
				TraceEquivalenceClassFilter.TraceEquivalenceClassModes.ASSEMBLY);

		/*
		 * Register a handler for equivalence class representatives.
		 */
		filter.getExecutionTraceOutputPort().subscribe(new AbstractInputPort("Execution traces", null) {

			@Override
			public void newEvent(final Object event) {
				throw new UnsupportedOperationException("Not supported yet.");
			}
		});
	}

	private ExecutionTrace genValidBookstoreTrace(final long traceId, final long offset) throws InvalidTraceException {
		/* Executions of a valid trace */
		final Execution exec0_0__bookstore_searchBook; // NOCS
		final Execution exec1_1__catalog_getBook; // NOCS
		final Execution exec2_1__crm_getOrders; // NOCS
		final Execution exec3_2__catalog_getBook; // NOCS

		/* Manually create Executions for a trace */
		exec0_0__bookstore_searchBook = this.executionFactory.genExecution("Bookstore", "bookstore", "searchBook", traceId,
				(1 * (1000 * 1000)) + offset, // tin //NOCS (MagicNumberCheck)
				(10 * (1000 * 1000)) + offset, // tout // NOCS (MagicNumberCheck)
				0, 0); // eoi, ess // NOCS (MagicNumberCheck)

		exec1_1__catalog_getBook = this.executionFactory.genExecution("Catalog", "catalog", "getBook", traceId,
				(2 * (1000 * 1000)) + offset, // NOCS (MagicNumberCheck)
				(4 * (1000 * 1000)) + offset, // NOCS (MagicNumberCheck)
				1, 1); // NOCS (MagicNumberCheck)
		exec2_1__crm_getOrders = this.executionFactory.genExecution("CRM", "crm", "getOrders", traceId,
				(5 * (1000 * 1000)) + offset, // NOCS (MagicNumberCheck)
				(8 * (1000 * 1000)) + offset, // NOCS (MagicNumberCheck)
				2, 1); // NOCS (MagicNumberCheck)
		exec3_2__catalog_getBook = this.executionFactory.genExecution("Catalog", "catalog", "getBook", traceId,
				(6 * (1000 * 1000)) + offset, // NOCS (MagicNumberCheck)
				(7 * (1000 * 1000)) + offset, // NOCS (MagicNumberCheck)
				3, 2); // NOCS (MagicNumberCheck)

		/*
		 * Create an Execution Trace and add Executions in
		 * arbitrary order
		 */
		final ExecutionTrace executionTrace = new ExecutionTrace(traceId);

		executionTrace.add(exec3_2__catalog_getBook);
		executionTrace.add(exec2_1__crm_getOrders);
		executionTrace.add(exec0_0__bookstore_searchBook);
		executionTrace.add(exec1_1__catalog_getBook);

		try {
			/* Make sure that trace is valid: */
			executionTrace.toMessageTrace(this.systemEntityFactory.getRootExecution());
		} catch (final InvalidTraceException ex) {
			TestTraceEquivalenceFilterAssemblyEquivalence.LOG.error("", ex);
			Assert.fail("Test invalid since used trace invalid");
			throw new InvalidTraceException("Test invalid since used trace invalid", ex);
		}

		return executionTrace;
	}
}
