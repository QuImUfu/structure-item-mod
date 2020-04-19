package quimufu.structure_item;

import com.google.common.collect.Lists;
import com.mojang.datafixers.Dynamic;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.ForgeRegistry;

import javax.annotation.Nullable;
import java.util.List;

import static quimufu.structure_item.RandomS___Mod.LOGGER;

public class MyItem extends net.minecraft.item.Item {
    static Item.Properties p = (new Item.Properties()).group(ItemGroup.REDSTONE).maxStackSize(1);

    public MyItem() {
        super(p);
        setRegistryName("structure_item", "item");

    }

    @Override
    public void addInformation(ItemStack itemStack, @Nullable World world, List<ITextComponent> textComponents, ITooltipFlag tooltipFlag) {
        if (!itemStack.hasTag() || !itemStack.getTag().contains("structure", 8)) {
            textComponents.add((new TranslationTextComponent("item.structure_item.item.tooltip.tag.invalid")).applyTextStyle(TextFormatting.RED));
        } else {
            CompoundNBT tag = itemStack.getTag();
            if (tooltipFlag.isAdvanced()) {
                textComponents.add(new TranslationTextComponent("item.structure_item.item.tooltip.structure"));
                textComponents.add(new StringTextComponent("  " + tag.getString("structure")));
                if (tag.contains("allowedOn", 8)) {
                    textComponents.add(new TranslationTextComponent("item.structure_item.item.tooltip.allowed.on"));
                    textComponents.add(new StringTextComponent("  " + tag.getString("allowedOn")));
                }
                if (tag.contains("offset")) {
                    BlockPos offset = BlockPos.deserialize(new Dynamic<INBT>(NBTDynamicOps.INSTANCE, tag.get("offset")));
                    textComponents.add(new TranslationTextComponent("item.structure_item.item.tooltip.fixed.offset"));
                    TextComponent c = new TranslationTextComponent("item.structure_item.item.tooltip.xyz",
                            new StringTextComponent(String.valueOf(offset.getX())),
                            new StringTextComponent(String.valueOf(offset.getY())),
                            new StringTextComponent(String.valueOf(offset.getZ())));
                    textComponents.add(c);
                } else {
                    textComponents.add(new TranslationTextComponent("item.structure_item.item.tooltip.dynamic.offset"));
                }
                if (tag.contains("blacklist", 9)) {
                    textComponents.add(new TranslationTextComponent("item.structure_item.item.tooltip.blacklist"));
                    ListNBT bl = tag.getList("blacklist", 8);
                    int i = 0;
                    for (INBT entry : bl) {
                        textComponents.add(new StringTextComponent("  " + entry.getString()));
                        i++;
                        if (i == 4) {
                            textComponents.add(new TranslationTextComponent("item.structure_item.item.tooltip.blacklist.more",
                                    new StringTextComponent(String.valueOf(bl.size() - i))));
                        }
                    }
                }
            }
        }
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext c) {
        if (!c.getWorld().isRemote) {
            ServerPlayerEntity player;
            if (c.getPlayer() instanceof ServerPlayerEntity) {
                player = ((ServerPlayerEntity) c.getPlayer());
            } else {
                player = null;
            }
            CompoundNBT tag = c.getItem().getTag();
            if (tag == null) {
                TranslationTextComponent message =
                        new TranslationTextComponent("items.structure.spawner.no.tag");
                sendPlayerChat(player, message);
                return ActionResultType.FAIL;
            }
            Block allowed = null;
            if (tag.contains("allowedOn", 8)) {
                String allowedOn = tag.getString("allowedOn");
                allowed = getBlock(allowedOn);
                if (allowed == null) {
                    TranslationTextComponent message =
                            new TranslationTextComponent("items.structure.spawner.invalid.block",
                                    new StringTextComponent(allowedOn));
                    sendPlayerChat(player, message);
                    return ActionResultType.FAIL;
                }
            }
            Block current = c.getWorld().getBlockState(c.getPos()).getBlock();
            if (allowed != null && !current.equals(allowed)) {
                TextComponent currentName = new TranslationTextComponent(current.getTranslationKey());
                TextComponent allowedName = new TranslationTextComponent(allowed.getTranslationKey());
                TranslationTextComponent message =
                        new TranslationTextComponent("items.structure.spawner.invalid.block.clicked",
                                currentName, allowedName);
                sendPlayer(player, message);
                return ActionResultType.FAIL;
            }
            if (!tag.contains("structure", 8)) {
                LOGGER.info("No structure name set");
                TranslationTextComponent message =
                        new TranslationTextComponent("items.structure.spawner.no.structure");
                sendPlayerChat(player, message);
                return ActionResultType.FAIL;
            }
            String structureName = tag.getString("structure");
            ResourceLocation structureResourceID = ResourceLocation.tryCreate(structureName);
            if (structureResourceID == null) {
                TranslationTextComponent message =
                        new TranslationTextComponent("items.structure.spawner.invalid.structure.name");
                sendPlayerChat(player, message);
                return ActionResultType.FAIL;
            }
            Template x = ((ServerWorld) c.getWorld()).getStructureTemplateManager().getTemplate(structureResourceID);
            if (x == null) {
                TranslationTextComponent message =
                        new TranslationTextComponent("items.structure.spawner.structure.nonexistent",
                                new StringTextComponent(structureResourceID.toString()));
                sendPlayerChat(player, message);
                return ActionResultType.FAIL;
            }

            BlockPos loc = c.getPos().offset(c.getFace());
            if (tag.contains("offset")) {
                BlockPos offset = BlockPos.deserialize(new Dynamic<INBT>(NBTDynamicOps.INSTANCE, tag.get("offset")));
                loc = loc.add(offset);
            } else if (c.getPlayer() != null) {
                Direction direction = Direction.getFacingDirections(c.getPlayer())[0];
                BlockPos size = x.getSize();
                loc = loc.add(getDirectionalOffset(direction, size));
            } else {
                LOGGER.info("No player & no offset");
            }

            MyPlacementSettings ps = (new MyPlacementSettings());
            if (tag.contains("blacklist", 9)) {
                ListNBT bl = tag.getList("blacklist", 8);
                List<Block> blacklist = Lists.newArrayList();
                for (INBT b : bl) {
                    Block block = getBlock(b.getString());
                    if (block != null) {
                        blacklist.add(block);
                    } else {
                        TranslationTextComponent message =
                                new TranslationTextComponent("items.structure.spawner.invalid.block",
                                        new StringTextComponent(b.getString()));
                        sendPlayerChat(player, message);
                    }

                }
                ps.forbidOverwrite(blacklist);
            }
            ps.setWorld(c.getWorld())
                    .setSize(x.getSize())
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE)
                    .setChunk(null);
            boolean succes = x.addBlocksToWorld(c.getWorld(), loc, ps, 2);
            if (succes) {
                c.getItem().shrink(1);
                return ActionResultType.SUCCESS;
            }
            TranslationTextComponent message =
                    new TranslationTextComponent("items.structure.spawner.invalid.location");
            sendPlayer(player, message);
            return ActionResultType.FAIL;
        }
        return ActionResultType.FAIL;
    }

    private static void sendPlayer(ServerPlayerEntity player, TextComponent message) {
        if (player == null)
            return;
        ServerPlayNetHandler connection = player.connection;
        STitlePacket packet = new STitlePacket(STitlePacket.Type.SUBTITLE, message);
        connection.sendPacket(packet);
        packet = new STitlePacket(STitlePacket.Type.TITLE, new StringTextComponent(""));
        connection.sendPacket(packet);
    }

    private static void sendPlayerChat(ServerPlayerEntity player, TextComponent message) {
        if (player != null)
            player.sendMessage(message, ChatType.SYSTEM);
        LOGGER.info(message.getFormattedText());
    }

    private BlockPos getDirectionalOffset(Direction direction, BlockPos size) {
        BlockPos loc = new BlockPos(0, 0, 0);
        switch (direction) {
            case WEST:
                loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                loc = loc.offset(Direction.WEST, size.getX() - 1);
                break;
            case EAST: //positive x
                loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                break;
            case NORTH:
                loc = loc.offset(Direction.NORTH, size.getZ() - 1);
                loc = loc.offset(Direction.WEST, size.getX() / 2);
                break;
            case SOUTH: //positive z
                loc = loc.offset(Direction.WEST, size.getX() / 2);
                break;
            case UP:    //positive y
                loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                loc = loc.offset(Direction.WEST, size.getX() / 2);
                loc = loc.offset(Direction.UP);
                break;
            case DOWN:
                loc = loc.offset(Direction.NORTH, size.getZ() / 2);
                loc = loc.offset(Direction.WEST, size.getX() / 2);
                loc = loc.offset(Direction.DOWN, size.getY());
                break;
        }
        return loc;
    }

    private Block getBlock(String loc) {
        ResourceLocation location = ResourceLocation.tryCreate(loc);
        ForgeRegistry<Block> blocks = (ForgeRegistry<Block>) GameRegistry.findRegistry(Block.class);
        if (location == null || !blocks.containsKey(location)) {
            return null;
        }
        return GameRegistry.findRegistry(Block.class).getValue(location);
    }
}
