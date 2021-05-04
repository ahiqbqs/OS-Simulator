class Instruction {

    final static int ARITHMETIC = 0;
    final static int CBI = 1;
    final static int JUMP = 2;
    final static int IO = 3;
    //enum TYPE {ARITHMETIC, CBI, JUMP, IO};

    final static int RD = 0;
    final static int WR = 1;
    final static int ST = 2;
    final static int LW = 3;
    final static int MOV = 4;
    final static int ADD = 5;
    final static int SUB = 6;
    final static int MUL = 7;
    final static int DIV = 8;
    final static int AND = 9;
    final static int OR = 10;
    final static int MOVI = 11;
    final static int ADDI = 12;
    final static int MULI = 13;
    final static int DIVI = 14;
    final static int LDI = 15;
    final static int SLT = 16;
    final static int SLTI= 17;
    final static int HLT = 18;
    final static int NOP = 19;
    final static int JMP = 20;
    final static int BEQ = 21;
    final static int BNE = 22;
    final static int BEZ = 23;
    final static int BNZ = 24;
    final static int BGZ = 25;
    final static int BLZ = 26;

    //enum OPCODE {RD, WR, ST, LW, MOV, ADD, SUB, MUL, DIV, AND, OR, MOVI, ADDI, MULI, DIVI, LDI, SLT, SLTI,
    //             HLT, NOP, JMP, BEQ, BNE, BEZ, BNZ, BGZ, BLZ};

    int type;
    int opcode;

    int reg1;
    int reg2;
    int reg3;

    public Instruction (int opcode, int reg1, int reg2, int reg3) {
        this.opcode = opcode;
        this.reg1 = reg1;
        this.reg2 = reg2;
        this.reg3 = reg3;
    }
}

/*class RInstruction extends Instruction {

    int regS1;      //4 bits
    int regS2;      //4 bits
    int regD;       //4 bits

    public RInstruction(int opcode, int regS1, int regS2, int regD) {
        super(opcode);
        type = ARITHMETIC;

        this.regS1 = regS1;
        this.regS2 = regS2;
        this.regD = regD;
    }
}

class CBIInstruction extends Instruction {

    int regB;       //4 bits
    int regD;       //4 bits
    int address;    //16 bits

    public CBIInstruction(int opcode, int regB, int regD, int address) {
        super(opcode);
        type = CBI;

        this.regB = regB;
        this.regD = regD;
        this.address= address;
    }

}

class JumpInstruction extends Instruction {

    int address;    //24 bits

    public JumpInstruction(int opcode, int address) {
        super(opcode);
        type = JUMP;

        this.address = address;
    }

}

class IOInstruction extends Instruction {

    int reg1;       //4 bits
    int reg2;       //4 bits
    int address;    //16 bits

    public IOInstruction(int opcode, int reg1, int reg2, int address) {
        super(opcode);
        type = IO;

        this.reg1 = reg1;
        this.reg2 = reg2;
        this.address= address;
    }

}
*/

