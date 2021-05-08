import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.exit;

public class Driver {

    public static final boolean CHECK_OUTPUT_MODE = true;      //to check output when complete.
    public static boolean logging;       //set to true if we want to output the results(buffers) to a file.

    static ExecutorService executor;
    static CPU [] cpu;
    static SyncObject syncObject;


    public static void main(String[] args) throws FileNotFoundException {

        Scanner userInput = new Scanner (System.in);
        int iterations;

        ArrayList<MemoryClass> coreDumpArray;
        int numTimingFields = 4;    //4 columns: Job #, Waiting Time, Completion Time, Execution Time
        long [][][] timingArray = null;
        long [][] avgTimingArray;   //just Waiting Time(avg) and Completion Time(avg)
        int numJobs = 0;


        /////////////////////////////////////////////////////////////////////////////////
        //                  Get User Input
        /////////////////////////////////////////////////////////////////////////////////

        System.out.println ("Welcome to the SimpleOS Simulator.\t");
        System.out.print ("Enter Scheduling Policy (1 for FIFO, 2 for Priority:\t");
        int policy = userInput.nextInt();

        System.out.print ("Log output for 1 iteration?(y/n)\t");
        String logChoice = userInput.next();

        if (logChoice.equals("y") || logChoice.equals("Y")) {
            logging = true;
            iterations = 1;
        }
        else {
            logging = false;
            System.out.print("Enter Number of iterations:\t");
            iterations = userInput.nextInt();
        }


        /////////////////////////////////////////////////////////////////////////////////
        //                  Begin Outer Loop
        /////////////////////////////////////////////////////////////////////////////////

        for (int i = 0; i < iterations; i++) {

            java.io.File file = new java.io.File("Program-File.txt");

            try ( //try with resources (auto-closes Scanner)
                Scanner input = new Scanner(file);
            ) {
                Queues.initQueues();
                MemorySystem.initMemSystem();
                Loader loader = new Loader();
                LongScheduler longScheduler = new LongScheduler(policy);
                syncObject = new SyncObject();

                cpu = new CPU[CPU.CPU_COUNT];
                executor = Executors.newFixedThreadPool(CPU.CPU_COUNT);

                for (int j = 0; j < CPU.CPU_COUNT; j++) {
                    cpu[j] = new CPU(j);
                    executor.execute(cpu[j]);
                }

                coreDumpArray = new ArrayList();

                loader.load(input);
                numJobs = Queues.diskQueue.size();

                /////////////////////////////////////////////////////////////////////////////////
                //                        Begin Main Driver Loop
                /////////////////////////////////////////////////////////////////////////////////
                do {
                    longScheduler.schedule();       //load processes into memory from disk.
                    ShortScheduler.schedule();      //pick one job from the ready Queue to run on a CPU

                    if (Queues.readyQueue.size() == 0) {

                        //need to wait for the CPUs to finish running before longScheduler overwrites memory.
                        //and before we do a core dump.
                        while (Queues.freeCpuQueue.size() < CPU.CPU_COUNT) {
                            synchronized (syncObject) {
                                try {
                                    syncObject.wait();
                                }
                                catch (InterruptedException ie) {
                                    System.err.println(ie.toString());
                                }
                            }

                        }

                        if (logging) {
                            //if the readyQueue is empty, then we have processed all the jobs in the ready queue
                            //and we are ready to do a core dump.
                            saveMemoryForCoreDump(coreDumpArray);
                        }
                    }
                }

                while (checkForMoreJobs());


                /////////////////////////////////////////////////////////////////////////////////
                //                          END Main Driver Loop
                /////////////////////////////////////////////////////////////////////////////////

                if (logging) {
                    writeOutputFile ();
                    writeCoreDumpFiles (coreDumpArray);

                    if (CHECK_OUTPUT_MODE)
                        //compare output to gold standard
                        checkOutputIsCorrect();
                }

                //create timing array (if not already created)
                if (timingArray == null) {
                    timingArray = new long [iterations][Queues.doneQueue.size()][numTimingFields];
                }
                saveTimingDataForThisIteration(i, timingArray);


                //shutdown the CPU's.
                for (int k = 0; k < CPU.CPU_COUNT; k++) {
                    try {
                        Queues.cpuActiveQueue[k].put(-1);
                    }
                    catch (InterruptedException ie) {
                        System.err.println(ie.toString());
                    }
                }

                executor.shutdown();
            }
        }

        /////////////////////////////////////////////////////////////////////////////////
        //                  End Outer Loop
        /////////////////////////////////////////////////////////////////////////////////

        // Process and Output Timing Data
        avgTimingArray = new long [numJobs][numTimingFields];
        outputTimingData(avgTimingArray, timingArray, numJobs, iterations);
    }



    public static boolean checkForMoreJobs () {
        //return ((Queues.diskQueue.size() != 0) || (Queues.readyQueue.size() != 0) || (Queues.runningQueue.size() != 0));
        return ((Queues.diskQueue.size() != 0) || (Queues.readyQueue.size() != 0));
    }

    public static void saveMemoryForCoreDump(ArrayList<MemoryClass> coreDumpArray) {
        MemoryClass currCoreDump = new MemoryClass();
        System.arraycopy(MemorySystem.memory.memArray, 0, currCoreDump.memArray, 0, MemorySystem.memory.MEM_SIZE);
        coreDumpArray.add(currCoreDump);
    }


    //debugging method: compares the output to an output file that we know is correct.
    public static boolean checkOutputIsCorrect() throws FileNotFoundException {

        java.io.File goodFile = new java.io.File("CorrectOutput.txt");
        try ( //try with resources (auto-closes Scanner)
            Scanner goodInput = new Scanner(goodFile);
        ) {
            //String goodResults = new Scanner(goodFile).useDelimiter("\\Z").next();

            ArrayList<String> goodResults = new ArrayList<>();

            while (goodInput.hasNext()) {
                StringBuilder temp = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    temp.append(goodInput.nextLine());
                    temp.append("\r\n");
                }
                goodResults.add(temp.toString());
            }

            System.out.println (goodResults.size());

            Queues.doneQueue.sort((PCB o1, PCB o2) -> o1.jobId - o2.jobId);

            for (int i = 0; i < goodResults.size(); i++) {
                boolean isMatch = goodResults.get(i).equals(Queues.doneQueue.get(i).trackingInfo.buffers);
                if (!isMatch) {
                    System.out.println("Warning: output does not match gold standard.");
                    System.out.println(goodResults.get(i));
                    System.out.println(Queues.doneQueue.get(i).trackingInfo.buffers);
                }
            }
            return false;
        }
    }


    // Log Mem Usage, Output and # IO Operations to File
    public static void writeOutputFile() {

        java.io.File outputFile = new java.io.File("output.txt");
        try {
            java.io.PrintWriter output = new java.io.PrintWriter(outputFile);
            String strMemUsage = "max memory usage: " + LongScheduler.maxMemUsed;
            System.out.println(strMemUsage);
            output.println(strMemUsage);

            for (PCB thisPCB : Queues.doneQueue) {
                output.println("Job:" + thisPCB.jobId + "\tNumber of io operations: " + thisPCB.trackingInfo.ioCounter);
            }

            for (PCB thisPCB : Queues.doneQueue) {
                output.print(thisPCB.trackingInfo.buffers);
            }
            output.close();
        }
        catch (FileNotFoundException ex){
            ex.printStackTrace();
        }

    }

    // Log Core Dumps to Files
    public static void writeCoreDumpFiles(ArrayList<MemoryClass> coreDumpArray) {
        try {
            for (int j = 0; j < coreDumpArray.size(); j++) {
                java.io.File coreDumpFile = new java.io.File("coreDump" + (j + 1) + ".txt");
                java.io.PrintWriter coreDump = new java.io.PrintWriter(coreDumpFile);

                String padding = "00000000";
                for (int k = 0; k < MemorySystem.memory.MEM_SIZE; k++) {
                    //coreDump.println(coreDumpArray.get(j).memArray[k]);
                    String unpaddedHex = Integer.toHexString(coreDumpArray.get(j).memArray[k]).toUpperCase();
                    String paddedHex = padding.substring(unpaddedHex.length()) + unpaddedHex;
                    paddedHex = "0x" + paddedHex;
                    coreDump.println(paddedHex);
                }
                coreDump.close();
            }
        }
        catch (FileNotFoundException ex){
            ex.printStackTrace();
        }
    }

    // save Timing Data for current iteration.
    public static void saveTimingDataForThisIteration(int i, long [][][] timingArray) {
        int j=0; //j = job counter
        for (PCB thisPCB: Queues.doneQueue) {
            //i=iteration counter
            //waitStartTime - time entered Ready Queue (set by Long Term Scheduler)
            //runStartTime  - time first started executing (entered Running Queue, set by Dispatcher)
            //runEndTime    - Completion Time = runEndTime - waitStartTime?
            timingArray[i][j][0] = thisPCB.jobId;
            timingArray[i][j][1] = thisPCB.trackingInfo.runStartTime - thisPCB.trackingInfo.waitStartTime;
            timingArray[i][j][2] = thisPCB.trackingInfo.runEndTime - thisPCB.trackingInfo.waitStartTime;
            timingArray[i][j][3] = thisPCB.trackingInfo.runEndTime - thisPCB.trackingInfo.runStartTime;
            j++;
        }
    }

    // Process and Output Timing Data
    public static void outputTimingData(long [][] avgTimingArray, long [][][] timingArray, int numJobs, int iterations) {

        java.io.File timingFile = new java.io.File("timing.csv");

        try {
            java.io.PrintWriter timing = new java.io.PrintWriter(timingFile);

            timing.println("Data is in nanoseconds (divide by 1000000 to get milliseconds, 10^9 to get seconds.)");
            timing.println("Job#,AvgWaitTime,AvgCompletionTime,AvgRunTime");
            for (int i = 0; i < numJobs; i++) {
                avgTimingArray[i][0] = timingArray[0][i][0];        //job number
                timing.print(avgTimingArray[i][0] + ",");

                for (int j = 0; j < iterations; j++) {
                    avgTimingArray[i][1] += timingArray[j][i][1];   //sum wait times
                    avgTimingArray[i][2] += timingArray[j][i][2];   //sum completion times
                    avgTimingArray[i][3] += timingArray[j][i][3];   //sum execution times
                }
                avgTimingArray[i][1] = Math.round((double) avgTimingArray[i][1] / (double) iterations);  //take avg of wait times across the iterations (NANOSECONDS)
                timing.print(avgTimingArray[i][1] + ",");

                avgTimingArray[i][2] = Math.round((double) avgTimingArray[i][2] / (double) iterations);  //take avg of completion times across the iterations (NANOSECONDS)
                timing.print(avgTimingArray[i][2] + ",");

                avgTimingArray[i][3] = Math.round((double) avgTimingArray[i][3] / (double) iterations);  //take avg of execution times across the iterations (NANOSECONDS)
                timing.print(avgTimingArray[i][3] + ",");

                timing.println();
            }

            //finally calculate avg Wait Time, Completion Time, Execution Time across all jobs.
            timing.println("Avg For All Jobs, After running " + iterations + " iterations:");

            double avgWaitTime = 0;
            double avgCompletionTime = 0;
            double avgExecutionTime = 0;
            for (int i = 0; i < numJobs; i++) {
                avgWaitTime += avgTimingArray[i][1];
                avgCompletionTime += avgTimingArray[i][2];
                avgExecutionTime += avgTimingArray[i][3];
            }
            avgWaitTime = avgWaitTime / (double) numJobs;
            avgCompletionTime = avgCompletionTime / (double) numJobs;
            avgExecutionTime = avgExecutionTime / (double) numJobs;
            timing.println("  ," + Math.round(avgWaitTime) + "," + Math.round(avgCompletionTime) + "," + Math.round(avgExecutionTime));
            timing.println("  ," + avgWaitTime / Math.pow(10, 9) + "," + avgCompletionTime / Math.pow(10, 9) + "," + avgExecutionTime / Math.pow(10, 9));
            timing.close();

        }
        catch (FileNotFoundException ex){
            ex.printStackTrace();
        }
    }


}
