
public class ShortScheduler {

    //select a free CPU.
    public static void schedule () {

        try {
            Integer currFreeCPU = Queues.freeCpuQueue.take();
            Dispatcher.dispatch(Driver.cpu[currFreeCPU]);

        } catch (InterruptedException ie) {
            System.err.println(ie.toString());
        }

    }
}
