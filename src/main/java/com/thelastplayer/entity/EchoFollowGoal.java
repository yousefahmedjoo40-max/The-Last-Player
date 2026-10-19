package com.thelastplayer.entity;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

/** ECHO hovers beside his owner and teleports to them if left far behind. */
public class EchoFollowGoal extends Goal {
    private final EchoEntity echo;
    private PlayerEntity target;
    private int recalc;

    public EchoFollowGoal(EchoEntity echo) {
        this.echo = echo;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!echo.isFollowing() || echo.isStaring()) return false;
        PlayerEntity p = echo.getOwnerPlayer();
        if (p == null || p.isSpectator() || p.level != echo.level) return false;
        this.target = p;
        return echo.distanceToSqr(p) > 9.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && echo.isFollowing() && !echo.isStaring()
                && echo.distanceToSqr(target) > 4.0D;
    }

    @Override
    public void start() { this.recalc = 0; }

    @Override
    public void stop() {
        this.target = null;
        echo.getNavigation().stop();
    }

    @Override
    public void tick() {
        echo.getLookControl().setLookAt(target, 10.0F, (float) echo.getMaxHeadXRot());
        if (--recalc > 0) return;
        recalc = 10;
        if (echo.distanceToSqr(target) > 18.0D * 18.0D) {
            echo.moveTo(target.getX() + 1.0D, target.getY() + 1.5D, target.getZ() + 1.0D, target.yRot, 0.0F);
            echo.getNavigation().stop();
        } else {
            echo.getNavigation().moveTo(target.getX(), target.getY() + 1.3D, target.getZ(), 1.0D);
        }
    }
}
