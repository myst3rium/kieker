/***************************************************************************
 * Copyright 2012 Kieker Project (http://kieker-monitoring.net)
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

package kieker.tools.traceAnalysis.filter.visualization;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.analysis.plugin.AbstractPlugin;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.common.logging.Log;
import kieker.common.logging.LogFactory;
import kieker.tools.traceAnalysis.filter.IGraphOutputtingFilter;
import kieker.tools.traceAnalysis.filter.IGraphProducingFilter;
import kieker.tools.traceAnalysis.filter.visualization.graph.AbstractEdge;
import kieker.tools.traceAnalysis.filter.visualization.graph.AbstractGraph;
import kieker.tools.traceAnalysis.filter.visualization.graph.AbstractVertex;
import kieker.tools.traceAnalysis.filter.visualization.graph.IOriginRetentionPolicy;

/**
 * Abstract superclass for all graph filters.
 * 
 * @param <G>
 * @param <V>
 * @param <E>
 * @param <O>
 * @author Holger Knoche
 */
@Plugin
public abstract class AbstractGraphFilter<G extends AbstractGraph<V, E, O>, V extends AbstractVertex<V, E, O>, E extends AbstractEdge<V, E, O>, O> extends
		AbstractFilterPlugin implements IGraphOutputtingFilter<G> {

	private static final Log LOG = LogFactory.getLog(AbstractGraphFilter.class);

	/**
	 * The name of the filter's graph input port.
	 */
	public static final String INPUT_PORT_NAME_GRAPH = "graphs";

	private final Configuration configuration;

	private final List<IGraphProducingFilter<?>> producers = new ArrayList<IGraphProducingFilter<?>>();

	/**
	 * Creates a new filter with the given configuration;
	 * 
	 * @param configuration
	 *            The filter configuration to use
	 */
	public AbstractGraphFilter(final Configuration configuration) {
		super(configuration);

		this.configuration = configuration;
	}

	@Override
	protected void notifyNewIncomingConnection(final String inputPortName, final AbstractPlugin connectedPlugin, final String outputPortName)
			throws AnalysisConfigurationException {
		final Set<AbstractPlugin> predecessors = connectedPlugin.getIncomingPlugins(true);
		predecessors.add(connectedPlugin);

		for (final AbstractPlugin plugin : predecessors) {
			if (!(plugin instanceof IGraphProducingFilter)) {
				continue;
			}

			final IGraphProducingFilter<?> graphProducer = (IGraphProducingFilter<?>) plugin;
			this.producers.add(graphProducer);
		}
	}

	@Override
	public boolean init() {
		if (!super.init()) {
			return false;
		}

		// Request the desired origin retention policy from the known producers
		try {
			for (final IGraphProducingFilter<?> producer : this.producers) {
				producer.requestOriginRetentionPolicy(this.getDesiredOriginRetentionPolicy());
			}
		} catch (final AnalysisConfigurationException e) {
			LOG.error(e.getMessage(), e);
			return false;
		}

		return true;
	}

	protected abstract IOriginRetentionPolicy getDesiredOriginRetentionPolicy() throws AnalysisConfigurationException;

	public Configuration getCurrentConfiguration() {
		return this.configuration;
	}

	@InputPort(name = INPUT_PORT_NAME_GRAPH,
			description = "Graphs to process",
			eventTypes = { AbstractGraph.class })
	public void processGraph(final G graph) {
		final G processedGraph = this.performConcreteGraphProcessing(graph);
		this.deliver(this.getGraphOutputPortName(), processedGraph);
	}

	public String getGraphOutputPortName() {
		return OUTPUT_PORT_NAME_GRAPH;
	}

	public String getGraphInputPortName() {
		return INPUT_PORT_NAME_GRAPH;
	}

	/**
	 * This method encapsulates the concrete graph processing performed by the concrete filters.
	 * 
	 * @param graph
	 *            The graph to process
	 * @return The processed graph, which may be the same as the input graph
	 */
	protected abstract G performConcreteGraphProcessing(G graph);
}