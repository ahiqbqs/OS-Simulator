import java.util.Scanner;
import java.util.LinkedList;
import static java.lang.Integer.parseInt;


//note: currently the Loader assumes that there is enough disk space to load all the jobs.
//      it does NOT check that there is enough space before loading each job.


public class Loader {

    public void load(Scanner input) {

        /////////////////////////////////////////////////////////////////////////////////
        //                              BEGIN LOADER
        /////////////////////////////////////////////////////////////////////////////////

        int diskCounter = 0;            //track where we are on the disk.

        while (input.hasNext()) {

            PCB currJob = new PCB();    //temp variable; stores info about current job being read

            //1. read job card info into PCB.
            //2. read program file into disk.
            //3. read data card info into PCB.
            //4. read data file into disk.
            //5. add PCB to the PCB table.

            String s1 = input.next();
            if (!s1.equals("//")) {
                System.err.println("Error: // expected.  Instead found: " + s1);
                System.exit(0);
            } else {
                String controlCard = input.next();
                if (!controlCard.equalsIgnoreCase("JOB")) {
                    System.err.println("Error: JOB expected.  Instead found: " + controlCard);
                    System.exit(0);
                } else {
                    /////////////////////////////////////////////////////////////////////////////////
                    //                           BEGIN READING JOB CARD
                    /////////////////////////////////////////////////////////////////////////////////

                    String jobIdHex = input.next();
                    int jobId = Integer.parseInt(jobIdHex, 16);
                    //System.out.println("jobid " + jobId);
                    currJob.jobId = jobId;

                    String codeSizeHex = input.next();
                    int codeSize = parseInt(codeSizeHex, 16);
                    //System.out.println("codeSize " + codeSize);
                    currJob.codeSize = codeSize;

                    String priorityHex = input.next();
                    int priority = Integer.parseInt(priorityHex, 16);
                    //System.out.println("priority " + priorityHex);
                    currJob.priority = priority;

                    /////////////////////////////////////////////////////////////////////////////////
                    //                           END READING JOB CARD
                    /////////////////////////////////////////////////////////////////////////////////


                    //make a note of where the job starts on the disk.
                    currJob.memories.base_register = diskCounter;
                    currJob.pc = 0;               //set program counter to 0.
                    currJob.goodFinish = false;
                    //currJob.status = PCB.state.NEW;

                    /////////////////////////////////////////////////////////////////////////////////
                    //                           BEGIN READING INSTRUCTIONS
                    /////////////////////////////////////////////////////////////////////////////////

                    //read in instructions.
                    for (int i = 0; i < codeSize; i++) {
                        String instruction = input.next();

                        if (!instruction.startsWith("0x")) {
                            System.err.println("Error: 0x expected.  Instead found: " + instruction);
                            System.exit(0);
                        } else {
                            //System.out.println(instruction);
                            MemorySystem.disk.writeDisk(instruction, diskCounter);
                            diskCounter++;
                        }
                    }

                    /////////////////////////////////////////////////////////////////////////////////
                    //                           END READING INSTRUCTIONS
                    /////////////////////////////////////////////////////////////////////////////////


                    //next, read in Data card
                    controlCard = input.next();
                    if (!controlCard.equals("//")) {
                        System.err.println("Error: // expected.  Instead found: " + controlCard);
                        System.exit(0);
                    } else {
                        controlCard = input.next();
                        if (!controlCard.equalsIgnoreCase("Data")) {
                            System.err.println("Error: Data expected.  Instead found: " + controlCard);
                            System.exit(0);
                        } else {

                            /////////////////////////////////////////////////////////////////////////////////
                            //                           BEGIN READING DATA CARD
                            /////////////////////////////////////////////////////////////////////////////////

                            String inputBufferSizeHex = input.next();
                            int inputBufferSize = Integer.parseInt(inputBufferSizeHex, 16);
                            //System.out.println(inputBufferSize);
                            currJob.inputBufferSize = inputBufferSize;

                            String outputBufferSizeHex = input.next();
                            int outputBufferSize = Integer.parseInt(outputBufferSizeHex, 16);
                            //System.out.println(outputBufferSize);
                            currJob.outputBufferSize = outputBufferSize;

                            String tempBufferSizeHex = input.next();
                            int tempBufferSize = Integer.parseInt(tempBufferSizeHex, 16);
                            //System.out.println(tempBufferSize);
                            currJob.tempBufferSize = tempBufferSize;

                            /////////////////////////////////////////////////////////////////////////////////
                            //                           END READING DATA CARD
                            /////////////////////////////////////////////////////////////////////////////////

                            /////////////////////////////////////////////////////////////////////////////////
                            //                           BEGIN READING DATA SECTION
                            /////////////////////////////////////////////////////////////////////////////////

                            for (int i = 0; i < inputBufferSize + outputBufferSize + tempBufferSize; i++) {
                                String data = input.next();
                                if (!data.startsWith("0x")) {
                                    System.err.println("Error: 0x expected.  Instead found: " + data);
                                    System.exit(0);
                                } else {
                                    //System.out.println(data);
                                    MemorySystem.disk.writeDisk(data, diskCounter);
                                    diskCounter++;
                                }
                            }

                            /////////////////////////////////////////////////////////////////////////////////
                            //                           END READING DATA SECTION
                            /////////////////////////////////////////////////////////////////////////////////



                            //check for // END or //END or //end - all are acceptable.
                            controlCard = input.next();
                            if (controlCard.equals("//")) {
                                controlCard = input.next();
                            } else if (controlCard.startsWith("//")) {
                                controlCard = controlCard.substring(2);
                            } else {
                                System.err.println("Error: // expected.  Instead found: " + controlCard);
                                System.exit(0);
                            }

                            if (!controlCard.equalsIgnoreCase("END")) {
                                System.err.println("Error: END expected.  Instead found: " + controlCard);
                                System.exit(0);
                            } else {
                                //finished reading a job
                                //endOfJob = true;
                                Queues.diskQueue.add(currJob);
                                //System.out.println(currJob);
                            }

                        }
                    }
                }
            }

            //currJob.trackingInfo.waitStartTime = System.currentTimeMillis();
        }

        //System.out.println (diskCounter);  //2027 lines.
        //for (PCB currPCB: diskQueue) {
        //    System.out.println(currPCB);
        //}

        /////////////////////////////////////////////////////////////////////////////////
        //                              END LOADER
        /////////////////////////////////////////////////////////////////////////////////


    }
}
