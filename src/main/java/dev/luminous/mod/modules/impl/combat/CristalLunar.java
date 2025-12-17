package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventHandler;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.events.impl.Render3DEvent;
import dev.luminous.api.events.impl.UpdateWalkingPlayerEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.entity.EntityUtil;
import dev.luminous.api.utils.entity.InventoryUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;

import static dev.luminous.api.utils.world.BlockUtil.getBlock;

public class CristalLunar extends Module {
    public static CristalLunar INSTANCE;

    private final Timer breakTimer = new Timer();
    private final Timer placeTimer = new Timer();

    private final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    private final SliderSetting targetRange = add(new SliderSetting("TargetRange", 10.0, 0.0, 20.0, () -> page.getValue() == Page.General));
    private final SliderSetting breakRange = add(new SliderSetting("BreakRange", 6.0, 0.0, 6.0, () -> page.getValue() == Page.General));
    private final SliderSetting placeRange = add(new SliderSetting("PlaceRange", 6.0, 0.0, 6.0, () -> page.getValue() == Page.General));
    private final SliderSetting minDamage = add(new SliderSetting("MinDamage", 3.0, 0.0, 36.0, () -> page.getValue() == Page.General));
    private final SliderSetting maxSelfDamage = add(new SliderSetting("MaxSelf", 10.0, 0.0, 36.0, () -> page.getValue() == Page.General));
    private final SliderSetting breakDelay = add(new SliderSetting("BreakDelay", 50, 0, 500, () -> page.getValue() == Page.General));
    private final SliderSetting placeDelay = add(new SliderSetting("PlaceDelay", 50, 0, 500, () -> page.getValue() == Page.General));

    private final BooleanSetting rotate = add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.General));
    private final BooleanSetting autoSwitch = add(new BooleanSetting("AutoSwitch", true, () -> page.getValue() == Page.General));
    private final EnumSetting<SwingSide> swingMode = add(new EnumSetting<>("Swing", SwingSide.Server, () -> page.getValue() == Page.General));

    private final BooleanSetting render = add(new BooleanSetting("Render", true, () -> page.getValue() == Page.Render));
    private final ColorSetting renderColor = add(new ColorSetting("Color", new Color(255, 255, 255, 100), () -> page.getValue() == Page.Render && render.getValue()));
    private final SliderSetting lineWidth = add(new SliderSetting("LineWidth", 2.0, 0.1, 5.0, 0.1, () -> page.getValue() == Page.Render && render.getValue()));

    private BlockPos renderPos = null;
    private PlayerEntity target = null;

    public CristalLunar() {
        super("CristalLunar", Category.Combat);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        renderPos = null;
        target = null;
    }

    @Override
    public void onDisable() {
        renderPos = null;
        target = null;
    }

    @EventHandler
    public void onUpdateWalking(UpdateWalkingPlayerEvent event) {
        if (nullCheck()) return;

        target = CombatUtil.getClosestEnemy(targetRange.getValueFloat());
        if (target == null) return;

        if (breakTimer.passedMs((long) breakDelay.getValue())) {
            breakCrystal();
        }

        if (placeTimer.passedMs((long) placeDelay.getValue())) {
            placeCrystal();
        }
    }

    private void breakCrystal() {
        EndCrystalEntity bestCrystal = null;
        double bestDamage = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity)) continue;
            EndCrystalEntity crystal = (EndCrystalEntity) entity;

            double distance = mc.player.getPos().distanceTo(crystal.getPos());
            if (distance > breakRange.getValue()) continue;

            double damage = CombatUtil.calculateDamage(crystal.getPos(), target);
            double selfDamage = CombatUtil.calculateDamage(crystal.getPos(), mc.player);

            if (selfDamage > maxSelfDamage.getValue()) continue;
            if (damage < minDamage.getValue()) continue;

            if (damage > bestDamage) {
                bestDamage = damage;
                bestCrystal = crystal;
            }
        }

        if (bestCrystal != null) {
            if (rotate.getValue()) {
                EntityUtil.faceVector(bestCrystal.getPos());
            }

            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(bestCrystal, mc.player.isSneaking()));
            swingMode.getValue().swing(Hand.MAIN_HAND);
            breakTimer.reset();
        }
    }

    private void placeCrystal() {
        if (autoSwitch.getValue()) {
            int crystalSlot = InventoryUtil.findItem(Items.END_CRYSTAL);
            if (crystalSlot == -1) return;
            InventoryUtil.switchToSlot(crystalSlot);
        }

        BlockPos bestPos = null;
        double bestDamage = 0;

        ArrayList<BlockPos> positions = new ArrayList<>();
        for (double x = -placeRange.getValue(); x <= placeRange.getValue(); x++) {
            for (double y = -placeRange.getValue(); y <= placeRange.getValue(); y++) {
                for (double z = -placeRange.getValue(); z <= placeRange.getValue(); z++) {
                    BlockPos pos = BlockPosX.floor(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
                    if (canPlaceCrystal(pos)) {
                        positions.add(pos);
                    }
                }
            }
        }

        for (BlockPos pos : positions) {
            double damage = CombatUtil.calculateDamage(new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), target);
            double selfDamage = CombatUtil.calculateDamage(new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), mc.player);

            if (selfDamage > maxSelfDamage.getValue()) continue;
            if (damage < minDamage.getValue()) continue;

            if (damage > bestDamage) {
                bestDamage = damage;
                bestPos = pos;
            }
        }

        if (bestPos != null) {
            renderPos = bestPos;

            if (rotate.getValue()) {
                EntityUtil.faceVector(new Vec3d(bestPos.getX() + 0.5, bestPos.getY() + 0.5, bestPos.getZ() + 0.5));
            }

            BlockUtil.placeCrystal(bestPos, rotate.getValue());
            swingMode.getValue().swing(Hand.MAIN_HAND);
            placeTimer.reset();
        }
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        if (!getBlock(pos).equals(Blocks.OBSIDIAN) && !getBlock(pos).equals(Blocks.BEDROCK)) {
            return false;
        }

        if (!getBlock(pos.up()).equals(Blocks.AIR)) {
            return false;
        }

        if (!getBlock(pos.up(2)).equals(Blocks.AIR)) {
            return false;
        }

        Box box = new Box(pos.up());
        for (Entity entity : mc.world.getOtherEntities(null, box)) {
            if (entity instanceof EndCrystalEntity) continue;
            return false;
        }

        return mc.player.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)) <= placeRange.getValue();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (nullCheck() || !render.getValue()) return;
        if (renderPos == null) return;

        MatrixStack matrixStack = event.getMatrixStack();
        Box box = new Box(renderPos.up());

        Render3DUtil.drawFill(matrixStack, box, ColorUtil.injectAlpha(renderColor.getValue(), renderColor.getValue().getAlpha()));
        Render3DUtil.drawBox(matrixStack, box, ColorUtil.injectAlpha(renderColor.getValue(), 255), (float) lineWidth.getValue());
    }

    public enum Page {
        General,
        Render
    }
}
