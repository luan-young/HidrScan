package com.example.luan.hidrscan.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by luan on 12/03/18.
 */

public class SortArgMinMax {

    static int argmax(double[] arr) {
        int idx = 0;
        double max = arr[idx];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
                idx = i;
            }
        }
        return idx;
    }

    static Integer[] argsort(final double[] array) {
        Integer[] arrRet = new Integer[array.length];
        for (int i = 0; i < arrRet.length; i++) {
            arrRet[i] = i;
        }

        Arrays.sort(arrRet, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(array[o1], array[o2]);
            }
        });

        return arrRet;
    }
}
