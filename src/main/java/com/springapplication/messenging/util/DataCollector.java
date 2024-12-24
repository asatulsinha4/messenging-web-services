package com.springapplication.messenging.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * This application is designed to work in the background
 * it will sleep for rest of the time, except on Sundays
 * when it wakes up, creates views of pdbcs labels of the past week
 * makes a sloght change in seedcdb.pm and fires farm jobs to collect
 * cook data and email it.
 * 
 * @version $Header: pdbcs/test/generic/utl/DataCollector.java /main/2
 *          2024/04/12 02:58:06 atatsinh Exp $
 * @author Atul Sinha
 * @since 1.8
 * 
 */
public class DataCollector {

    // private static StackTraceElement[] elements;
    private static Class<?> clazz;
    // private static final Logger log;
    private static final CustomLogger log;
    private static SimpleFormatter formatter;
    /**
     * last pdbcs label post which the application will collect data
     * Eg: 240708, 240812.1
     */
    private static String lastLabel;
    /**
     * The DB versions whose cook data has to be completed.
     * Eg: {19,21,23}
     */
    private static int[] cook_arr;
    /**
     * user who is running this application on their OCI host, as the farm jobs and
     * view creation will happen for this user
     */
    private static String user;
    /**
     * email id from which the email reports have to be sent
     */
    private static String senderEmail;
    /**
     * Email ID(s) of the interested parties who will receive the email report
     */
    private static String resultsRecepientEmail;
    /**
     * Email ID of the person who will receive Error email report in case of any
     * error/exception occurrence
     */
    private static String errorRecepientEmail;
    /**
     * Time unit in hours after which the application will start checking for farm
     * results
     */
    private static int sleepDelayPostFarmSubmit;
    /**
     * Time unit in minutes: the interval between each session of farm jobs
     * completion check
     * It will check until all jobs are checked as completed or
     * farmJobsCompletionCheckTimeout, whichever is first
     */
    private static int delayBetweenEachSessionOfFarmCompletionCheck;
    /**
     * Time unit in hours: the timeout till which the application will check for
     * completion of farm jobs
     */
    private static int farmJobsCompletionCheckTimeout;
    /**
     * Time unit in days: Uptil which the application will try to use view and
     * submit farm jobs in case of ADE or Farm error
     */
    private static int adeOrFarmTimeout;
    /**
     * Time unit in hours: The delay between each successive try
     * The application will retry until successful farm submit or adeOrFarmTimeout,
     * whichever comes first
     */
    private static int delayBetweenEachTryForADEOrFarmFailure;
    /**
     * This is the directory where all the log files will be created, make sure this
     * exists
     */
    private static String currentDirectory;
    /**
     * This number defines the number of farm jobs that will be submitted
     */
    private static int farmIterations;
    /**
     * This is the URL that will be used to update apex database with JSON payload
     */
    private static String dbUpdateURL;
    /**
     * Access this link to view the data in the database
     */
    private static String dbGetURL;

    static class CustomLogger extends Logger {

        protected CustomLogger(String name, String resourceBundleName) {
            super(name, resourceBundleName);
        }

        public void severe(String msg, Throwable thrown) {
            super.log(Level.SEVERE, msg, thrown);
        }

        public void warning(String msg, Throwable thrown) {
            super.log(Level.WARNING, msg, thrown);
        }

    }

    static {
        /**
         * Returns something like this:
         * java.base/java.lang.Thread.getStackTrace(Thread.java:1610)
         * test2.main(test2.java:8)
         */
        // elements = Thread.currentThread().getStackTrace();
        clazz = new Object() {
        }.getClass().getEnclosingClass();
        log = new CustomLogger(clazz.getName(), null);

        /**
         * [2023-11-24T13:43:05,+00:00] - [ERROR ] - {"message": "Exception encountered
         * in DatabaseConnection constructor"}
         * java.lang.RuntimeException: Sample exception
         * at CustomFormatterExample.main(CustomFormatterExample.java:15)
         * 
         */
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tFT%1$tT,%1$tz] - %2$s - [%4$s] - {\"message\": \"%5$s\"}%6$s%n");
        log.setLevel(Level.ALL);
        cook_arr = new int[] { 19, 23 };
        // user who is running this application on their OCI host, as the farm jobs and
        // view creation will happen for this user
        user = "atatsinh";
        // email id from which the email reports have to be sent
        senderEmail = "Cook Time Reporter <atul.a.sinha@oracle.com>";
        // Email ID(s) of the interested parties who will receive the email report
        resultsRecepientEmail = "adbs-install-qa_ww_grp@oracle.com, rajesh.parasuraman@oracle.com";
        // Email ID of the person who will receive Error email report in case of any
        // error/exception occurrence
        errorRecepientEmail = "atul.a.sinha@oracle.com";
        // Time unit in hours after which the application will start checking for farm
        // results
        sleepDelayPostFarmSubmit = 4;
        // Time unit in minutes: the interval between each session of farm jobs
        // completion check
        // It will check until all jobs are checked as completed or
        // farmJobsCompletionCheckTimeout, whichever is first
        delayBetweenEachSessionOfFarmCompletionCheck = 30;
        // Time unit in hours: the timeout till which the application will check for
        // completion of farm jobs
        farmJobsCompletionCheckTimeout = 7;
        // Time unit in days: Uptil which the application will try to use view and
        // submit farm jobs in case of ADE or Farm error
        adeOrFarmTimeout = 2;
        // Time unit in hours: The delay between each successive try
        // The application will retry until successful farm submit or adeOrFarmTimeout,
        // whichever comes first
        delayBetweenEachTryForADEOrFarmFailure = 4;
        // This is the directory where all the log files will be created, make sure this
        // exists
        currentDirectory = "/scratch/atatsinh/work/projects/cook_data_collector/prod/";
        // This number defines the number of farm jobs that will be submitted
        farmIterations = 1;
        // This is the URL that will be used to update apex database with JSON payload
        dbUpdateURL = "https://apex.oraclecorp.com/pls/apex/dbinstall/label/cooktime";
        // Access this link to view the data in the database
        dbGetURL = "https://apex.oraclecorp.com/pls/apex/dbinstall/label/cooktime";

        // logGen.setLevel(Level.ALL);
        formatter = new SimpleFormatter() {
            @Override
            public String format(LogRecord record) {
                return super.format(record);
            }
        };
    }

    public static void main(String args[]) {
        // String currentDirectory =
        // clazz.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

        // PrintStream originalErr = System.err;
        // PrintStream originalOut = System.out;
        if (args.length > 1) {
            try {
                // logger can handle multiple log files. But once it starts writing, it creates
                // a lck file
                // with the same same, representing lock. Using multple streams to write to that
                // same file is not advisable
                // as it creates conflicts and irregular behaviour. Not advisable.
                // PrintStream printOut = new PrintStream(currentDirectory + clazz.getName() +
                // ".log");
                // System.setErr(printOut);
                // System.setOut(printOut);

                ConsoleHandler ch = new ConsoleHandler();
                ch.setFormatter(formatter);
                log.addHandler(ch);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // add handler for global logs
        try {
            FileHandler fhGlobal = new FileHandler(currentDirectory + clazz.getName() + ".%g.log", 10000000, 10, true);
            fhGlobal.setFormatter(formatter);
            log.addHandler(fhGlobal);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        lastLabel = args[0];
        String filesDirectory = currentDirectory + "/files/";
        List<String> records = new ArrayList<>();

        // the directory where this file is kept, all the other files will be kept there
        // FileHandler genFH = new FileHandler(currentDirectory, 0, 0, false)
        // multiple handlers can be added to the logger
        // closing a file handler will stop from getting logs
        // for console handler, even if it is closed, console will still get logs
        // if you wish to stop, you have to remove handler from log
        while (true) {
            try {
                ZonedDateTime startUTC = ZonedDateTime.now(ZoneId.of("UTC"));
                log.info("Today is: " + startUTC);
                ZonedDateTime nextUTC = startUTC.plusHours(3);
                System.out.println(ZonedDateTime.now(ZoneId.of("UTC")).toString());
                long time = Duration.between(startUTC, nextUTC).toMillis();
                if (startUTC.getDayOfWeek() == DayOfWeek.FRIDAY) {
                    log.info("Starting cook data collection");
                    nextUTC = startUTC.plusWeeks(1);
                    String date = String.valueOf(startUTC.toLocalDate().toString());
                    String workingDirectory = filesDirectory + date + "/";
                    String logFilePath = workingDirectory + date + ".%g.log";
                    records.add(0, workingDirectory);
                    // Create the directory if it doesn't exist
                    File logDirectory = new File(logFilePath).getParentFile();
                    if (!logDirectory.exists()) {
                        boolean directoryCreated = logDirectory.mkdirs();
                        log.info("Created directory: " + workingDirectory);
                        if (!directoryCreated) {
                            System.err.println("Failed creating directory: " + logDirectory);
                            System.exit(0);
                        }
                    }
                    FileHandler fh = new FileHandler(logFilePath, 1000000, 2, true);
                    fh.setFormatter(formatter);
                    log.addHandler(fh);
                    start_collecting_data(workingDirectory);
                    ZonedDateTime endUTC = ZonedDateTime.now(ZoneId.of("UTC"));
                    time = Duration.between(endUTC, nextUTC).toMillis();
                    log.info("Completed");
                    if (records.size() > 5) {
                        try {
                            executeCommandOnce("rm -rf " + records.get(5), false);
                        } catch (Exception e) {
                            log.warning("Failed to remove the last record: " + records.get(5), e);
                            e.printStackTrace();
                        }
                    }
                    fh.close();
                }
                log.info("Sleeping...zzz, waking up next around " + nextUTC);
                Thread.sleep(time);
            } catch (Exception e) {
                e.printStackTrace();
                log.severe("Error occurred in starter: ", e);
                sendErrorEmail("Error occurred in starter: ", e);
                System.exit(1);
            }
        }
    }

    /**
     * This functions retries the list of labels post lastLabel
     * 
     * @return list containing PDBCS labels
     */
    public static List<String> getLabelList() {
        List<String> labels = new ArrayList<>();
        log.info("Trying to find labels post " + lastLabel);
        try {
            Label lastLabelObj = new Label(lastLabel);
            List<String> labelList = executeCommandOnce("ade showlabels -series PDBCS_MAIN_LINUX.X64", false);
            List<Label> labelObjList = new ArrayList<>();
            for (String label : labelList) {
                try {
                    Label currLabel = new Label(label.trim().split(".X64_")[1]);
                    if (lastLabelObj.compareTo(currLabel) < 0) {
                        labelObjList.add(currLabel);
                    }
                } catch (InvalidLabelIDException invalidLabelIDException) {
                    log.warning("An invalid label is found: " + label, invalidLabelIDException);
                    sendErrorEmail("An invalid label is found: " + label, invalidLabelIDException);
                } catch (Exception exception) {
                    log.warning("Error occurred while getting label list:\n" + labelList, exception);
                    sendErrorEmail("Error occurred while getting label list:\n" + labelList, exception);
                }
            }
            Collections.sort(labelObjList);
            for (Label label : labelObjList) {
                labels.add(label.value);
            }
            if (labels.size() > 0) {
                log.info("Labels found: " + labels);
            } else {
                log.warning("No labels found post " + lastLabel + ", not collecting data anymore");
            }
        } catch (Exception e) {
            log.severe("Failed getting labels post " + lastLabel, e);
            sendErrorEmail("Failed getting labels post " + lastLabel, e);
        }
        return labels;
    }

    /**
     * This function's use is to create view, submit farm jobs and then destroy
     * views
     * post data retrieval
     * 
     * @param workingDirectory the directory where relevant logs will be created
     */
    public static void start_collecting_data(String workingDirectory) {
        log.info("Cook data collection started");
        List<String> labels = getLabelList();
        if (labels.size() == 0)
            return;
        List<String> cmds = new ArrayList<>();
        // @formatter:off
          // int[] arr = new int[] { 240208, 240209 };
          /*
           * Map:
           * {
           * "240812": {
           *      "farm": [
           *          8576464,
           *          6873673,
           *          9988776
           *      ],
           *      "ifarm": [
           *          6453736,
           *          8877661,
           *          3322445
           *      ]
           *   }
           * }
           * label : farm or ifarm: list of jobs
           * 
           */
          // @formatter:on
        HashMap<String, HashMap<String, HashSet<Integer>>> label_farm_map = new HashMap<>();
        // Set of all farm jobs
        HashSet<Integer> farmJobs = new HashSet<Integer>();
        String farm_submit = "farm submit -jobzone PHX";
        String ifarm_submit = "farm submit -jobzone PHX";
        for (int cook : cook_arr) {
            farm_submit = farm_submit + " lrgsrgcook_dwcs_" + cook;
            ifarm_submit = ifarm_submit + " lrgsrgcook_dwcs_" + cook;
        }
        farm_submit = farm_submit + " -config \"FARM_MANDATE_DB_CREATE=0\" -retry 2";
        ifarm_submit = ifarm_submit + " -config 'IFARM_RUN=1;FARM_MANDATE_DB_CREATE=0' -retry 2";
        cmds.add("ade co -c \"dummy change\" $PDBCS/src/seedcdb.pm");
        ZonedDateTime currentUTC = ZonedDateTime.now(ZoneId.of("UTC"));
        // give a timeout of 2 days
        ZonedDateTime timeout = currentUTC.plusDays(adeOrFarmTimeout);
        boolean retryOcurred = false;
        int loopEnd = (adeOrFarmTimeout * 24) / delayBetweenEachTryForADEOrFarmFailure;
        if ((adeOrFarmTimeout * 24) % delayBetweenEachTryForADEOrFarmFailure != 0)
            loopEnd++;
        try {
            for (String label : labels) {
                List<String> view_cmds = new ArrayList<>();
                view_cmds.addAll(cmds);
                String view_name = "dc_" + label;
                view_cmds.add(0, "ade createview " + view_name + " -label PDBCS_MAIN_LINUX.X64_" + label);
                view_cmds.add(1, "ade useview " + view_name);
                String txn_name = user + "_" + view_name;
                view_cmds.add(2, "ade begintrans " + txn_name + " || ade begintrans -reopen " + txn_name);
                view_cmds.add(3, "ade ciall; ade unbranch -all");
                for (int i = 1; i <= farmIterations; i++) {
                    view_cmds.add(farm_submit + " 1>> " + workingDirectory + view_name + "_farm.log 2>&1");
                    view_cmds.add(ifarm_submit + " 1>> " + workingDirectory + view_name + "_ifarm.log 2>&1");
                }
                // view_cmds.add(farm_submit + " 1>> " + workingDirectory + view_name +
                // "_farm.log 2>&1");
                // view_cmds.add(farm_submit + " 1>> " + workingDirectory + view_name +
                // "_farm.log 2>&1");
                view_cmds.add("exit");
                view_cmds.add("exit");
                // print(view_cmds);
                for (int i = 0; i <= loopEnd; i++) {
                    try {
                        if ((Duration.between(ZonedDateTime.now(ZoneId.of("UTC")), timeout).toMillis() < 0)) {
                            throw new MaxAttemptsOrTimeoutReachedException("Timeout reached");
                        }
                        log.info("Creating and entering view: " + view_name + " of Label: " + label
                                + " and submit farm jobs :: Attempt: " + i);
                        List<String> ade_out = executeCommands(view_cmds, "");
                        if (grepFromList(ade_out, "error", false)) {
                            log.warning(
                                    "Found errors while trying to enter view " + view_name + " and submit farm jobs");
                            sendErrorEmail("Attempt " + i + ": Found errors while entering view for label " + label
                                    + "\n" + print(ade_out, false));
                        }
                        if ((!grepFromList(ade_out, "ENTERING ORACLE LINUX", true))) {
                            log.severe("Failed entering view or found error for label " + label + " or error occurred");
                            if (i == loopEnd) {
                                throw new MaxAttemptsOrTimeoutReachedException(
                                        "Max attempts reached, Failed entering view for label " + label
                                                + ", stopping any new view operations\n" + print(ade_out, false));
                            } else {
                                throw new ADEOrUseviewException(
                                        "Failed entering view for label " + label + "\n" + print(ade_out, false));
                            }
                        }
                        // List<String> farm_output = farm_emulator(view_cmds);
                        // for (String output_line : farm_output) {
                        // executeCommandOnce(
                        // "echo \"" + output_line + "\" 1>> /scratch/atatsinh/" + view_name +
                        // "_farm.log 2>&1",
                        // false);
                        // }
                        log.info("Storing farm jobs details for normal run :: Attemp: " + i);
                        List<String> farm_jobs = farmJobList(workingDirectory + view_name + "_farm.log", i, loopEnd,
                                label);
                        log.info("Storing farm jobs details for IFARM runs :: Attempt: " + i);
                        List<String> ifarm_jobs = farmJobList(workingDirectory + view_name + "_ifarm.log", i, loopEnd,
                                label);
                        label_farm_map.putIfAbsent(label, new HashMap<>());
                        label_farm_map.get(label).putIfAbsent("farm", new HashSet<Integer>());
                        label_farm_map.get(label).putIfAbsent("ifarm", new HashSet<Integer>());
                        String farm_jobs_ids = "";
                        for (String line : farm_jobs) {
                            String[] arrs = line.trim().split(" ");
                            String id = arrs[arrs.length - 1];
                            farm_jobs_ids = farm_jobs_ids + id + " ";
                            int farm_job = Integer.valueOf(id);
                            farmJobs.add(farm_job);
                            label_farm_map.get(label).get("farm").add(farm_job);
                        }
                        executeCommandOnce(
                                "echo \"farm jobs for normal run:\n" + farm_jobs_ids + "\" 1>> " + workingDirectory
                                        + view_name + ".log 2>&1",
                                false);
                        farm_jobs_ids = "";
                        for (String line : ifarm_jobs) {
                            String[] arrs = line.trim().split(" ");
                            String id = arrs[arrs.length - 1];
                            farm_jobs_ids = farm_jobs_ids + id + " ";
                            int farm_job = Integer.valueOf(id);
                            farmJobs.add(farm_job);
                            label_farm_map.get(label).get("ifarm").add(farm_job);
                        }
                        executeCommandOnce(
                                "echo \"farm jobs for ifarm run:\n" + farm_jobs_ids + "\" 1>> " + workingDirectory
                                        + view_name + ".log 2>&1",
                                false);
                        lastLabel = label;
                        break;
                    } catch (MaxAttemptsOrTimeoutReachedException maxAttemptsOrTimeoutReachedException) {
                        log.severe("Max attempts reached, no longer going for any new view operations",
                                maxAttemptsOrTimeoutReachedException);
                        sendErrorEmail("Max attempts reached, no longer going for any new view operations",
                                maxAttemptsOrTimeoutReachedException);
                        throw maxAttemptsOrTimeoutReachedException;
                    } catch (Exception e) {
                        retryOcurred = true;
                        log.severe("Error occurred while creating view and submitting farm jobs for label " + label, e);
                        sendErrorEmail("Error occurred while creating view and submitting farm jobs for label " + label,
                                e);
                        log.info("Sleeping for " + delayBetweenEachTryForADEOrFarmFailure
                                + " hours, retrying again around"
                                + ZonedDateTime.now(ZoneId.of("UTC"))
                                        .plusHours(delayBetweenEachTryForADEOrFarmFailure));
                        Thread.sleep(TimeUnit.HOURS.toMillis(delayBetweenEachTryForADEOrFarmFailure));
                    }
                }
            }
        } catch (Exception e) {
            log.severe("Error occurred, stopping any more view operations", e);
        }
        try {
            if (retryOcurred) {
                log.info("Detected a retry while submitting farm jobs, skipping any sleep");
            } else {
                log.info("Sleeping for " + sleepDelayPostFarmSubmit + " hours");
                Thread.sleep(TimeUnit.HOURS.toMillis(sleepDelayPostFarmSubmit));
                log.info("Woke up, continuing");
            }
            // Submitting farm jobs is completed till this point
            // Call furthur functions to gather data
            check_farm_data(label_farm_map, farmJobs, workingDirectory);
            // data collecting over, destroy views for freeing
            log.info("Data collection done, destroying views");
            for (String label : labels) {
                List<String> view_cmds = new ArrayList<>();
                String view_name = "dc_" + label;
                log.info("Destroying view: " + view_name);
                view_cmds.add("ade useview " + view_name);
                view_cmds.add("ade ciall");
                view_cmds.add("ade unbranch -all");
                view_cmds.add("ade endtrans");
                view_cmds.add("exit");
                view_cmds.add("ade destroyview -force " + view_name);
                view_cmds.add("exit");
                // print(view_cmds);
                try {
                    executeCommands(view_cmds, "");
                } catch (Exception e) {
                    log.severe("Error occurred while destroying view " + view_name, e);
                    sendErrorEmail("Error occurred while destroying view " + view_name, e);
                }
            }
        } catch (Exception e) {
            log.severe("Error occurred while trying to check farm data", e);
            sendErrorEmail("Error occurred while trying to check farm data", e);
            System.exit(1);
        }
    }

    public static List<String> farmJobList(String file_path, int iteration, int loopEnd, String label)
            throws MaxAttemptsOrTimeoutReachedException, FarmException, Exception {
        List<String> farm_jobs = executeCommandOnce(
                "cat " + file_path + " | grep \"Your submission ID is PDBCS_MAIN_LINUX.X64\"", false);
        if ((farm_jobs.size() == 1 && (farm_jobs.get(0).trim().length() == 0)) || farm_jobs.isEmpty()
                || (farm_jobs.size() == 0)) {
            if (iteration == loopEnd) {
                throw new MaxAttemptsOrTimeoutReachedException(
                        "Max attempts reached, failed submitting farm jobs for label: "
                                + label + "\n" + print(
                                        Files.readAllLines(
                                                Paths.get(file_path)),
                                        false)
                                + "\n\nvar farm_jobs:\n" + farm_jobs);
            } else {
                throw new FarmException(
                        "Failed getting farm job IDs for submitted farm jobs for "
                                + label + "\n" + print(
                                        Files.readAllLines(
                                                Paths.get(file_path)),
                                        false)
                                + "\n\nvar farm_jobs:\n" + farm_jobs);
            }
        }

        return farm_jobs;
    }

    /**
     * This function sends email to {@code errorRecepientEmail} in case of an error
     * call this function where error/exception occurrs to send email
     * 
     * @param message The message to send in the body of the email
     */
    public static void sendErrorEmail(String message) {
        String body = "This is an Automated E-Mail from Cook Time Reporter\n";
        body = body + "An error has occurred: \n============================================\n";
        body = body + message + "\n\n";
        try {
            send_mail(errorRecepientEmail, senderEmail,
                    "Cook Time Report ERROR",
                    body, "", new ArrayList<>());
        } catch (Exception e1) {
            log.severe("Error occurred while sending error email: ", e1);
            e1.printStackTrace();
        }
    }

    public static void sendErrorEmail(String message, Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        String body = "This is an Automated E-Mail from Cook Time Reporter\n";
        body = body + "An error has occurred: \n============================================\n";
        body = body + message + "\n\n";
        body = body + sStackTrace;
        try {
            send_mail(errorRecepientEmail, senderEmail,
                    "Cook Time Report ERROR",
                    body, "", new ArrayList<>());
        } catch (Exception e1) {
            log.severe("Error occurred while sending error email: ", e1);
            e1.printStackTrace();
        }
    }

    public static void check_farm_data(HashMap<String, HashMap<String, HashSet<Integer>>> label_farm_map,
            HashSet<Integer> farmJobs,
            String workingDirectory) {
        log.info("Checking farm results available or not");
        if (farmJobs.isEmpty()) {
            log.warning("Found no farm jobs, terminating");
            return;
        }
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Shut down the scheduler gracefully when the program exits
        // Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));
        ScheduledFuture<?> handle = scheduler.schedule(new Runnable() {
            public void run() {
                // handle.wait();
                log.warning(farmJobsCompletionCheckTimeout + " hour timeout: Shutting down scheduler");
                scheduler.shutdown();
            }
        }, farmJobsCompletionCheckTimeout, TimeUnit.HOURS);

        /**
         * scheduleAtFixedRate vs scheduleAtFixedDelay
         * For example, suppose I schedule an alarm to go off with a fixed rate of once
         * an hour, and every time it goes off, I have a cup of coffee, which takes 10
         * minutes. Suppose that starts at midnight, I'd have:
         * 
         * 00:00: Start making coffee
         * 00:10: Finish making coffee
         * 01:00: Start making coffee
         * 01:10: Finish making coffee
         * 02:00: Start making coffee
         * 02:10: Finish making coffee
         * If I schedule with a fixed delay of one hour, I'd have:
         * 
         * 00:00: Start making coffee
         * 00:10: Finish making coffee
         * 01:10: Start making coffee
         * 01:20: Finish making coffee
         * 02:20: Start making coffee
         * 02:30: Finish making coffee
         * Which one you want depends on your task.
         */

        // Schedule the task to run every 30 minutes
        scheduler.scheduleAtFixedRate(() -> {

            HashSet<Integer> farmJobsSet = new HashSet<>();
            farmJobsSet.addAll(farmJobs);
            for (int jobID : farmJobsSet) {
                try {
                    String cmd = "farm showjobs -job " + jobID + " -details";
                    List<String> output = executeCommandOnce(cmd, false);
                    String[] arr = output.get(2).trim().split("\\s+");
                    String status = "";
                    if (arr[0].equals(String.valueOf(jobID))) {
                        int i = 0;
                        while (i < arr.length) {
                            if (arr[i].startsWith((user + "_"))) {
                                i++;
                                status = arr[i];
                                i++;
                                if (status.equals("finished")) {
                                    log.info("farm job:" + jobID + " results available");
                                    farmJobs.remove(jobID);
                                }
                                break;
                            }
                            i++;
                        }
                    } else {
                        log.severe("Error with farm job command: " + output);
                        sendErrorEmail("Error with farm job command: " + print(output, false));
                        System.exit(1);
                    }
                } catch (Exception e) {
                    log.severe("Exception occurred during farm status check for " + jobID, e);
                    sendErrorEmail("Exception occurred during farm status check for " + jobID, e);
                }
            }

            if (farmJobs.isEmpty()) {
                log.info("Results of all farm jobs are available, proceeding with data collection");
                handle.cancel(true);
                scheduler.shutdown();
            } else {
                log.info("Sleeping for 30 mins");
            }
        }, 0, delayBetweenEachSessionOfFarmCompletionCheck, TimeUnit.MINUTES); // Run the task every 30 minutes

        // Add additional code here if needed

        try {
            log.info("Added scheduler for farm jobs check status");
            if (scheduler.awaitTermination(1, TimeUnit.DAYS)) {
                log.info("scheduler terminated, proceeding....");
            } else {
                log.warning("Timed out: 1 Days");
                if (!farmJobs.isEmpty()) {
                    log.warning("Not all jobs data available, remaining jobs: " + farmJobs);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.warning("Problem occurred while in scheduler, ", e);
            sendErrorEmail("Problem occurred while in scheduler", e);
            System.exit(1);
        }

        getFarmResults(label_farm_map, workingDirectory);

    }

    /**
     * This function retrieves the timing logs of the cook job
     * 
     * @param label_farm_map
     * @param workingDirectory The directory where the relevant logs will be created
     */
    public static void getFarmResults(HashMap<String, HashMap<String, HashSet<Integer>>> label_farm_map,
            String workingDirectory) {
        log.info("Getting Farm Job Results locations");
        List<String> cmds;
        for (String label : label_farm_map.keySet()) {
            for (String jobType : label_farm_map.get(label).keySet()) {
                int i = 0;
                for (int job : label_farm_map.get(label).get(jobType)) {
                    i++;
                    try {
                        List<String> res = executeCommandOnce("farm showjobs -job " +
                                job + " -details ",
                                true,
                                "Results location", false, 0, true);
                        String results_location = res.get(0).split(":")[1].trim();
                        log.info("Job: " + job + "\n" + results_location);
                        System.out.println();
                        List<String> cookDirectories = executeCommandOnce(
                                "ls " + results_location + " | grep lrgsrgcook_dwcs_*", false);
                        cmds = new ArrayList<>();
                        for (String cook_directory : cookDirectories) {
                            String cook = cook_directory.split("_dwcs_")[1];
                            String result_location = results_location + "/" + cook_directory + "/";
                            Path directory = Paths.get(result_location);
                            String a = jobType.equals("ifarm") ? "ia.log" : "a.log";
                            String b = jobType.equals("ifarm") ? "ib.log" : "b.log";
                            if (Files.exists(directory) && Files.isDirectory(directory)) {
                                log.info("Directory found for job: " + job + " and cook: " + cook + ": "
                                        + result_location);
                                directory = Paths.get(result_location + "cook_build_logs");
                                if (Files.exists(directory) && Files.isDirectory(directory)) {
                                    cmds.add("cd " + result_location + "cook_build_logs");
                                    cmds.add("pwd");
                                    cmds.add("ls -ltr");
                                    cmds.add("exit");
                                    executeCommands(cmds, workingDirectory + label + "_" + cook + "_" + i + "_" + a);
                                    cmds.clear();
                                    cmds.add("cd " + result_location + "pilot_logs/");
                                    cmds.add("pwd");
                                    cmds.add(
                                            "cat pilot_cook.log*  | zgrep -e \"Module End - Completed\" -e \"Step\" -e \"Module Start\" -e \"Total time for cook deployment\"");
                                    cmds.add("exit");
                                    executeCommands(cmds, workingDirectory + label + "_" + cook + "_" + i + "_" + b);
                                    cmds.clear();
                                } else {
                                    log.warning("Not found: " + result_location + "cook_build_logs");
                                    sendErrorEmail("Not found: " + result_location + "cook_build_logs for job: " + job
                                            + " for label: " + label);
                                }
                            } else {
                                log.warning("Not found: " + result_location);
                                sendErrorEmail("Not found: " + result_location + "cook_build_logs for job: " + job
                                        + " for label: " + label);
                            }
                        }
                    } catch (Exception e) {
                        log.severe("Error occurred while trying to retrieve logs from farm results for job" + job
                                + " label " + label + ": ", e);
                        e.printStackTrace();
                        sendErrorEmail("Error occurred while trying to retrieve logs from farm results for job" + job
                                + " label " + label + ": ", e);
                    }
                }
            }
        }
        generateDataFiles(workingDirectory);
    }

    public static boolean grepFromList(List<String> list, String pattern, boolean case_sensitive) {
        if (!case_sensitive)
            pattern = pattern.toLowerCase();
        for (String line : list) {
            if (!case_sensitive)
                line = line.toLowerCase();
            if (line.contains(pattern))
                return true;
        }
        return false;
    }

    /**
     * This function generates csv files containing the timing of each steps of cook
     * at the end, it also sends the report as email
     * 
     * @param workingDirectory The directory where all the logs exists
     */
    public static void generateDataFiles(String workingDirectory) {
        try {
            log.info("String data retrieve and further processing");
            log.info("Gathering data for normal farm run");
            // data: {cook}: {label}: {column data}
            List<String> csvContentAll = new ArrayList<>();
            List<String> csvContentAvg = new ArrayList<>();
            String[] logPattern = { "*_b.log", "*_ib.log" };
            String body = "This is an Automated E-Mail from Cook Time Reporter\n\n";
            String htmlBody = "<body>\n<br>This is an Automated E-Mail from Cook Time Reporter\n<br>\n<br>\n";
            String table = "";
            List<TableData> tableData = new ArrayList<>();
            List<String> steps = new ArrayList<>();
            for (String pattern : logPattern) {
                log.info("Getting data from log files with pattern: " + pattern);
                HashMap<Integer, HashMap<String, List<StepData>>> dataMap = generateDataMap(workingDirectory, pattern);
                if (dataMap.isEmpty()) {
                    log.warning("No data found for pattern: " + pattern);
                    continue;
                }
                List<Integer> cooks = new ArrayList<>(dataMap.keySet());
                Collections.sort(cooks);
                List<String> tables = new ArrayList<>();
                List<String> tablesForBody = new ArrayList<>();
                List<String> htmlTables = new ArrayList<>();
                List<String> htmlTablesForBody = new ArrayList<>();
                List<String> cookSteps = new ArrayList<>();
                log.info("Arranging table");
                String type = pattern.equals("*_b.log") ? "normal" : "ifarm";
                for (int cook : cooks) {
                    log.info("Cook: " + cook);
                    List<String> tableHeaders = new ArrayList<>();
                    List<String> tableHeaders2 = new ArrayList<>();
                    List<Integer> colLength = new ArrayList<>();
                    List<Integer> colLength2 = new ArrayList<>();
                    HashMap<String, List<String>> tableMap = new HashMap<String, List<String>>() {
                        {
                            put("Cook " + cook + "C labels", new ArrayList<String>());
                        }
                    };
                    HashMap<String, List<String>> tableMap2 = new HashMap<String, List<String>>() {
                        {
                            put(cook + "C labels", new ArrayList<String>());
                        }
                    };
                    tableHeaders.add("Cook " + cook + "C labels");
                    tableHeaders2.add(cook + "C labels");
                    colLength.add(tableHeaders.get(0).length() + 2);
                    colLength2.add(tableHeaders2.get(0).length() + 2);
                    String emptyLine = "";
                    String cookLine = "";
                    HashMap<String, List<StepData>> map = dataMap.get(cook);
                    List<Label> labels = new ArrayList<>();
                    // log.info("Map for cook " + cook + ":\n" + map);
                    for (String label : map.keySet()) {
                        log.info("Label: " + label);
                        boolean storeHeaders = false;
                        if (cookSteps.size() == 0) {
                            storeHeaders = true;
                        }
                        if (emptyLine.length() == 0) {
                            cookLine = cookLine + "\"Cook " + cook + "C labels\"";
                            for (int i = 0; i < map.get(label).size(); i++) {
                                emptyLine = emptyLine + ",";
                                cookLine = cookLine + "," + map.get(label).get(i).getHeader();
                                tableHeaders.add(map.get(label).get(i).getHeader());
                                log.info("Adding header " + map.get(label).get(i).getHeader());
                                tableMap.put(map.get(label).get(i).getHeader(), new ArrayList<String>());
                                colLength.add(14);
                                if (i != (map.get(label).size() - 1)) {
                                    String header = map.get(label).get(i).getHeader().split("\\(")[0].trim()
                                            .split(" ")[1]
                                            .trim();
                                    if (storeHeaders) {
                                        cookSteps.add(header + " == " + map.get(label).get(i).getHeader());
                                    }
                                    tableHeaders2.add(header);
                                    tableMap2.put(header, new ArrayList<>());
                                    colLength2.add(10);
                                } else {
                                    String header = map.get(label).get(i).getHeader();
                                    tableHeaders2.add(header);
                                    tableMap2.put(header, new ArrayList<>());
                                    colLength2.add(10);
                                }
                            }
                        }
                        Label Obj = new Label(label);
                        labels.add(Obj);
                    }
                    log.info("First row of table: \n" + cookLine);
                    csvContentAll.add(cookLine);
                    csvContentAvg.add(cookLine);
                    Collections.sort(labels);
                    int rows = 0;
                    for (Label l : labels) {
                        log.info("For label: " + l.value + " and cook: " + cook);
                        rows++;
                        String label = l.value;
                        tableMap.get("Cook " + cook + "C labels").add(label);
                        tableMap2.get(cook + "C labels").add(label);
                        String lineAll = "" + label;
                        String lineAvg = "" + label;
                        for (StepData stepData : map.get(label)) {
                            lineAll = lineAll + ",\"" + stepData.listToString() + "\"";
                            lineAvg = lineAvg + "," + stepData.getAvgData();
                            tableMap.get(stepData.getHeader()).add(stepData.getAvgData());
                            log.info("Adding " + stepData.getHeader() + " ::: avgValue: \"" + stepData.getAvgData()
                                    + "\" ::: real value: \"" + stepData.listToString() + "\"");
                            if (stepData.getHeader().equals("Total")) {
                                tableMap2.get(stepData.getHeader()).add(stepData.getAvgData());
                            } else {
                                tableMap2.get(stepData.getHeader().split("\\(")[0].trim().split(" ")[1].trim())
                                        .add(stepData.getAvgData());
                            }
                        }
                        csvContentAll.add(lineAll);
                        csvContentAll.add(lineAvg);
                        csvContentAvg.add(lineAvg);
                    }
                    csvContentAll.add(emptyLine);
                    csvContentAvg.add(emptyLine);
                    // log.info("Data to be processed into table: \nHeaders:" + tableHeaders +
                    // "\nMap:" + tableMap + "\nLengths" + colLength + "\nRows:" + rows);
                    tables.add(printTable(tableHeaders, tableMap, colLength, rows));
                    // Remove "INDEX" that prinTable adds to the front of the list
                    tableHeaders.remove(0);
                    htmlTables.add(printHTMLTable(tableHeaders, tableMap, colLength, rows));
                    tableData.addAll(tableToTableData(tableHeaders, tableMap, type, rows));
                    tablesForBody.add(printTable(tableHeaders2, tableMap2, colLength2, rows));
                    // Remove "INDEX" that prinTable adds to the front of the list
                    tableHeaders2.remove(0);
                    htmlTablesForBody.add(printHTMLTable(tableHeaders2, tableMap2, colLength2, rows));
                }
                body = body + "Report Generated and attached for cooks: " + cooks + " for " + type.toUpperCase()
                        + " run \n\n\n";
                htmlBody = htmlBody + "<br>Report Generated and attached for cooks: " + cooks + " for <b>"
                        + type.toUpperCase()
                        + "</b> run\n<br>\n<br>\n<br>\n";
                for (int i = 0; i < tables.size(); i++) {
                    body = body + "\n" + tablesForBody.get(i) + "\n";
                    htmlBody = htmlBody + "<br>\n" + htmlTablesForBody.get(i) + "\n<br>\n";
                    table = table + "\n" + tables.get(i);
                }
                if (steps.size() == 0) {
                    steps.addAll(cookSteps);
                } else {
                    if (steps.size() != cookSteps.size()) {
                        sendErrorEmail("Different Cook Steps observed for farm and ifarm runs\nsteps:\n"
                                + print(steps, false) + "\ntype: " + type + " cookSteps:\n" + print(cookSteps, false));
                    } else {
                        for (int k = 0; k < steps.size(); k++) {
                            if (!steps.get(k).equals(cookSteps.get(k))) {
                                sendErrorEmail("Different Cook Steps observed for farm and ifarm runs\nsteps:\n"
                                        + print(steps, false) + "\ntype: " + type + " cookSteps:\n"
                                        + print(cookSteps, false));
                            }
                        }
                    }
                }
            }
            body = body + "\n\nAll timings reported are in [HH:MM:SS]\n\n";
            htmlBody = htmlBody + "<br>\n<br>All timings reported are in [HH:MM:SS]\n<br>\n";
            body = body + print(steps, false);
            htmlBody = htmlBody + printHtml(steps, false);
            body = body + "\n\nVisit " + dbGetURL + " to view all the data in the database\n\n";
            htmlBody = htmlBody + "\n<br>\n<br> Visit " + dbGetURL + " to view all the data in the database<br>\n<br>";
            // throw new Exception("Stopper for more debugging");
            // System.exit(0);
            String local = "/Users/atatsinh/Documents/test_code/dev/cook_data/tool/test/test2_extraction/";
            local = "/scratch/atatsinh/work/projects/cook_data_collector/files/";
            local = workingDirectory;
            List<Attachement_File> attachement_Files = new ArrayList<>();
            // attachement_Files.add(fileWriter(local + "allData.csv", csvContentAll));
            fileWriter(local + "allData.csv", csvContentAll);
            // attachement_Files.add(fileWriter(local + "avgData.csv", csvContentAvg));
            fileWriter(local + "avgData.csv", csvContentAvg);
            try {
                executeCommandOnce("zip -r " + local + "files.zip " + workingDirectory + "*.log", false);
                // attachement_Files.add(new Attachement_File(workingDirectory + "files.zip"));
            } catch (Exception e) {
                log.warning("failed to zip up the files for sending as attachment", e);
            }
            fileWriter(local + "finalTable.txt", Arrays.asList(table.split("\n")));
            // attachement_Files.add(fileWriter(local + "finalTable.txt",
            // Arrays.asList(table.split("\n"))));
            for (int i = 0; i < 2; i++) {
                try {
                    // adbs-install-qa_ww_grp@oracle.com
                    send_mail(resultsRecepientEmail,
                            senderEmail,
                            "Cook Time Report",
                            body, htmlBody, attachement_Files);
                    break;
                } catch (Exception e) {
                    String message = "Failed sending email";
                    if (i == 0) {
                        message = message + ", retrying....";
                        log.severe(message, e);
                    }
                    log.severe("Failed sending email", e);
                }
            }
            for (int i = 0; i < 2; i++) {
                try {
                    log.info("Sending json payload \n" + printTableDataJson(tableData) + "\n to " + dbUpdateURL);
                    JsonPostRequestHandler jsonPostRequestHandler = new JsonPostRequestHandler();
                    HttpResponseBundle httpResponseBundle = jsonPostRequestHandler.send(printTableDataJson(tableData),
                            dbUpdateURL);
                    int responseCode = httpResponseBundle.getResponseCode();
                    log.info("Response code: " + responseCode);
                    log.info("Response: " + httpResponseBundle.toString());
                    if (responseCode != 200) {
                        throw new Exception("Invalid response code received: " + httpResponseBundle.toString());
                    } else {
                        log.info("Response received is 200!!");
                        if (httpResponseBundle.getServerResponse().length() != 0) {
                            log.warning("Invalid db response!!", new Exception(httpResponseBundle.getServerResponse()));
                            sendErrorEmail("Invalid db response received",
                                    new Exception(httpResponseBundle.getServerResponse()));
                            if (i != 1) {
                                log.warning("Retrying DB insertions");
                            } else {
                                log.severe("Failed db data insertions, no more retries");
                            }
                        } else {
                            log.info("DB insertion went smoothly");
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.severe("Correct response code not received", e);
                    sendErrorEmail("Correct response code not received", e);
                }
            }
        } catch (Exception e) {
            log.severe("Error occurred while generating data", e);
            sendErrorEmail("Error occurred while generating data", e);
        }
    }

    public static HashMap<Integer, HashMap<String, List<StepData>>> generateDataMap(String workingDirectory,
            String pattern) throws Exception {
        log.info("Starting to retrieve data from the log files with related pattern: " + pattern);
        // data: {cook}: {label}: {column data}
        HashMap<Integer, HashMap<String, List<StepData>>> dataMap = new HashMap<>();
        // get the list of files
        String cmd = "ls " + workingDirectory + pattern;
        List<String> files = executeCommandOnce(cmd, false);
        log.info("Found files: \n" + print(files, false));
        // iterate through all files to get it's data

        for (String filePath : files) {
            log.info("For file: " + filePath);
            String[] temp = filePath.split("/");
            String fileName = temp[temp.length - 1];
            String[] info = fileName.split("_");
            String label = info[0];
            int cook = Integer.valueOf(info[1]);
            int itr = Integer.valueOf(info[2]);
            log.info("label: " + label + " cook: " + cook + " itr: " + itr);
            boolean create = false;
            dataMap.putIfAbsent(cook, new HashMap<String, List<StepData>>());
            dataMap.get(cook).putIfAbsent(label, new ArrayList<>());
            List<StepData> stepList = dataMap.get(cook).get(label);
            if (stepList.size() == 0)
                create = true;
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            boolean data_started = false;
            int i = 12;
            int cook_step_count = 0;
            log.info("String to read the file: " + fileName);
            while (i < lines.size()) {
                String line = lines.get(i).trim();
                if (data_started) {
                    if (line.startsWith("Running Cook Step:")) {
                        while (i < lines.size()) {
                            line = lines.get(i).trim();
                            if (line.startsWith("Running Cook Step:")) {
                                StepData stepData;
                                int endIndex = line.length();
                                endIndex = endIndex - 3;
                                String stepHeader = line.substring(7, endIndex).trim();
                                log.info("for file: " + fileName + " ::: line: " + line + " ::: Header: "
                                        + stepHeader);
                                if (create) {
                                    stepList.add(new StepData(label, cook, stepHeader));
                                    stepData = stepList.get(stepList.size() - 1);
                                } else {
                                    stepData = stepList.get(cook_step_count);
                                }
                                cook_step_count++;
                                int j = i + 1;
                                while (j < lines.size()) {
                                    String line2 = lines.get(j).trim();
                                    if (line2.startsWith("Running Cook Step:")
                                            || line2.startsWith(">>> [Total time for cook deployment in")) {
                                        break;
                                    }
                                    j++;
                                }
                                i = j;
                                log.info("DataLine: " + lines.get(j - 1));
                                int seconds = Integer
                                        .valueOf(lines.get(j - 1).split("in")[1].trim().split("Seconds")[0].trim());
                                log.info("data: " + seconds);
                                log.info("Cook: " + cook + " ::: label: " + label + " ::: itr: " + itr
                                        + " ::: Time taken for: " + stepHeader + " is " + seconds);
                                stepData.addData(seconds, itr);
                            } else if (line.startsWith(">>> [Total time for cook deployment in HH:MM:SS]")) {
                                StepData stepData;
                                if (create) {
                                    stepList.add(new StepData(label, cook_step_count, "Total"));
                                    stepData = stepList.get(stepList.size() - 1);
                                } else {
                                    stepData = stepList.get(cook_step_count);
                                }
                                String[] arr = line.split("]:")[1].trim().split(":");
                                int seconds = Integer.valueOf(arr[0]) * 3600 + Integer.valueOf(arr[1]) * 60
                                        + Integer.valueOf(arr[2]);
                                stepData.addData(seconds, itr);
                                log.info("Cook: " + cook + " ::: label: " + label + " ::: itr: " + itr
                                        + " ::: Time taken for: Total run" + " is [HH:MM:SS]" + line.split("]:")[1]
                                        + " or in seconds: " + seconds);
                                i = lines.size();
                            }
                        }
                    }
                } else if (line.startsWith("Running Pre-Cook Steps")) {
                    data_started = true;
                }
                i++;
            }
        }
        return dataMap;
    }

    public static List<TableData> tableToTableData(List<String> headers, HashMap<String, List<String>> data,
            String type,
            int rows) {
        // @formatter:off
          /**
           * Headers:[Cook 23C labels, Cook Step:1 (Clone database software), Cook Step:2 (Create oracle home zip), Cook Step:3 (Creating CDB), Cook Step:4 (Creating PDB), Cook Step:5 (Creating clone template), Total]
          Map:{
              Cook 23C labels=[Cook 23C labels, 240628, 240630, 240701, 240702, 240703], 
              Cook Step:1 (Clone database software)=[00:00:29, 00:00:29, 00:00:30, 00:00:26, 00:00:30], 
              Cook Step:2 (Create oracle home zip)=[00:04:20, 00:04:17, 00:04:30, 00:04:17, 00:04:25]
              Cook Step:3 (Creating CDB)=[00:49:58, 00:25:02, 01:04:48, 00:26:00, 01:00:23], 
              Cook Step:4 (Creating PDB)=[00:57:56, 00:53:05, 01:01:02, 00:54:34, 01:02:27], 
              Cook Step:5 (Creating clone t00:11:17, 00:10:32, 00:11:40, 00:10:53, 00:11:06], 
              Total=[Total, 02:22:05, 01:46:38, 02:37:12, 01:49:47, 02:33:13], 
          }
          Lengths[7, 17, 14, 14, 14, 14, 14, 14]
          Rows:6"
           */
          // @formatter:on
        List<TableData> list = new ArrayList<>();
        int i = 0;
        log.info("Data to be processed: \nheaders: " + headers + "\ndata: \n" + data + "\ntype: " + type + "\nrows: "
                + rows);
        while (i < rows) {
            Map<String, String> rowData = new HashMap<>();
            int db_version = Integer.valueOf(headers.get(0).split("k")[1].trim().split("C")[0].trim());
            String label = data.get(headers.get(0)).get(i);
            for (int j = 1; j < headers.size(); j++) {
                rowData.put(headers.get(j), data.get(headers.get(j)).get(i));
            }
            boolean ifarm = type.equals("ifarm") ? true : false;
            TableData tableData = new TableData(rowData, db_version, ifarm, label);
            list.add(tableData);
            i++;
        }
        return list;
    }

    public static Attachement_File fileWriter(String filePath, List<String> content) throws IOException {
        Files.write(Paths.get(filePath), content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        Attachement_File file = new Attachement_File(filePath);
        return file;
    }

    public static String print(List<String> list, boolean print) {
        String lines = "";
        for (String line : list) {
            lines = lines + line + "\n";
            if (print)
                System.out.println(line);
        }
        return lines;
    }

    public static String printTableDataJson(List<TableData> list) {
        String out = "[";
        for (int i = 0; i < list.size(); i++) {
            out = out + list.get(i).toString();
            if (i != (list.size() - 1)) {
                out = out + ",";
            }
        }
        out = out + "]";
        return out;
    }

    public static String printHtml(List<String> list, boolean print) {
        String lines = "";
        for (String line : list) {
            lines = lines + "<br>" + line + "\n";
            if (print)
                System.out.println(line);
        }
        lines = lines + "<br>\n";
        return lines;
    }

    public static List<String> farm_emulator(List<String> list) {
        List<String> output = new ArrayList<String>();
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            int number = 100000 + random.nextInt(900000);
            output.add("Farm Command Line - 2.9\n");
            output.add("Command used: ADB_DEP_REGRESS -sregress\n");
            output.add(
                    "INFO: This job (Label: PDBCS_MAIN_LINUX.X64_240131;Platform: LINUX.X64; Run_Type: DEV) will be sent to Farm Zone: PHX\n");
            output.add("INFO: Resetting Job Zone: NONE to Site Mapping: PHX\n");
            output.add("Check label argument passed: PDBCS_MAIN_LINUX.X64_240131\n");
            output.add(
                    "Note: -db create will run by default, as FARM_MANDATE_DB_CREATE=1 env is set in your product\n");
            output.add("INFO: Executing make -f /ade/atatsinh_pv7/pdbcs/Makefile print_ALL_DEV_LRGS\n");
            output.add("Warning: lrgopsprvdepsimrac_atp_21_v was not found in Makefile\n");
            output.add(
                    "LRG list: lrgopsprvdep3c3p_atp_19_v,lrgopsprvdep3c3p_atp_21_v,lrgopsprvdepbkrstr_atp_19_v,lrgopsprvdepracdeploy_adw_19_rr,lrgopsprvdepracdeploy_adw_21_rr,lrgopsprvdeprdbms_atp_19_v,lrgopsprvdeprdbms_atp_21_v,lrgopsprvdeprdbmsmain_atp_21_v,lrgopsprvdepsimrac_atp_19_v.\n");
            output.add("Logging in ... done\n");
            output.add("Check label argument passed: PDBCS_MAIN_LINUX.X64_240131\n");
            output.add("Farm Load Balancer:Determined_Jobzone:PHX\n");

            output.add("Submitting ... \n");
            output.add("Metadata transfer from view to farm filers is going on.\n");
            output.add("You will get the transfer progress from /tmp/PDBCS_MAIN_LINUX.X64_T" + String.valueOf(number)
                    + ".log file\n");
            output.add("You will get the detailed logs from /tmp/PDBCS_MAIN_LINUX.X64_T" + String.valueOf(number)
                    + ".out file\n");
            output.add("Do not destroy your view or reboot your host\n");
            output.add("\n");
            output.add("Your submission ID is PDBCS_MAIN_LINUX.X64_T" + String.valueOf(number) + ". Job # is "
                    + String.valueOf(number) + "\n");
        }
        return output;
    }

    /**
     * This function executes the command once.
     * 
     * @param command   command line
     * @param grep      boolean if you wish to grep a pattern from the output
     * @param pattern   pattern to grep
     * @param multiples define if all occurences of pattern to return or just first
     *                  occurence.
     * @param waitTime  milliseconds after which you wish to terminate the command
     * @param echo      true if you wish to print related outputs on console
     * @return output as list of strings, indexed line-wise
     * @throws Exception
     */
    public static List<String> executeCommandOnce(String command, boolean grep, String pattern, boolean multiples,
            long waitTime, boolean echo) throws Exception {
        Process p;
        List<String> result = new ArrayList<String>();
        log.info("command: " + command + " ::: grep: " + grep + " multiple: " + multiples);
        if (echo) {
            System.out.print("command: " + command);
            if (grep) {
                System.out.print(" | grep '" + pattern + "'\nSearching for:");
                if (!multiples)
                    System.out.print(" only first match");
                else
                    System.out.print(" all matches");
            }
            System.out.print("\n");
        }
        try {
            if (grep) {
                p = Runtime.getRuntime().exec(command);
                String line = "";
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                pattern = pattern.toLowerCase();
                while ((line = reader.readLine()) != null) {
                    String[] arr = line.toLowerCase().split(pattern);
                    if ((arr.length == 1) && (arr[0].length() == line.length()))
                        continue;
                    else {
                        result.add(line);
                        if (!multiples) {
                            p.destroyForcibly();
                            break;
                        }
                    }
                }
                if (multiples)
                    p.waitFor();
            } else {
                p = Runtime.getRuntime().exec(command);
                synchronized (p) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    result = reader.lines().map(s -> s.trim()).collect(Collectors.toList());
                    if (waitTime > 0)
                        p.wait(waitTime);
                    else
                        p.waitFor();
                }
            }
            log.info("command processed: " + print(result, false));
        } catch (Exception exception) {
            log.severe("Error occurred while processing " + command, exception);
            throw exception;
        }
        return result;
    }

    /**
     * This function executes multiple commands.
     * 
     * @param commands        List of commands to be executed
     * @param file_pathString file path of the file where output will be redirected
     * @return output
     */
    public static List<String> executeCommands(List<String> commands, String file_pathString) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("/bin/bash");
        List<String> output = new ArrayList<String>();
        log.info("Received commands to process: " + print(commands, false));
        try {
            List<String> pipeReader = new ArrayList<>();
            // close outputstream for process if builder inherits IO
            // builder.inheritIO();
            builder.redirectErrorStream(true);
            if (file_pathString.length() > 0) {
                File file = new File(file_pathString);
                builder.redirectOutput(ProcessBuilder.Redirect.appendTo(file));
            } else {
                builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
            }
            Process process = builder.start();
            synchronized (process) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                Thread outputThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                            pipeReader.add(line);
                        }
                    } catch (Exception e) {
                        log.severe("error occrred while trying to retreive response", e);
                    }
                });
                if (file_pathString.length() == 0) {
                    outputThread.start();
                }
                for (String command : commands) {
                    try {
                        writer.write("echo");
                        writer.newLine();
                        writer.write("echo");
                        writer.newLine();
                        writer.write("echo \">>" + command + "\"");
                        writer.newLine();
                        writer.write("echo");
                        writer.newLine();
                        writer.write(command);
                        writer.newLine();
                        writer.flush();

                    } catch (IOException e) {
                        log.severe("Error occurred while executing cmd: " + command, e);
                        // e.printStackTrace();
                    }
                }
                // it is neccessary to exit, otherwise it will get stuck as no other commands
                // are
                // given, hence infinite process, terminates on ctrl+c
                // try {
                // writer.write("exit");
                // writer.newLine();
                // writer.flush();
                // } catch (IOException exception) {
                // exception.printStackTrace();
                // }
                writer.close();
                if (file_pathString.length() == 0) {
                    outputThread.join();
                    reader.close();
                    output.addAll(pipeReader);
                } else {
                    output = Files.readAllLines(Paths.get(file_pathString));
                }
                process.waitFor();
                // output = reader.lines().map(s -> s.trim()).collect(Collectors.toList());
            }
        } catch (Exception exception) {
            log.severe("Failed processing commands", exception);
            throw exception;
        }
        log.info("Commands processed: ");
        if (file_pathString.length() > 0) {
            log.info("Output written to file: " + file_pathString);
        } else {
            log.info("Output: \n" + print(output, false));
        }
        return output;
    }

    /**
     * This function executes command normally, with no restrictions of characters
     * 
     * @param cmd command string
     * @return output of command as list of lines
     * @throws Exception
     */
    public static List<String> executeCommandOnce(String cmd, boolean echo) throws Exception {
        List<String> output = new ArrayList<>();
        Process p;
        log.info("command received: " + cmd);
        if (echo)
            System.out.println("command: " + cmd + "\n\n");
        try {
            // exec parser doesn't understand shell grammer, hence open
            // a shell command using /bin/sh -c and then try commands
            // breaking commands into string array is better, again for the above reasons
            String[] command = { "/bin/sh", "-c", cmd };
            // multiple commands can be executed using &&, &, || symbols.
            p = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            output = reader.lines().map(line -> line.trim()).collect(Collectors.toList());
            p.waitFor();
            // if ((Boolean) arguments.get("progress"))
            // System.out.println("process completed\noutput ready with lines: " +
            // output.size());
        } catch (Exception e) {
            log.severe("Error occurred while executing cmd", e);
            throw e;
        }
        log.info(print(output, echo));
        return output;
    }

    /**
     * This function helps sending email. Internally, it uses /usr/sbin/sendmail
     * binary
     * 
     * @param Recipient    email address of the receiver
     * @param Sender       email address of the sender
     * @param Subject      subject of the email
     * @param body_message Body content
     * @param attachements attachements if any
     * @throws Exception in case system execution of /usr/bin/sendmail fails
     */
    public static void send_mail(String Recipient, String Sender, String Subject, String text_body_message,
            String html_body_message,
            List<Attachement_File> attachements) throws Exception {
        String boundary = "===============BOUNDARY============";
        String email_headers = "To: " + Recipient + "\n";
        email_headers = email_headers + "From: " + Sender + "\n";
        email_headers = email_headers + "Subject: " + Subject + "\n";
        email_headers = email_headers + "MIME-Version: 1.0" + "\n";
        email_headers = email_headers + "Content-Type: multipart/mixed; boundary=\"" + boundary + "\"";
        String text_body = "";
        if (text_body_message.length() > 0) {
            // text_body = text_body + "--" + boundary + "\n";
            text_body = text_body + "Content-Type: text/plain; charset=\"UTF-8\"\n";
            text_body = text_body + "Content-Disposition: inline\n\n";
            text_body = text_body + text_body_message + "\n";
        }
        String html_body = "";
        if (html_body_message.length() > 0) {
            html_body = "Content-Type: text/html; charset=\"UTF-8\"\n\n";
            html_body = html_body + "<html>\n<body>\n";
            html_body = html_body + html_body_message + "\n</body>\n</html>\n";
        }
        String email_body = "";
        if ((text_body.length() > 0) ^ (html_body.length() > 0)) {
            String tmpBody = text_body.length() > 0 ? text_body : html_body;
            email_body = email_body + "--" + boundary + "\n" + tmpBody;
        } else if ((text_body.length() > 0) && (html_body.length() > 0)) {
            email_body = email_body + "--" + boundary + "\n";
            String altBoundary = "===========alt-boundary=======";
            email_body = email_body + "Content-Type: multipart/alternative; boundary=\"" + altBoundary + "\"\n\n";
            email_body = email_body + "--" + altBoundary + "\n";
            email_body = email_body + text_body + "\n" + "--" + altBoundary + "\n";
            email_body = email_body + html_body + "\n" + "--" + altBoundary + "--\n";
        }
        String attachements_body = "";
        if (attachements.size() > 0) {
            for (Attachement_File file : attachements) {
                attachements_body = attachements_body + "--" + boundary + "\n";
                attachements_body = attachements_body + "Content-Type: " + file.getContent_type() + "; name\""
                        + file.getName() + "\"\n";
                attachements_body = attachements_body + "Content-Disposition: attachment; filename=\"" + file.getName()
                        + "\"\n";
                attachements_body = attachements_body + "Content-Transfer-Encoding: base64\n\n";
                attachements_body = attachements_body + "$(base64 \"" + file.getFilepath() + "\")\n\n";
            }
        }
        String email = email_headers + "\n\n" + email_body + "\n\n" + attachements_body + "\n\n--" + boundary + "--";
        String cmd = "echo \"" + email + "\" | /usr/sbin/sendmail -t";
        log.info("sending email: \n" + email);
        // System.out.println(email + "\n\noutput:");
        try {
            print(executeCommandOnce(cmd, false), true);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Prints table, similar to sql manner. Please make sure not to include "index"
     * as a header and rows is the total number of rows that will be in the table
     * excluding the headers. It is text overflow handled, meaning if you wish to
     * control the length of a column and want the extra text not fitting in the
     * column
     * to appear in next line, it can be done by entering proper column length
     * values.
     * 
     * @param headers   headings of each column, excluding index
     * @param data      HashMap of heading: data key value pair, data being a list
     *                  representing values for every row.
     * @param colLength List of integers representing the maximum length of a
     *                  column.
     *                  This can be determined by max length of all the values for
     *                  that column + 2:
     *                  for space gaps from dividing line
     * @param rows      total number of rows in the table, exluding headers of
     *                  course
     * 
     * @return the output table string
     */
    public static String printTable(List<String> headers, HashMap<String, List<String>> data_org,
            List<Integer> colLength,
            int rows) {
        String line = "+";
        String output = "";
        HashMap<String, List<String>> data = new HashMap<>();
        // Create deep copy
        for (Map.Entry<String, List<String>> entry : data_org.entrySet()) {
            data.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        colLength.add(0, (Math.max(5, (String.valueOf(rows).length())) + 2));
        for (int col : colLength) {
            line = line + String.join("", Collections.nCopies(col, "-")) + "+";
        }
        String t = "|";
        rows++;
        for (int i = 0; i < headers.size(); i++) {
            data.get(headers.get(i)).add(0, headers.get(i));
        }
        headers.add(0, "INDEX");
        // System.out.println(t + "\n" + line);
        // output = output + t + "\n" + line + "\n";
        int i = 1;
        log.info("Data to be processed into table: \nHeaders:" + headers + "\nMap:" + data + "\nLengths" + colLength
                + "\nRows:" + rows);
        // System.out.println(line);
        output = output + line + "\n";
        boolean row_overflow = false;
        while (i <= rows) {
            t = "|";
            String var = String.valueOf(i);
            if (i == 1) {
                var = "Sr.No";
            } else {
                var = String.valueOf(i - 1);
            }
            if (row_overflow)
                var = "";
            int n = colLength.get(0) - var.length() - 1, j = 1;
            t = t + String.join("", Collections.nCopies(n, " ")) + var + " |";
            boolean local_row_overflow = false;
            for (String title : headers) {
                if (title.toLowerCase().equals("index"))
                    continue;
                var = data.get(title).get(i - 1);
                n = colLength.get(j) - var.length() - 1;
                if (n <= 0) {
                    n = colLength.get(j) - 2;
                    data.get(title).set(i - 1, var.substring(n, var.length()));
                    var = var.substring(0, n);
                    n = 1;
                    row_overflow = true;
                    local_row_overflow = true;
                } else {
                    data.get(title).set(i - 1, "");
                }
                t = t + " " + var + String.join("", Collections.nCopies(n, " ")) + "|";
                j++;
            }
            row_overflow = local_row_overflow;
            // System.out.println(t);
            output = output + t + "\n";
            if (!row_overflow) {
                if (i == 1) {
                    output = output + line + "\n";
                    // System.out.println(line);
                }
                i++;
            }
        }
        // System.out.println(line);
        output = output + line + "\n";
        log.info("Table generated: \n" + output);
        return output;
    }

    public static String printHTMLTable(List<String> headers, HashMap<String, List<String>> data,
            List<Integer> colLength, int rows) {
        String table = "<table cellpadding='4' style='border: 1px solid #000000; border-collapse: collapse;' border='1'>\n";
        headers.add(0, "Sr.No");
        log.info("Data to be processed into table: \nHeaders:" + headers + "\nMap:" + data + "\nLengths" + colLength
                + "\nRows:" + rows);
        table = table + "<tr>\n";
        for (String header : headers) {
            table = table + "<th bgcolor='#abebc6'>" + header + "</th>\n";
        }
        headers.remove(0);
        table = table + "</tr>\n";
        for (int i = 1; i <= rows; i++) {
            table = table + "<tr>\n";
            table = table + "<td align='right'>" + i + "</td>\n";
            for (String header : headers) {
                table = table + "<td align='right'>" + data.get(header).get(i - 1) + "</td>\n";
            }
            table = table + "</tr>\n";
        }
        table = table + "</table>\n";
        log.info("Table generated:\n" + table);
        return table;
    }

}

class Attachement_File {
    private String name;
    private String filepath;
    private String content_type;

    public Attachement_File() {

    }

    public Attachement_File(String name, String filepath, String content_type) {
        setName(name);
        setFilepath(filepath);
        setContent_type(content_type);
    }

    public Attachement_File(String filepath) {
        setFilepath(filepath);
        String[] arr = filepath.split("/");
        this.name = arr[arr.length - 1];
        String[] arr2 = name.split("\\.");
        String format = arr2[arr2.length - 1].toLowerCase();
        // System.out.println(Arrays.asList(arr) + "\n\n" + name + "\n\n" +
        // Arrays.asList(arr2));
        if (format.equals("zip")) {
            setContent_type("application/zip");
        } else if (format.equals("pdf")) {
            setContent_type("application/pdf");
        } else if (format.equals("csv")) {
            setContent_type("text/csv");
        } else if (format.equals("txt")) {
            setContent_type("text/plain");
        } else {
            setContent_type("application/octet-stream");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getContent_type() {
        return content_type;
    }

    public void setContent_type(String content_type) {
        this.content_type = content_type;
    }

    @Override
    public String toString() {
        return "Attachement_File [name=" + name + ", filepath=" + filepath + ", content_type=" + content_type + "]";
    }
}

class StepData implements Comparable<StepData> {
    private String label;
    private String header;
    private List<Data> data;
    private int dataSize;
    private String avgData;
    private int cook;

    class Data implements Comparable<Data> {
        public Integer itr;
        public int data;

        public Data() {

        }

        public Data(int itr, int data) {
            this.itr = itr;
            this.data = data;
        }

        @Override
        public int compareTo(StepData.Data o) {
            return itr.compareTo(o.itr);
        }

        @Override
        public String toString() {
            return "Data [itr=" + itr + ", data=" + data + "]";
        }

    }

    public StepData() {

    }

    public StepData(String lable, int cook, String header) {
        setLabel(lable);
        data = new ArrayList<>();
        setCook(cook);
        setHeader(header);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Data> getData() {
        return data;
    }

    public void setData(List<Data> data) {
        this.data = data;
        setDataSize(this.data.size());
    }

    public int getDataSize() {
        return dataSize;
    }

    public void setDataSize(int dataSize) {
        this.dataSize = dataSize;
    }

    public String getAvgData() {
        return avgData;
    }

    public void setAvgData(String avgData) {
        this.avgData = avgData;
    }

    public void computeAvg() {
        int total = 0;
        for (Data d : data) {
            int seconds = d.data;
            total = total + seconds;
        }
        total = (int) Math.round((double) (total / this.dataSize));
        setAvgData(convertSecondsToHHMMSS(total));
        Collections.sort(data);
    }

    public String convertSecondsToHHMMSS(int seconds) {
        return String.format("%02d", seconds / 3600) + ":" + String.format("%02d", (seconds / 60) % 60) + ":"
                + String.format("%02d", seconds % 60);
    }

    public String listToString() {
        String output = "";
        int i = 0;
        Collections.sort(data);
        for (Data d : this.data) {
            int seconds = d.data;
            if (i != 0) {
                output = output + ", ";
            }
            output = output + convertSecondsToHHMMSS(seconds);
            i++;
        }
        return output;
    }

    public void addData(int seconds, int itr) {
        Data d = new Data(itr, seconds);
        this.data.add(d);
        setDataSize(this.data.size());
        computeAvg();
    }

    public int getCook() {
        return cook;
    }

    public void setCook(int cook) {
        this.cook = cook;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return "StepData [label=" + label + ", header=" + header + ", data=" + data + ", dataSize=" + dataSize
                + ", avgData=" + avgData + ", cook=" + cook + "]";
    }

    @Override
    public int compareTo(StepData o) {
        if (getHeader().equals("Total")) {
            return 1;
        }
        return stepNumberFromHeader(getHeader()).compareTo(stepNumberFromHeader(o.getHeader()));
    }

    public Integer stepNumberFromHeader(String header) {
        return Integer.valueOf(header.split(":")[1].split(" ")[0]);
    }

}

class Label implements Comparable<Label> {
    public String value;
    public LocalDate date;
    public Integer decimal;

    public Label(String value) throws InvalidLabelIDException {
        this.value = value;
        String[] arr = value.split("\\.");
        String[] d1 = arr[0].split("(?<=\\G..)");
        this.date = LocalDate.of(Integer.parseInt("20" + d1[0]), Integer.parseInt(d1[1]), Integer.parseInt(d1[2]));
        if (arr.length > 1) {
            try {
                this.decimal = Integer.valueOf(arr[1]);
            } catch (NumberFormatException numberFormatExceptionumberFormatException) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                numberFormatExceptionumberFormatException.printStackTrace(pw);
                String sStackTrace = sw.toString();
                throw new InvalidLabelIDException(value + "\n"
                        + numberFormatExceptionumberFormatException.getLocalizedMessage() + "\n" + sStackTrace);
            }
        } else {
            this.decimal = 0;
        }
    }

    @Override
    public int compareTo(Label o) {
        int compare = date.compareTo(o.date);
        if (compare == 0) {
            return decimal.compareTo(o.decimal);
        }
        return compare;
    }
}

class ADEOrUseviewException extends Exception {
    public ADEOrUseviewException(String msg) {
        super(msg);
    }

    public ADEOrUseviewException() {
        super("Either ADE commands are failing or there is some other issue while entering the view");
    }
}

class FarmException extends Exception {
    public FarmException(String msg) {
        super(msg);
    }

    public FarmException() {
        super("Error occurred while handling farm commands");
    }

}

class MaxAttemptsOrTimeoutReachedException extends Exception {
    public MaxAttemptsOrTimeoutReachedException(String msg) {
        super(msg);
    }

    public MaxAttemptsOrTimeoutReachedException() {
        super("Max attempts or timeout reached");
    }
}

class InvalidLabelIDException extends Exception {
    public InvalidLabelIDException(String msg) {
        super("Invalid label id: " + msg);
    }

    public InvalidLabelIDException() {
        super("Invalid label id");
    }
}

class XMLConfigReader {

    public Map<String, String> read(String XMLConfigFilePath) throws Exception {
        Map<String, String> map = new HashMap<>();
        // Initialize DocumentBuilderFactory and DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Parse the XML file
        XMLConfigFilePath = "/Users/atatsinh/Documents/test_code/dev/cook_reporter/config.xml";
        Document document = builder.parse(XMLConfigFilePath);

        // Normalize the XML structure
        document.getDocumentElement().normalize();

        // Get the root element
        Element root = document.getDocumentElement();
        System.out.println("Root element: " + root.getNodeName());

        // Read database configuration
        NodeList dbList = document.getElementsByTagName("database");
        Node dbNode = dbList.item(0);
        if (dbNode.getNodeType() == Node.ELEMENT_NODE) {
            Element dbElement = (Element) dbNode;
            String url = dbElement.getElementsByTagName("url").item(0).getTextContent();
            String username = dbElement.getElementsByTagName("username").item(0).getTextContent();
            String password = dbElement.getElementsByTagName("password").item(0).getTextContent();

            System.out.println("Database URL: " + url);
            System.out.println("Username: " + username);
            System.out.println("Password: " + password);
        }

        // Read application configuration
        NodeList appList = document.getElementsByTagName("application");
        Node appNode = appList.item(0);
        if (appNode.getNodeType() == Node.ELEMENT_NODE) {
            Element appElement = (Element) appNode;
            String appName = appElement.getElementsByTagName("name").item(0).getTextContent();
            String appVersion = appElement.getElementsByTagName("version").item(0).getTextContent();

            System.out.println("Application Name: " + appName);
            System.out.println("Application Version: " + appVersion);
        }
        return map;
    }
}

class TableData {
    public String label;
    public Integer db_version;
    public String clone_database_software;
    public String create_oracle_home_zip;
    public String creating_cdb;
    public String creating_pdb;
    public String creating_clone_template;
    public boolean ifarm;
    public String total;

    public TableData() {

    }

    public TableData(Map<String, String> data, int db_version, boolean ifarm, String label) {
        this.label = label;
        this.ifarm = ifarm;
        this.db_version = db_version;
        this.clone_database_software = "0 " + data.get("Cook Step:1 (Clone database software)");
        this.create_oracle_home_zip = "0 " + data.get("Cook Step:2 (Create oracle home zip)");
        this.creating_cdb = "0 " + data.get("Cook Step:3 (Creating CDB)");
        this.creating_pdb = "0 " + data.get("Cook Step:4 (Creating PDB)");
        this.creating_clone_template = "0 " + data.get("Cook Step:5 (Creating clone template)");
        this.total = "0 " + data.get("Total");
    }

    @Override
    public String toString() {
        return "{\"label\":\"" + label + "\", \"db_version\":" + db_version + ", \"clone_database_software\":\""
                + clone_database_software + "\", \"create_oracle_home_zip\":\"" + create_oracle_home_zip
                + "\", \"creating_cdb\":\""
                + creating_cdb + "\", \"creating_pdb\":\"" + creating_pdb + "\", \"creating_clone_template\":\""
                + creating_clone_template + "\", \"ifarm\":" + ifarm + ", \"total\":\"" + total + "\"}";
    }

}

class JsonPostRequestHandler {

    public HttpResponseBundle send(String payload, String urlString) throws IOException {

        // Set the URL for the POST request
        URL url = new URL(urlString);

        // Open a connection to the URL
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        // Set the HTTP method to POST
        connection.setRequestMethod("POST");

        // Set the content-type header to indicate that the payload is JSON
        connection.setRequestProperty("Content-Type", "application/json");

        // Enable output for the connection, so we can write the payload to the server
        connection.setDoOutput(true);

        // Get the output stream from the connection
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);

        // Write the JSON payload to the output stream
        writer.write(payload);
        writer.flush();
        writer.close();

        // Get the response code from the server
        String responseMessage = connection.getResponseMessage();
        int responseCode = connection.getResponseCode();
        // System.out.println("Response code: " + responseCode);

        // Read the response from the server (if any)
        // ...

        // Read the response from the server (successful or error response)
        InputStream inputStream;
        if (responseCode >= 200 && responseCode < 300) {
            // If the response is successful, use the regular input stream
            inputStream = connection.getInputStream();
        } else {
            // If the response is an error, use the error stream
            inputStream = connection.getErrorStream();
        }

        // Convert the InputStream into a String
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line = "";
        while ((line = inputReader.readLine()) != null) {
            response.append(line);
        }
        inputReader.close();
        String serverResponse = response.toString();

        HttpResponseBundle httpResponseBundle = new HttpResponseBundle(responseCode, responseMessage, serverResponse);
        // Disconnect from the server
        connection.disconnect();
        return httpResponseBundle;
    }
}

class HttpResponseBundle {
    private String responseMessage;
    private int responseCode;
    private String serverResponse;

    public HttpResponseBundle() {

    }

    public HttpResponseBundle(int responseCode, String responseMessage, String serverResponse) {
        setResponseCode(responseCode);
        setResponseMessage(responseMessage);
        setServerResponse(serverResponse);
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getServerResponse() {
        return serverResponse;
    }

    public void setServerResponse(String serverResponse) {
        this.serverResponse = serverResponse;
    }

    @Override
    public String toString() {
        return "HttpResponseBundle [responseMessage=" + responseMessage + ", responseCode=" + responseCode
                + ", serverResponse=" + serverResponse + "]";
    }

}
