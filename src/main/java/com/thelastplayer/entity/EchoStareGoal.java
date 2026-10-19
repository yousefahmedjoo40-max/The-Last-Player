package com.thelastplayer.entity;

import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

/** From day 2 on, ECHO sometimes freezes and stares at the sky. */
public class EchoStareGoal extends Goal {
    private final EchoEntity echo;
    private int timer;

    public EchoStareGoal(EchoEntity echo) {
        this.echo = echo;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return echo.getMood() >= 1 && echo.getMood() <= 2 && echo.getRandom().nextInt(700) == 0;
    }

    @Override
    public boolean canContinueToUse() { return timer > 0; }

    @Override
    public void start() {
        timer = 80 + echo.getRandom().nextInt(80);
        echo.setStaring(true);
        echo.getNavigation().stop();
    }

    @Override
    public void stop() { echo.setStaring(false); }

    @Override
    public void tick() {
        timer--;
        echo.getLookControl().setLookAt(echo.getX(), echo.getY() + 40.0D, echo.getZ());
    }
}
