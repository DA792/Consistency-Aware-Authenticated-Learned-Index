package index.learned_node_info;

import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PreTrain {

    public static long[] uniformSelection(long[] sortedArray, int n) {
        long[] selectedNumbers = new long[n];

        int arrayLength = sortedArray.length;
        int interval = arrayLength / n;
        int remainder = arrayLength % n;

        for (int i = 0; i < n; i++) {
            // Calculate the starting index for each interval
            int startIndex = i * interval + Math.min(i, remainder);

            // Select the middle element in each interval
            int selectedIndex = startIndex + interval / 2;
            selectedNumbers[i] = sortedArray[selectedIndex];
        }

        return selectedNumbers;
    }

    public static int trainSegLen(long[] trainData, int err) {
        OptPLA optPLA = new OptPLA(trainData, err);
        return optPLA.segmentList.size();
    }

    public static void preTrain(long[] dataset, int[] speed, int[] errRes) {

        int n = dataset.length;
        speed[0] = 1;

        for (int i = 1; i < n; ++i) {

            long[] selection = uniformSelection(dataset, i + 1);

            List<Integer> tmpSpeed = new ArrayList<>();
            int maxTag = -1;
            int bestErr = 0;

            int err = 1;
            int segLen;
            do {
                segLen = trainSegLen(selection, err);
                int v = speed[segLen - 1] + (int) (Math.floor(Math.log(2 * err) / Math.log(2)));

                if (maxTag == -1 || v > tmpSpeed.get(maxTag)) {
                    maxTag = tmpSpeed.size();
                    bestErr = err;
                }

                tmpSpeed.add(v);

                err++;
            } while (segLen > 1);

            speed[i] = tmpSpeed.get(maxTag);
            errRes[i] = bestErr;
        }
    }

    public static void main(String[] args) {
        long[] dataset = Utils.buildRandArr(1000, 1, 100000, null);
        Arrays.sort(dataset);

        int[] speed = new int[dataset.length];
        int[] errs = new int[dataset.length];

        preTrain(dataset, speed, errs);
    }

}
