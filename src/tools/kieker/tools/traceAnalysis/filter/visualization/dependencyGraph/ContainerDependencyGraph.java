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

package kieker.tools.traceAnalysis.filter.visualization.dependencyGraph;

import kieker.tools.traceAnalysis.filter.visualization.graph.IOriginRetentionPolicy;
import kieker.tools.traceAnalysis.systemModel.ExecutionContainer;

/**
 * This class represents container dependency graphs.
 * 
 * @author Holger Knoche
 * 
 */
public class ContainerDependencyGraph extends AbstractDependencyGraph<ExecutionContainer> {

	/**
	 * Creates a new graph with the given root entity.
	 * 
	 * @param rootEntity
	 *            The root entity to use for this graph
	 */
	public ContainerDependencyGraph(final ExecutionContainer rootEntity, final IOriginRetentionPolicy originPolicy) {
		super(rootEntity, originPolicy);
	}

}