public class DiningPhilosophers {
    private static final int NUM_PHILOSOPHERS = 5;
    private static final Semaphore[] forks = new Semaphore[NUM_PHILOSOPHERS];

    public static void main(String[] args) {
        // Initialize the semaphores representing the forks
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            forks[i] = new Semaphore(1);
        }

        // Create the philosophers and start them
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            Philosopher p = new Philosopher(i);
            p.start();
        }
    }

    private static class Philosopher extends Thread {
        private final int id;

        public Philosopher(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // Think for a while
                    Thread.sleep(500);

                    // Pick up the left fork
                    forks[id].acquire();
                    System.out.println("Philosopher " + id + " picked up left fork.");

                    // Pick up the right fork
                    forks[(id + 1) % NUM_PHILOSOPHERS].acquire();
                    System.out.println("Philosopher " + id + " picked up right fork.");

                    // Eat
                    System.out.println("Philosopher " + id + " is eating.");
                    Thread.sleep(1000);

                    // Put down the forks
                    forks[id].release();
                    System.out.println("Philosopher " + id + " put down left fork.");
                    forks[(id + 1) % NUM_PHILOSOPHERS].release();
                    System.out.println("Philosopher " + id + " put down right fork.");
                } catch (InterruptedException e) {
                    // Handle the exception
                }
            }
        }
    }
}
