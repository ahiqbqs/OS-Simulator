import java.util.LinkedList;

public class ShortScheduler {

    //pick a job to run off of the Ready Queue.
    public static void schedule () {
        Queues.runningQueue.push(Queues.readyQueue.pop());
        //Queues.runningQueue.getFirst().status = PCB.state.RUNNING;
    }
}
