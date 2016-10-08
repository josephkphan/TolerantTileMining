package com.jp.Solver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class MinMaxSolver {

    //Data gathered from the database text file
    List<List<Integer>> database = new ArrayList<List<Integer>>();        //stores all the data transactions
    List<Integer> transaction_freq = new ArrayList<Integer>();            //stores the frequency of each transaction
    List<Integer> num_element_row = new ArrayList<Integer>();                //stores the number of elements in a transaction
    HashMap<Integer, Integer> item_freq = new HashMap<Integer, Integer>();    //hash number of occurrences of an item
    HashMap<Integer, List<Integer>> item_locations = new HashMap<Integer, List<Integer>>(); //contains all locations where a value appeared
    HashMap<Integer, Integer> sorted_item_location = new HashMap<Integer, Integer>();//gives location in sorted_items_list given item value
    ArrayList sorted;                                                        //helps sort hash map freqData
    List<Integer> sorted_items_list = new ArrayList<Integer>();            //contains a sorted list from highest to lowest based on frequency
    int num_items;                                                            //contains number of unique items
    int num_transactions;                                                    //contains number of transactions

    //Data used and changed during the search process
    int[] binary_array;                                                    //used to perform depth first search
    Stack<int[]> intersection = new Stack<int[]>();                        //stores number of intersection per transaction
    Stack<int[]> error = new Stack<int[]>();                                //stores number of error between set and a transaction
    Stack<int[]> check = new Stack<int[]>();                                //"1" represents removed, 0 means not removed
    Stack<List<Integer>> cur_item_freq = new Stack<List<Integer>>();    //contains updated frequency of all items

    //Final Results
    List<List<Integer>> optimal_sets = new ArrayList<List<Integer>>();    //set that will give the most output
    List<Double> top_points = new ArrayList<Double>();                    //used to find the best sets
    List<Integer> num_tc = new ArrayList<Integer>();                        //store the objective function value of the best sets
    int temp_tc;                                                            //store number of transactions contributing to current set

    //Parameters
    int num_k;                                                                //stores the number of top K sets user wants
    double EC;                                                                //Error Constant - used to calculate objective function value
    boolean rf;                                                            //determines where algorithm should prone and remove useless transactions
    boolean th;                                                            //true if algorithm uses a threshold
    double threshold;                                                        //contains value of threshold
    int choose_ub;                                                            //chooses the upper bound algorithm
    int min_size, max_size;                                                //contains set size range user wants to find optimal set
    int size_counter;


    /***********************************ONLY VALUES USERS NEED TO ADJUST******************************/
    public void User_Set_Parameters() {
        read_database("/home/jphan/workspace/optimize/data/freq/emea_unique.txt");    //File directory to database
        set_min_size(1);                            //User input Size desired for Set Must be >=1 and <=max_size
        set_max_size(20);                            //User input Size desired for Set Must be >=1 and >=min_size
        set_accuracy(50);                            //User input from 50 - 100 inclusive. 50 = error value = 1, 100 = no error accepted
        K_sets(1);                                    //User input for top K. if K=1, user is looking for the best
        set_threshold(false, 0);                    //user input if user select a threshold, input [true,(threshold value)], else false
        remove_freq(true);
        set_ub_method(3);
    }

    /***********************************ONLY VALUES USERS NEED TO ADJUST******************************/


    // Description: sets the min_size for sets. input should be >=1
    public void set_min_size(int x) {
        min_size = x;
    }

    public void set_max_size(int x) {
        max_size = x;
    }

    // Description: sets the error constant. Must take in an integer between 0 and 100 inclusive.
    public void set_accuracy(double x) {
        System.out.println("Percent Error Accuracy: " + x);
        if (x == 100) {
            EC = -1;
            return;
        }
        EC = x / (100 - x);
    }

    //Description: sets Threshold
    public void set_threshold(boolean b, int t) {
        threshold = t;
        th = b;
    }

    //Description: sets the amount of optimal sets the user wants
    public void K_sets(int x) {
        num_k = x;
        System.out.println("K= " + x);
    }

    //Description: Sets if user wants to remove frequency in search method
    public void remove_freq(boolean b) {
        if (b)
            System.out.println("Removing Freq");
        else
            System.out.println("Not Removing Freq");
        rf = b;
    }

    //Description: Sets the algorithm for the upperbound search
    public void set_ub_method(int ub) {
        choose_ub = ub;
        System.out.println("Upperbound Bound Method: " + ub);
    }

    //Description: initializes all necessary stacks and arrays
    public void initalize() {
        List<Integer> temp;
        sortArray();
        create_sorted_array();
        if (!th)
            for (int j = 0; j < num_k; j++) {
                top_points.add(0.0);
                temp = new ArrayList<Integer>();
                optimal_sets.add(temp);
                num_tc.add(0);
            }
        binary_array = new int[num_items];
        check.push(new int[num_transactions]);
        intersection.push(new int[num_transactions]);
        error.push(new int[num_transactions]);
        size_counter = 0;
    }

    /*Description: Performs Depth first search to find the optimal Solution	 */
    public void depth_first_search() {
        int current_point = 0;
        while (true) {
            current_point = increment_binary_array(current_point);
            current_point = backtrack_binary_array(current_point);
            if (current_point == -1)
                break;
        }
    }

    /* Description: does the "depth part" adding in new elements into the binary Array.
     *  After each addition, this will call the upper bound Function. THis function also  */
    public int increment_binary_array(int current_point) {
        double points;
        for (int i = current_point; i < num_items; i++) {
            binary_array[i] = 1;
            size_counter++;
            points = add(sorted_items_list.get(i));
            if (th) {
                if (points >= threshold)
                    makeOptimalSet(points);
            } else {
                if (points > top_points.get(num_k - 1))        //found a better optimal solution
                    makeOptimalSet(points);
            }
            update_freq(i);                            //removes rows that will never give a positive value
            switch (choose_ub) {                            //checks upper bound for remaining elements
                case 1:
                    if ((check_upper_bound1((i + 1), points)))
                        return i;
                    break;
                case 2:
                    if ((check_upper_bound2(i + 1, points)))
                        return i;
                    break;
                case 3:
                    if ((check_upper_bound3(i + 1)))
                        return i;
                    break;
            }
        }
        return (num_items - 2);                            //returns index of last element in array
    }

    /*Description: Helper to depth first search. This function is called after maximum depth is reached
     * and performs the width part of the search. updates Stack and returns the current index */
    public int backtrack_binary_array(int current_point) {
        while (true) {
            if (current_point < 0)
                return -1;
            if (binary_array[current_point] == 1)
                break;
            current_point--;
        }
        clear_part_of_binary_array(current_point);
        pop_stacks();
        return current_point + 1;
    }

    //Description: pops appropriate stacks for the depth first search
    public void pop_stacks() {
        intersection.pop();
        error.pop();
        cur_item_freq.pop();
        check.pop();
        size_counter--;
    }

    //Description: Used to set values to 0 from the given start point to the end of binary_array
    public void clear_part_of_binary_array(int start_point) {
        if (start_point < num_items)
            for (int i = start_point; i < num_items; i++)
                binary_array[i] = 0;
    }

    // Description: Called when a candidate surpasses current maximum objective function output. Saves the candidate in optimal_sets	 */
    public void makeOptimalSet(double x) {
        if (size_counter < min_size)
            return;
        List<Integer> temp = new ArrayList<Integer>();
        for (int i = 0; i < num_items; i++)
            if (binary_array[i] == 1)
                temp.add(sorted_items_list.get(i));
        if (th) {
            optimal_sets.add(temp);
            top_points.add(x);
            num_tc.add(temp_tc);
        } else {
            int index = num_k - 1;

            for (int i = 0; i < num_k; i++)
                if (top_points.get(i) < x) {
                    index = i;
                    break;
                }
            optimal_sets.add(index, temp);
            optimal_sets.remove(num_k);
            top_points.add(index, x);
            top_points.remove(num_k);
            num_tc.add(index, temp_tc);
            num_tc.remove(num_k);
        }
    }

    //Description: adds a new element to the set. updates stack and returns the objective function output
    public double add(int x) {
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
        for (int i = 0; i < num_transactions; i++) {            //calculates the object value function output
            cur_i[i] = prev_i[i];
            cur_e[i] = prev_e[i];
            rowPoints = 0;
            if (index < item_loc.size())
                compare = item_loc.get(index);
            else
                compare = num_transactions + 1;            // i will never reach this number i always < compare

            if (i < compare) {
                cur_e[i]++;                                // value not found in the row
            } else {
                index++;
                cur_i[i]++;                                //value was found
            }
            if (!(EC == -1 && cur_e[i] > 0))
                rowPoints += cur_i[i] - cur_e[i] * EC;
            points += Math.max(rowPoints * transaction_freq.get(i), 0);
            if (rowPoints > 0)
                temp_tc += transaction_freq.get(i);
        }
        intersection.push(cur_i);
        error.push(cur_e);
        return points;
    }

    /*Description: Removes rows and their corresponding elements and frequencies that can never
     * have a positive contribution	 */
    public void update_freq(int new_element_loc) {
        double numerator, denominator;
        int poss;
        int[] uni = intersection.peek();
        int[] err = error.peek();
        int[] oldChe = check.peek();
        int[] che = new int[num_transactions];
        List<Integer> cur = cur_item_freq.peek();
        List<Integer> newFreq = new ArrayList<Integer>();

        for (int i = 0; i < num_items; i++)                            //copies frequency array
            newFreq.add(cur.get(i));
        for (int i = 0; i < num_transactions; i++)                    //copies check array
            che[i] = oldChe[i];

        if (rf) {
            for (int i = 0; i < num_transactions; i++) {
                if ((che[i] == 0) && (EC == -1) && (err[i] > 0)) {
                    removeFreq(i, newFreq);
                    che[i] = 1;
                } else if ((che[i] == 0) && (uni[i] <= err[i] * EC)) {     //checks all rows with no contribution
                    poss = possible_points(new_element_loc, i);
                    numerator = uni[i] + poss;
                    denominator = uni[i] + err[i] * EC + poss;
                    if ((numerator * 2) < denominator) {         // checks if transaction will ever produce a positive contribution
                        che[i] = 1;                                 //makes sure row are not removed twice
                        removeFreq(i, newFreq);
                    }
                }
            }
        }
        cur_item_freq.push(newFreq);
        check.push(che);
    }

    //Description: Passes in Row that will never contribute. subtracts its frequencies from the curDataFreq.
    public void removeFreq(int x, List<Integer> new_freq) {
        int n_freq, index, t_freq;
        List<Integer> row = database.get(x);
        t_freq = transaction_freq.get(x);

        for (int i = 0; i < row.size(); i++) {
            index = sorted_item_location.get(row.get(i));
            n_freq = new_freq.get(index);
            n_freq -= t_freq;
            new_freq.set(index, n_freq);
        }
    }

    //Description: finds the number of elements can be added to increase the rows contribution
    public int possible_points(int new_element_loc, int x) {
        int poss_points = 0;
        List<Integer> current_transaction = database.get(x);
        for (int i = 0; i < current_transaction.size(); i++) {            //Possible points = items that can be added to set and increase its value
            if (new_element_loc < sorted_item_location.get(current_transaction.get(i)))
                poss_points++;
        }
        return poss_points;
    }


    /*Description: Sorts array by most contribution. Then finds best case scenario if search
     * continued down in "Depth". returns this value */
    public boolean check_upper_bound3(int x) {
        if (x >= num_items) {    //case: finished depth in depth first search. past array boundary
            pop_stacks();
            return false;
        }
        if (x + max_size - size_counter >= num_items)
            return true;
        int prev_ub, cur_ub, freq, counter, index, a, size;
        double its, e, f, poss;
        int[] cur_I, cur_E, ch, Freq;
        size = index = prev_ub = a = 0;
        List<Integer> cur = cur_item_freq.peek();
        cur_I = intersection.peek();
        cur_E = error.peek();
        ch = check.peek();
        for (int i = 0; i < num_transactions; i++)
            if (ch[i] == 0)
                size += transaction_freq.get(i);
        UB_data[] ub_array = new UB_data[size];
        Freq = new int[num_items - x];
        for (int i = x; i < num_items; i++)                    //gets frequencies needed for upper bound search and sorts them
            Freq[a++] = cur.get(i);
        a--;
        Arrays.sort(Freq);

        for (int i = 0; i < num_transactions; i++) {
            if (ch[i] == 0) {
                poss = possible_points(x - 1, i);
                its = cur_I[i];
                e = cur_E[i] * EC;
                f = transaction_freq.get(i);
                for (int j = 0; j < f; j++)
                    ub_array[index++] = new UB_data(its, e, poss);
            }
        }

        Arrays.sort(ub_array);
        int end = Math.min(num_items, x + max_size - size_counter);
        for (int i = x; i < end; i++) {
            counter = cur_ub = 0;
            freq = Freq[a--];
            for (int j = 0; j < size; j++) {
                if ((counter < freq) && ub_array[j].check_counter()) {
                    ub_array[j].increment_counter(1);
                    ub_array[j].increment_points(1);
                } else {
                    ub_array[j].decrement_points(EC);
                    ub_array[j].increment_error(1);
                }
                if (!(EC == -1 && (ub_array[j].get_error() > 0)))
                    cur_ub += Math.max(ub_array[j].get_points(), 0); //calculates the upper bound
            }
            if (cur_ub <= prev_ub)
                break;
            else
                prev_ub = cur_ub;
            if (th) {
                if (prev_ub >= threshold)
                    return false;
            } else if (prev_ub > top_points.get(num_k - 1))
                return false;

        }
        return true;
    }

    public boolean check_upper_bound2(int x, double cur_ub) {
        if (x >= num_items) {    //case: finished depth in depth first search. past array boundary
            pop_stacks();
            return false;
        }
        if (x + max_size - size_counter >= num_items)
            return true;

        int counter, index, a, size, poss;
        double its, e, f;
        int[] cur_I, cur_E, ch, Freq;
        size = index = a = 0;
        List<Integer> cur = cur_item_freq.peek();
        cur_I = intersection.peek();
        cur_E = error.peek();
        ch = check.peek();

        for (int i = 0; i < num_transactions; i++)
            if (ch[i] == 0)
                size += transaction_freq.get(i);
        double[] ub_array = new double[size];
        Freq = new int[num_items - x];
        for (int i = x; i < num_items; i++)                    //gets frequencies needed for upper bound search and sorts them
            Freq[a++] = cur.get(i);
        a--;
        Arrays.sort(Freq);

        int[] ec = new int[size];
        for (int i = 0; i < num_transactions; i++) {
            if (ch[i] == 0) {
                its = cur_I[i];
                e = cur_E[i] * EC;
                f = transaction_freq.get(i);
                for (int j = 0; j < f; j++) {
                    ub_array[index] = its - e;
                    if (EC == -1)
                        ec[index] = 1;
                    index++;
                }
            }
        }

        Arrays.sort(ub_array);
        for (int i = 0; i < max_size - size_counter; i++) {
            counter = 0;
            poss = Freq[a--];
            cur_ub = 0;
            for (int j = size - 1; j >= 0; j--) {
                if (counter++ < poss) {
                    ub_array[j]++;
                } else {
                    ub_array[j] -= EC;
                    ec[j] = 1;
                }
                if (!(EC == -1 && (ec[j] == 1)))
                    cur_ub += Math.max(ub_array[j], 0);
            }
            if (th) {
                if (cur_ub >= threshold)
                    return false;
            } else if (cur_ub > top_points.get(num_k - 1))
                return false;
        }
        return true;

    }

    public boolean check_upper_bound1(int x, double ub) {
        if (x >= num_items) {    //case: finished depth in depth first search. past array boundary
            pop_stacks();
            return false;
        }
        if (x + max_size - size_counter >= num_items)
            return true;
        int[] freq = new int[num_items - x];
        int a = 0;
        for (int i = x; i < num_items; i++)                    //gets frequencies needed for upper bound search and sorts them
            freq[a++] = cur_item_freq.peek().get(i);
        a--;
        Arrays.sort(freq);

        for (int i = 0; i < (max_size - size_counter); i++) {
            ub += freq[a--];
            if (th) {
                if (ub >= threshold)
                    return false;
            } else if (ub > top_points.get(num_k - 1))
                return false;
        }

        return true;
    }

    /* Description: Finds the frequency of every element and stores total frequency in freqData hash map.
     * stores local of every value in item_locations hash map	 */
    public void add_to_freq_array(int x, int i) {
        List<Integer> item_loc;
        if (item_freq.containsKey(x)) {                             //Value was found
            //increment frequency
            int value = item_freq.get(x);
            item_freq.put(x, value + transaction_freq.get(i));     //stores frequency of an item

            //update item_loc
            item_loc = item_locations.get(x);
            item_loc.add(i); //adds row index location
            item_locations.put(x, item_loc);
        } else {
            //Value was not found, create a new key
            item_freq.put(x, transaction_freq.get(i));
            item_loc = new ArrayList<Integer>();
            item_loc.add(i);
            item_locations.put(x, item_loc);
        }
    }

    //Description: sorts the hash map
    public void sortArray() {
        sorted = new ArrayList(item_freq.entrySet());

        Collections.sort(sorted, new Comparator() {
            public int compare(Object o1, Object o2) {
                Map.Entry e1 = (Map.Entry) o1;
                Map.Entry e2 = (Map.Entry) o2;
                Integer first = (Integer) e1.getValue();
                Integer second = (Integer) e2.getValue();
                return second.compareTo(first);
            }
        });

    }

    /*Description: translates the hash map into an
     * array. greatest frequency values closer to index 0	 */
    public void create_sorted_array() {
        Iterator i = sorted.iterator();
        int j = 0;
        List<Integer> cf = new ArrayList<Integer>();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            int val = (Integer) e.getKey();
            sorted_items_list.add(val);                            //stores items from highest frequency to lowest

            int freq = (Integer) e.getValue();
            cf.add(freq);
            sorted_item_location.put(val, j++);                    //stores location of an item in the sorted array
        }
        cur_item_freq.push(cf);                                    //saves total number of frequency for ever item
        num_items = sorted_items_list.size();                    //saves number of unique items
    }

    /*Description: Reads in data from text file and stores them in TwoDim array list.
     * each value is separated by a space, each row is separated by a new line */
    public void read_database(String fileName) {
        BufferedReader br = null;
        int freq, arraySize;
        System.out.println("Reading File:" + fileName);

        try {

            String sCurrentLine;
            try {
                br = new BufferedReader(new FileReader(fileName));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            while ((sCurrentLine = br.readLine()) != null) {
                String[] dataSet = sCurrentLine.split(" ");
                arraySize = dataSet.length;
                num_element_row.add(arraySize - 1);            // stores the number of elements per row
                List<Integer> row = new ArrayList<Integer>();
                freq = Integer.parseInt(dataSet[0]);
                transaction_freq.add(freq);                    //stores number of times each transaction appears
                for (int i = 1; i < arraySize; i++) {
                    row.add(Integer.parseInt(dataSet[i]));
                    add_to_freq_array(Integer.parseInt(dataSet[i]), transaction_freq.size() - 1); //stores transaction
                }
                database.add(row);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        num_transactions = transaction_freq.size();            //saves the number of transactions
    }


    //****************************Printing Methods For Debugging*************************************//


    //Description: converts the binary array into the set candidate and prints it out for user to see
    public void print_current_set() {
        List<Integer> test_set = new ArrayList<Integer>();
        for (int i = 0; i < num_items; i++)
            if (binary_array[i] == 1)
                test_set.add(sorted_items_list.get(i));
        print_ArrayList(test_set, "\nTesting Set: ");
    }

    //prints out database from text file
    public void print_database() {
        System.out.println("Database ArrayList:");
        for (int i = 0; i < database.size(); i++) {
            List row = database.get(i);
            for (int j = 0; j < row.size(); j++)
                System.out.println(" " + row.get(j));
            System.out.println("");
        }
    }

    //prints out the sorted frequency hash map
    public void print_sorted_item_list() {
        Iterator i = sorted.iterator();
        while (i.hasNext())
            System.out.println((Map.Entry) i.next());
    }

    //prints out hashmap - checking is storing index for elements work
    public void print_hash_indexes(int x) {
        List<Integer> item_loc;
        item_loc = item_locations.get(x);
        System.out.println("check hash index:");
        for (int i = 0; i < item_loc.size(); i++)
            System.out.println(item_loc.get(i));
    }

    //prints out given array with string as a header title
    public void print_array(int[] a, String s) {
        System.out.println(s);
        for (int i = 0; i < a.length; i++)
            System.out.println(a[i]);
    }

    //prints out a given arrayList with string as a header title
    public void print_ArrayList(List<Integer> L, String s) {
        System.out.println(s);
        for (int i = 0; i < L.size(); i++)
            System.out.print(L.get(i) + " ");
    }

    //prints out optimal set with its object function output
    public void print_optimal_sets() {
        for (int i = 0; i < optimal_sets.size(); i++) {
            print_ArrayList(optimal_sets.get(i), "Set " + (i + 1) + ":");
            System.out.println("\nObjective Function Value: " + top_points.get(i));
            System.out.println("# Transactions Contributing: " + num_tc.get(i));
            System.out.println("Size: " + optimal_sets.get(i).size() + "\n");
        }
        System.out.println("Number of Sets: " + optimal_sets.size());
    }
}

