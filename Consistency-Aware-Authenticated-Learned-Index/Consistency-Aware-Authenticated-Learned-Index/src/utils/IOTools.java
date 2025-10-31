package utils;

import java.io.*;

public class IOTools {
    public static long low;
    public static long high;

    public static long[] readData(String filePath, int len) {
        low = Long.MAX_VALUE;
        high = Long.MIN_VALUE;
        long[] data = new long[len];
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            for (int i = 0; i < len && (line = reader.readLine()) != null; ++i) {
                data[i] = Long.parseLong(line);
                low = Math.min(data[i], low);
                high = Math.max(data[i], high);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static long getFileSize(String path) {
        FileInputStream fis  = null;
        try {
            File file = new File(path);
            fis = new FileInputStream(file);
            return fis.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
