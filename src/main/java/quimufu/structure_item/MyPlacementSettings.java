package quimufu.structure_item;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class MyPlacementSettings extends PlacementSettings {
    private List<Block> blacklist;
    private World world;
    private BlockPos size;

    public MyPlacementSettings forbidOverwrite(List<Block> blocks) {
        if (blocks.size() == 0) {
            blacklist = null;
            return this;
        }
        blacklist = Lists.newArrayList(blocks);
        return this;
    }

    public MyPlacementSettings setWorld(World w) {
        world = w;
        return this;
    }

    public MyPlacementSettings setSize(BlockPos p) {
        size = p;
        return this;
    }

    @Override
    public List<Template.BlockInfo> func_227459_a_(List<List<Template.BlockInfo>> blocks, @Nullable BlockPos pos) {
        if (world == null || pos == null || size == null) {
            return super.func_227459_a_(blocks, pos);
        }

        List<List<Template.BlockInfo>> eligibleStructures = new ArrayList<>();
        if (blacklist == null) {
            eligibleStructures = blocks;
        } else {
            for (List<Template.BlockInfo> struct : blocks) {
                if (isValid(struct, pos)) {
                    eligibleStructures.add(struct);
                }
            }
        }
        if (eligibleStructures.size() == 0)
            setIgnoreEntities(true);
        List<Template.BlockInfo> locs = super.func_227459_a_(eligibleStructures, pos);
        if (!locs.isEmpty()) {
            List<Entity> entitiesWithinAABB = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos,pos.add(size)));
            for (Template.BlockInfo blockInfo : locs) {
                BlockPos posToClean = blockInfo.pos.add(pos);
                for (Entity e : entitiesWithinAABB) {
                    e.getBoundingBox().contains(new Vec3d(posToClean));
                    if (e.getBoundingBox().intersects(new AxisAlignedBB(posToClean))) {
                        e.remove();
                    }
                }
            }
        }
        return locs;
    }

    private boolean isValid(List<Template.BlockInfo> struct, BlockPos pos) {
        for (Template.BlockInfo bi : struct) {
            BlockPos posToCheck = bi.pos.add(pos);
            if (world.isBlockPresent(posToCheck)) {
                Block blockToCheck = world.getBlockState(posToCheck).getBlock();
                for (Block b : blacklist) {
                    if (blockToCheck.equals(b)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
