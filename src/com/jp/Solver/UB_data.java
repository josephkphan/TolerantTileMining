package com.jp.Solver;

/**
 * Created by jphan on 10/7/16.
 */
public class UB_data implements Comparable<UB_data>{
    double point;			//intersection - error
    double counter;			//used to keep track of points given during upper bound process
    double possible;		//max amount of points a transaction can increase by
    double error;			//used for case EC = -1 (100% accuracy)

    public UB_data(double i, double e, double m){
        error = e;
        point = i-e;
        counter = 0;
        possible = m;
    }

    public double get_points(){return point;}
    public double get_counter(){return counter;}
    public double get_possible(){return possible;}
    public double get_error(){return error;}

    public boolean check_counter(){ return (counter < possible); }
    public void increment_points(double x){point+=x;}
    public void decrement_points(double x){point-=x;}
    public void increment_counter(double x){counter+=x;}
    public void increment_error(double x){error+=x;}

    public void print(){
        System.out.println("point: " + point + ", counter: " + counter + ", possible: " + possible +  ", error: " + error);
    }

    @Override
    public int compareTo(UB_data other) {
        return -1 * Double.valueOf(this.point).compareTo(other.point);
    }
}
