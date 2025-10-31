package index.learned_node_info;

import utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class OptPLA {
    List<Segment> segmentList = new ArrayList<>();
    List<long[]> lower;
    List<long[]> upper;
    int upperStart = 0;
    int lowerStart = 0;
    long firstX;
    int err;
    List<Long> segDataList;
    long[][] rectangle = new long[4][2];

    private void clear() {
        lower.clear();
        upper.clear();
        lowerStart = 0;
        upperStart = 0;
        rectangle = new long[4][2];
        segDataList = new ArrayList<>();
    }

    public OptPLA(int err) {
        this.err = err;
        lower = new ArrayList<>();
        upper = new ArrayList<>();
        segDataList = new ArrayList<>();
    }

    public void stop() {
        addRegToSegments();
    }

    public OptPLA(long[] dataset, int err) {
        this.err = err;
        lower = new ArrayList<>();
        upper = new ArrayList<>();
        segDataList = new ArrayList<>();
        for (long data : dataset) {
            addKey(data);
        }
        this.stop();
    }
    double getSlop(long[] p1, long[] p2) {
        return ((double) (p2[1] - p1[1])) / (p2[0] - p1[0]);
    }

    public Segment[] getSegments() {
        return this.segmentList.toArray(new Segment[segmentList.size()]);
    }

    public void addRegToSegments() {
        double[] slopAndIntercept = getSlopAndIntercept();
        Model model = new Model(slopAndIntercept[0], slopAndIntercept[1]);
        segmentList.add(new Segment(model, segDataList.stream().mapToLong(Long::longValue).toArray()));
        this.clear();
    }

    double[] getSlopAndIntercept() {
        if (segDataList.size() == 1) {
            return new double[] {0, (rectangle[0][1] + rectangle[1][1]) / 2.0};
        }
        double intercept, slop;
        double slop1 = getSlop(rectangle[2], rectangle[0]);
        double slop2 = getSlop(rectangle[3], rectangle[1]);


        slop = (slop1 + slop2) / 2;
        if (slop1 == slop2)
            intercept = rectangle[0][0] - rectangle[0][1] / slop;
        else {
            double tmp = slop2 - slop1;
            double x0 = (rectangle[0][1] - slop1 * rectangle[0][0] + slop2 * rectangle[1][0] - rectangle[1][1]) / tmp;
            double y0 = (slop1 * slop2 * (rectangle[1][0] - rectangle[0][0]) + rectangle[0][1] * slop2 - rectangle[1][1] * slop1) / tmp;
            intercept = x0 - y0 / slop;
        }
        return new double[]{slop, intercept};
    }

    double cross(long[] pointO, long[] pointA, long[] pointB) {
        return getSlop(pointB, pointO) - getSlop(pointA, pointO);
    }

    public void addKey(long key) {
        long x = key, y = segDataList.size();
        long[] p1 = new long[]{x, y + err};
        long[] p2 = new long[]{x, y - err};

        if (segDataList.size() == 0) {
            firstX = x;
            rectangle[0] = p1;
            rectangle[1] = p2;
            lower.clear();
            upper.add(p1);
            lower.add(p2);
            segDataList.add(key);
            return;
        }
        if (segDataList.size() == 1) {
            rectangle[2] = p2;
            rectangle[3] = p1;
            upper.add(p1);
            lower.add(p2);
            segDataList.add(key);
            return;
        }

        double slope1 = getSlop(rectangle[2], rectangle[0]);
        double slope2 = getSlop(rectangle[3], rectangle[1]);
        if (getSlop(p1, rectangle[2]) < slope1 || getSlop(p2, rectangle[3]) > slope2) {
            addRegToSegments();
            addKey(key);
            return;
        }

        if (getSlop(p1, rectangle[1]) < slope2) {
            double min = getSlop(lower.get(lowerStart), p1);
            int min_i = lowerStart;
            for (int i = lowerStart + 1; i < lower.size(); ++i) {
                double val = getSlop(lower.get(i), p1);
                if (val > min) break;
                min = val;
                min_i = i;
            }

            rectangle[1] = lower.get(min_i);
            rectangle[3] = p1;
            lowerStart = min_i;

            int end = upper.size();
            for (; end >= upperStart + 2 && cross(upper.get(end - 2), upper.get(end - 1), p1) <= 0; --end) {
                upper.remove(end - 1);
            }
            upper.add(p1);
        }

        if (getSlop(p2, rectangle[0]) > slope1) {
            double max = getSlop(upper.get(upperStart), p2);
            int max_i = upperStart;
            for (int i = upperStart + 1; i < upper.size(); ++i) {
                double val = getSlop(upper.get(i), p2);
                if (val < max) break;
                max = val;
                max_i = i;
            }

            rectangle[0] = upper.get(max_i);
            rectangle[2] = p2;
            upperStart = max_i;

            int end = lower.size();
            for (; end >= lowerStart + 2 && cross(lower.get(end - 2), lower.get(end - 1), p2) >= 0; --end) {
                lower.remove(end - 1);
            }
            lower.add(p2);
        }
        segDataList.add(key);
    }

    public static void main(String[] args) {
        int err = 5;
        long[] arr = Utils.buildRandArr(100, 1, 1000, null);
        OptPLA optPLA = new OptPLA(arr, err);
        System.out.println();
    }
}
