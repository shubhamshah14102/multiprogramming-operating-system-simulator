package PageReplacement;

// Code for FIFO page replacement algorithm in java

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

class FIFO {
    static int pageFaults(int incomingStream[], int n, int frames) {
        System.out.println("Incoming \t Pages");
        // Using Hashset to quickly check if a given
        // incoming stream item in set or not
        HashSet s = new HashSet<>(frames);

        // Queue created to store pages in FIFO manner
        // since set will not store order or entry
        // we will use queue to note order of entry of incoming page
        Queue queue = new LinkedList<>();

        int page_faults = 0;

        for (int i = 0; i < n; i++) {
            // if set has lesser item than frames
            if (s.size() < frames) {
                // If incoming item is not present, add to set
                if (!s.contains(incomingStream[i])) {
                    s.add(incomingStream[i]);
                    page_faults++;

                    // Push the incoming page into the queue
                    queue.add(incomingStream[i]);

                }
            }

            // If the set is full then we need to do page replacement
            // in FIFO manner that is remove first item from both
            // set and queue then insert incoming page
            else {
                // If incoming item is not present
                if (!s.contains(incomingStream[i])) {
                    // remove the first page from the queue
                    int val = (int) queue.peek();

                    // remove from queue
                    queue.poll();

                    // Remove from set
                    s.remove(val);

                    // insert incoming page to set
                    s.add(incomingStream[i]);

                    // push incoming page to queue
                    queue.add(incomingStream[i]);
                    page_faults++;

                }
            }
            // printing happens here
            System.out.print(incomingStream[i] + "\t");
            System.out.print(queue + " \n");
        }

        return page_faults;
    }

    public static void main(String args[]) {
        int incomingStream[] = { 7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1 };
        int frames = 3;

        int len = incomingStream.length;
        int pageFaults = pageFaults(incomingStream, len, frames);
        int hit = len - pageFaults;

        System.out.println("Page faults: " + pageFaults);
        System.out.println("Page fault Ratio: " + (double) pageFaults / len);
        System.out.println("Hits: " + hit);
        System.out.println("Hit Ratio : " + (double) hit / len);
    }
}