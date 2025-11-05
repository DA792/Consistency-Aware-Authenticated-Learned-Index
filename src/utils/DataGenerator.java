package utils;

import java.io.*;
import java.util.*;

/**
 * 二维空间数据生成器
 * 生成随机分布的二维点数据集
 */
public class DataGenerator {
    
    /**
     * 生成均匀分布的数据集
     * @param count 数据点数量
     * @param maxX X坐标最大值
     * @param maxY Y坐标最大值
     * @param outputPath 输出文件路径
     */
    public static void generateUniformData(int count, int maxX, int maxY, String outputPath) {
        System.out.println("生成均匀分布数据集...");
        System.out.println("数据点数: " + count);
        System.out.println("X范围: [0, " + maxX + "]");
        System.out.println("Y范围: [0, " + maxY + "]");
        
        Random random = new Random(42);  // 固定种子,保证可重复性
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // 写入CSV头
            writer.write("x,y\n");
            
            // 生成数据点
            for (int i = 0; i < count; i++) {
                long x = random.nextInt(maxX + 1);
                long y = random.nextInt(maxY + 1);
                writer.write(x + "," + y + "\n");
                
                // 进度显示
                if ((i + 1) % 100000 == 0) {
                    System.out.println("已生成: " + (i + 1) + " / " + count + " (" + 
                                     String.format("%.1f%%", (i + 1) * 100.0 / count) + ")");
                }
            }
            
            System.out.println("数据集生成完成!");
            System.out.println("保存路径: " + outputPath);
            
        } catch (IOException e) {
            System.err.println("生成数据集失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 生成高斯分布的数据集
     * @param count 数据点数量
     * @param centerX 中心点X坐标
     * @param centerY 中心点Y坐标
     * @param stdDev 标准差
     * @param outputPath 输出文件路径
     */
    public static void generateGaussianData(int count, double centerX, double centerY, 
                                           double stdDev, String outputPath) {
        System.out.println("生成高斯分布数据集...");
        System.out.println("数据点数: " + count);
        System.out.println("中心点: (" + centerX + ", " + centerY + ")");
        System.out.println("标准差: " + stdDev);
        
        Random random = new Random(42);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("x,y\n");
            
            for (int i = 0; i < count; i++) {
                long x = Math.max(0, (long)(centerX + random.nextGaussian() * stdDev));
                long y = Math.max(0, (long)(centerY + random.nextGaussian() * stdDev));
                writer.write(x + "," + y + "\n");
                
                if ((i + 1) % 100000 == 0) {
                    System.out.println("已生成: " + (i + 1) + " / " + count + " (" + 
                                     String.format("%.1f%%", (i + 1) * 100.0 / count) + ")");
                }
            }
            
            System.out.println("数据集生成完成!");
            System.out.println("保存路径: " + outputPath);
            
        } catch (IOException e) {
            System.err.println("生成数据集失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 生成混合分布的数据集 (多个簇)
     * @param count 数据点数量
     * @param numClusters 簇的数量
     * @param maxX X坐标最大值
     * @param maxY Y坐标最大值
     * @param outputPath 输出文件路径
     */
    public static void generateClusteredData(int count, int numClusters, 
                                            int maxX, int maxY, String outputPath) {
        System.out.println("生成簇状分布数据集...");
        System.out.println("数据点数: " + count);
        System.out.println("簇数量: " + numClusters);
        
        Random random = new Random(42);
        
        // 生成簇中心
        long[][] centers = new long[numClusters][2];
        for (int i = 0; i < numClusters; i++) {
            centers[i][0] = random.nextInt(maxX + 1);
            centers[i][1] = random.nextInt(maxY + 1);
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("x,y\n");
            
            for (int i = 0; i < count; i++) {
                // 随机选择一个簇
                int clusterIdx = random.nextInt(numClusters);
                long centerX = centers[clusterIdx][0];
                long centerY = centers[clusterIdx][1];
                
                // 在簇周围生成点
                double stdDev = Math.min(maxX, maxY) / (numClusters * 2.0);
                long x = Math.max(0, Math.min(maxX, 
                        (long)(centerX + random.nextGaussian() * stdDev)));
                long y = Math.max(0, Math.min(maxY, 
                        (long)(centerY + random.nextGaussian() * stdDev)));
                
                writer.write(x + "," + y + "\n");
                
                if ((i + 1) % 100000 == 0) {
                    System.out.println("已生成: " + (i + 1) + " / " + count + " (" + 
                                     String.format("%.1f%%", (i + 1) * 100.0 / count) + ")");
                }
            }
            
            System.out.println("数据集生成完成!");
            System.out.println("保存路径: " + outputPath);
            
        } catch (IOException e) {
            System.err.println("生成数据集失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("===== 二维空间数据集生成器 =====\n");
        
        // 生成50万点的均匀分布数据集
        System.out.println("生成 50万 数据点的均匀分布数据集");
        System.out.println("========================================\n");
        
        String outputPath = "src/data/uniform_500k.csv";
        generateUniformData(500000, 499999, 499999, outputPath);
        
        System.out.println("\n===== 数据集生成完成! =====");
        System.out.println("\n生成的数据集:");
        System.out.println("  - uniform_500k.csv  (50万点, 均匀分布)");
        System.out.println("\n文件位置: " + outputPath);
    }
}

