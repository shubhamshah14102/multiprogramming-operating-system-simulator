package ProcessScheduling;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

class RoundRobin {
    int NOP = 0, counter = 0;
    int[][] group;
    Scanner sc = new Scanner(System.in);
    int[] process, AT, BT, RT, CT, TAT, WT;
    int quantum = 0;
    int AWT = 0, ATAT = 0;
    Queue<Integer> queue = new LinkedList<>();

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
        System.out.println("Enter Quantum time");
        quantum = sc.nextInt();
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
        int completion = 0;
        while (counter > 0) {
            for (int i = 0; i < NOP; i++) {
                if (AT[i] > completion)
                    continue;
                if (RT[i] <= quantum && RT[i] > 0) {
                    completion += RT[i];
                    CT[i] = completion;
                    RT[i] = 0;
                    counter--;
                    TAT[i] = CT[i] - AT[i];
                    WT[i] = TAT[i] - BT[i];
                    ATAT += TAT[i];
                    AWT += WT[i];
                    queue.add(i);
                } else if (RT[i] > quantum && RT[i] > 0) {
                    completion += quantum;
                    RT[i] -= quantum;
                    queue.add(i);
                }
            }
        }
    }

    public void display() {
        System.out.println("Process\tArrival Time\tBurst Time\tCompletion Time\tTurnAround Time\tWaiting Time");
        for (int i = 0; i < NOP; i++)
            System.out.println("P" + process[i] + "\t" + AT[i] + "\t\t" + BT[i] + "\t\t" + CT[i] + "\t\t" + TAT[i]
                    + "\t\t" + WT[i]);
        displayGanttChart();
        System.out.println("\nAverage Turn Around time = " + (ATAT / (float) NOP));
        System.out.println("Average Waiting time = " + (AWT / (float) NOP));
    }

    public void displayGanttChart() {
        System.out.println("\n\n|==============================| GANTT CHART |==============================|\n");

        String first = "";
        String second = "";
        while (!queue.isEmpty()) {

            first += "| P" + queue.poll() + " |";
            second += "  " + quantum + "  ";
            first += " ==== ";
            second += "      ";
        }
        System.out.println(first);
        System.out.println(second);
        System.out.println("\n|==============================|=============|==============================|");
    }

    public static void main(String[] args) {
        RoundRobin rr = new RoundRobin();
        rr.acceptAT_BT();
        rr.calculate();
        rr.display();
    }
}
