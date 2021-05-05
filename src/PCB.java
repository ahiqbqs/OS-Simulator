//Process Control Block
public class PCB {

    /* fields not implemented yet, from project specification
    cpuid:              // information the assigned CPU (for multiprocessor system)

    struct state:       // record of environment that is saved on interrupt
                        // including the pc, registers, permissions, buffers, caches, active
                        // pages/blocks
    struct registers:   // accumulators, index, general
    struct sched:       // burst-time, priority, queue-type, time-slice, remain-time
    struct accounts:    // cpu-time, time-limit, time-delays, start/end times, io-times
    struct progeny:     // child-procid, child-code-pointers
    parent: ptr;        // pointer to parent (if this process is spawned, else ‘null’)
    struct resources:   // file-pointers, io-devices – unitclass, unit#, open-file-tables
    status_info:        // pointer to ‘ready-list of active processes’ or
                        // ‘resource-list on blocked processes’
    */

    //job card info
    int jobId;
    int codeSize;
    int priority;

    //data control card info
    int inputBufferSize;
    int outputBufferSize;
    int tempBufferSize;

    int pc;     // the job’s pc holds the address of the instruction to fetch

    int [] registers;

    boolean goodFinish;


    //public enum state {NEW, READY, RUNNING, BLOCKED, COMPLETE};
    //state status;  //{new, ready, running, blocked}

    Memories memories;  // page-table-base, pages, page-size (not yet implemented)
                        // base-registers – logical/physical map, limit-reg (not yet implemented)

    TrackingInfo trackingInfo;


    public PCB() {
        memories = new Memories();
        registers = new int [CPU.NUM_REGISTERS];
        trackingInfo = new TrackingInfo();
    }

    public String toString() {
        String record = "jobID: " + jobId + ".\t "
                + "codeSize: " + codeSize + ".\t"
                + "priority: " + priority + ".\t"
                + "inputBufferSize:" + inputBufferSize + ".\t"
                + "outputBufferSize:" + outputBufferSize + ".\t"
                + "tempBufferSize:" + tempBufferSize + ".\t"
                //+ "status:" + status + ".\t"
                + "pc:" + pc + ".\t";
        return record;
    }

    public int getJobSizeInMemory() {
        return codeSize + inputBufferSize + outputBufferSize + tempBufferSize;
    }

}

class Memories {

    //int disk_start_reg;     //start address of job in disk.

    int base_register;  //start address of the code in memory/disk


    //int limit_register; //length of job
    //int end_register;   //end address of the code in memory
    //int input_buffer_loc;  //start address of input buffer
    //int output_buffer_loc; //start address of output buffer;



}

class TrackingInfo {

    int ioCounter;                  //number of io operations each process made
    String buffers;                 //at job completion, output buffers written to this String.

    long waitStartTime;             //time entered Ready Queue (set by Long Term Scheduler)
    long runStartTime;              //time first started executing (entered Running Queue, set by Dispatcher)

    long runEndTime;                //Completion Time = runEndTime - waitStartTime?

    //Execution Time: runEndTime - runStartTime?

    /*
    //below fields necessary for context-switching - each time we go from Running->Ready->Running etc, we
    //need to track the times.
    long startedWaitingAgainTime;   //time entered Ready Queue again
    long startedRunningAgainTime;   //time starting Executing again

    long totalTimeWaiting;          //sum of the periods spent waiting in the ready queue
                                    //(timeWaitingInReadyQueue += startingRunningAgainTime - startedWaitingAgainTime)
    */

}