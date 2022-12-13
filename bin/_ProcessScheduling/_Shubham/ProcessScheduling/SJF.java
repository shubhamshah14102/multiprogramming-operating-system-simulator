package ProcessScheduling;

import java.util.*;

public class SJF {
    int NOP = 0, counter = 0;
    int[][] group;
    Scanner sc = new Scanner(System.in);
    int[] process, AT, BT, RT, CT, TAT, WT, f;
    int quantum = 0;
    int AWT = 0, ATAT = 0;

    public void acceptCount() {
        System.out.println("Enter number of Process");
        NOP = sc.nextInt();
        counter = NOP;
        process = new int[counter];
        AT = new int[counter];
        BT = new int[counter];
        RT = new int[counter];
        CT = new int[counter];
        TAT = new int[counter];
        WT = new int[counter];
        f = new int[counter]; // f means it is flag it checks process is completed or not
    }

    public void acceptAT_BT() {
        acceptCount();
        System.out.println("**** Please Enter the details **** \n");
        for (int i = 0; i < counter; i++) {
            System.out.println("Enter AT and BT for Process : " + (i + 1));
            process[i] = i;
            AT[i] = sc.nextInt();
            BT[i] = sc.nextInt();
            RT[i] = BT[i];
        }
    }

    public void calculate() {
        acceptAT_BT();
        int completion = 0;
        int tot = 0;
        while (true) {
            int min = Integer.MAX_VALUE;
            if (tot == NOP) // total no of process = completed process loop will be terminated
                break;
            for (int i = 0; i < NOP; i++) {
                /*
                 * If i'th process arrival time <= system time and its flag=0 and burst<min
                 * That process will be executed first
                 */
                if ((AT[i] <= completion) && (f[i] == 0) && (BT[i] < min)) {
                    min = BT[i];
                    counter = i;
                }
            }
            /*
             * If c==n means c value can not updated because no process arrival time< system
             * time so we increase the system time
             */
            if (counter == NOP)
                completion++;
            else {
                CT[counter] = completion + BT[counter];
                completion += BT[counter];
                TAT[counter] = CT[counter] - AT[counter];
                WT[counter] = TAT[counter] - BT[counter];
                ATAT += TAT[counter];
                AWT += WT[counter];
                f[counter] = 1;
                tot++;
            }
        }
    }

    public void display() {
        calculate();
        System.out.println("Process\tArrival Time\tBurst Time\tCompletion Time\tTurnAround Time\tWaiting Time");
        for (int i = 0; i < NOP; i++)
            System.out.println("P" + process[i] + "\t" + AT[i] + "\t\t" + BT[i] + "\t\t" + CT[i] + "\t\t" + TAT[i]
                    + "\t\t" + WT[i]);
        // displayGanttChart();
        System.out.println("\nAverage Turn Around time = " + (ATAT / (float) NOP));
        System.out.println("Average Waiting time = " + (AWT / (float) NOP));
    }

    public static void main(String args[]) {
        SJF sjf = new SJF();
        sjf.display();
    }
}