package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventHandler;
import dev.luminous.api.events.impl.LookAtEvent;
import dev.luminous.api.events.impl.MoveEvent;
import dev.luminous.api.events.impl.UpdateWalkingPlayerEvent;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.entity.EntityUtil;
import dev.luminous.api.utils.entity.InventoryUtil;
import dev.luminous.api.utils.entity.MovementUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.Comparator;

public class SelfTrap extends Module {
    public static SelfTrap INSTANCE;
    private final Timer timer = new Timer();

    public final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    public final SliderSetting placeDelay =
            add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> page.is(Page.General)));
    private final SliderSetting blocksPer =
            add(new SliderSetting("BlocksPer", 2, 1, 8, () -> page.is(Page.General)));
    private final BooleanSetting packetPlace =
            add(new BooleanSetting("PacketPlace", true, () -> page.is(Page.General)));
    private final BooleanSetting onlyTick =
            add(new BooleanSetting("OnlyTick", true, () -> page.is(Page.General)));
    private final BooleanSetting breakCrystal =
            add(new BooleanSetting("Break", true, () -> page.is(Page.General)).setParent());
    private final BooleanSetting eatPause =
            add(new BooleanSetting("EatingPause", true, () -> breakCrystal.isOpen()));
    private final BooleanSetting autoDisable =
            add(new BooleanSetting("AutoOff", false, () -> page.is(Page.General)).setParent());
    private final SliderSetting disableHealth =
            add(new SliderSetting("OffHP", 16, 0, 36, () -> page.is(Page.General) && autoDisable.isOpen()));
    private final BooleanSetting center =
            add(new BooleanSetting("Center", true, () -> page.is(Page.General)));
    public final BooleanSetting extend =
            add(new BooleanSetting("Extend", true, () -> page.is(Page.General)));
    private final BooleanSetting inventory =
            add(new BooleanSetting("InventorySwap", true, () -> page.is(Page.General)));
    private final BooleanSetting enderChest =
            add(new BooleanSetting("EnderChest", true, () -> page.is(Page.General)));
    private final BooleanSetting prioritize =
            add(new BooleanSetting("Prioritize", true, () -> page.is(Page.General)));

    private final BooleanSetting rotate =
            add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.Rotate));
    private final BooleanSetting yawStep =
            add(new BooleanSetting("YawStep", false, () -> page.getValue() == Page.Rotate));
    private final SliderSetting steps =
            add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, () -> page.getValue() == Page.Rotate && yawStep.getValue()));
    private final BooleanSetting checkFov =
            add(new BooleanSetting("OnlyLooking", true, () -> page.getValue() == Page.Rotate && yawStep.getValue()));
    private final SliderSetting fov =
            add(new SliderSetting("Fov", 5f, 0f, 30f, () -> checkFov.getValue() && page.getValue() == Page.Rotate));
    private final SliderSetting priority = add(new SliderSetting("Priority", 10, 0, 100, () -> page.getValue() == Page.Rotate));

    private final BooleanSetting detectMining =
            add(new BooleanSetting("DetectMining", false, () -> page.is(Page.Check)));
    private final BooleanSetting usingPause =
            add(new BooleanSetting("UsingPause", true, () -> page.is(Page.Check)));
    public final BooleanSetting inAir =
            add(new BooleanSetting("InAir", true, () -> page.is(Page.Check)));
    private final BooleanSetting moveDisable =
            add(new BooleanSetting("AutoDisable", true, () -> page.is(Page.Check)));
    private final BooleanSetting jumpDisable =
            add(new BooleanSetting("JumpDisable", true, () -> page.is(Page.Check)));

    private final BooleanSetting head =
            add(new BooleanSetting("Head", true, () -> page.is(Page.Mode)));
    private final BooleanSetting feet =
            add(new BooleanSetting("Feet", true, () -> page.is(Page.Mode)));
    private final BooleanSetting chest =
            add(new BooleanSetting("Chest", true, () -> page.is(Page.Mode)));

    public enum Page {
        General,
        Rotate,
        Check,
        Mode
    }

    double startX = 0;
    double startY = 0;
    double startZ = 0;
    int progress = 0;
    public Vec3d directionVec = null;
    private final ArrayList<BlockPos> placePositions = new ArrayList<>();

    public SelfTrap() {
        super("SelfTrap", Category.Combat);
        setChinese("自我困住");
        INSTANCE = this;
    }

    @EventHandler
    public void onRotate(LookAtEvent event) {
        if (directionVec != null && rotate.getValue() && yawStep.getValue()) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
    }


    @Override
    public void onEnable() {
        if (nullCheck()) {
            if (moveDisable.getValue() || jumpDisable.getValue()) disable();
            return;
        }
        startX = mc.player.getX();
        startY = mc.player.getY();
        startZ = mc.player.getZ();
        shouldCenter = true;
    }

    @EventHandler
    public void onUpdateWalking(UpdateWalkingPlayerEvent event) {
        if (!onlyTick.getValue()) {
            onUpdate();
        }
    }

    private boolean shouldCenter = true;

    @EventHandler(priority = -1)
    public void onMove(MoveEvent event) {
        if (nullCheck() || !center.getValue() || mc.player.isFallFlying()) {
            return;
        }

        BlockPos blockPos = EntityUtil.getPlayerPos(true);
        if (mc.player.getX() - blockPos.getX() - 0.5 <= 0.2 && mc.player.getX() - blockPos.getX() - 0.5 >= -0.2 && mc.player.getZ() - blockPos.getZ() - 0.5 <= 0.2 && mc.player.getZ() - 0.5 - blockPos.getZ() >= -0.2) {
            if (shouldCenter && (mc.player.isOnGround() || MovementUtil.isMoving())) {
                event.setX(0);
                event.setZ(0);
                shouldCenter = false;
            }
        } else {
            if (shouldCenter) {
                Vec3d centerPos = EntityUtil.getPlayerPos(true).toCenterPos();
                float rotation = getRotationTo(mc.player.getPos(), centerPos).x;
                float yawRad = rotation / 180.0f * 3.1415927f;
                double dist = mc.player.getPos().distanceTo(new Vec3d(centerPos.x, mc.player.getY(), centerPos.z));
                double cappedSpeed = Math.min(0.2873, dist);
                double x = -(float) Math.sin(yawRad) * cappedSpeed;
                double z = (float) Math.cos(yawRad) * cappedSpeed;
                event.setX(x);
                event.setZ(z);
            }
        }
    }

    @Override
    public void onUpdate() {
        if (nullCheck()) return;
        if (!timer.passedMs((long) placeDelay.getValue())) return;

        if (autoDisable.getValue() && EntityUtil.getHealth(mc.player) >= disableHealth.getValue()) {
            disable();
            return;
        }

        directionVec = null;
        progress = 0;
        placePositions.clear();

        if (!MovementUtil.isMoving() && !mc.options.jumpKey.isPressed()) {
            startX = mc.player.getX();
            startY = mc.player.getY();
            startZ = mc.player.getZ();
        }
        BlockPos pos = EntityUtil.getPlayerPos(true);

        double distanceToStart = MathHelper.sqrt((float) mc.player.squaredDistanceTo(startX, startY, startZ));

        if (getBlock() == -1) {
            CommandManager.sendChatMessageWidthId("§c§oObsidian" + (enderChest.getValue() ? "/EnderChest" : "") + "?", hashCode());
            disable();
            return;
        }
        if ((moveDisable.getValue() && distanceToStart > 1.0 || jumpDisable.getValue() && Math.abs(startY - mc.player.getY()) > 0.5)) {
            disable();
            return;
        }
        if (usingPause.getValue() && mc.player.isUsingItem()) {
            return;
        }

        if (!inAir.getValue() && !mc.player.isOnGround()) return;

        if (feet.getValue()) collectSurround(pos);
        if (chest.getValue()) collectSurround(pos.up());
        if (head.getValue()) {
            collectPlacePos(pos.up(2));
        }

        if (prioritize.getValue()) {
            placePositions.sort(Comparator.comparingDouble(blockPos -> {
                double dist = mc.player.squaredDistanceTo(blockPos.toCenterPos());
                if (blockPos.getY() - pos.getY() == 2) dist += 100;
                return dist;
            }));
        }

        for (BlockPos placePos : placePositions) {
            if (progress >= blocksPer.getValue()) break;
            tryPlaceBlock(placePos);
        }
    }

    private void collectPlacePos(BlockPos pos) {
        if (pos != null && !placePositions.contains(pos)) {
            placePositions.add(pos);
        }
    }

    private void collectSurround(BlockPos pos) {
        try {
            for (Direction i : Direction.values()) {
                if (i == Direction.UP) continue;
                BlockPos offsetPos = pos.offset(i);
                if (BlockUtil.getPlaceSide(offsetPos) != null) {
                    collectPlacePos(offsetPos);
                } else if (BlockUtil.canReplace(offsetPos)) {
                    collectPlacePos(getHelperPos(offsetPos));
                }
                if (selfIntersectPos(offsetPos) && extend.getValue()) {
                    for (Direction i2 : Direction.values()) {
                        if (i2 == Direction.UP) continue;
                        BlockPos offsetPos2 = offsetPos.offset(i2);
                        if (selfIntersectPos(offsetPos2)) {
                            for (Direction i3 : Direction.values()) {
                                if (i3 == Direction.UP) continue;
                                collectPlacePos(offsetPos2);
                                BlockPos offsetPos3 = offsetPos2.offset(i3);
                                collectPlacePos(BlockUtil.getPlaceSide(offsetPos3) != null || !BlockUtil.canReplace(offsetPos3) ? offsetPos3 : getHelperPos(offsetPos3));
                                if (placePositions.size() >= 16) break;
                            }
                        }
                        collectPlacePos(BlockUtil.getPlaceSide(offsetPos2) != null || !BlockUtil.canReplace(offsetPos2) ? offsetPos2 : getHelperPos(offsetPos2));
                        if (placePositions.size() >= 16) break;
                    }
                }
                if (placePositions.size() >= 16) break;
            }
        } catch (Exception e) {
            return;
        }
    }
    private void tryPlaceBlock(BlockPos pos) {
        try {
            if (pos == null) return;
            if (detectMining.getValue() && Alien.BREAK.isMining(pos)) return;
            if (!(progress < blocksPer.getValue())) return;
            int block = getBlock();
            if (block == -1) return;

            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null) return;

            Vec3d placeVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5,
                                         pos.getY() + 0.5 + side.getVector().getY() * 0.5,
                                         pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);

            if (!BlockUtil.canPlace(pos, 6, true)) return;

            if (rotate.getValue()) {
                if (!faceVector(placeVec)) return;
            }

            if (breakCrystal.getValue()) {
                CombatUtil.attackCrystal(pos, rotate.getValue(), eatPause.getValue());
                if (BlockUtil.hasEntity(pos, false)) return;
            } else if (BlockUtil.hasEntity(pos, false)) return;

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
            timer.reset();
        } catch (Exception e) {
            return;
        }
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

    public static boolean selfIntersectPos(BlockPos pos) {
        return mc.player.getBoundingBox().intersects(new Box(pos));
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
            if (InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN) != -1 || !enderChest.getValue()) {
                return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
            }
            return InventoryUtil.findBlockInventorySlot(Blocks.ENDER_CHEST);
        } else {
            if (InventoryUtil.findBlock(Blocks.OBSIDIAN) != -1 || !enderChest.getValue()) {
                return InventoryUtil.findBlock(Blocks.OBSIDIAN);
            }
            return InventoryUtil.findBlock(Blocks.ENDER_CHEST);
        }
    }

    public BlockPos getHelperPos(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (detectMining.getValue() && Alien.BREAK.isMining(pos.offset(i))) continue;
            if (!BlockUtil.isStrictDirection(pos.offset(i), i.getOpposite())) continue;
            if (BlockUtil.canPlace(pos.offset(i))) return pos.offset(i);
        }
        return null;
    }

    public static Vec2f getRotationTo(Vec3d posFrom, Vec3d posTo) {
        Vec3d vec3d = posTo.subtract(posFrom);
        return getRotationFromVec(vec3d);
    }

    private static Vec2f getRotationFromVec(Vec3d vec) {
        double d = vec.x;
        double d2 = vec.z;
        double xz = Math.hypot(d, d2);
        d2 = vec.z;
        double d3 = vec.x;
        double yaw = normalizeAngle(Math.toDegrees(Math.atan2(d2, d3)) - 90.0);
        double pitch = normalizeAngle(Math.toDegrees(-Math.atan2(vec.y, xz)));
        return new Vec2f((float) yaw, (float) pitch);
    }

    private static double normalizeAngle(double angleIn) {
        double angle = angleIn;
        if ((angle %= 360.0) >= 180.0) {
            angle -= 360.0;
        }
        if (angle < -180.0) {
            angle += 360.0;
        }
        return angle;
    }
}
