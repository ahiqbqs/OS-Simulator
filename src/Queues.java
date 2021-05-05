import java.util.LinkedList;

public class Queues {

    static LinkedList<PCB> diskQueue;
    static LinkedList<PCB> readyQueue;
    static LinkedList<PCB> waitingQueue;      //not used yet

    //runningQueue: 0 or 1 items currently running.
    //(this is kind of a workaround for Java, which does not allow pass by reference -
    //hence we need a container (runningQueue) for the PCB of the job currently running)
    static LinkedList<PCB> runningQueue;

    //for reporting purposes, to track complete jobs
    static LinkedList<PCB> doneQueue;

    static public void initQueues () {
        diskQueue = new LinkedList<>();
        readyQueue = new LinkedList<>();
        waitingQueue = new LinkedList<>();      //not used yet
        runningQueue = new LinkedList<>();
        doneQueue = new LinkedList<>();
    }
}
