package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventHandler;
import dev.luminous.api.events.impl.LookAtEvent;
import dev.luminous.api.events.impl.UpdateWalkingPlayerEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.entity.EntityUtil;
import dev.luminous.api.utils.entity.InventoryUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FeetPlace extends Module {
    public static FeetPlace INSTANCE;

    private final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    private final SliderSetting placeDelay = add(new SliderSetting("Delay", 50, 0, 500, 1, () -> page.getValue() == Page.General));
    private final SliderSetting blocksPerTick = add(new SliderSetting("BlocksPerTick", 4, 1, 12, 1, () -> page.getValue() == Page.General));
    private final SliderSetting range = add(new SliderSetting("Range", 5.0, 3.0, 8.0, 0.1, () -> page.getValue() == Page.General));
    private final BooleanSetting packetPlace = add(new BooleanSetting("PacketPlace", true, () -> page.getValue() == Page.General));
    private final BooleanSetting onlyTick = add(new BooleanSetting("OnlyTick", true, () -> page.getValue() == Page.General));
    private final BooleanSetting breakCrystal = add(new BooleanSetting("BreakCrystal", true, () -> page.getValue() == Page.General).setParent());
    private final BooleanSetting eatPause = add(new BooleanSetting("EatingPause", true, () -> breakCrystal.isOpen() && page.getValue() == Page.General));
    private final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true, () -> page.getValue() == Page.General));
    private final BooleanSetting enderChest = add(new BooleanSetting("EnderChest", true, () -> page.getValue() == Page.General));
    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true, () -> page.getValue() == Page.General));
    private final BooleanSetting support = add(new BooleanSetting("Support", true, () -> page.getValue() == Page.General));

    private final BooleanSetting smart = add(new BooleanSetting("Smart", true, () -> page.getValue() == Page.Logic).setParent());
    private final BooleanSetting prioritizeClose = add(new BooleanSetting("PrioritizeClose", true, () -> page.getValue() == Page.Logic && smart.isOpen()));
    private final BooleanSetting antiStep = add(new BooleanSetting("AntiStep", true, () -> page.getValue() == Page.Logic));
    private final BooleanSetting antiScaffold = add(new BooleanSetting("AntiScaffold", true, () -> page.getValue() == Page.Logic));
    private final BooleanSetting extend = add(new BooleanSetting("Extend", true, () -> page.getValue() == Page.Logic).setParent());
    private final SliderSetting extendRange = add(new SliderSetting("ExtendRange", 2, 1, 4, 1, () -> page.getValue() == Page.Logic && extend.isOpen()));
    private final BooleanSetting onlySelf = add(new BooleanSetting("OnlySelf", false, () -> page.getValue() == Page.Logic));

    private final BooleanSetting rotate = add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.Rotation));
    private final BooleanSetting yawStep = add(new BooleanSetting("YawStep", false, () -> page.getValue() == Page.Rotation));
    private final SliderSetting steps = add(new SliderSetting("Steps", 0.05, 0, 1, 0.01, () -> page.getValue() == Page.Rotation && yawStep.getValue()));
    private final BooleanSetting checkFov = add(new BooleanSetting("OnlyLooking", true, () -> page.getValue() == Page.Rotation));
    private final SliderSetting fov = add(new SliderSetting("Fov", 10, 0, 50, 1, () -> page.getValue() == Page.Rotation && checkFov.getValue()));
    private final SliderSetting priority = add(new SliderSetting("Priority", 15, 0, 100, 1, () -> page.getValue() == Page.Rotation));

    private final Timer timer = new Timer();
    private final List<BlockPos> placePositions = new ArrayList<>();
    public Vec3d directionVec = null;
    private int progress = 0;

    public FeetPlace() {
        super("FeetPlace", "Advanced feet trapping like surround", Category.Combat);
        setChinese("脚下陷阱");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return String.valueOf((int) blocksPerTick.getValue());
    }

    @EventHandler
    public void onUpdateWalking(UpdateWalkingPlayerEvent event) {
        if (!onlyTick.getValue()) {
            onUpdate();
        }
    }

    @EventHandler
    public void onRotate(LookAtEvent event) {
        if (directionVec != null && rotate.getValue() && yawStep.getValue()) {
            event.setTarget(directionVec, steps.getValueFloat(), priority.getValueFloat());
        }
    }

    @Override
    public void onUpdate() {
        if (!timer.passedMs((long) placeDelay.getValue())) return;

        directionVec = null;
        progress = 0;
        placePositions.clear();

        int block = getBlock();
        if (block == -1) {
            if (autoDisable.getValue()) {
                disable();
            }
            return;
        }

        findPlacePositions();

        if (placePositions.isEmpty()) {
            if (autoDisable.getValue()) {
                disable();
            }
            return;
        }

        if (prioritizeClose.getValue() && smart.getValue()) {
            placePositions.sort(Comparator.comparingDouble(pos ->
                mc.player.squaredDistanceTo(pos.toCenterPos())));
        }

        for (BlockPos placePos : placePositions) {
            if (progress >= blocksPerTick.getValue()) break;
            tryPlaceBlock(placePos);
        }
    }

    private void findPlacePositions() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {
                if (!onlySelf.getValue()) continue;
                addPlayerPositions(player);
            } else {
                if (mc.player.distanceTo(player) > range.getValue()) continue;
                if (Alien.FRIEND.isFriend(player.getName().getString())) continue;
                addPlayerPositions(player);
            }
        }
    }

    private void addPlayerPositions(PlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();

        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue;

            BlockPos pos = playerPos.offset(dir);
            if (BlockUtil.canPlace(pos, range.getValue(), false)) {
                addPlacePos(pos);
            }

            if (antiStep.getValue() && dir != Direction.DOWN) {
                BlockPos abovePos = pos.up();
                if (BlockUtil.canPlace(abovePos, range.getValue(), false)) {
                    addPlacePos(abovePos);
                }
            }

            if (antiScaffold.getValue() && dir != Direction.DOWN) {
                BlockPos belowPos = pos.down();
                if (BlockUtil.canPlace(belowPos, range.getValue(), false)) {
                    addPlacePos(belowPos);
                }
            }
        }

        if (extend.getValue()) {
            addExtendedPositions(playerPos);
        }
    }

    private void addExtendedPositions(BlockPos center) {
        int extendDist = (int) extendRange.getValue();

        for (int x = -extendDist; x <= extendDist; x++) {
            for (int z = -extendDist; z <= extendDist; z++) {
                if (x == 0 && z == 0) continue;

                BlockPos pos = center.add(x, 0, z);

                if (mc.player.squaredDistanceTo(pos.toCenterPos()) > range.getValue() * range.getValue()) {
                    continue;
                }

                if (intersectsPlayer(pos)) {
                    if (BlockUtil.canPlace(pos, range.getValue(), false)) {
                        addPlacePos(pos);
                    }
                }
            }
        }
    }

    private void addPlacePos(BlockPos pos) {
        if (!placePositions.contains(pos)) {
            if (support.getValue()) {
                BlockPos base = pos.down();
                if (BlockUtil.canReplace(base) && !placePositions.contains(base) && BlockUtil.getPlaceSide(base) != null) {
                    placePositions.add(base);
                }
            }
            placePositions.add(pos);
        }
    }

    private boolean intersectsPlayer(BlockPos pos) {
        Box box = new Box(pos);
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && onlySelf.getValue()) continue;
            if (player.getBoundingBox().intersects(box)) {
                return true;
            }
        }
        return false;
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
        timer.reset();
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

    public enum Page {
        General,
        Logic,
        Rotation
    }
}
