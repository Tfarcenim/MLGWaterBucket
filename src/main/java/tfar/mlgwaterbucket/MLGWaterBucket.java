package tfar.mlgwaterbucket;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import tfar.mlgwaterbucket.mixin.MinecraftMixin;

import java.util.List;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MLGWaterBucket.MODID)
public class MLGWaterBucket
{
    // Directly reference a log4j logger.

    public static final String MODID = "mlgwaterbucket";

    public MLGWaterBucket() {
        MinecraftForge.EVENT_BUS.addListener(this::doClientStuff);
    }

    public static final ItemStack water = Items.WATER_BUCKET.getDefaultInstance();

    public static int threshold = 12;

    private void doClientStuff(TickEvent.RenderTickEvent event) {
        PlayerEntity player = Minecraft.getInstance().player;
        if (event.phase == TickEvent.Phase.END || player == null || player.isElytraFlying() || player.isInWater() || player.getMotion().y > -2) {
            return;
        }

        double ground = distanceToGround();

        if (ground > threshold) {
            return;
        }

        if (ground > 8) {
            //hotbar is fast
            for (int i = 0; i < 9;i++) {
                if (player.inventory.mainInventory.get(i).getItem() == Items.WATER_BUCKET) {
                    return;
                }
            }

            for (int i = 9; i < 36;i++) {
                if (player.inventory.mainInventory.get(i).getItem() == Items.WATER_BUCKET) {
                    PlayerController playerController = Minecraft.getInstance().playerController;
                    Container container = player.container;
                    playerController.windowClick(container.windowId,i,0, ClickType.QUICK_MOVE,player);
                    return;
                }
            }

        }

        if (ground > 5.5) {
            if (player.inventory.hasItemStack(water)) {
                if (player.getHeldItemMainhand().getItem() == Items.WATER_BUCKET || player.getHeldItemOffhand().getItem() == Items.WATER_BUCKET) {
                    return;
                } else {
                    //hotbar is fast
                    for (int i = 0; i < 9;i++) {
                        if (player.inventory.mainInventory.get(i).getItem() == Items.WATER_BUCKET) {
                            player.inventory.currentItem = i;
                            return;
                        }
                    }
                }
            } else {
                System.out.println("this might hurt");
            }
        }

        if (ground > 2.5) {
            player.rotationPitch = 90;
            return;
        }

            if (player.getHeldItemMainhand().getItem() == Items.WATER_BUCKET || player.getHeldItemOffhand().getItem() == Items.WATER_BUCKET) {
                ((MinecraftMixin) Minecraft.getInstance()).$rightClickMouse();
        }
    }

    private static double distanceToGround() {
        PlayerEntity player = Minecraft.getInstance().player;

        AxisAlignedBB bb = player.getBoundingBox();

        AxisAlignedBB toCheck = new AxisAlignedBB(bb.minX,bb.minY - threshold,bb.minZ,bb.maxX,bb.maxY,bb.maxZ);

        List<BlockPos> list = Utils.getAllInBox(toCheck).map(BlockPos::toImmutable).collect(Collectors.toList());

        for (BlockPos pos : list) {
            BlockState state = player.world.getBlockState(pos);
            VoxelShape shape1 = state.getCollisionShape(player.world,pos, ISelectionContext.forEntity(player));

            shape1 = shape1.withOffset(pos.getX(),pos.getY(),pos.getZ());

            VoxelShape playerShape = VoxelShapes.create(toCheck);

            if (!VoxelShapes.combine(shape1,playerShape, IBooleanFunction.AND).isEmpty()) {
                return player.getPosY() - pos.getY();
            }
        }
        return 255;
    }
}
