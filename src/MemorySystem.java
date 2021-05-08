public class MemorySystem {

    //public short[] registers;
    static public DiskClass disk;
    static public MemoryClass memory;

    static public void initMemSystem() {
        //java: int is 32 bits, short is 16 bits.
        //registers = new short[15];
        //disk: 2048 words.  1 word = 4 bytes (or 8 hex characters).
        disk = new DiskClass();
        //memory: 1024 words.  1 word = 4 bytes (or 8 hex characters).
        memory = new MemoryClass();
    }

}

//disk: 2048 words.  1 word = 4 bytes (or 8 hex characters).
class DiskClass {
    public final int DISK_SIZE = 2048;

    int[] diskArray;

    public DiskClass() {
        diskArray = new int[DISK_SIZE];
    }

    public void writeDisk(String line, int diskCounter) {
        //int hexLong = Long.decode(line).intValue();

        int hexInt = Long.decode(line).intValue();
        diskArray[diskCounter] = hexInt;

        //System.out.println (hexInt);
        //line = line.substring(2);
        //System.out.println (line.substring(2,3));
        //System.out.println (line.substring(3,4));
        //diskArray[diskCounter].word[0] = line.substring(2,3);

    }

    //returns a line of code from the disk (as an int)
    public int readDisk(int diskCounter) {
          return diskArray[diskCounter];
    }

}

//memory: 1024 words.  1 word = 4 bytes (or 8 hex characters).
class MemoryClass {
    public final int MEM_SIZE = 1024;

    int[] memArray;

    public MemoryClass() {
        memArray = new int [MEM_SIZE];
    }

    //This method is the only module of your simulator by which RAM can be accessed.
    // A known absolute/physical address must always be passed to this method.
    // The Memory simply fetches an instruction or datum or writes datum into RAM (or cache – more on this later!).
    public void writeMemoryAddress(int ramAddress, int data) {
        if ((ramAddress < 0) || (ramAddress > MEM_SIZE - 1))
            System.out.println ("Error, attempting to write to invalid memory address: " + ramAddress);
        else
            memArray[ramAddress] = data;
    }

    public int readMemoryAddress(int ramAddress) {
        if ((ramAddress < 0) || (ramAddress > MEM_SIZE - 1)) {
            System.out.println("Error, attempting to write to invalid memory address: " + ramAddress);
            return -1;
        }
        else
            return (memArray[ramAddress]);
    }


    public boolean checkAddressInBounds(int memLoc) {
        return !((memLoc < 0 ) || (memLoc > MEM_SIZE - 1));
    }
}

