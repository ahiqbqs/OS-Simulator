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


        //long [1000][30][4]  e.g. 1000 trials, 30 jobs, 3 columns (Job #, Waiting Time, Completion Time, Execution Time)
        long [][][] timingArray = null;
        int numJobs = 0;
        int numTimingFields = 4;
        long [][] avgTimingArray = null;

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
        //                  Begin Main Driver Loop
        /////////////////////////////////////////////////////////////////////////////////

        for (int i = 0; i < iterations; i++) {

            // Create a file
            java.io.File file = new java.io.File("Program-File.txt");

            try ( //try with resources (auto-closes Scanner)
                  Scanner input = new Scanner(file);
                  //java.io.PrintWriter output = new java.io.PrintWriter(file);
            ) {
                MemorySystemClass memorySystem = new MemorySystemClass();

                LinkedList<PCB> diskQueue = new LinkedList<>();
                LinkedList<PCB> readyQueue = new LinkedList<>();
                LinkedList<PCB> waitingQueue = new LinkedList<>();      //not used yet

                //runningQueue: 0 or 1 items currently running.
                //(this is kind of a workaround for Java, which does not allow pass by reference -
                //hence we need a container (runningQueue) for the PCB of the job currently running)
                LinkedList<PCB> runningQueue = new LinkedList<>();

                //for reporting purposes, to track complete jobs
                LinkedList<PCB> doneQueue = new LinkedList<>();

                Loader loader = new Loader();

                LongScheduler longScheduler = new LongScheduler(diskQueue, readyQueue, waitingQueue, memorySystem, policy);
                //ShortScheduler shortScheduler = new ShortScheduler();
                //Dispatcher dispatcher = new Dispatcher();
                CPU cpu = new CPU(memorySystem, runningQueue, doneQueue, logging);

                loader.load(input, diskQueue, memorySystem.disk);
                numJobs = diskQueue.size();
                coreDumpArray = new ArrayList();

                Scanner scan = new Scanner(System.in);

                do {

                    //load processes into memory from disk.
                    longScheduler.schedule();

                    //pick one job from the ready Queue to run.
                    ShortScheduler.Schedule(readyQueue, runningQueue);

                    //System.out.println("\n" + runningQueue.getFirst().toString());

                    //prepare job to run on CPU.
                    //extract parameter data from the PCB and set the CPU's PC, registers, etc.
                    Dispatcher.dispatch(runningQueue.getFirst(), cpu);

                    cpu.runCPU();
                    //scan.nextLine();


                    //////////////////////BEGIN Save Core Dump Info////////////////////////////////
                    if (logging) {

                        //before the long-term scheduler runs, output memory to a file. (Core Dump)
                        if (readyQueue.size() == 0) {
                            //if the readyQueue is empty, then we have processed all the jobs in the ready queue
                            //and we are ready to do a core dump.
                            MemoryClass currCoreDump = new MemoryClass();
                            System.arraycopy(memorySystem.memory.memArray, 0, currCoreDump.memArray, 0, memorySystem.memory.MEM_SIZE);
                            coreDumpArray.add(currCoreDump);
                        }

                    }
                    //////////////////////END Save Core Dump Info/////////////////////////////////



                }
                while (checkForMoreJobs(diskQueue, readyQueue, runningQueue));

                /////////////////////////////////////////////////////////////////////////////////
                //                Log Mem Usage, Output and # IO Operations to File
                /////////////////////////////////////////////////////////////////////////////////
                if (logging) {

                    java.io.File outputFile = new java.io.File("output.txt");
                    java.io.PrintWriter output = new java.io.PrintWriter(outputFile);

                    String strMemUsage = "max memory usage: " + longScheduler.maxMemUsed;
                    System.out.println(strMemUsage);
                    output.println(strMemUsage);

                    for (PCB thisPCB : doneQueue) {
                        //System.out.println("Job:" + thisPCB.getJobId() + "\tNumber of io operations: " + thisPCB.trackingInfo.ioCounter);
                        output.println("Job:" + thisPCB.getJobId() + "\tNumber of io operations: " + thisPCB.trackingInfo.ioCounter);
                        //System.out.print("Job:" + thisPCB.getJobId() + "\tEntered Waiting Queue: " + thisPCB.trackingInfo.waitStartTime);
                        //System.out.print("\tEntered Running Queue: " + thisPCB.trackingInfo.runStartTime);
                        //System.out.println("\tFinish Time: " + thisPCB.trackingInfo.runEndTime);
                    }

                    for (PCB thisPCB : doneQueue) {
                        output.print(thisPCB.trackingInfo.buffers);

                    }
                    output.close();


                    for (int j = 0; j < coreDumpArray.size(); j++) {

                    java.io.File coreDumpFile = new java.io.File("coreDump" + (j+1) + ".txt");
                    java.io.PrintWriter coreDump = new java.io.PrintWriter(coreDumpFile);

                    //handle the core dump.
                    String padding = "00000000";
                    for (int k = 0; k < memorySystem.memory.MEM_SIZE; k++) {
                        //coreDump.println(coreDumpArray.get(j).memArray[k]);
                        String unpaddedHex = Integer.toHexString(coreDumpArray.get(j).memArray[k]).toUpperCase();
                        String paddedHex = padding.substring(unpaddedHex.length()) + unpaddedHex;
                        paddedHex = "0x" + paddedHex;
                        coreDump.println(paddedHex);
                    }

                    coreDump.close();

                    }

                }


                /////////////////////////////////////////////////////////////////////////////////
                //                           Save Timing Data for this Iteration
                /////////////////////////////////////////////////////////////////////////////////

                //check the timing array has been created
                if (timingArray == null) {
                    numJobs = doneQueue.size();
                    timingArray = new long [iterations][doneQueue.size()][numTimingFields];
                }
                int j=0; //j = job counter
                for (PCB thisPCB: doneQueue) {
                    //i=iteration counter
                    //waitStartTime - time entered Ready Queue (set by Long Term Scheduler)
                    //runStartTime  - time first started executing (entered Running Queue, set by Dispatcher)
                    //runEndTime    - Completion Time = runEndTime - waitStartTime?
                    timingArray[i][j][0] = thisPCB.getJobId();
                    timingArray[i][j][1] = thisPCB.trackingInfo.runStartTime - thisPCB.trackingInfo.waitStartTime;
                    timingArray[i][j][2] = thisPCB.trackingInfo.runEndTime - thisPCB.trackingInfo.waitStartTime;
                    timingArray[i][j][3] = thisPCB.trackingInfo.runEndTime - thisPCB.trackingInfo.runStartTime;
                    j++;
                }

            }
        }

        /////////////////////////////////////////////////////////////////////////////////
        //                           Process and Output Timing Data
        /////////////////////////////////////////////////////////////////////////////////

        java.io.File timingFile = new java.io.File("timing.csv");
        java.io.PrintWriter timing = new java.io.PrintWriter(timingFile);

        //calculate average Wait Time and Completion Time for each job.
        avgTimingArray = new long [numJobs][numTimingFields];

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
        timing.println("  ," + avgWaitTime/Math.pow(10,9) + "," + avgCompletionTime/Math.pow(10,9) + "," + avgExecutionTime/Math.pow(10,9));
        timing.close();

    }


    public static boolean checkForMoreJobs (LinkedList<PCB> diskQueue, LinkedList<PCB> readyQueue, LinkedList<PCB> runningQueue) {
        if ((diskQueue.size() != 0) || (readyQueue.size() != 0) || (runningQueue.size() != 0)) {
            return true;
        }
        else
            return false;
    }
}
