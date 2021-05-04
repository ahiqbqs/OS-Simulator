import java.util.LinkedList;
import java.util.Comparator;

//Long-Term Scheduler.

//Quick and Dirty algorithm: load 15 jobs at at time into Memory, and load their PCB's onto the ReadyQueue.

//After each Load, the LongScheduler also checks the percentage of RAM space used, and sets maxMemUsed.


public class LongScheduler {

    final static int FIFO = 1;
    final static int PRIORITY = 2;

    final static int NUM_JOBS_TO_LOAD = 15;     //in class, suggested loading 15 at a time.

    int algorithm = FIFO;                       //current scheduling algorithm.

    LinkedList<PCB> diskQueue;
    LinkedList<PCB> readyQueue;
    LinkedList<PCB> waitingQueue;
    MemorySystemClass memorySystem;

    int maxMemUsed;


    public LongScheduler (LinkedList<PCB> diskQueue, LinkedList<PCB> readyQueue, LinkedList<PCB> waitingQueue, MemorySystemClass memorySystem, int policy) {
        this.diskQueue = diskQueue;
        this.readyQueue = readyQueue;
        this.waitingQueue = waitingQueue;
        this.memorySystem = memorySystem;

        this.algorithm = policy;
    }


    public void schedule () {

        if (readyQueue.size() == 0) {

            //load processes into memory from disk.
            //these processes are "Ready" to run.

            //algorithms:
            //FIFO - in order listed in diskQueue.
            //PRIORITY - in order of Priority (16 = highest priority, 0 = lowest?)

            if (algorithm == PRIORITY) {
                //how to handle this?
                //1. we could create a new PCB table, sorted by priority. or,
                //2. we could search the existing PCB table, for highest priority. or,
                //3 we could sort the existing PCB table.
                //for now, let's go with option 3: sort the existing PCB table.

                //sort PCB table: priority level 16(highest) will be at start, 0 at end
                diskQueue.sort((PCB o1, PCB o2) -> o2.getPriority() - o1.getPriority());

                //for (PCB currPCB: diskQueue) {
                //    System.out.println(currPCB.getJobId() + " " + currPCB.getPriority());
                //}
            }

            int jobCounter = 0;  //track number of jobs loaded
            int memCounter = 0;  //current mem location being written to
            int diskCounter = 0; //current disk location being read

            int data = 0;
            int jobSize = 0;

            boolean outOfRoom = false;

            PCB currPCB;

            //keep reading in jobs until A) 15 jobs read in, B) memory limit reached, or C )no more jobs to load.
            while ((jobCounter < NUM_JOBS_TO_LOAD) && (!outOfRoom) && (!diskQueue.isEmpty())) {

                currPCB = diskQueue.get(0);  //read the topmost job on the disk table.

                diskCounter = currPCB.memories.getBase_register();

                jobSize = currPCB.getJobSizeInMemory();

                //check: do we have enough memory to store this job?
                if (memorySystem.memory.checkAddressInBounds(memCounter + jobSize)) {

                    currPCB.memories.setBase_register(memCounter);
                    //currPCB.memories.setEnd_register(memCounter + jobSize -1);
                    //currPCB.memories.setInput_buffer_loc(memCounter + currPCB.getCodeSize() - 1); //??
                    currPCB.setStatus(PCB.state.READY);
                    readyQueue.add(currPCB);
                    diskQueue.pop();
                    currPCB.trackingInfo.waitStartTime = System.nanoTime();

                    System.arraycopy(memorySystem.disk.diskArray, diskCounter, memorySystem.memory.memArray, memCounter, jobSize);

                    memCounter += jobSize;
                    /*
                    //diskCounter += jobSize;
                    //read a job into memory.
                    for (int i = 0; i < jobSize; i++) {
                        //read a line of the job from disk, then write it to memory.
                        data = memorySystem.disk.readDisk(diskCounter);
                        memorySystem.memory.writeMemoryAddress(memCounter, data);
                        diskCounter++;
                        memCounter++;
                    }
                    */

                    jobCounter++;
                } else {
                    outOfRoom = true;

                }

            }

            int currMemUsage = calcMemUsage();
            if (currMemUsage > maxMemUsed) {
                maxMemUsed = currMemUsage;
            }
            //System.out.println ("current memory usage: " + currMemUsage + "\tmax memory usage: " + maxMemUsed);

            /*
            for (PCB thisPCB : diskQueue) {
                System.out.println(thisPCB);
            }
            System.out.println();
            for (PCB thisPCB : readyQueue) {
                System.out.println(thisPCB);
            }*/

        }

    }


    //sum up memory usage of all the jobs currently in memory (readyQueue and waitingQueue).
    public int calcMemUsage () {
        int counter = 0;
        for (PCB thisPCB : readyQueue) {
            counter += thisPCB.getJobSizeInMemory();
        }
        for (PCB thisPCB : waitingQueue) {
            counter += thisPCB.getJobSizeInMemory();
        }
        return counter;
    }

}
