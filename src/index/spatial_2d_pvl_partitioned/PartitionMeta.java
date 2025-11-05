package index.spatial_2d_pvl_partitioned;

/**
 * 分区元数据
 * 用于快速确定Z区间属于哪个分区
 */
public class PartitionMeta {
    public final int partitionId;
    public final long zMin;
    public final long zMax;
    
    public PartitionMeta(int partitionId, long zMin, long zMax) {
        this.partitionId = partitionId;
        this.zMin = zMin;
        this.zMax = zMax;
    }
    
    /**
     * 检查Z值是否在该分区范围内
     */
    public boolean contains(long z) {
        return z >= zMin && z <= zMax;
    }
    
    /**
     * 检查Z区间是否与该分区重叠
     */
    public boolean overlaps(long zStart, long zEnd) {
        return !(zEnd < zMin || zStart > zMax);
    }
    
    @Override
    public String toString() {
        return String.format("Partition[id=%d, zMin=%d, zMax=%d]", 
                           partitionId, zMin, zMax);
    }
}

