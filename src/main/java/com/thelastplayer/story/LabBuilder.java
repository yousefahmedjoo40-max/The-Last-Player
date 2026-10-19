package com.thelastplayer.story;

import com.thelastplayer.registry.ModBlocks;
import com.thelastplayer.registry.ModItems;
import com.thelastplayer.block.LabTerminalBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Builds the secret room, the two archive bunkers and the abandoned lab. */
public final class LabBuilder {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    // ------------------------------------------------------------------ geometry helpers
    public static BlockPos labCore(BlockPos o) { return new BlockPos(o.getX(), o.getY() + 2, o.getZ() + 32); }
    public static BlockPos labHallCenter(BlockPos o) { return new BlockPos(o.getX(), o.getY() + 2, o.getZ() + 7); }
    public static BlockPos labEntrance(BlockPos o, int surfaceY) { return new BlockPos(o.getX(), surfaceY + 1, o.getZ() + 3); }

    public static boolean insideHall(BlockPos p, BlockPos o) {
        return p.getX() >= o.getX() - 6 && p.getX() <= o.getX() + 6
                && p.getZ() >= o.getZ() + 1 && p.getZ() <= o.getZ() + 13
                && p.getY() >= o.getY() && p.getY() <= o.getY() + 6;
    }

    public static int groundY(ServerWorld w, int x, int z) {
        w.getChunk(x >> 4, z >> 4);   // force-load, otherwise getHeight returns sea level
        return w.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    public static BlockPos surfaceAbove(ServerWorld w, BlockPos p) {
        return new BlockPos(p.getX(), groundY(w, p.getX(), p.getZ()) + 1, p.getZ());
    }

    /** Finds a land spot between minD and maxD blocks from "around". y = ground block. */
    public static BlockPos pickSpot(ServerWorld w, BlockPos around, int minD, int maxD) {
        Random r = new Random();
        BlockPos last = around;
        for (int i = 0; i < 16; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            int dist = minD + r.nextInt(Math.max(1, maxD - minD));
            int x = around.getX() + (int) (Math.cos(ang) * dist);
            int z = around.getZ() + (int) (Math.sin(ang) * dist);
            int g = groundY(w, x, z);
            last = new BlockPos(x, g, z);
            Biome.Category cat = w.getBiome(last).getBiomeCategory();
            if (w.getBlockState(last).getFluidState().isEmpty() && cat != Biome.Category.OCEAN
                    && cat != Biome.Category.RIVER && g > 55) {
                return last;
            }
        }
        return last;
    }

    // ------------------------------------------------------------------ block builder
    private static final class B {
        final ServerWorld w;
        final Random r;
        final List<Long> lights = new ArrayList<>();

        B(ServerWorld w, long seed) { this.w = w; this.r = new Random(seed); }

        BlockState wall() {
            int n = r.nextInt(100);
            if (n < 72) return Blocks.STONE_BRICKS.defaultBlockState();
            if (n < 86) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }

        void set(int x, int y, int z, BlockState s) { w.setBlock(new BlockPos(x, y, z), s, 2); }

        void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                        set(x, y, z, s);
        }

        void shell(int x1, int y1, int z1, int x2, int y2, int z2) {
            for (int x = x1; x <= x2; x++)
                for (int y = y1; y <= y2; y++)
                    for (int z = z1; z <= z2; z++)
                        set(x, y, z, wall());
        }

        void air(int x1, int y1, int z1, int x2, int y2, int z2) { fill(x1, y1, z1, x2, y2, z2, AIR); }

        /** hollow room: interior x1..x2, z1..z2, y0+1..y0+h ; floor at y0, ceiling at y0+h+1 */
        void room(int x1, int z1, int x2, int z2, int y0, int h) {
            shell(x1 - 1, y0, z1 - 1, x2 + 1, y0 + h + 1, z2 + 1);
            air(x1, y0 + 1, z1, x2, y0 + h, z2);
            fill(x1, y0, z1, x2, y0, z2, Blocks.POLISHED_ANDESITE.defaultBlockState());
        }

        void light(int x, int y, int z) {
            set(x, y, z, Blocks.SEA_LANTERN.defaultBlockState());
            lights.add(new BlockPos(x, y, z).asLong());
        }

        void terminal(int x, int y, int z, Direction facing) {
            set(x, y, z, ModBlocks.LAB_TERMINAL.get().defaultBlockState().setValue(LabTerminalBlock.FACING, facing));
        }

        void chest(int x, int y, int z, Direction facing, ItemStack... items) {
            set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
            TileEntity te = w.getBlockEntity(new BlockPos(x, y, z));
            if (te instanceof ChestTileEntity) {
                for (int i = 0; i < items.length; i++) ((ChestTileEntity) te).setItem(i, items[i]);
            }
        }

        /** surface hut + ladder shaft down to a room whose ceiling is at ceilY. Shaft column = (cx, cz+2). */
        void entrance(int cx, int cz, int g, int y0, int ceilY) {
            // hut
            shell(cx - 2, g, cz + 1, cx + 2, g + 4, cz + 5);
            air(cx - 1, g + 1, cz + 2, cx + 1, g + 3, cz + 4);
            fill(cx - 1, g, cz + 2, cx + 1, g, cz + 4, Blocks.POLISHED_ANDESITE.defaultBlockState());
            air(cx, g + 1, cz + 5, cx, g + 2, cz + 5);            // doorway
            // shaft shell between ceiling and ground
            shell(cx - 1, ceilY, cz + 1, cx + 1, g - 1, cz + 3);
        }

        void finishShaft(int cx, int cz, int g, int y0, int ceilY) {
            air(cx, ceilY, cz + 2, cx, g, cz + 2);
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            for (int y = y0 + 1; y <= g + 3; y++) set(cx, y, cz + 2, ladder);
        }
    }

    // ------------------------------------------------------------------ secret room (the device)
    public static BlockPos buildSecretRoom(ServerWorld w, int cx, int cz) {
        int g = groundY(w, cx, cz);
        int y0 = Math.max(10, g - 12);
        B b = new B(w, cx * 31L + cz);
        b.room(cx - 3, cz - 3, cx + 3, cz + 3, y0, 3);
        Block lantern = Blocks.SOUL_LANTERN;
        b.set(cx - 2, y0 + 1, cz - 2, lantern.defaultBlockState());
        b.set(cx + 2, y0 + 1, cz - 2, lantern.defaultBlockState());
        b.set(cx - 2, y0 + 1, cz + 2, lantern.defaultBlockState());
        b.set(cx + 2, y0 + 1, cz + 2, lantern.defaultBlockState());
        b.set(cx - 3, y0 + 3, cz - 3, Blocks.COBWEB.defaultBlockState());
        b.set(cx + 3, y0 + 3, cz + 3, Blocks.COBWEB.defaultBlockState());
        b.fill(cx - 3, y0 + 1, cz + 3, cx - 1, y0 + 2, cz + 3, Blocks.BOOKSHELF.defaultBlockState());
        b.set(cx, y0 + 1, cz, ModBlocks.ECHO_DEVICE.get().defaultBlockState());
        return new BlockPos(cx, y0 + 1, cz);
    }

    // ------------------------------------------------------------------ archive bunker (day 2)
    /** returns surface position of the entrance hut */
    public static BlockPos buildBunker(ServerWorld w, int cx, int cz, boolean letterBunker) {
        int g = groundY(w, cx, cz);
        int y0 = Math.max(12, g - 10);
        int ceil = y0 + 4;
        B b = new B(w, cx * 17L + cz);
        b.entrance(cx, cz, g, y0, ceil);
        b.room(cx - 4, cz + 2, cx + 4, cz + 8, y0, 3);
        b.finishShaft(cx, cz, g, y0, ceil);
        b.light(cx - 3, ceil, cz + 5);
        b.light(cx + 3, ceil, cz + 5);
        b.terminal(cx - 4, y0 + 1, cz + 5, Direction.EAST);
        b.terminal(cx + 4, y0 + 1, cz + 5, Direction.WEST);
        if (letterBunker) {
            b.chest(cx, y0 + 1, cz + 8, Direction.NORTH, new ItemStack(ModItems.OLD_LETTER.get()));
        } else {
            b.chest(cx, y0 + 1, cz + 8, Direction.NORTH, StoryItems.logBook());
        }
        return new BlockPos(cx, g, cz + 3);
    }

    // ------------------------------------------------------------------ the lab (day 3)
    /** returns origin (cx, y0, cz). Fills data.labLights. */
    public static BlockPos buildLab(ServerWorld w, int cx, int cz, StoryData d) {
        int g = groundY(w, cx, cz);
        int y0 = Math.max(12, g - 14);
        int ceil = y0 + 5;
        B b = new B(w, cx * 13L + cz);

        b.entrance(cx, cz, g, y0, ceil);

        // hall (interior x -5..5, z 2..12)
        b.room(cx - 5, cz + 2, cx + 5, cz + 12, y0, 4);
        // records room west of hall
        b.room(cx - 16, cz + 4, cx - 7, cz + 10, y0, 4);
        // hidden cell behind records room
        b.room(cx - 20, cz + 6, cx - 18, cz + 8, y0, 3);
        // corridor to final room
        b.room(cx - 1, cz + 14, cx + 1, cz + 24, y0, 3);
        // final room
        b.room(cx - 6, cz + 26, cx + 6, cz + 38, y0, 6);

        // openings
        b.air(cx - 6, y0 + 1, cz + 7, cx - 6, y0 + 3, cz + 7);                 // hall -> records
        b.air(cx - 1, y0 + 1, cz + 13, cx + 1, y0 + 3, cz + 13);               // hall -> corridor
        b.air(cx - 1, y0 + 1, cz + 25, cx + 1, y0 + 3, cz + 25);               // corridor -> final
        b.finishShaft(cx, cz, g, y0, ceil);

        // hall decor
        b.light(cx - 4, ceil, cz + 5); b.light(cx + 4, ceil, cz + 5);
        b.light(cx - 4, ceil, cz + 10); b.light(cx + 4, ceil, cz + 10);
        b.terminal(cx - 5, y0 + 1, cz + 4, Direction.EAST);
        b.terminal(cx + 5, y0 + 1, cz + 7, Direction.WEST);
        b.terminal(cx - 3, y0 + 1, cz + 12, Direction.NORTH);
        b.terminal(cx + 3, y0 + 1, cz + 12, Direction.NORTH);
        b.fill(cx + 5, y0 + 1, cz + 9, cx + 5, y0 + 3, cz + 11, Blocks.IRON_BLOCK.defaultBlockState());
        b.set(cx - 5, y0 + 4, cz + 2, Blocks.COBWEB.defaultBlockState());
        b.set(cx + 5, y0 + 4, cz + 12, Blocks.COBWEB.defaultBlockState());

        // records room
        b.light(cx - 9, y0 + 5, cz + 7); b.light(cx - 14, y0 + 5, cz + 7);
        b.fill(cx - 15, y0 + 1, cz + 4, cx - 8, y0 + 2, cz + 4, Blocks.BOOKSHELF.defaultBlockState());
        b.terminal(cx - 16, y0 + 1, cz + 6, Direction.EAST);
        b.terminal(cx - 16, y0 + 1, cz + 8, Direction.EAST);
        b.chest(cx - 8, y0 + 1, cz + 10, Direction.NORTH,
                new ItemStack(ModItems.OLD_LETTER.get()), StoryItems.maintenanceHint());
        b.set(cx - 12, y0 + 4, cz + 10, Blocks.COBWEB.defaultBlockState());

        // hidden cell
        b.chest(cx - 19, y0 + 1, cz + 7, Direction.EAST, new ItemStack(ModItems.MEMORY_LOG.get()));
        b.set(cx - 19, y0 + 1, cz + 6, Blocks.SOUL_TORCH.defaultBlockState());

        // corridor lights
        b.light(cx, y0 + 4, cz + 17);
        b.light(cx, y0 + 4, cz + 22);

        // final room
        int[][] fl = {{-5, 28}, {5, 28}, {-5, 36}, {5, 36}, {0, 27}, {0, 37}, {-3, 32}, {3, 32}};
        for (int[] l : fl) b.light(cx + l[0], y0 + 7, cz + l[1]);
        b.set(cx, y0 + 1, cz + 32, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        b.set(cx, y0 + 2, cz + 32, ModBlocks.RESET_CORE.get().defaultBlockState());
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = 29; sz <= 35; sz += 6)
                b.fill(cx + 3 * sx, y0 + 1, cz + sz, cx + 3 * sx, y0 + 4, cz + sz, Blocks.IRON_BLOCK.defaultBlockState());
        b.terminal(cx - 6, y0 + 1, cz + 32, Direction.EAST);
        b.terminal(cx + 6, y0 + 1, cz + 32, Direction.WEST);
        b.terminal(cx, y0 + 1, cz + 38, Direction.NORTH);

        // patched wall marker LAST (cobblestone, as the maintenance note says)
        b.fill(cx - 17, y0 + 1, cz + 7, cx - 17, y0 + 2, cz + 7, Blocks.COBBLESTONE.defaultBlockState());

        long[] arr = new long[b.lights.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = b.lights.get(i);
        d.labLights = arr;
        d.setDirty();
        return new BlockPos(cx, y0, cz);
    }

    /** lights out / on in the lab (used in the final scene) */
    public static void setLabLights(ServerWorld w, StoryData d, boolean on) {
        BlockState s = on ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
        for (long l : d.labLights) w.setBlock(BlockPos.of(l), s, 2);
    }

    public static int surfaceYOfLab(ServerWorld w, BlockPos origin) {
        return groundY(w, origin.getX(), origin.getZ());
    }

    private LabBuilder() {}
}
