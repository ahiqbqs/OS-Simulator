//Long-Term Scheduler.

//Quick and Dirty algorithm: load 15 jobs at at time into Memory, and load their PCB's onto the ReadyQueue.
//After each Load, the LongScheduler also checks the percentage of RAM space used, and sets maxMemUsed.


public class LongScheduler {

    final static int FIFO = 1;
    final static int PRIORITY = 2;
    final static int SJF = 3;
    final static int NUM_JOBS_TO_LOAD = 15;     //in class, suggested loading 15 at a time.

    int algorithm = FIFO;                       //current scheduling algorithm.

    static int maxMemUsed;

    public LongScheduler (int policy) {
        this.algorithm = policy;
    }


    //load processes into memory from disk.
    //these processes are "Ready" to run.
    public void schedule () {
        if (Queues.readyQueue.size() == 0) {

            //algorithms:
            //FIFO - in order listed in diskQueue.
            //PRIORITY - in order of Priority (16 = highest priority, 0 = lowest?)
            //SJF - shortest job first.
            if (algorithm == PRIORITY) {
                Queues.diskQueue.sort((PCB o1, PCB o2) -> o2.priority - o1.priority);
            } else if (algorithm == SJF) {
                Queues.diskQueue.sort((PCB o1, PCB o2) -> o1.getJobSizeInMemory() - o2.getJobSizeInMemory());
                //diskQueue.sort((PCB o1, PCB o2) -> o1.getCodeSize() - o2.getCodeSize());
                /*
                for (PCB thisPCB : Queues.diskQueue) {
                    System.out.print(thisPCB.jobId + "\t");
                    System.out.print(thisPCB.getJobSizeInMemory() + "\t");
                    System.out.print(thisPCB.codeSize + "\t");
                    System.out.println();
                }*/
            }

            int jobCounter = 0;  //track number of jobs loaded
            int memCounter = 0;  //current mem location being written to
            int diskCounter = 0; //current disk location being read

            int jobSize = 0;
            boolean outOfRoom = false;
            PCB currPCB;

            //keep reading in jobs until A) 15 jobs read in, B) memory limit reached, or C )no more jobs to load.
            while ((jobCounter < NUM_JOBS_TO_LOAD) && (!outOfRoom) && (!Queues.diskQueue.isEmpty())) {

                currPCB = Queues.diskQueue.get(0);  //read the top-most job on the disk table.
                diskCounter = currPCB.memories.base_register;
                jobSize = currPCB.getJobSizeInMemory();

                //check: do we have enough memory to store this job?
                if (MemorySystem.memory.checkAddressInBounds(memCounter + jobSize)) {

                    currPCB.memories.base_register = memCounter;
                    //currPCB.status = PCB.state.READY;
                    Queues.readyQueue.add(currPCB);
                    Queues.diskQueue.pop();
                    currPCB.trackingInfo.waitStartTime = System.nanoTime();
                    System.arraycopy(MemorySystem.disk.diskArray, diskCounter, MemorySystem.memory.memArray, memCounter, jobSize);

                    memCounter += jobSize;
                    jobCounter++;
                } else {
                    outOfRoom = true;
                }

            }

            int currMemUsage = calcMemUsage();
            if (currMemUsage > maxMemUsed) {
                maxMemUsed = currMemUsage;
            }
        }

    }

    //sum up memory usage of all the jobs currently in memory (readyQueue and waitingQueue).
    public int calcMemUsage () {
        int counter = 0;
        for (PCB thisPCB : Queues.readyQueue) {
            counter += thisPCB.getJobSizeInMemory();
        }
        for (PCB thisPCB : Queues.waitingQueue) {
            counter += thisPCB.getJobSizeInMemory();
        }
        return counter;
    }

}
