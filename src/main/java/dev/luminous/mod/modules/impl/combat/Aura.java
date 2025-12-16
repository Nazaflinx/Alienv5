package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventHandler;
import dev.luminous.api.events.impl.LookAtEvent;
import dev.luminous.api.events.impl.UpdateWalkingPlayerEvent;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.api.utils.entity.EntityUtil;
import dev.luminous.api.utils.entity.InventoryUtil;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.SwingSide;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Aura extends Module {
    public static Aura INSTANCE;

    private final EnumSetting<Page> page = add(new EnumSetting<>("Page", Page.General));

    private final SliderSetting range = add(new SliderSetting("Range", 6.0, 3.0, 7.0, 0.1, () -> page.getValue() == Page.General));
    private final SliderSetting wallRange = add(new SliderSetting("WallRange", 3.5, 3.0, 7.0, 0.1, () -> page.getValue() == Page.General));
    private final SliderSetting attackSpeed = add(new SliderSetting("AttackSpeed", 20, 1, 20, 1, () -> page.getValue() == Page.General));
    private final EnumSetting<Priority> priority = add(new EnumSetting<>("Priority", Priority.Distance, () -> page.getValue() == Page.General));
    private final EnumSetting<SwingSide> swingMode = add(new EnumSetting<>("Swing", SwingSide.All, () -> page.getValue() == Page.General));
    private final BooleanSetting multiTarget = add(new BooleanSetting("MultiTarget", false, () -> page.getValue() == Page.General).setParent());
    private final SliderSetting targetCount = add(new SliderSetting("Targets", 3, 1, 10, 1, () -> page.getValue() == Page.General && multiTarget.isOpen()));
    private final BooleanSetting autoWeapon = add(new BooleanSetting("AutoWeapon", true, () -> page.getValue() == Page.General));
    private final BooleanSetting weaponOnly = add(new BooleanSetting("WeaponOnly", false, () -> page.getValue() == Page.General));
    private final BooleanSetting pauseEating = add(new BooleanSetting("PauseEating", true, () -> page.getValue() == Page.General));
    private final BooleanSetting tpsSync = add(new BooleanSetting("TPSSync", true, () -> page.getValue() == Page.General));

    private final BooleanSetting rotate = add(new BooleanSetting("Rotate", true, () -> page.getValue() == Page.Rotation));
    private final BooleanSetting yawStep = add(new BooleanSetting("YawStep", false, () -> page.getValue() == Page.Rotation));
    private final SliderSetting steps = add(new SliderSetting("Steps", 0.1, 0.01, 1, 0.01, () -> page.getValue() == Page.Rotation && yawStep.getValue()));
    private final BooleanSetting checkFov = add(new BooleanSetting("CheckFov", true, () -> page.getValue() == Page.Rotation));
    private final SliderSetting fov = add(new SliderSetting("Fov", 90, 10, 180, 1, () -> page.getValue() == Page.Rotation && checkFov.getValue()));
    private final SliderSetting rotationPriority = add(new SliderSetting("Priority", 15, 0, 100, 1, () -> page.getValue() == Page.Rotation));

    private final BooleanSetting players = add(new BooleanSetting("Players", true, () -> page.getValue() == Page.Target).setParent());
    private final BooleanSetting armorLow = add(new BooleanSetting("ArmorLow", true, () -> page.getValue() == Page.Target && players.isOpen()));
    private final BooleanSetting ignoreFriends = add(new BooleanSetting("IgnoreFriends", true, () -> page.getValue() == Page.Target && players.isOpen()));
    private final BooleanSetting mobs = add(new BooleanSetting("Mobs", true, () -> page.getValue() == Page.Target));
    private final BooleanSetting animals = add(new BooleanSetting("Animals", false, () -> page.getValue() == Page.Target));
    private final BooleanSetting villagers = add(new BooleanSetting("Villagers", false, () -> page.getValue() == Page.Target));

    private final BooleanSetting render = add(new BooleanSetting("Render", true, () -> page.getValue() == Page.Render));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(255, 0, 0, 100), () -> page.getValue() == Page.Render && render.getValue()));
    private final SliderSetting lineWidth = add(new SliderSetting("LineWidth", 2.0, 0.1, 5.0, 0.1, () -> page.getValue() == Page.Render && render.getValue()));

    private final Timer attackTimer = new Timer();
    private final List<Entity> targets = new ArrayList<>();
    public Vec3d directionVec = null;
    private int weaponSlot = -1;

    public Aura() {
        super("Aura", "Advanced combat aura with multi-target", Category.Combat);
        setChinese("战斗光环");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return targets.isEmpty() ? null : targets.size() > 1 ? String.valueOf(targets.size()) : targets.get(0).getName().getString();
    }

    @EventHandler
    public void onUpdateWalking(UpdateWalkingPlayerEvent event) {
        onUpdate();
    }

    @EventHandler
    public void onRotate(LookAtEvent event) {
        if (directionVec != null && rotate.getValue() && yawStep.getValue()) {
            event.setTarget(directionVec, steps.getValueFloat(), rotationPriority.getValueFloat());
        }
    }

    @Override
    public void onUpdate() {
        directionVec = null;
        targets.clear();

        if (weaponOnly.getValue() && !EntityUtil.isHoldingWeapon(mc.player)) {
            return;
        }

        if (pauseEating.getValue() && mc.player.isUsingItem()) {
            return;
        }

        updateTargets();

        if (targets.isEmpty()) {
            return;
        }

        if (autoWeapon.getValue()) {
            switchToWeapon();
        }

        attack();
    }

    private void updateTargets() {
        List<Entity> potentialTargets = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (!isValidTarget(entity)) continue;

            double distance = mc.player.distanceTo(entity);
            if (distance > range.getValue()) continue;

            if (!mc.player.canSee(entity) && distance > wallRange.getValue()) {
                continue;
            }

            if (checkFov.getValue() && !isInFov(entity, fov.getValue())) {
                continue;
            }

            potentialTargets.add(entity);
        }

        potentialTargets.sort(getComparator());

        int maxTargets = multiTarget.getValue() ? (int) targetCount.getValue() : 1;
        for (int i = 0; i < Math.min(maxTargets, potentialTargets.size()); i++) {
            targets.add(potentialTargets.get(i));
        }
    }

    private Comparator<Entity> getComparator() {
        return switch (priority.getValue()) {
            case Distance -> Comparator.comparingDouble(e -> mc.player.distanceTo(e));
            case Health -> Comparator.comparingDouble(e -> {
                if (e instanceof LivingEntity living) {
                    return living.getHealth() + living.getAbsorptionAmount();
                }
                return Double.MAX_VALUE;
            });
            case Angle -> Comparator.comparingDouble(this::getAngleDifference);
        };
    }

    private double getAngleDifference(Entity entity) {
        Vec3d toEntity = entity.getEyePos().subtract(mc.player.getEyePos());
        double yaw = Math.toDegrees(Math.atan2(toEntity.z, toEntity.x)) - 90.0;
        double pitch = Math.toDegrees(-Math.atan2(toEntity.y, Math.sqrt(toEntity.x * toEntity.x + toEntity.z * toEntity.z)));

        double yawDiff = Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
        double pitchDiff = Math.abs(MathHelper.wrapDegrees(pitch - mc.player.getPitch()));

        return yawDiff + pitchDiff;
    }

    private boolean isInFov(Entity entity, double fovRange) {
        Vec3d toEntity = entity.getEyePos().subtract(mc.player.getEyePos());
        double yaw = Math.toDegrees(Math.atan2(toEntity.z, toEntity.x)) - 90.0;
        double yawDiff = Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
        return yawDiff <= fovRange / 2.0;
    }

    private void attack() {
        long delay = tpsSync.getValue() ?
            (long) (1000.0 / attackSpeed.getValue() * Alien.SERVER.getTPSFactor()) :
            (long) (1000.0 / attackSpeed.getValue());

        if (!attackTimer.passedMs(delay)) {
            return;
        }

        for (Entity target : targets) {
            if (rotate.getValue()) {
                if (!faceEntity(target)) continue;
            }

            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            EntityUtil.swingHand(Hand.MAIN_HAND, swingMode.getValue());
        }

        attackTimer.reset();
    }

    private boolean faceEntity(Entity entity) {
        Vec3d targetVec = entity.getEyePos();

        if (!yawStep.getValue()) {
            Alien.ROTATION.lookAt(targetVec);
            return true;
        } else {
            directionVec = targetVec;
            return Alien.ROTATION.inFov(targetVec, 30);
        }
    }

    private void switchToWeapon() {
        if (weaponSlot == -1 || !isWeapon(mc.player.getInventory().getStack(weaponSlot))) {
            findWeapon();
        }

        if (weaponSlot != -1 && weaponSlot != mc.player.getInventory().selectedSlot) {
            InventoryUtil.switchToSlot(weaponSlot);
        }
    }

    private void findWeapon() {
        weaponSlot = -1;
        int bestSlot = -1;
        float bestDamage = 0;

        for (int i = 0; i < 9; i++) {
            if (!isWeapon(mc.player.getInventory().getStack(i))) continue;

            float damage = CombatUtil.getAttackDamage(mc.player.getInventory().getStack(i));
            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = i;
            }
        }

        weaponSlot = bestSlot;
    }

    private boolean isWeapon(net.minecraft.item.ItemStack stack) {
        return stack.getItem() == Items.DIAMOND_SWORD ||
               stack.getItem() == Items.NETHERITE_SWORD ||
               stack.getItem() == Items.DIAMOND_AXE ||
               stack.getItem() == Items.NETHERITE_AXE ||
               stack.getItem() == Items.IRON_SWORD ||
               stack.getItem() == Items.IRON_AXE;
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == mc.player) return false;
        if (entity instanceof ArmorStandEntity) return false;
        if (!(entity instanceof LivingEntity)) return false;
        if (!entity.isAlive()) return false;

        if (entity instanceof PlayerEntity player) {
            if (!players.getValue()) return false;
            if (ignoreFriends.getValue() && Alien.FRIEND.isFriend(player.getName().getString())) return false;
            if (armorLow.getValue() && EntityUtil.isArmorLow(player, 10)) return true;
            return true;
        }

        if (entity instanceof VillagerEntity && villagers.getValue()) return true;
        if (entity instanceof MobEntity && mobs.getValue()) return true;
        if (entity instanceof AnimalEntity && animals.getValue()) return true;

        return false;
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (!render.getValue() || targets.isEmpty()) return;

        for (Entity target : targets) {
            Vec3d pos = new Vec3d(
                MathHelper.lerp(mc.getTickDelta(), target.lastRenderX, target.getX()),
                MathHelper.lerp(mc.getTickDelta(), target.lastRenderY, target.getY()),
                MathHelper.lerp(mc.getTickDelta(), target.lastRenderZ, target.getZ())
            );

            Render3DUtil.draw3DBox(matrixStack,
                ((IEntity) target).getDimensions().getBoxAt(pos).expand(0.05),
                color.getValue(),
                true,
                false);
        }
    }

    public enum Priority {
        Distance,
        Health,
        Angle
    }

    public enum Page {
        General,
        Rotation,
        Target,
        Render
    }
}
