package tfar.mlgwaterbucket;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.*;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
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
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::doClientStuff);
    }

    public static final ItemStack water = Items.WATER_BUCKET.getDefaultInstance();

    public static boolean pickupWater;

    private void doClientStuff(TickEvent.RenderTickEvent event) {
        PlayerEntity player = Minecraft.getInstance().player;

        if (pickupWater && player.isInWater()) {
            ((MinecraftMixin) Minecraft.getInstance()).$rightClickMouse();
            pickupWater = false;
        }
        if (event.phase == TickEvent.Phase.END || player == null || player.isElytraFlying() || player.isInWater() || player.getMotion().y > -.4) {
            return;
        }

        double ground = distanceToGround(-player.getMotion().y);
        int threshold = ClientConfig.threshold.get();

        double fallDist = Minecraft.getInstance().player.fallDistance;

        if (fallDist < threshold) {
            return;
        }

        double eta = -ground/player.getMotion().y;

        //if there is more than 6 ticks to impact, do nothing
        if (eta > 6) {
            return;
        }

        //5 to 8 ticks before impact
        if (eta > 5) {
            //if already in hotbar, skip
            for (int i = 0; i < 9;i++) {
                if (isSupportedItem(player.inventory.mainInventory.get(i))) {
                    return;
                }
            }

            //attempt a shift click
            for (int i = 9; i < 36;i++) {
                if (isSupportedItem(player.inventory.mainInventory.get(i))) {
                    PlayerController playerController = Minecraft.getInstance().playerController;
                    Container container = player.container;
                    playerController.windowClick(container.windowId,i,0, ClickType.QUICK_MOVE,player);
                    Minecraft.getInstance().getConnection().sendPacket(new CCloseWindowPacket(container.windowId));
                    return;
                }
            }

        }

        //2.5 to 5 ticks
        if (eta > 3) {
            if (player.inventory.hasItemStack(water)) {
                if (holdingSupportedItem(player)) {
                    return;
                } else {
                    //hotbar is fast
                    for (int i = 0; i < 9;i++) {
                        if (isSupportedItem(player.inventory.mainInventory.get(i))) {
                            player.inventory.currentItem = i;
                            return;
                        }
                    }
                }
            } else {
                System.out.println("this might hurt");
            }
        }

        //1.5 to 2 ticks before impact
        if (eta > 2) {
            player.rotationPitch = 90;
            return;
        }

            if (holdingSupportedItem(player)) {
                ((MinecraftMixin) Minecraft.getInstance()).$rightClickMouse();
                pickupWater = true;
        }
    }

    public static boolean holdingSupportedItem(PlayerEntity player) {
        return isSupportedItem(player.getHeldItemMainhand()) || isSupportedItem(player.getHeldItemOffhand());
    }

    public static boolean isSupportedItem(ItemStack stack) {
        return stack.getItem() == Items.WATER_BUCKET/* || stack.getItem() == Items.SLIME_BLOCK*/;
    }

    private static double distanceToGround(double ySpeed) {
        PlayerEntity player = Minecraft.getInstance().player;

        AxisAlignedBB bb = player.getBoundingBox();

        AxisAlignedBB toCheck = new AxisAlignedBB(bb.minX,bb.minY - (ySpeed * 20),bb.minZ,bb.maxX,bb.maxY,bb.maxZ);

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

    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair2 = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair2.getRight();
        CLIENT = specPair2.getLeft();
    }


    public static class ClientConfig {

        public static ForgeConfigSpec.IntValue threshold;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            threshold = builder.
                    comment("This is the minimum distance required at which mlg saves will kick in")
                    .defineInRange("threshold",12,4,2047);
            builder.pop();
        }
    }


}
