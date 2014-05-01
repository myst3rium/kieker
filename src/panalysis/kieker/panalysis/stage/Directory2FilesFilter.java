/***************************************************************************
 * Copyright 2014 Kieker Project (http://kieker-monitoring.net)
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

package kieker.panalysis.stage;

import java.io.File;
import java.io.FileFilter;

import kieker.common.util.filesystem.BinaryCompressionMethod;
import kieker.common.util.filesystem.FSUtil;
import kieker.panalysis.framework.core.AbstractFilter;
import kieker.panalysis.framework.core.Context;
import kieker.panalysis.framework.core.IOutputPort;

/**
 * @author Christian Wulf
 * 
 * @since 1.10
 */
public class Directory2FilesFilter extends AbstractFilter<Directory2FilesFilter> {

	public final IOutputPort<Directory2FilesFilter, File> FILE = this.createOutputPort();

	private final File inputDirectory;
	private final FileFilter filter;

	public Directory2FilesFilter(final File inputDir) {
		this.inputDirectory = inputDir;

		this.filter = new FileFilter() {
			public boolean accept(final File pathname) {
				final String name = pathname.getName();
				return pathname.isFile()
						&& name.startsWith(FSUtil.FILE_PREFIX)
						&& (name.endsWith(FSUtil.NORMAL_FILE_EXTENSION) || BinaryCompressionMethod.hasValidFileExtension(name));
			}
		};
	}

	/**
	 * @since 1.10
	 */
	@Override
	protected boolean execute(final Context<Directory2FilesFilter> context) {
		final File inputDir = this.inputDirectory;

		final File[] inputFiles = inputDir.listFiles(this.filter);

		for (final File file : inputFiles) {
			context.put(this.FILE, file);
		}

		return true;
	}

}
