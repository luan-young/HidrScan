package com.example.luan.hidrscan.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luan on 12/03/18.
 */

public class TupleUtil {

    static List<Tuple<Integer, Integer>> Generate2Permutations(int n) {
        List<Tuple<Integer, Integer>> perms = new ArrayList<Tuple<Integer, Integer>>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    perms.add(new Tuple<Integer, Integer>(i, j));
                }
            }
        }
        return perms;
    }

    static List<Tuple<Integer, Integer>> Generate2Combinations(int n) {
        List<Tuple<Integer, Integer>> combs = new ArrayList<Tuple<Integer, Integer>>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                combs.add(new Tuple<Integer, Integer>(i, j));
            }
        }
        return combs;
    }
}

class Tuple<X, Y> {
    public final X x;
    public final Y y;

    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }
}
