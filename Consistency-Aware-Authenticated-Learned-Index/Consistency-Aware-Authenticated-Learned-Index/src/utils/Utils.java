package utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Utils {
    //find the tar left bound
    public static int findLeftBound(long[] arr, long tar, int left, int right) {
        int l = left, r = right;
        while (l <= r) {
            int mid = (l + r) / 2;
            if (arr[mid] <= tar) l = mid + 1;
            else r = mid - 1;
        }
        return l - 1;
    }

    //find the tar right bound
    public static int findRightBound(long[] arr, long tar, int left, int right) {
        int l = left, r = right;
        while (l <= r) {
            int mid = (l + r) / 2;
            if (arr[mid] < tar) l = mid + 1;
            else r = mid - 1;
        }
        return r + 1;
    }

    public static List<Long> sortMerge(long[] arr, List<Long> list) {
        List<Long> mergedList = new ArrayList<>(arr.length + list.size());
        int i = 0, j = 0;
        while (i < arr.length || j < list.size()) {
            if (j >= list.size() || i < arr.length && arr[i] < list.get(j)) {
                mergedList.add(arr[i++]);
            } else {
                mergedList.add(list.get(j++));
            }
        }
        return mergedList;
    }

    public static long[] sortMerge(List<Long> list1, List<Long> list2) {
        long[] mergedList = new long[list1.size() + list2.size()];
        int pos = 0;
        int i = 0, j = 0;
        while (i < list1.size() || j < list2.size()) {
            if (j >= list2.size() || i < list1.size() && list1.get(i) < list2.get(j)) {
                mergedList[pos++] = list1.get(i++);
            } else {
                mergedList[pos++] = list2.get(j++);
            }
        }
        return mergedList;
    }

    public static long[] sortMerge(long[] arr1, long[] arr2) {
        long[] mergedList = new long[arr1.length + arr2.length];
        int pos = 0;
        int i = 0, j = 0;
        while (i < arr1.length || j < arr2.length) {
            if (j >= arr2.length || i < arr1.length && arr1[i] < arr2[j]) {
                mergedList[pos++] = arr1[i++];
            } else {
                mergedList[pos++] = arr2[j++];
            }
        }
        return mergedList;
    }

    public static long[] buildRandArr(int len, long low, long high, long[] dataset) {
        long[] arr = new long[len];
        HashSet<Long> st = new HashSet<>();
        if (dataset != null) for (long data: dataset) st.add(data);
        for (int i = 0; i < len; ++i) {
            long num = (long) (Math.random() * (high - low)) + low;
            while (st.contains(num)) {
                num = (int) (Math.random() * (high - low)) + low;
            }
            st.add(num);
            arr[i] = num;
        }
        return arr;
    }



    public static byte[] encPosHash(String sk, BigInteger r, byte[] hash, int pos) {
        return SHA.bytesXor(hash, SHA.hashToBytes(sk + r + pos));
    }

    public static void main(String[] args) {

        long[] arr = new long[]{51, 53};
        int leftBound = findLeftBound(arr, 60, 0, 1);
    }
}
