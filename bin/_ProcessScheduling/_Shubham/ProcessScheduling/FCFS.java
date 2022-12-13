package ProcessScheduling;

import java.util.Arrays;
import java.util.Scanner;

public class FCFS {
    int[] process, AT, BT, CT, TAT, WT;
    Scanner sc = new Scanner(System.in);
    int count;
    double avgTAT = 0, avgWT = 0;
    int ScheduleLength = 0;
    int[][] group; // Store the collection sorted as per the AT
    // 0= Process, 1 =Arrival , 2= BT, 3= CT, 4= TAT, 5= ET

    public void acceptCount() {
        System.out.println("Enter number of elements");
        count = sc.nextInt();
        process = new int[count];
        AT = new int[count];
        BT = new int[count];
        CT = new int[count];
        TAT = new int[count];
        WT = new int[count];
        group = new int[count][6];
    }

    public void acceptAT_BT() {
        acceptCount();
        System.out.println("**** Please Enter the details **** \n");
        for (int i = 0; i < count; i++) {
            System.out.println("Enter AT and BT for Process : " + i);
            AT[i] = sc.nextInt();
            BT[i] = sc.nextInt();
            group[i][0] = i;
            group[i][1] = AT[i];
            group[i][2] = BT[i];
        }

        Arrays.sort(group, (a, b) -> a[1] - b[1]);
    }

    public void calculateCompletionTime() {
        int currentTime = 0;
        for (int i = 0; i < count; i++) {
            if (group[i][1] <= currentTime)
                currentTime = currentTime + group[i][2];
            else
                currentTime = group[i][1] + group[i][2];
            group[i][3] = currentTime;
        }
    }

    public void calculateTAT_WT_avgTAT_avgWT_() {
        for (int i = 0; i < count; i++) {
            group[i][4] = group[i][3] - group[i][1];
            avgTAT += group[i][4];
            group[i][5] = group[i][4] - group[i][2];
            avgWT += group[i][5];
        }
        avgTAT = (float) avgTAT / count;
        avgWT = (float) avgWT / count;
    }

    public void disPlayResults() {
        calculateCompletionTime();
        calculateTAT_WT_avgTAT_avgWT_();
        ScheduleLength = group[count - 1][3] - group[0][1];

        Arrays.sort(group, (a, b) -> a[0] - b[0]);

        System.out.println("Process | AT | BT | CT | TAT | WT");
        for (int i = 0; i < count; i++)
            System.out.println(
                    "P" + group[i][0] + "      | " + group[i][1] + "  | " + group[i][2] + " | " + group[i][3]
                            + " |  " + group[i][4] + " | " + group[i][5]);

        displayGanttChart();
        System.out.println("\n\nAverage Turn Around time = " + avgTAT);
        System.out.println("Average Waiting time = " + avgWT);
        System.out.println("Schedule Length = " + ScheduleLength);
        System.out.println("ThroughPut = " + ((float) count / ScheduleLength));
    }

    public void displayGanttChart() {
        System.out.print("+");
        for (int i = 0; i < count; i++)
            System.out.print("-----+");
    }

    public static void main(String[] args) {
        FCFS fcfs = new FCFS();
        fcfs.acceptAT_BT();
        fcfs.disPlayResults();
    }

}
