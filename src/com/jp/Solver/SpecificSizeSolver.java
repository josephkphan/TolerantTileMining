package com.jp.Solver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by jphan on 10/8/16.
 */
public class SpecificSizeSolver {
    /*
 * Program written by: Joseph Phan
 * File Name: optimize
 * File Description: Given a Database, this program will find the top K sets under the given error accuracy
 * that gives the greatest objective function outputs:
 *
 * How data is stored:
 * 		Each row is a transaction. The first element of a row will represent the frequency of occurrence of
 * 		that transaction. Each item is separated by a space. There will no spaces in the beginning or end of
 * 		a transaction.
 *
 * Terminology:
 * item = one value or element from a transaction
 * transaction = one set of items or a "row" in the database.
 * possible points = number of elements in a transaction that can possibly contribute to increase the value
 *
 * Process:
 * 1) Reads in database from a text file and saves the data needed. note that data must
 *    be stored in a certain format. first value is number of occurrences of the transactions
 *    followed by a space and then the transaction is next. each item should be separated by a space
 *
 * 2) Saves data from the text file - frequency of each item, location each item occurs, number of transactions
 *    and number of unique values and then sort all items highest number of occurrences to lowest number of
 *    occurrences in an array
 *
 * 3) Performs Depth first search type algorithm and proning to search through possible sets
 *
 * 4) At each addition of a new element, the search process first finds the new objective function output
 * 	  determines which transactions will never improve the objective function output and removes them. Then performs
 * 	  an upper bound calculation to determine whether or not the search process should continue down in a depth manner
 *
 * 5) After search complete, this program will display the results.
 */


import java.io.*;
import java.util.*;
    public class tiling {

        //Data gathered from the database text file
        List<List<Integer>> database = new ArrayList<List<Integer>>(); 		//stores all the data transactions
        List<Integer> transaction_freq = new ArrayList<Integer>(); 			//stores the frequency of each transaction
        List<Integer> num_element_row= new ArrayList<Integer>();				//stores the number of elements in a transaction
        HashMap<Integer,Integer> item_freq = new HashMap <Integer,Integer>();	//hash number of occurrences of an item
        HashMap<Integer, List<Integer>> item_locations =  new HashMap<Integer, List<Integer>>(); //contains all locations where a value appeared
        HashMap<Integer,Integer>sorted_item_location = new HashMap <Integer,Integer>();//gives location in sorted_items_list given item value
        ArrayList sorted; 														//helps sort hash map freqData
        List<Integer> sorted_items_list = new ArrayList<Integer>(); 			//contains a sorted list from highest to lowest based on frequency
        int num_items;															//contains number of unique items
        int num_transactions;													//contains number of transactions

        //Data used and changed during the search process
        Stack<int[]> intersection = new Stack<int[]>(); 						//stores number of intersection per transaction
        Stack<int[]> error = new Stack<int[]>();								//stores number of error between set and a transaction
        Stack<int[]> check = new Stack<int[]>(); 								//"1" represents removed, 0 means not removed
        Stack< List<Integer>> cur_item_freq = new Stack< List<Integer>>();  	//contains updated frequency of all items
        int current_point;
        double set_points;
        boolean ub_flag;
        List<Integer> current_set= new ArrayList<Integer>();

        //Final Results
        List<List<Integer>> optimal_sets = new ArrayList<List<Integer>>(); 	//set that will give the most output
        List<Double> top_points = new ArrayList<Double>(); 					//used to find the best sets
        List<Integer>num_tc = new ArrayList<Integer>();						//store the objective function value of the best sets
        int temp_tc;

        //Parameters
        int num_k;																//stores the number of top K sets user wants
        double EC;																//Error Constant - used to calculate objective function value
        boolean th;															//true if algorithm uses a threshold
        double threshold;														//contains value of threshold
        int specified_size;													//contains minimum size
        int size_counter;														//contains current set size






        /***********************************ONLY VALUES USERS NEED TO ADJUST******************************/
        public void User_Set_Parameters(){
            read_database("/home/jphan/workspace/optimize/data/freq/emea_unique.txt");	//File directory to database
            set_size(9);								//User input Size desired for Set Must be >=1
            set_accuracy(50);							//User input from 50 - 100 inclusive. 50 = error value = 1, 100 = no error accepted
            K_sets(1);									//User input for top K. if K=1, user is looking for the best
            set_threshold(false,0 );					//user input if user select a threshold, input [true,(threshold value)], else false
        }
        /***********************************ONLY VALUES USERS NEED TO ADJUST******************************/






        public static void main(String[] args) {
            long startTime = System.currentTimeMillis();
            tiling O = new tiling();
            O.User_Set_Parameters();
            O.initalize();
            //O.print_startup();						//prints out ordered list of items based on frequencies
            O.depth_first_search();
            O.print_optimal_sets();
            long endTime = System.currentTimeMillis();
            System.out.println("Total execution time: " + (endTime-startTime) + "specified_size" + "\n");
        }
        /*********************************SETTING PARAMETERS***************************************/
        // Description: sets the min_size for sets. input should be >=1
        public void set_size(int x){
            specified_size = x;
            System.out.println("Set Size Specified: " + x);
        }

        // Description: sets the error constant. should take in an integer between 50 and 100 inclusive.
        public void set_accuracy(double x){
            System.out.println("Percent Error Accuracy: " + x);
            if(x==100){
                EC=-1;
                return;
            }
            EC = x/(100-x);
        }
        //Description: sets Threshold
        public void set_threshold(boolean b, int t){
            threshold = t;
            th = b;
            if(th)
                System.out.println("Threshold Set at: " + threshold);
        }

        //Description: sets the amount of optimal sets the user wants
        public void K_sets(int x){
            num_k = x;
            System.out.println("Looking for top " + x + " set(s)");
        }

        //Description: initializes all necessary stacks and arrays
        public void initalize(){
            List<Integer> temp;
            sortArray();
            create_sorted_array();
            if(!th)
                for(int j=0; j<num_k; j++){
                    top_points.add(0.0);
                    temp = new ArrayList<Integer>();
                    optimal_sets.add(temp);
                    num_tc.add(0);
                }
            check.push( new int[num_transactions]);
            intersection.push( new int[num_transactions]);
            error.push( new int[num_transactions]);
            size_counter = 0;
        }

        public void print_startup(){
            print_sorted_item_list();
        }

        /********************************************Algorithm*******************************************/

	/*Description: Performs Depth first search to find the optimal Solution	 */
        public void depth_first_search(){
            set_points = 0;
            current_point = 0;
            while(current_point >=0 && current_point < num_items){
                ub_flag=false;
                if(current_set.size()==1 && upperbound(current_point))
                    break;
                if(fill_set_until_full())
                    break;
                if(!ub_flag)
                    last_item_check();

                backtrack();
            }
        }

        //Description: Fills the current set until the the current set size is the specified size
        //If it cannot fill the current set, then the algorithm is completed
        public boolean fill_set_until_full(){
            while(current_set.size()<specified_size){
                if(current_point >= num_items)
                    return true;
                set_points = add(sorted_items_list.get(current_point));
                update_freq(current_point);
                if(upperbound(current_point +1)){
                    ub_flag=true;
                    return false;
                }
                current_point++;
            }
            check_current_set();
            //print_current_set();
            return false;
        }
        //Description: removes the last item and adds in the next one. Repeats this process
        //until least frequent item is added or upper bound is smaller than current max
        public void last_item_check(){
            for(current_point=current_point; current_point<num_items; current_point++){
                pop_stacks();
                if(upperbound(current_point+1)){
                    ub_flag = true;
                    break;
                }

                set_points = add(sorted_items_list.get(current_point));
                update_freq(current_point);
                //print_current_set();
                check_current_set();
            }
        }
        //Description: removes items from the set and finds next part where the fill_set_until_full
        //should start
        public void backtrack(){
            if(current_set.size()==0){	//Algorithm is finished
                current_point = num_items+1;
                return;
            }
            if(!ub_flag){
                int temp_index1, temp_index2;
                while(current_set.size()>=2){
                    temp_index1 = sorted_item_location.get(current_set.get(current_set.size()-1)) - 1;
                    temp_index2 = sorted_item_location.get(current_set.get(current_set.size()-2));
                    if(temp_index1==temp_index2){
                        pop_stacks();
                    }else
                        break;

                }
                if(current_set.size()==1)
                    current_point = sorted_item_location.get(current_set.get(current_set.size()-1)) + 1;
                else{
                    current_point = sorted_item_location.get(current_set.get(current_set.size()-2)) + 1;
                    pop_stacks();
                }

            }else{// Means Upper bound was lower then current top k
                current_point = sorted_item_location.get(current_set.get(current_set.size()-1)) + 1;
                if(current_point + specified_size - current_set.size()>=num_items && current_set.size()>=2){
                    // Makes sure doesnt run into scenario where the algoritvuse there would not be a possible item after
                    current_point = sorted_item_location.get(current_set.get(current_set.size()-2)) + 1;
                    pop_stacks();
                }
            }
            pop_stacks();
        }

        //Description: Used to compare current set with either the current top k or threshold
        public void check_current_set(){
            if(th){										//user wanted to find a threshold
                if(set_points>=threshold)				//checks current set with threshold
                    makeOptimalSet();
            }else{
                if(set_points>top_points.get(num_k-1))	//found a solution better than the top k
                    makeOptimalSet();
            }
        }

        //Used when optimal set is found. copies set to save it in top k
        public List<Integer> copy_set(List<Integer> src){
            List<Integer> temp = new ArrayList<Integer>();
            for(int i=0; i<src.size(); i++)
                temp.add(src.get(i));
            return temp;
        }

        //Description: removes the last layer of history added from the most recent item from the set
        public void pop_stacks(){
            intersection.pop();
            error.pop();
            cur_item_freq.pop();
            check.pop();
            current_set.remove(current_set.size()-1);
        }



        // Description: Called when a candidate surpasses current maximum objective function output. Saves the candidate in optimal_sets	 */
        public void makeOptimalSet(){
            if(th){												//user asked for Threshold
                optimal_sets.add(copy_set(current_set));
                top_points.add(set_points);
                num_tc.add(temp_tc);
            }else{
                int index=num_k-1;

                for(int i=0;i<num_k;i++)						//Finds the where the item should be added
                    if(top_points.get(i)<set_points){
                        index = i;
                        break;
                    }
                optimal_sets.add(index, copy_set(current_set));
                optimal_sets.remove(num_k);
                top_points.add(index, set_points);
                top_points.remove(num_k);
                num_tc.add(index,temp_tc);
                num_tc.remove(num_k);
            }
        }

        //Description: adds a new element to the set. updates stack and returns the objective function output
        public double add(int x){

            current_set.add(x);
            List<Integer> item_loc;
            int[] prev_i, prev_e;
            int index, compare;
            double points, rowPoints;
            points = index = 0;
            prev_i = intersection.peek();
            prev_e = error.peek();
            int[] cur_i = new int[num_transactions];
            int[] cur_e = new int[num_transactions];
            item_loc = item_locations.get(x);
            temp_tc = 0;
            for (int i=0; i<num_transactions; i++){			//calculates the object value function output
                cur_i[i]= prev_i[i];
                cur_e[i]= prev_e[i];
                rowPoints=0;
                if(index < item_loc.size())
                    compare = item_loc.get(index);
                else
                    compare = num_transactions+1; 			// i will never reach this number i always < compare

                if(i<compare){
                    cur_e[i]++;								// value not found in the row
                }else{
                    index++;
                    cur_i[i]++;								//value was found
                }
                if( !(EC== -1 && cur_e[i]>0) )
                    rowPoints += cur_i[i] - cur_e[i]* EC;
                points += Math.max(rowPoints * transaction_freq.get(i), 0);
                if(rowPoints>0)
                    temp_tc+=transaction_freq.get(i);
            }
            intersection.push(cur_i);
            error.push(cur_e);
            return points;
        }
        //Finds upper bound by adding the next top x frequencies to the current set points
        //x is the number of items that can still be added to the set (until it reached max size)
        public boolean upperbound(int x){
            double upperbound = set_points;
            if(x+specified_size-current_set.size()>=num_items)
                return false;

            int[] freq = new int[num_items - x];
            int a = 0;
            for(int i=x; i<num_items;i++)					//gets frequencies needed for upper bound search and sorts them
                freq[a++] = cur_item_freq.peek().get(i);
            a--;
            Arrays.sort(freq);

            for(int i=0; i<(specified_size-current_set.size());i++)
                upperbound+=freq[a--];						//adds top x frequencies to current set's objective function value

            if(th){
                if(upperbound <= threshold)					//compares upper bound with threshold or top k
                    return true;
            }else{
                if(upperbound < top_points.get(num_k-1))
                    return true;
            }
            return false;
        }


        /*Description: Removes rows and their corresponding elements and frequencies that can never
         * have a positive contribution	 */
        public void update_freq(int new_element_loc){
            double numerator, denominator;
            int poss;
            int[] uni = intersection.peek();
            int[] err = error.peek();
            int[] oldChe = check.peek();
            int[] che = new int[num_transactions];
            List<Integer> cur = cur_item_freq.peek();
            List<Integer> newFreq =  new ArrayList<Integer>();

            for (int i=0; i<num_items;i++)							//copies frequency array
                newFreq.add(cur.get(i));
            for(int i=0; i<num_transactions;i++)					//copies check array
                che[i] = oldChe[i];

            for(int i=0; i<num_transactions; i++){
                if( (che[i]==0) && (EC==-1) && (err[i]>0)){
                    removeFreq(i,newFreq);
                    che[i]=1;
                }else if( (che[i]==0) && (uni[i]<=err[i]*EC) ){	 //checks all rows with no contribution
                    poss = possible_points(new_element_loc,i);
                    numerator = uni[i] + poss;
                    denominator = uni[i] + err[i]*EC + poss;
                    if( (numerator * 2) <= denominator){ 		 // checks if transaction will ever produce a positive contribution
                        che[i]=1;								 //makes sure row are not removed twice
                        removeFreq(i, newFreq);
                    }
                }
            }

            cur_item_freq.push(newFreq);
            check.push(che);
        }

        //Description: Passes in Row that will never contribute. subtracts its frequencies from the curDataFreq.
        public void removeFreq(int x, List<Integer> new_freq){
            int n_freq, index, t_freq;
            List<Integer> row = database.get(x);
            t_freq = transaction_freq.get(x);

            for(int i=0; i<row.size(); i++){					//Deducts the frequencies of the elements in a non-contributing transaction
                index = sorted_item_location.get(row.get(i));
                n_freq = new_freq.get(index);
                n_freq -= t_freq;
                new_freq.set(index, n_freq);
            }
        }

        //Description: finds the number of elements can be added to increase the rows contribution
        public int possible_points(int new_element_loc, int x){
            int poss_points = 0;
            List<Integer> current_transaction = database.get(x);
            for(int i=0; i<current_transaction.size();i++){			//Possible points = items that can be added to set and increase its value
                if(new_element_loc < sorted_item_location.get(current_transaction.get(i)))
                    poss_points++;
            }
            return poss_points;
        }


        /*******************************************Reading Files and Set up**********************************************/

	/* Description: Finds the frequency of every element and stores total frequency in freqData hash map.
	 * stores local of every value in item_locations hash map	 */
        public void add_to_freq_array(int x, int i){
            List<Integer> item_loc;
            if(item_freq.containsKey(x)){ 							 //Value was found
                //increment frequency
                int value = item_freq.get(x);
                item_freq.put(x, value+ transaction_freq.get(i));	 //stores frequency of an item

                //update item_loc
                item_loc = item_locations.get(x);
                item_loc.add(i); //adds row index location
                item_locations.put(x, item_loc);
            }else{
                //Value was not found, create a new key
                item_freq.put(x, transaction_freq.get(i));
                item_loc = new ArrayList<Integer>();
                item_loc.add(i);
                item_locations.put(x, item_loc);
            }
        }

        //Description: sorts the hash map
        public void sortArray(){
            sorted = new ArrayList( item_freq.entrySet());

            Collections.sort( sorted , new Comparator() {
                public int compare( Object o1, Object o2)
                {
                    Map.Entry e1 = (Map.Entry)o1;
                    Map.Entry e2 = (Map.Entry)o2;
                    Integer first = (Integer)e1.getValue();
                    Integer second = (Integer)e2.getValue();
                    return second.compareTo(first);
                }
            });

        }

        /*Description: translates the hash map into an
         * array. greatest frequency values closer to index 0	 */
        public void create_sorted_array(){
            Iterator i = sorted.iterator();
            int j = 0;
            List<Integer> cf = new ArrayList<Integer>();
            while(i.hasNext()){
                Map.Entry e = (Map.Entry)i.next();
                int val = (Integer) e.getKey();
                sorted_items_list.add(val);							//stores items from highest frequency to lowest

                int freq = (Integer) e.getValue();
                cf.add(freq);
                sorted_item_location.put(val, j++);					//stores location of an item in the sorted array
            }
            cur_item_freq.push(cf);									//saves total number of frequency for ever item
            num_items = sorted_items_list.size();					//saves number of unique items
        }

        /*Description: Reads in data from text file and stores them in TwoDim array list.
         * each value is separated by a space, each row is separated by a new line */
        public void read_database(String fileName){
            BufferedReader br = null;
            int freq, arraySize;
            System.out.println("Reading File:"+ fileName);

            try {

                String sCurrentLine;
                br = new BufferedReader(new FileReader(fileName));

                while ((sCurrentLine = br.readLine()) != null) {
                    String[] dataSet = sCurrentLine.split(" ");
                    arraySize=dataSet.length;
                    num_element_row.add(arraySize-1); 			// stores the number of elements per row
                    List<Integer> row = new ArrayList<Integer>();
                    freq = Integer.parseInt(dataSet[0]);
                    transaction_freq.add(freq);					//stores number of times each transaction appears
                    for(int i=1; i<arraySize; i++){
                        row.add(Integer.parseInt(dataSet[i]));
                        add_to_freq_array(Integer.parseInt(dataSet[i]), transaction_freq.size()-1); //stores transaction
                    }
                    database.add(row);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (br != null)br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            num_transactions = transaction_freq.size();			//saves the number of transactions
        }



        /*****************************Printing Methods For Debugging*************************************/


        //Description: converts the binary array into the set candidate and prints it out for user to see
        public void print_current_set(){
            print_ArrayList(current_set, "\nTesting Set: ");
            System.out.println("");
            System.out.println("Set points: " + set_points);
            //System.out.println("Set Size" +current_set.size());
        }
        //prints out database from text file
        public void print_database(){
            System.out.println("Database ArrayList:");
            for(int i=0; i < database.size(); i++){
                List row= database.get(i);
                for (int j=0; j<row.size();j++ )
                    System.out.println(" "+row.get(j));
                System.out.println("");
            }
        }
        //prints out the sorted frequency hash map
        public void print_sorted_item_list(){
            Iterator i = sorted.iterator();
            while(i.hasNext())
                System.out.println((Map.Entry)i.next() );
        }
        //prints out hashmap - checking is storing index for elements work
        public void print_hash_indexes(int x){
            List<Integer> item_loc;
            item_loc = item_locations.get(x);
            System.out.println("check hash index:");
            for (int i=0; i<item_loc.size(); i++)
                System.out.println(item_loc.get(i));
        }
        //prints out given array with string as a header title
        public void print_array(int[] a, String s){
            System.out.println(s);
            for (int i=0; i<a.length; i++)
                System.out.println(a[i]);
        }
        //prints out a given arrayList with string as a header title
        public void print_ArrayList(List<Integer> L, String s){
            System.out.println(s);
            for (int i=0; i<L.size(); i++)
                System.out.print(L.get(i) + " ");
        }
        //prints out optimal set with its object function output
        public void print_optimal_sets(){
            for(int i=0; i < optimal_sets.size(); i++){
                print_ArrayList(optimal_sets.get(i), "Set " + (i+1) + ":");
                System.out.println("\nObjective Function Value: " + top_points.get(i));
                System.out.println("# Transactions Contributing: " + num_tc.get(i));
                System.out.println("Size: " +optimal_sets.get(i).size()+ "\n");
            }
            System.out.println("Number of Sets: "+ optimal_sets.size());
        }
    }


}
