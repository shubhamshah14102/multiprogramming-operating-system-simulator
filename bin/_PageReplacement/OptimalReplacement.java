package PageReplacement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class OptimalReplacement {

    // creation of the main class to implement Optimal page replacement algorithm
public static void main(pagestring[] args) throws IOException
{
Countbuffer bfr = new Countbuffer(new InputStreamReader(System.in));
int frames, pointer = 0, hit = 0, fault = 0,strng_size;
boolean isFull = false;
int buffer[];
int ref[];
int mem_layout[][];

//Entering the number of frames
System.out.println(" Enter the total number of Frames: ");
frames = Integer.parseInt(br.readLine());

//Entering the string size of the reference
System.out.println(" Enter the reference string size:");
strng_size = Integer.parseInt(br.readLine());

ref = new int[ref_len];
mem_layout = new int[strng_size][frames];
buffer = new int[frames];
for(int j = 0; j < frames; j++)
buffer[j] = -1;

//code to enter the reference string to carry out optimal page replacement
System.out.println(" Enter the reference string: ");
for(int i = 0; i < strng_size; i++)
{
ref[i] = Integer.parseInt(br.readLine());
}

System.out.println();
for(int i = 0; i < strng_size; i++)
{
int search = -1;
for(int j = 0; j < frames; j++)
{
if(buffer[j] == ref[i])
{
search = j;
hit++;
break;
}
}
// code to update the stack checking its capacity
if(search == -1)
{
if(isFull)
{
int index[] = new int[frames];
boolean index_flag[] = new boolean[frames];
for(int j = i + 1; j < ref_len; j++)
{
for(int k = 0; k < frames; k++)
{
if((ref[j] == buffer[k]) && (index_flag[k] == false))
{
index[k] = j;
index_flag[k] = true;
break;
}
}
}

//updating pointer to the correct memory location after checking capacity
buffer[pointer] = ref[i];
fault++;
if(!isFull)
{
pointer++;
if(pointer == frames)
{
pointer = 0;
isFull = true;
}
}
}
for(int j = 0; j < frames; j++)
mem_layout[i][j] = buffer[j];
}

// code to display the number strings
for(int i = 0; i < frames; i++)
{
for(int j = 0; j < ref_len; j++)
System.out.printf("%3d ",mem_layout[j][i]);
System.out.println();
}

System.out.println("Hits: " + hit);
System.out.println("Hit Ratio: " + (float)((float)hit/str_len));
System.out.println("Faults: " + fault);
}

}
