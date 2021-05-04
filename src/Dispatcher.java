public class Dispatcher {

    //The Dispatcher method assigns a process to the CPU.
    // It is also responsible for context switching of jobs when necessary (more on this later!).
    // For now, the dispatcher will extract parameter data from the PCB and accordingly set the CPU’s PC,
    // and other registers, before the OS calls the CPU to execute the job.


    public static void dispatch(PCB currJob, CPU cpu) {

        System.arraycopy (currJob.registers, 0, cpu.reg, 0, currJob.registers.length);

        cpu.jobId = currJob.getJobId();
        cpu.pc = currJob.getPc();

        cpu.base_reg = currJob.memories.getBase_register();
        cpu.codeSize = currJob.getCodeSize();
        cpu.inputBufferSize = currJob.getInputBufferSize();
        cpu.outputBufferSize = currJob.getOutputBufferSize();
        cpu.tempBufferSize = currJob.getTempBufferSize();

        //endData_reg: the last memory location used by the job; last memory location of the job's data.
        //cpu.endData_reg = cpu.codeSize + cpu.inputBufferSize + cpu.outputBufferSize + cpu.tempBufferSize - 1;
        cpu.endData_reg = currJob.getJobSizeInMemory() - 1;

        cpu.goodFinish = currJob.goodFinish;  //set to true when job completes properly (HLT) - this variable may not be necessary.

        cpu.ioCounter = currJob.trackingInfo.ioCounter;

        currJob.trackingInfo.runStartTime = System.nanoTime();
    }

    //reverse of dispatch function: save the cpu information back into the PCB currJob
    public static void save(PCB currJob, CPU cpu) {

        System.arraycopy (cpu.reg, 0, currJob.registers, 0, currJob.registers.length);

        //currJob.setJobId(cpu.jobId);
        currJob.setPc(cpu.pc);
        /*
        currJob.memories.setBase_register(cpu.base_reg);
        currJob.setCodeSize(cpu.codeSize);
        currJob.setInputBufferSize(cpu.inputBufferSize);
        currJob.setOutputBufferSize(cpu.outputBufferSize);
        currJob.setTempBufferSize(cpu.tempBufferSize);
        */

        currJob.goodFinish = cpu.goodFinish;   //set to true when job completes properly (HLT) - this variable may not be necessary.
        currJob.trackingInfo.ioCounter = cpu.ioCounter;
    }


}
