public class Dispatcher {

    //The Dispatcher method assigns a process to the CPU.
    // It is also responsible for context switching of jobs when necessary (more on this later!).
    // For now, the dispatcher will extract parameter data from the PCB and accordingly set the CPU’s PC,
    // and other registers, before the OS calls the CPU to execute the job.


    public static void dispatch(PCB currJob, CPU cpu) {

        System.arraycopy (currJob.registers, 0, cpu.reg, 0, currJob.registers.length);

        cpu.jobId = currJob.jobId;
        cpu.pc = currJob.pc;
        cpu.base_reg = currJob.memories.base_register;
        cpu.codeSize = currJob.codeSize;
        cpu.inputBufferSize = currJob.inputBufferSize;
        cpu.outputBufferSize = currJob.outputBufferSize;
        cpu.tempBufferSize = currJob.tempBufferSize;
        cpu.endData_reg = currJob.getJobSizeInMemory() - 1;
        cpu.goodFinish = currJob.goodFinish;  //set to true when job completes properly (HLT) - this variable may not be necessary.
        cpu.ioCounter = currJob.trackingInfo.ioCounter;

        currJob.trackingInfo.runStartTime = System.nanoTime();
    }

    //save PCB info from CPU back into PCB
    public static void save(PCB currJob, CPU cpu) {

        System.arraycopy (cpu.reg, 0, currJob.registers, 0, currJob.registers.length);

        currJob.pc = cpu.pc;
        currJob.goodFinish = cpu.goodFinish;   //set to true when job completes properly (HLT) - this variable may not be necessary.
        currJob.trackingInfo.ioCounter = cpu.ioCounter;

        if (currJob.goodFinish) { //job successfully completed
            if (cpu.logging)
                currJob.trackingInfo.buffers = cpu.outputResults();
            currJob.trackingInfo.runEndTime = System.nanoTime();
            Queues.runningQueue.pop();
            Queues.doneQueue.add(currJob);
        }


        /*
        currJob.memories.setBase_register(cpu.base_reg);
        currJob.setCodeSize(cpu.codeSize);
        currJob.setInputBufferSize(cpu.inputBufferSize);
        currJob.setOutputBufferSize(cpu.outputBufferSize);
        currJob.setTempBufferSize(cpu.tempBufferSize);
        */
    }

}
