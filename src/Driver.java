import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.LinkedList;

public class Driver {




    public static void main(String[] args) throws FileNotFoundException {

        Scanner userInput = new Scanner (System.in);
        boolean logging = false;
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
                CPU cpu = new CPU(logging);

                coreDumpArray = new ArrayList();

                loader.load(input);
                numJobs = Queues.diskQueue.size();

                /////////////////////////////////////////////////////////////////////////////////
                //                        Begin Main Driver Loop
                /////////////////////////////////////////////////////////////////////////////////
                do {
                    longScheduler.schedule();       //load processes into memory from disk.
                    ShortScheduler.schedule();      //pick one job from the ready Queue to run.

                    //prepare job to run on CPU, extract info from PCB and set CPU's pc, registers, etc.
                    Dispatcher.dispatch(Queues.runningQueue.getFirst(), cpu);

                    cpu.runCPU();

                    Dispatcher.save(Queues.runningQueue.getFirst(), cpu);
                    //scan.nextLine();


                    if ((logging) && (Queues.readyQueue.size() == 0)) {
                        //if the readyQueue is empty, then we have processed all the jobs in the ready queue
                        //and we are ready to do a core dump.
                        saveMemoryForCoreDump(coreDumpArray);
                    }
                }
                while (checkForMoreJobs());
                /////////////////////////////////////////////////////////////////////////////////
                //                          END Main Driver Loop
                /////////////////////////////////////////////////////////////////////////////////

                if (logging) {
                    writeOutputFile ();
                    writeCoreDumpFiles (coreDumpArray);
                }

                //create timing array (if not already created)
                if (timingArray == null) {
                    timingArray = new long [iterations][Queues.doneQueue.size()][numTimingFields];
                }
                saveTimingDataForThisIteration(i, timingArray);


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
        return ((Queues.diskQueue.size() != 0) || (Queues.readyQueue.size() != 0) || (Queues.runningQueue.size() != 0));
    }

    public static void saveMemoryForCoreDump(ArrayList<MemoryClass> coreDumpArray) {
        MemoryClass currCoreDump = new MemoryClass();
        System.arraycopy(MemorySystem.memory.memArray, 0, currCoreDump.memArray, 0, MemorySystem.memory.MEM_SIZE);
        coreDumpArray.add(currCoreDump);
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
