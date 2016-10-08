package com.jp.Solver;

public class Main {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        Optimizer O = new Optimizer();
        O.User_Set_Parameters();
        O.initalize();
        O.depth_first_search();
        O.print_optimal_sets();
        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime-startTime) + "ms");
    }
}
