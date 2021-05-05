import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class CPU {

    public static final int NUM_REGISTERS = 16;
    public static final int DMA_DELAY = 10;     //delay DMA by X nanoseconds to simulate actual IO.

    boolean logging;        //set to true if we want to output the results(buffers) to a file.

    /////////////////////////////////////////////////////////////////////////////////
    //                  BEGIN variables set/preserved by context switcher
    /////////////////////////////////////////////////////////////////////////////////
    int jobId;

    int [] reg;             //16 registers.
                            //reg0 = Accumulator.
                            //reg1 = Zero register (0).

    int pc;                 //program counter.  (logical address)
    //(PhysicalAddressPC = base_register + pc.)

    int base_reg;           //start of job in memory (physical address)
                            //logical address starts at 0.
    int codeSize;
    int inputBufferSize;
    int outputBufferSize;
    int tempBufferSize;

    //to speed things up
    int endData_reg;        //last line of data/job (codeSize + inputBufferSize + outputBufferSize + tempBufferSize - 1)

    int ioCounter;          //track number of IO operations made.

    boolean goodFinish;     //track if logical halt reached - program ran to completion successfully.

    /////////////////////////////////////////////////////////////////////////////////
    //                  END variables set/preserved by context switcher
    /////////////////////////////////////////////////////////////////////////////////


    public CPU (boolean logging) {
        reg = new int[NUM_REGISTERS];
        this.logging = logging;
    }


    public void runCPU() {

        int currLine;
        Instruction instruction;

        while (pc < codeSize) {
            currLine = fetch(pc);
            instruction = decode(currLine);
            //printInstruction(instruction);
            execute(instruction);
            //printDataBuffers();
        }

        //PCB currPCB = Queues.runningQueue.getFirst();
        //save PCB info back into PCB
        //Dispatcher.save(currPCB, this);

        /*
        if (goodFinish) { //job successfully completed
            if (logging)
                currPCB.trackingInfo.buffers = outputResults();
            currPCB.trackingInfo.runEndTime = System.nanoTime();

            Queues.runningQueue.pop();
            Queues.doneQueue.add(currPCB);
        }
        */

    }




    // The Decode method is a part of the CPU. Its function is to completely decode a fetched instruction –
    // using the different kinds of address translation schemes of the CPU architecture.
    // (See the supplementary information in the file: Instruction Format.)
    // On decoding, the needed parameters must be loaded into the appropriate registers or data structures
    // pertaining to the program/job and readied for the Execute method to function properly.
    public Instruction decode (int currLine) {

        Instruction currInst;
        int reg1, reg2, reg3;

        int temp = (currLine >> 24);
        // why & 0b111111 in the code below?  because java int is signed.
        // we need to remove the sign from the starting 111111's (if they exist)
        int opcode = temp & 0b111111;

        // & 0b11 to remove the sign, if it exists (the starting 111111's)
        int type = (temp >> 6) & 0b11;

        switch (type) {
            case Instruction.ARITHMETIC:
                currLine >>= 12;  //remove last 12 empty bits
                reg3 = currLine & 0b1111;
                currLine >>= 4;
                reg2 = currLine & 0b1111;
                currLine >>= 4;
                reg1 = currLine & 0b1111;
                currInst = new Instruction(opcode, reg1, reg2, reg3);
                break;

            case Instruction.CBI:
                reg3 = currLine & 0xFFFF; //address
                currLine >>= 16;
                reg2 = currLine & 0b1111;
                currLine >>= 4;
                reg1 = currLine & 0b1111;
                currInst = new Instruction(opcode, reg1, reg2, reg3);
                break;

            case Instruction.JUMP:
                reg1 = currLine & 0xFFFFFF; //24-bit address
                currInst = new Instruction(opcode, reg1, 0, 0);
                break;

            case Instruction.IO:
                reg3 = currLine & 0xFFFF; //address
                currLine >>= 16;
                reg2 = currLine & 0b1111;
                currLine >>= 4;
                reg1 = currLine & 0b1111;
                currInst = new Instruction(opcode, reg1, reg2, reg3);
                break;

            default:
                System.err.println("Invalid opcode: " + type);
                currInst = null;
                break;
        }

        return currInst;
    }

    //Execute
    //This method is essentially a switch-loop of the CPU.
    // One of its key functions is to increment the PC value on ‘successful’ execution of the current instruction.
    // Note also that if an I/O operation is done via an interrupt, or due to any other preemptive instruction,
    // the job is suspended until the DMA-Channel method completes the read/write operation, or the interrupt is serviced.
    public void execute(Instruction instruction) {

        switch (instruction.opcode) {
            //handle DMA (IO) instructions.
            case Instruction.ST:
            case Instruction.LW:
            case Instruction.RD:
            case Instruction.WR:
                DMA dma = new DMA(instruction);
                Thread dmaThread = new Thread(dma);
                dmaThread.start();
                try {
                    dmaThread.join();
                }
                catch (InterruptedException ie) {
                    System.err.println(ie.toString());
                }
                break;

            //case Instruction.ARITHMETIC:
            case Instruction.MOV: //Transfers the content of one register into another
                //reg[rCommand.regD] = reg[rCommand.regS2];
                reg[instruction.reg3 + instruction.reg1] = reg[instruction.reg2];
                break;

            case Instruction.ADD: //Adds content of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] + reg[instruction.reg2];
                break;

            case Instruction.SUB: //Subtracts content of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] - reg[instruction.reg2];
                break;

            case Instruction.MUL: //Multiplies content of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] * reg[instruction.reg2];
                break;

            case Instruction.DIV: //Divides content of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] / reg[instruction.reg2];
                break;

            case Instruction.AND: //Logical AND of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] & reg[instruction.reg2];
                break;

            case Instruction.OR: //Logical OR of two S-regs into D-reg
                reg[instruction.reg3] = reg[instruction.reg1] | reg[instruction.reg2];
                break;

            case Instruction.SLT: //Sets the D-reg to 1 if  first S-reg is less than second B-reg,
                // and 0 otherwise
                reg[instruction.reg3] = (reg[instruction.reg1] < reg[instruction.reg2] ? 1 : 0);
                break;

            case Instruction.NOP: //Does nothing and moves to next instruction
                break;

            case Instruction.MOVI://Transfers address/data directly into a register
                //transfers the contents of one register to another
                reg[instruction.reg2] = instruction.reg3;
                break;

            case Instruction.ADDI: //Adds a data directly to the content of a register
                reg[instruction.reg2] += instruction.reg3;
                break;

            case Instruction.MULI: //Multiplies a data directly to the content of a register
                reg[instruction.reg2] *= instruction.reg3;
                break;

            case Instruction.DIVI: //Divides a data directly to the content of a register
                reg[instruction.reg2] /= instruction.reg3;
                break;

            case Instruction.LDI: //Loads a data/address directly to the content of a register
                reg[instruction.reg2] = instruction.reg3 + reg[instruction.reg1];
                break;

            case Instruction.SLTI: //Sets the D-reg to 1 if  first S-reg is less than a data, and 0 otherwise
                reg[instruction.reg2] = (reg[instruction.reg3] < instruction.reg1 ? 1 : 0);
                break;

            case Instruction.BEQ: //Branches to an address when content of B-reg = D-reg
                if (reg[instruction.reg1] == reg[instruction.reg2])
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;

            case Instruction.BNE: //Branches to an address when content of B-reg <> D-reg
                if (reg[instruction.reg1] != reg[instruction.reg2])
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;

            case Instruction.BEZ: //Branches to an address when content of B-reg = 0
                if (reg[instruction.reg1] == 0)
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;

            case Instruction.BNZ: //Branches to an address when content of B-reg <> 0
                if (reg[instruction.reg1] != 0)
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;

            case Instruction.BGZ: //Branches to an address when content of B-reg > 0
                if (reg[instruction.reg1] > 0)
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;

            case Instruction.BLZ: //Branches to an address when content of B-reg < 0
                if (reg[instruction.reg1] < 0)
                    // divide by 4 to convert from byte to word.
                    // - 1 because pc counter is incremented at end of Execute.
                    pc = (instruction.reg3 / 4) - 1;
                break;


            //case Instruction.JUMP:
            case Instruction.HLT: //Logical end of program
                goodFinish = true;
                break;

            case Instruction.JMP: //Jumps to a specified location
                pc = (instruction.reg1 / 4) - 1;
                break;

            default:
                System.err.println("Invalid opcode in execute: " + instruction.type);
                break;
        }

        pc++;
    }

    //Fetch
    //With support from the Memory module/method, this method fetches instructions or data from RAM
    // depending on the content of the CPU’s program counter (PC).
    // On instruction fetch, the PC value should point to the next instruction to be fetched.
    // The Fetch method therefore calls the Effective-Address method to translate the logical
    // address to the corresponding absolute address,
    // using the base-register value and a ‘calculated’ offset/address displacement.
    // The Fetch, therefore, also supports the Decode method of the CPU.
    public int fetch (int address) {
        //check that the code being fetched is inside the job's code section
        if ((address >= 0) && (address < codeSize)) {
            return MemorySystem.memory.readMemoryAddress(getEffectiveAddress(address));
        }
        else {
            System.err.println ("Error.  Invalid fetch at address: " + address);
            System.exit(-1);
            return -1;
        }
    }


    //read the specified memory address
    public int readBuffer(int address) {
        //Convert from bytes to words - the instruction addresses go by bytes.
        //e.g. pc = pc + 4, we need pc = pc + 1.  if address, we need address/4.
        address = address/4;

        //check if address is in bounds of the current job's DATA section.
        if ((address >= codeSize) && (address <= endData_reg)) {
            return MemorySystem.memory.readMemoryAddress(getEffectiveAddress(address));
        }
        else {
            System.err.println ("Error.  Invalid memory write at address: " + address);
            System.exit(-1);
            return -1;
        }
    }

    //write the specified memory address
    public void writeBuffer(int address, int data) {
        //Convert from bytes to words - the instruction addresses go by bytes.
        //e.g. pc = pc + 4, we need pc = pc + 1.  if address, we need address/4.
        address = address/4;

        //check if address is in bounds of the current job's DATA section.
        if ((address >= codeSize) && (address <= endData_reg)) {
            MemorySystem.memory.writeMemoryAddress(getEffectiveAddress(address), data);
        }
        else {
            System.err.println ("Error.  Invalid memory read at address: " + address);
            System.exit(-1);
        }
    }

    public int getEffectiveAddress (int offset) {
        return base_reg + offset;
    }


    //debugging method - prints current instruction.
    public void printInstruction(Instruction instruction) {

        String [] opStrings = {"RD", "WR", "ST", "LW", "MOV", "ADD", "SUB", "MUL", "DIV", "AND", "OR", "MOVI", "ADDI",
                "MULI", "DIVI", "LDI", "SLT", "SLTI", "HLT", "NOP", "JMP", "BEQ", "BNE", "BEZ", "BNZ", "BGZ", "BLZ"};

        switch (instruction.type) {

            case Instruction.ARITHMETIC:
                //RInstruction rCommand = (RInstruction) instruction;
                System.out.print("Rtype. Opcode: " + instruction.opcode + " " + opStrings[instruction.opcode] + "\t");
                System.out.println("S1: " + instruction.reg1 + "  S2: " + instruction.reg2 + "  D: " + instruction.reg3);
                break;

            case Instruction.CBI:
                //CBIInstruction cbiCommand = (CBIInstruction) instruction;
                System.out.print("CBI.   Opcode: " + instruction.opcode + " " + opStrings[instruction.opcode] + "\t");
                System.out.println("B: " + instruction.reg1 + "  D: " + instruction.reg2 + "  Ad: " + instruction.reg3);
                break;

            case Instruction.JUMP:
                //JumpInstruction jumpCommand = (JumpInstruction) instruction;
                System.out.print("Jump.  Opcode: " + instruction.opcode + " " + opStrings[instruction.opcode] + "\t");
                System.out.println("Ad: " + instruction.reg1);

                break;

            case Instruction.IO:
                //IOInstruction ioCommand = (IOInstruction) instruction;
                System.out.print("IO.    Opcode: " + instruction.opcode + " " + opStrings[instruction.opcode] + "\t");
                System.out.println("Reg1: " + instruction.reg1 + "  Reg2: " + instruction.reg2 + "  Ad: " + instruction.reg3);
                break;

        }
    }

    //for printing the results of the job to a file.
    public String outputResults() {

        StringBuilder results = new StringBuilder();
        results.append("Job ID:\t" + jobId + "\r\nInput:\t");
        for (int i = 0; i < inputBufferSize; i++) {
            results.append(readBuffer((codeSize + i)*4) + " ");  //*4 to convert from words to bytes
        }
        results.append("\r\nOutput:\t");
        for (int i = 0; i < outputBufferSize; i++) {
            results.append(readBuffer((codeSize + inputBufferSize + i)*4) + " ");  //*4 to convert from words to bytes
        }
        results.append("\r\nTemp:\t");
        for (int i = 0; i < tempBufferSize; i++) {
            results.append(readBuffer((codeSize + inputBufferSize + outputBufferSize + i)*4) + " ");  //*4 to convert from words to bytes
        }
        results.append("\r\n");

        return results.toString();
    }


    //debugging method - prints registers and buffers to screen.
    public void printDataBuffers() {

        //System.out.print("Regs:\t");
        for (int i = 0; i < reg.length; i++) {
            System.out.print("R" + i + ":" + reg[i] + "\t");
            if (i==7)
                System.out.println();
        }
        System.out.println("PC:" + pc);

        System.out.print("Input:\t");
        for (int i = 0; i < inputBufferSize; i++) {
            System.out.print(readBuffer((codeSize + i)*4) + " ");  //*4 to convert from words to bytes
        }
        System.out.println();

        System.out.print("Output:\t");
        for (int i = 0; i < outputBufferSize; i++) {
            System.out.print(readBuffer((codeSize + inputBufferSize + i)*4) + " ");  //*4 to convert from words to bytes
        }

        System.out.println();

        System.out.print("Temp:\t");
        for (int i = 0; i < tempBufferSize; i++) {
            System.out.print(readBuffer((codeSize + inputBufferSize + outputBufferSize + i)*4) + " ");  //*4 to convert from words to bytes
        }

        System.out.println();
        System.out.println();
    }



    //separate thread to handle DMA instructions.
    class DMA implements Runnable {

        Instruction dmaCommand;

        public void run() {
            handleDMA();
        }

        public DMA(Instruction dmaCommand) {
            this.dmaCommand = dmaCommand;
        }

        public void handleDMA() {
            //handle DMA (IO) instructions.

            switch (dmaCommand.opcode) {
                //case Instruction.IO:
                case Instruction.RD:    //Reads content of I/P buffer into a accumulator
                    reg[dmaCommand.reg1] = readBuffer(dmaCommand.reg3 + reg[dmaCommand.reg2]);
                    break;

                case Instruction.WR:    //Writes the content of accumulator into O/P buffer
                    if (dmaCommand.reg3 != 0)
                        writeBuffer(dmaCommand.reg3, reg[dmaCommand.reg1]);
                    else
                        writeBuffer(reg[dmaCommand.reg2], reg[dmaCommand.reg1]);
                    break;

                //case Instruction.CBI:
                case Instruction.ST: //Stores content of a reg. into an address
                    writeBuffer(reg[dmaCommand.reg2] + dmaCommand.reg3, reg[dmaCommand.reg1]);
                    break;

                case Instruction.LW: //Loads the content of an address into a reg.
                    reg[dmaCommand.reg2] = readBuffer(dmaCommand.reg3 + reg[dmaCommand.reg1]);
                    break;

                default:
                    System.err.println("Invalid DMA opcode in execute: " + dmaCommand.type);
                    break;
            }

            ioCounter++;
            try {
                TimeUnit.NANOSECONDS.sleep(DMA_DELAY);
            }
            catch (InterruptedException ie) {
                System.err.println("Invalid DMA handler interruption: " + ie.toString());
            }

        }
    }


}
