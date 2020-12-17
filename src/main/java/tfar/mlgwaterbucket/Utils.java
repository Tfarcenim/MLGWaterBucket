package tfar.mlgwaterbucket;

import com.google.common.collect.AbstractIterator;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Utils {

    public static Stream<BlockPos> getAllInBox(AxisAlignedBB aabb) {
        return getAllInBox(MathHelper.floor(aabb.maxX),
                MathHelper.floor(aabb.maxY),
                MathHelper.floor(aabb.maxZ),
                MathHelper.floor(aabb.minX),
                MathHelper.floor(aabb.minY),
                MathHelper.floor(aabb.minZ));
    }

    public static Stream<BlockPos> getAllInBox(int maxX, int maxY, int maxZ, int minX, int minY, int minZ) {
        return StreamSupport.stream(getAllInBoxMutable(maxX, maxY, maxZ, minX, minY, minZ).spliterator(), false);
    }

    /**
     * Creates an Iterable that returns all positions in the box specified by the given corners. <strong>Coordinates must
     * be in order</strong>; e.g. x1 <= x2.
     *
     * This method uses {@link BlockPos.MutableBlockPos MutableBlockPos} instead of regular BlockPos, which grants better
     * performance. However, the resulting BlockPos instances can only be used inside the iteration loop (as otherwise
     * the value will change), unless {@link #toImmutable()} is called. This method is ideal for searching large areas
     * and only storing a few locations.
     *
     * @see #getAllInBox(BlockPos, BlockPos)
     * @see #getAllInBox(int, int, int, int, int, int)
     * @see #getAllInBoxMutable(BlockPos, BlockPos)
     */
    public static Iterable<BlockPos> getAllInBoxMutable(int x1, int y1, int z1, int x2, int y2, int z2) {
        int i = x1 - x2 + 1;
        int j = y1 - y2 + 1;
        int k = z1 - z2 + 1;
        int l = i * j * k;
        return () -> new AbstractIterator<BlockPos>() {
            private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();
            private int totalAmount;

            protected BlockPos computeNext() {
                if (this.totalAmount == l) {
                    return this.endOfData();
                } else {
                    int i1 = this.totalAmount % i;
                    int j1 = this.totalAmount / i;
                    int k1 = j1 % j;
                    int l1 = j1 / j;
                    ++this.totalAmount;
                    return this.mutablePos.setPos(x1 - i1, y1 - k1, z1 - l1);
                }
            }
        };
    }

}
