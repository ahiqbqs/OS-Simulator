import java.util.LinkedList;

public class ShortScheduler {

    //pick a job to run off of the Ready Queue.
    public static void Schedule (LinkedList<PCB> readyQueue, LinkedList<PCB> runningQueue) {
        runningQueue.push(readyQueue.pop());
        runningQueue.getFirst().setStatus(PCB.state.RUNNING);
    }
}
