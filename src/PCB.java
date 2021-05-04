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
    private int jobId;
    private int codeSize;
    private int priority;

    //data control card info
    private int inputBufferSize;
    private int outputBufferSize;
    private int tempBufferSize;

    private int pc;     // the job’s pc holds the address of the instruction to fetch

    int [] registers;

    boolean goodFinish;


    public enum state {NEW, READY, RUNNING, BLOCKED, COMPLETE};

    private state status;  //{new, ready, running, blocked}

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
                + "status:" + status + ".\t"
                + "pc:" + pc + ".\t";
        return record;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getCodeSize() {
        return codeSize;
    }

    public void setCodeSize(int codeSize) {
        this.codeSize = codeSize;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getInputBufferSize() {
        return inputBufferSize;
    }

    public void setInputBufferSize(int inputBufferSize) {
        this.inputBufferSize = inputBufferSize;
    }

    public int getOutputBufferSize() {
        return outputBufferSize;
    }

    public void setOutputBufferSize(int outputBufferSize) {
        this.outputBufferSize = outputBufferSize;
    }

    public int getTempBufferSize() {
        return tempBufferSize;
    }

    public void setTempBufferSize(int tempBufferSize) {
        this.tempBufferSize = tempBufferSize;
    }

    public state getStatus() {
        return status;
    }

    public void setStatus(state status) {
        this.status = status;
    }

    public int getJobSizeInMemory() {
        return codeSize + inputBufferSize + outputBufferSize + tempBufferSize;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }
}

class Memories {

    //int disk_start_reg;     //start address of job in disk.

    private int base_register;  //start address of the code in memory/disk

    //int limit_register; //length of job
    //int end_register;   //end address of the code in memory

    //int input_buffer_loc;  //start address of input buffer
    //int output_buffer_loc; //start address of output buffer;

    public int getBase_register() {
        return base_register;
    }
    public void setBase_register(int base_register) {
        this.base_register = base_register;
    }


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