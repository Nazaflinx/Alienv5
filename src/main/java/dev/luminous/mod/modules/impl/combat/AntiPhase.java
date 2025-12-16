package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventHandler;
import dev.luminous.api.events.impl.EntitySpawnEvent;
import dev.luminous.api.events.impl.LookAtEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.entity.EntityUtil;
import dev.luminous.api.utils.entity.InventoryUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AntiPhase extends Module {
    public static AntiPhase INSTANCE;

    private final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    private final SliderSetting placeDelay = add(new SliderSetting("PlaceDelay", 50, 0, 500, 1, () -> page.getValue() == Page.General));
    private final SliderSetting blocksPerTick = add(new SliderSetting("BlocksPerTick", 3, 1, 8, 1, () -> page.getValue() == Page.General));
    private final SliderSetting range = add(new SliderSetting("Range", 6.0, 3.0, 8.0, 0.1, () -> page.getValue() == Page.General));
    private final BooleanSetting packetPlace = add(new BooleanSetting("PacketPlace", true, () -> page.getValue() == Page.General));
    private final BooleanSetting breakCrystal = add(new BooleanSetting("BreakCrystal", true, () -> page.getValue() == Page.General).setParent());
    private final BooleanSetting eatPause = add(new BooleanSetting("EatingPause", true, () -> breakCrystal.isOpen() && page.getValue() == Page.General));
    private final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true, () -> page.getValue() == Page.General));
    private final BooleanSetting enderChest = add(new BooleanSetting("EnderChest", true, () -> page.getValue() == Page.General));
    private final EnumSetting<BlockMode> blockMode = add(new EnumSetting<>("BlockMode", BlockMode.Obsidian, () -> page.getValue() == Page.General));

    private final BooleanSetting predict = add(new BooleanSetting("Predict", true, () -> page.getValue() == Page.Logic).setParent());
    private final SliderSetting predictTicks = add(new SliderSetting("PredictTicks", 5, 1, 20, 1, () -> page.getValue() == Page.Logic && predict.isOpen()));
    private final BooleanSetting instant = add(new BooleanSetting("Instant", false, () -> page.getValue() == Page.Logic));
    private final BooleanSetting placeAround = add(new BooleanSetting("PlaceAround", true, () -> page.getValue() == Page.Logic).setParent());
    private final SliderSetting aroundRange = add(new SliderSetting("AroundRange", 1, 1, 3, 1, () -> page.getValue() == Page.Logic && placeAround.isOpen()));
    private final BooleanSetting onlyEnemies = add(new BooleanSetting("OnlyEnemies", true, () -> page.getValue() == Page.Logic));
    private final BooleanSetting ignoreFriends = add(new BooleanSetting("IgnoreFriends", true, () -> page.getValue() == Page.Logic));
    private final SliderSetting maxPearls = add(new SliderSetting("MaxPearls", 5, 1, 20, 1, () -> page.getValue() == Page.Logic));

    private final BooleanSetting rotate = add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.Rotation));
    private final BooleanSetting yawStep = add(new BooleanSetting("YawStep", false, () -> page.getValue() == Page.Rotation));
    private final SliderSetting steps = add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, () -> page.getValue() == Page.Rotation && yawStep.getValue()));
    private final BooleanSetting checkFov = add(new BooleanSetting("OnlyLooking", true, () -> page.getValue() == Page.Rotation));
    private final SliderSetting fov = add(new SliderSetting("Fov", 10, 0, 50, 1, () -> page.getValue() == Page.Rotation && checkFov.getValue()));
    private final SliderSetting priority = add(new SliderSetting("Priority", 20, 0, 100, 1, () -> page.getValue() == Page.Rotation));

    private final BooleanSetting render = add(new BooleanSetting("Render", true, () -> page.getValue() == Page.Render));
    private final ColorSetting boxColor = add(new ColorSetting("Box", new Color(255, 0, 0, 100), () -> page.getValue() == Page.Render && render.getValue()));
    private final ColorSetting lineColor = add(new ColorSetting("Line", new Color(255, 0, 0, 255), () -> page.getValue() == Page.Render && render.getValue()));
    private final SliderSetting lineWidth = add(new SliderSetting("LineWidth", 2.0, 0.1, 5.0, 0.1, () -> page.getValue() == Page.Render && render.getValue()));

    private final Map<EnderPearlEntity, PearlData> trackedPearls = new HashMap<>();
    private final Timer placeTimer = new Timer();
    public Vec3d directionVec = null;
    private int progress = 0;

    public AntiPhase() {
        super("AntiPhase", "Blocks enemy ender pearl teleports", Category.Combat);
        setChinese("反相位");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return String.valueOf(trackedPearls.size());
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof EnderPearlEntity pearl) {
            if (trackedPearls.size() >= maxPearls.getValue()) {
                return;
            }

            if (onlyEnemies.getValue()) {
                double closestDist = Double.MAX_VALUE;
                var closestPlayer = mc.world.getPlayers().stream()
                    .filter(p -> p != mc.player)
                    .filter(p -> !ignoreFriends.getValue() || !Alien.FRIEND.isFriend(p.getName().getString()))
                    .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(pearl.getX(), pearl.getY(), pearl.getZ())));

                if (closestPlayer.isEmpty()) {
                    return;
                }
            }

            PearlData data = new PearlData(pearl);
            trackedPearls.put(pearl, data);
        }
    }

    @Override
    public void onUpdate() {
        if (!placeTimer.passedMs((long) placeDelay.getValue())) return;

        directionVec = null;
        progress = 0;

        trackedPearls.entrySet().removeIf(entry -> {
            EnderPearlEntity pearl = entry.getKey();
            return !pearl.isAlive() || pearl.isRemoved() || mc.player.squaredDistanceTo(pearl) > range.getValue() * range.getValue() * 4;
        });

        if (trackedPearls.isEmpty()) return;

        int block = getBlock();
        if (block == -1) return;

        List<BlockPos> placePositions = new ArrayList<>();

        for (Map.Entry<EnderPearlEntity, PearlData> entry : trackedPearls.entrySet()) {
            EnderPearlEntity pearl = entry.getKey();
            PearlData data = entry.getValue();

            BlockPos landPos = predict.getValue() ?
                data.predictLanding((int) predictTicks.getValue()) :
                pearl.getBlockPos();

            if (mc.player.squaredDistanceTo(landPos.toCenterPos()) > range.getValue() * range.getValue()) {
                continue;
            }

            addBlockingPositions(landPos, placePositions);
        }

        placePositions.sort(Comparator.comparingDouble(pos ->
            mc.player.squaredDistanceTo(pos.toCenterPos())));

        for (BlockPos pos : placePositions) {
            if (progress >= blocksPerTick.getValue()) break;
            tryPlaceBlock(pos);
        }
    }

    private void addBlockingPositions(BlockPos center, List<BlockPos> positions) {
        if (BlockUtil.canPlace(center, range.getValue(), false)) {
            if (!positions.contains(center)) {
                positions.add(center);
            }
        }

        if (BlockUtil.canPlace(center.up(), range.getValue(), false)) {
            if (!positions.contains(center.up())) {
                positions.add(center.up());
            }
        }

        if (placeAround.getValue()) {
            int dist = (int) aroundRange.getValue();
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP) continue;

                for (int i = 1; i <= dist; i++) {
                    BlockPos pos = center.offset(dir, i);
                    if (BlockUtil.canPlace(pos, range.getValue(), false)) {
                        if (!positions.contains(pos)) {
                            positions.add(pos);
                        }
                    }
                }
            }
        }
    }

    private void tryPlaceBlock(BlockPos pos) {
        if (pos == null) return;
        if (progress >= blocksPerTick.getValue()) return;

        int block = getBlock();
        if (block == -1) return;

        Direction side = BlockUtil.getPlaceSide(pos);
        if (side == null) return;

        Vec3d directionVec = new Vec3d(
            pos.getX() + 0.5 + side.getVector().getX() * 0.5,
            pos.getY() + 0.5 + side.getVector().getY() * 0.5,
            pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
        );

        if (!BlockUtil.canPlace(pos, range.getValue(), true)) return;

        if (rotate.getValue()) {
            if (!faceVector(directionVec)) return;
        }

        if (breakCrystal.getValue()) {
            CombatUtil.attackCrystal(pos, rotate.getValue(), eatPause.getValue());
        } else if (BlockUtil.hasEntity(pos, false)) {
            return;
        }

        int old = mc.player.getInventory().selectedSlot;
        doSwap(block);

        if (BlockUtil.airPlace()) {
            BlockUtil.placedPos.add(pos);
            BlockUtil.clickBlock(pos, Direction.DOWN, false, Hand.MAIN_HAND, packetPlace.getValue());
        } else {
            BlockUtil.placedPos.add(pos);
            BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), false, Hand.MAIN_HAND, packetPlace.getValue());
        }

        if (inventory.getValue()) {
            doSwap(block);
            EntityUtil.syncInventory();
        } else {
            doSwap(old);
        }

        if (rotate.getValue() && !yawStep.getValue() && AntiCheat.INSTANCE.snapBack.getValue()) {
            Alien.ROTATION.snapBack();
        }

        progress++;
        placeTimer.reset();
    }

    private boolean faceVector(Vec3d directionVec) {
        if (!yawStep.getValue()) {
            Alien.ROTATION.lookAt(directionVec);
            return true;
        } else {
            this.directionVec = directionVec;
            if (Alien.ROTATION.inFov(directionVec, fov.getValueFloat())) {
                return true;
            }
        }
        return !checkFov.getValue();
    }

    @EventHandler
    public void onRotate(LookAtEvent event) {
        if (directionVec != null && rotate.getValue() && yawStep.getValue()) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!render.getValue()) return;

        for (Map.Entry<EnderPearlEntity, PearlData> entry : trackedPearls.entrySet()) {
            EnderPearlEntity pearl = entry.getKey();
            PearlData data = entry.getValue();

            if (predict.getValue()) {
                Vec3d predictedPos = data.getPredictedPos((int) predictTicks.getValue());
                Box box = new Box(
                    predictedPos.x - 0.25, predictedPos.y - 0.25, predictedPos.z - 0.25,
                    predictedPos.x + 0.25, predictedPos.y + 0.25, predictedPos.z + 0.25
                );
                Render3DUtil.draw3DBox(event.getMatrixStack(), box, boxColor.getValue(), true, false);
            }

            Vec3d start = mc.player.getEyePos();
            Vec3d end = pearl.getPos();
            Render3DUtil.drawLine(event.getMatrixStack(), start.x, start.y, start.z,
                end.x, end.y, end.z, lineColor.getValue(), lineWidth.getValueFloat());
        }
    }

    private void doSwap(int slot) {
        if (inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getBlock() {
        if (inventory.getValue()) {
            return switch (blockMode.getValue()) {
                case Obsidian -> InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
                case EnderChest -> InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
                case Both -> {
                    int obsidian = InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
                    if (obsidian != -1) yield obsidian;
                    yield InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
                }
            };
        } else {
            return switch (blockMode.getValue()) {
                case Obsidian -> InventoryUtil.findBlock(Blocks.OBSIDIAN);
                case EnderChest -> InventoryUtil.findBlock(Blocks.ENDER_CHEST);
                case Both -> {
                    int obsidian = InventoryUtil.findBlock(Blocks.OBSIDIAN);
                    if (obsidian != -1) yield obsidian;
                    yield InventoryUtil.findBlock(Blocks.ENDER_CHEST);
                }
            };
        }
    }

    private static class PearlData {
        private final EnderPearlEntity pearl;
        private Vec3d lastPos;
        private Vec3d velocity;

        public PearlData(EnderPearlEntity pearl) {
            this.pearl = pearl;
            this.lastPos = pearl.getPos();
            this.velocity = pearl.getVelocity();
        }

        public BlockPos predictLanding(int ticks) {
            Vec3d pos = pearl.getPos();
            Vec3d vel = pearl.getVelocity();

            for (int i = 0; i < ticks; i++) {
                pos = pos.add(vel);
                vel = vel.multiply(0.99, 0.99, 0.99);
                vel = vel.add(0, -0.03, 0);

                BlockHitResult result = mc.world.raycast(new RaycastContext(
                    pos, pos.add(vel),
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    pearl
                ));

                if (result.getType() == HitResult.Type.BLOCK) {
                    return result.getBlockPos();
                }
            }

            return BlockPos.ofFloored(pos);
        }

        public Vec3d getPredictedPos(int ticks) {
            Vec3d pos = pearl.getPos();
            Vec3d vel = pearl.getVelocity();

            for (int i = 0; i < ticks; i++) {
                pos = pos.add(vel);
                vel = vel.multiply(0.99, 0.99, 0.99);
                vel = vel.add(0, -0.03, 0);
            }

            return pos;
        }
    }

    public enum BlockMode {
        Obsidian,
        EnderChest,
        Both
    }

    public enum Page {
        General,
        Logic,
        Rotation,
        Render
    }
}
