import java.util.concurrent.locks.ReentrantLock;

public class ReaderWriterProblem {
  private static ReentrantLock mutex = new ReentrantLock();
  private static int readers = 0;

  // Reader thread
  static class Reader implements Runnable {
    public void run() {
      mutex.lock();
      try {
        readers++;
      } finally {
        mutex.unlock();
      }

      // read data
      System.out.println("Reader reading data");

      mutex.lock();
      try {
        readers--;
      } finally {
        mutex.unlock();
      }
    }
  }

  // Writer thread
  static class Writer implements Runnable {
    public void run() {
      mutex.lock();
      try {
        while (readers > 0) {
          // wait until all readers have finished reading
        }

        // write data
        System.out.println("Writer writing data");
      } finally {
        mutex.unlock();
      }
    }
  }

  public static void main(String[] args) {
    Thread reader1 = new Thread(new Reader());
    Thread reader2 = new Thread(new Reader());
    Thread writer1 = new Thread(new Writer());

    reader1.start();
    reader2.start();
    writer1.start();
  }
}
