package kieker.tpan;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Enumeration;

import java.util.TreeSet;
import kieker.common.logReader.LogReaderExecutionException;
import kieker.common.logReader.RecordConsumerExecutionException;
import kieker.common.logReader.filesystemReader.FSReader;
import kieker.tpan.datamodel.ExecutionTrace;
import kieker.tpan.datamodel.InvalidTraceException;
import kieker.tpan.datamodel.MessageTrace;
import kieker.tpan.logReader.JMSReader;
import kieker.tpan.plugins.DependencyGraphPlugin;
import kieker.tpan.plugins.SequenceDiagramPlugin;
import kieker.tpan.recordConsumer.BriefJavaFxInformer;
import kieker.tpan.recordConsumer.ExecutionSequenceRepositoryFiller;

import kieker.tpan.recordConsumer.MonitoringRecordTypeLogger;
import kieker.tpmon.core.TpmonController;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * ==================LICENCE=========================
 * Copyright 2006-2009 Kieker Project
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
 * ==================================================
 */

/**
 * @author Andre van Hoorn, Matthias Rohr
 * History
 * 2009-07-01 (AvH) Initial version
 * 2009-07-25 (MR) Checks against invalid input dir
 */
public class TpanTool {

    private static final Log log = LogFactory.getLog(TpanTool.class);
    private static final String SEQUENCE_DIAGRAM_FN_PREFIX = "sequenceDiagram";
    private static final String DEPENDENCY_GRAPH_FN_PREFIX = "dependencyGraph";
    private static final String MESSAGE_TRACES_FN_PREFIX = "messageTraces";
    private static final String EXECUTION_TRACES_FN_PREFIX = "executionTraces";

    private static CommandLine cmdl = null;
    private static final CommandLineParser cmdlParser = new BasicParser();
    private static final HelpFormatter cmdHelpFormatter = new HelpFormatter();
    private static final Options cmdlOpts = new Options();
    private static String inputDir = null;
    private static String outputDir = null;
    private static String task = null;
    private static String outputFnPrefix = null;
    private static TreeSet<Long> selectedTraces = null;


    static {
        cmdlOpts.addOption(OptionBuilder.withLongOpt("inputdir").withArgName("dir").hasArg(true).isRequired(false).withDescription("Log directory to read data from").withValueSeparator('=').create("i"));
        cmdlOpts.addOption(OptionBuilder.withLongOpt("outputdir").withArgName("dir").hasArg(true).isRequired(true).withDescription("Directory for the generated file(s)").withValueSeparator('=').create("o"));
        cmdlOpts.addOption(OptionBuilder.withLongOpt("output-filename-prefix").withArgName("dir").hasArg(true).isRequired(false).withDescription("Prefix for output filenames\n").withValueSeparator('=').create("p"));

        //OptionGroup cmdlOptGroupTask = new OptionGroup();
        //cmdlOptGroupTask.isRequired();
        cmdlOpts.addOption(OptionBuilder.withLongOpt("plot-Sequence-Diagram").hasArg(false).withDescription("Generate sequence diagrams (.pic format) from log data").create());
        cmdlOpts.addOption(OptionBuilder.withLongOpt("plot-Dependency-Graph").hasArg(false).withDescription("Generate a dependency graph (.dot format) from log data").create());
        cmdlOpts.addOption(OptionBuilder.withLongOpt("print-Message-Trace").hasArg(false).withDescription("Generate a message trace representation from log data").create());
        cmdlOpts.addOption(OptionBuilder.withLongOpt("print-Execution-Trace").hasArg(false).withDescription("Generate an execution trace representation from log data").create());
        cmdlOpts.addOption(OptionBuilder.withLongOpt("init-basic-JMS-reader").hasArg(false).withDescription("Creates a jms reader and shows incomming data in the command line").create());
        cmdlOpts.addOption(OptionBuilder.withLongOpt("init-basic-JMS-readerJavaFx").hasArg(false).withDescription("Creates a jms reader and shows incomming data in the command line and visualizes with javafx").create());

        //cmdlOpts.addOptionGroup(cmdlOptGroupTask);

        cmdlOpts.addOption(OptionBuilder.withLongOpt("select-traces").withArgName("id0,...,idn").hasArgs().isRequired(false).withDescription("Consider only the traces identified by the comma-separated list of trace IDs. Defaults to all traces.").create("t"));
    }

    private static boolean parseArgs(String[] args) {
        try {
            cmdl = cmdlParser.parse(cmdlOpts, args);
        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            printUsage();
            return false;
        }
        return true;
    }

    private static void printUsage() {
        cmdHelpFormatter.printHelp(TpanTool.class.getName(), cmdlOpts);
    }

    private static boolean initFromArgs() {
        inputDir = cmdl.getOptionValue("inputdir") + File.separator;
        if (inputDir.equals("${inputDir}/")) {
            log.error("Invalid iput dir '"+inputDir+"'. Add it as command-line parameter to you ant call (e.g., ant run-tpan -DinputDir=/tmp) or to a properties file.");
        }
        outputDir = cmdl.getOptionValue("outputdir") + File.separator;
        outputFnPrefix = cmdl.getOptionValue("output-filename-prefix", "");
        if (cmdl.hasOption("select-traces")) { /* Parse liste of trace Ids */
            String[] traceIdList = cmdl.getOptionValues("select-traces");
            selectedTraces = new TreeSet<Long>();
            int numSelectedTraces = traceIdList.length;
            try {
                for (String idStr : traceIdList) {
                    selectedTraces.add(Long.valueOf(idStr));
                }
                log.info(numSelectedTraces + " trace" + (numSelectedTraces > 1 ? "s" : "") + " selected");
            } catch (Exception e) {
                System.err.println("Failed to parse list of trace IDs: " + traceIdList + "(" + e.getMessage() + ")");
                return false;
            }
        }
        return true;
    }

    private static boolean dispatchTasks() {
        boolean retVal = true;
        int numRequestedTasks = 0;

        try {

           if (retVal && cmdl.hasOption("print-Message-Trace")) {
                numRequestedTasks++;
                retVal = task_genMessageTracesForTraceSet(inputDir, outputDir + File.separator + outputFnPrefix, selectedTraces);
            }
            if (retVal && cmdl.hasOption("print-Execution-Trace")) {
                numRequestedTasks++;
                retVal = task_genExecutionTracesForTraceSet(inputDir, outputDir + File.separator + outputFnPrefix, selectedTraces);
            }
            if (retVal && cmdl.hasOption("plot-Sequence-Diagram")) {
                numRequestedTasks++;
                retVal = task_genSequenceDiagramsForTraceSet(inputDir, outputDir + File.separator + outputFnPrefix, selectedTraces);
            }
            if (retVal && cmdl.hasOption("plot-Dependency-Graph")) {
                numRequestedTasks++;
                retVal = task_genDependencyGraphsForTraceSet(inputDir, outputDir + File.separator + outputFnPrefix, selectedTraces);
            }
           if (retVal && cmdl.hasOption("init-basic-JMS-reader")) {
                numRequestedTasks++;
                retVal = task_initBasicJmsReader("tcp://127.0.0.1:3035/","queue1");
                System.out.println("Finished to start task_initBasicJmsReader");
            }
            if (retVal && cmdl.hasOption("init-basic-JMS-readerJavaFx")) {
                numRequestedTasks++;
                retVal = task_initBasicJmsReaderJavaFx("tcp://127.0.0.1:3035/","queue1");
                System.out.println("Finished to start task_initBasicJmsReader");
            }

           if(!retVal) {
               System.err.println("A task failed");
           }
        } catch (Exception ex) {
            System.err.println("An error occured: " + ex.getMessage());
            System.err.println("");
            System.err.println("See 'kieker.log' for details");
            log.error("Exception", ex);
            retVal = false;
        }

        if (numRequestedTasks == 0) {
            System.err.println("No task requested");
            printUsage();
            retVal = false;
        }

        return retVal;
    }

    public static void main(String args[]) {
        try{
        if (true && ((!parseArgs(args) || !initFromArgs() || !dispatchTasks()))) {
            System.exit(1);
        }

        //Thread.sleep(2000);

        /* As long as we have a dependency from logAnalysis to tpmon,
         * we need t terminate tpmon explicitly. */
        TpmonController.getInstance().terminateMonitoring();
        } catch (Exception exc){
            System.err.println("An error occured. See log for details");
            log.fatal(args, exc);
        }
    }

    /**
     * Reads the traces from the directory inputDirName and write the
     * sequence diagrams for traces with IDs given in traceSet to
     * the directory outputFnPrefix.
     * If  traceSet is null, a sequence diagram for each trace is generated.
     *
     * @param inputDirName
     * @param outputFnPrefix
     * @param traceSet
     */
    private static boolean task_genSequenceDiagramsForTraceSet(String inputDirName, String outputFnPrefix, final TreeSet<Long> traceIds) throws IOException, InvalidTraceException, LogReaderExecutionException, RecordConsumerExecutionException {
        boolean retVal = true;
        log.info("Reading traces from directory '" + inputDirName + "'");
        /* Read log data and collect execution traces */
        TpanInstance analysisInstance = new TpanInstance();
        //analysisInstance.setLogReader(new FSReader(inputDirName));
        analysisInstance.setLogReader(new FSReader(inputDirName));
        ExecutionSequenceRepositoryFiller seqRepConsumer = new ExecutionSequenceRepositoryFiller();
        analysisInstance.addRecordConsumer(seqRepConsumer);
        analysisInstance.run();

        /* Generate and output sequence diagrams */
        Enumeration<ExecutionTrace> seqEnum = seqRepConsumer.getExecutionSequenceRepository().repository.elements();
        int numPlots = 0;
        long lastTraceId = -1;
        String outputFnBase = new File(outputFnPrefix + SEQUENCE_DIAGRAM_FN_PREFIX).getCanonicalPath();
        while (seqEnum.hasMoreElements()) {
            ExecutionTrace t = seqEnum.nextElement();
            Long id = t.getTraceId();
            if (traceIds == null || traceIds.contains(id)) {
                //String fileName = "/tmp/seqDia" + msgTrace.traceId + ".pic";
                MessageTrace mSeq = t.toMessageTrace();
                if (mSeq == null) {
                    log.error("Transformation to message trace failed for trace " + id);
                    retVal = false;
                }
                SequenceDiagramPlugin.writeDotForMessageTrace(mSeq, outputFnBase + "-" + id + ".pic");
                numPlots++;
                lastTraceId = t.getTraceId();
            }
        }
        if (numPlots > 0) {
            System.out.println("Wrote " + numPlots + " sequence diagram" + (numPlots > 1 ? "s" : "") + " to file" + (numPlots > 1 ? "s" : "") + " with name pattern '" + outputFnBase + "-<traceId>.pic'");
            System.out.println("Pic files can be converted using the pic2plot tool (package plotutils)");
            System.out.println("Example: pic2plot -T svg " + outputFnBase + "-" + ((numPlots > 0) ? lastTraceId : "<traceId>") + ".pic > " + outputFnBase + "-" + ((numPlots > 0) ? lastTraceId : "<traceId>") + ".svg");
        } else {
            System.out.println("Wrote 0 sequence diagrams");
        }
        return retVal;
    }

    /**
     * Reads the traces from the directory inputDirName and write the
     * dependency graph for traces with IDs given in traceSet to
     * the directory outputFnPrefix.
     * If  traceSet is null, a dependency graph containing the information
     * of all traces is generated.
     *
     * @param inputDirName
     * @param outputFnPrefix
     * @param traceSet
     */
    private static boolean task_genDependencyGraphsForTraceSet(String inputDirName, String outputFnPrefix, TreeSet<Long> traceIds) throws IOException, InvalidTraceException, LogReaderExecutionException, RecordConsumerExecutionException {
        boolean retVal = true;
        log.info("Reading traces from directory '" + inputDirName + "'");
        /* Read log data and collect execution traces */
        TpanInstance analysisInstance = new TpanInstance();
        //analysisInstance.setLogReader(new FSReader(inputDirName));
        analysisInstance.setLogReader(new FSReader(inputDirName));
        ExecutionSequenceRepositoryFiller seqRepConsumer = new ExecutionSequenceRepositoryFiller();
        analysisInstance.addRecordConsumer(seqRepConsumer);
        analysisInstance.run();

        /* Generate and output dependency graphs */
        Collection<ExecutionTrace> seqEnum = seqRepConsumer.getExecutionSequenceRepository().repository.values();
        String outputFnBase = new File(outputFnPrefix + DEPENDENCY_GRAPH_FN_PREFIX).getCanonicalPath();
        DependencyGraphPlugin.writeDotFromExecutionTraces(seqEnum, outputFnBase + ".dot", traceIds);
        System.out.println("Wrote dependency graph to file '" + outputFnBase + ".dot" + "'");
        System.out.println("Dot file can be converted using the dot tool");
        System.out.println("Example: dot -T svg " + outputFnBase + ".dot" + " > " + outputFnBase + ".svg");
        return retVal;
    }

    /**
     * Reads the traces from the directory inputDirName and write the
     * message trace representation for traces with IDs given in traceSet
     * to the directory outputFnPrefix.
     * If  traceSet is null, a message trace for each trace is generated.
     *
     * @param inputDirName
     * @param outputFnPrefix
     * @param traceSet
     */
    private static boolean task_genMessageTracesForTraceSet(String inputDirName, String outputFnPrefix, final TreeSet<Long> traceIds) throws IOException, InvalidTraceException, LogReaderExecutionException, RecordConsumerExecutionException {
        boolean retVal = true;
        log.info("Reading traces from directory '" + inputDirName + "'");
        /* Read log data and collect execution traces */
        TpanInstance analysisInstance = new TpanInstance();
        analysisInstance.setLogReader(new FSReader(inputDirName));
        ExecutionSequenceRepositoryFiller seqRepConsumer = new ExecutionSequenceRepositoryFiller();
        analysisInstance.addRecordConsumer(seqRepConsumer);
        analysisInstance.run();

        /* Generate and output message traces */
        Enumeration<ExecutionTrace> seqEnum = seqRepConsumer.getExecutionSequenceRepository().repository.elements();
        int numTraces = 0;
        String outputFn = new File(outputFnPrefix + MESSAGE_TRACES_FN_PREFIX + ".txt").getCanonicalPath();
        PrintStream ps = System.out;
        try {
            ps = new PrintStream(new FileOutputStream(outputFn));
            while (seqEnum.hasMoreElements()) {
                ExecutionTrace t = seqEnum.nextElement();
                Long id = t.getTraceId();
                if (traceIds == null || traceIds.contains(id)) {
                    numTraces++;
                    ps.println(t.toMessageTrace());
                }
            }
            System.out.println("Wrote " + numTraces + " messageTraces" + (numTraces > 1 ? "s" : "") + " to file '" + outputFn + "'");
        } catch (FileNotFoundException e) {
            log.error("File not found", e);
            retVal = false;
        } finally {
            ps.close();
        }
        return retVal;
    }

    /**
     * Reads the traces from the directory inputDirName and write the
     * execution trace representation for traces with IDs given in traceSet
     * to the directory outputFnPrefix.
     * If  traceSet is null, an execution trace for each trace is generated.
     *
     * @param inputDirName
     * @param outputFnPrefix
     * @param traceSet
     */
    private static boolean task_genExecutionTracesForTraceSet(String inputDirName, String outputFnPrefix, final TreeSet<Long> traceIds) throws IOException, LogReaderExecutionException, RecordConsumerExecutionException {
        boolean retVal = true;
        log.info("Reading traces from directory '" + inputDirName + "'");
        /* Read log data and collect execution traces */
        TpanInstance analysisInstance = new TpanInstance();
        //analysisInstance.setLogReader(new FSReader(inputDirName));
        analysisInstance.setLogReader(new FSReader(inputDirName));
        ExecutionSequenceRepositoryFiller seqRepConsumer = new ExecutionSequenceRepositoryFiller();
        analysisInstance.addRecordConsumer(seqRepConsumer);
        analysisInstance.run();

        /* Generate and output message traces */
        Enumeration<ExecutionTrace> seqEnum = seqRepConsumer.getExecutionSequenceRepository().repository.elements();
        int numTraces = 0;
        String outputFn = new File(outputFnPrefix + EXECUTION_TRACES_FN_PREFIX + ".txt").getCanonicalPath();
        PrintStream ps = System.out;
        try {
            ps = new PrintStream(new FileOutputStream(outputFn));
            while (seqEnum.hasMoreElements()) {
                ExecutionTrace t = seqEnum.nextElement();
                Long id = t.getTraceId();
                if (traceIds == null || traceIds.contains(id)) {
                    numTraces++;
                    ps.println(t);
                }
            }
            System.out.println("Wrote " + numTraces + " executionTraces" + (numTraces > 1 ? "s" : "") + " to file '" + outputFn + "'");
        } catch (FileNotFoundException e) {
            log.error("File not found", e);
            retVal = false;
        } finally {
            ps.close();
        }
        return retVal;
    }

    private static boolean task_initBasicJmsReader(String jmsProviderUrl, String jmsDestination) throws IOException, LogReaderExecutionException, RecordConsumerExecutionException{
        boolean retVal = true;

        /** As long as we have a dependency to tpmon, 
         *  we load it explicitly in order to avoid 
         *  later delays.*/
        TpmonController ctrl = TpmonController.getInstance();
        
        log.info("Trying to start JMS Listener to " + jmsProviderUrl + " "+jmsDestination);
        /* Read log data and collect execution traces */
        TpanInstance analysisInstance = new TpanInstance();
        analysisInstance.setLogReader(new JMSReader(jmsProviderUrl, jmsDestination));

        MonitoringRecordTypeLogger recordTypeLogger = new MonitoringRecordTypeLogger();
        analysisInstance.addRecordConsumer(recordTypeLogger);

        //ExecutionSequenceRepositoryFiller seqRepConsumer = new ExecutionSequenceRepositoryFiller();
        //analysisInstance.addRecordConsumer(seqRepConsumer);

        /*@Matthias: Deactivated this, since the ant task didn't run (Andre) */
        //BriefJavaFxInformer bjfx = new BriefJavaFxInformer();
        //analysisInstance.addRecordConsumer(bjfx);

        analysisInstance.run();
        return retVal;
    }

        private static boolean task_initBasicJmsReaderJavaFx(String jmsProviderUrl, String jmsDestination) throws IOException, LogReaderExecutionException, RecordConsumerExecutionException{
        boolean retVal = true;

        /** As long as we have a dependency to tpmon,
         *  we load it explicitly in order to avoid
         *  later delays.*/
        TpmonController ctrl = TpmonController.getInstance();

        log.info("Trying to start JMS Listener to " + jmsProviderUrl + " "+jmsDestination);
        /* Read log data and collect execution traces */
        TpanInstance analysisInstance = new TpanInstance();
        analysisInstance.setLogReader(new JMSReader(jmsProviderUrl, jmsDestination));

        MonitoringRecordTypeLogger recordTypeLogger = new MonitoringRecordTypeLogger();
        analysisInstance.addRecordConsumer(recordTypeLogger);

        //ExecutionSequenceRepositoryFiller seqRepConsumer = new ExecutionSequenceRepositoryFiller();
        //analysisInstance.addRecordConsumer(seqRepConsumer);

        /*@Matthias: Deactivated this, since the ant task didn't run (Andre) */
        BriefJavaFxInformer bjfx = new BriefJavaFxInformer();
        analysisInstance.addRecordConsumer(bjfx);

        analysisInstance.run();
        return retVal;
    }
}
