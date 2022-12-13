    import java.util.concurrent.Semaphore;

public class ProducerConsumer {
    static Semaphore semProd = new Semaphore(1);
    static Semaphore semCons = new Semaphore(0);

    static class Producer extends Thread {
        @Override
        public void run() {
            try {
                semProd.acquire();
                System.out.println("Producer acquired the semaphore");

                // produce something

                semCons.release();
                System.out.println("Producer released the semaphore");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class Consumer extends Thread {
        @Override
        public void run() {
            try {
                semCons.acquire();
                System.out.println("Consumer acquired the semaphore");

                // consume something

                semProd.release();
                System.out.println("Consumer released the semaphore");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Producer().start();
        new Consumer().start();
    }
}
