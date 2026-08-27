package iran.flame.network.lobby.utils;

import iran.flame.network.lobby.Main;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class SchedulerUtil {
    private static Boolean FOLIA = null;
    private SchedulerUtil() {}

    public static boolean isFolia() {
        if (FOLIA != null) return FOLIA;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            FOLIA = true;
        } catch (ClassNotFoundException e) {
            FOLIA = false;
        }
        return FOLIA;
    }

    public static void run(Plugin plugin, Runnable task) {
        if (isFolia()) {
            try {
                Object globalRegionScheduler = getServerMethod("getGlobalRegionScheduler");
                Method execute = globalRegionScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
                execute.invoke(globalRegionScheduler, plugin, task);
                return;
            } catch (Throwable t) {
                Main.getThis().getServer().getLogger().warning("[SchedulerUtil] run() failed on global region scheduler: " + t.getMessage());
            }
            task.run();
            return;
        }
        Main.getThis().getServer().getScheduler().runTask(plugin, task);
    }

    public static void runGlobal(Plugin plugin, Runnable task) {
        if (!plugin.isEnabled()) return;
        run(plugin, task);
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (isFolia()) {
            try {
                Object asyncScheduler = getServerMethod("getAsyncScheduler");
                Method runNow = asyncScheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
                Consumer<Object> consumer = scheduledTask -> task.run();
                runNow.invoke(asyncScheduler, plugin, consumer);
                return;
            } catch (Throwable t) {
                Main.getThis().getServer().getLogger().warning("[SchedulerUtil] runAsync() failed on async scheduler: " + t.getMessage());
            }
            task.run();
            return;
        }
        Main.getThis().getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static void runTimerAsync(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            try {
                Object asyncScheduler = getServerMethod("getAsyncScheduler");
                long delayMs = delayTicks * 50L;
                long periodMs = periodTicks * 50L;
                Method runAtFixedRate = asyncScheduler.getClass().getMethod(
                        "runAtFixedRate",
                        Plugin.class,
                        Consumer.class,
                        long.class,
                        long.class,
                        TimeUnit.class
                );
                Consumer<Object> consumer = scheduledTask -> task.run();
                runAtFixedRate.invoke(asyncScheduler, plugin, consumer, delayMs, periodMs, TimeUnit.MILLISECONDS);
                return;
            } catch (Throwable t) {
                Main.getThis().getServer().getLogger().warning("[SchedulerUtil] runTimerAsync() failed on async scheduler: " + t.getMessage());
            }
            task.run();
            return;
        }
        Main.getThis().getServer().getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    public static void runTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            try {
                Object globalRegionScheduler = getServerMethod("getGlobalRegionScheduler");
                Method runAtFixedRate = globalRegionScheduler.getClass().getMethod(
                        "runAtFixedRate",
                        Plugin.class,
                        Consumer.class,
                        long.class,
                        long.class
                );
                Consumer<Object> consumer = scheduledTask -> task.run();
                runAtFixedRate.invoke(globalRegionScheduler, plugin, consumer, delayTicks, periodTicks);
                return;
            } catch (Throwable t) {
                Main.getThis().getServer().getLogger().warning("[SchedulerUtil] runTimer() failed on global region scheduler: " + t.getMessage());
            }
            task.run();
            return;
        }
        Main.getThis().getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    public static void runEntity(Plugin plugin, Entity entity, Runnable task) {
        runEntity(plugin, entity, task, 0L);
    }

    public static void runEntity(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (isFolia()) {
            try {
                Object entityScheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                if (delayTicks > 0) {
                    try {
                        Method runDelayed = entityScheduler.getClass().getMethod(
                                "runDelayed",
                                Plugin.class,
                                Consumer.class,
                                Runnable.class,
                                long.class
                        );
                        Consumer<Object> consumer = scheduledTask -> task.run();
                        runDelayed.invoke(entityScheduler, plugin, consumer, null, delayTicks);
                        return;
                    } catch (NoSuchMethodException e) {
                        Main.getThis().getServer().getLogger().warning("[SchedulerUtil] runDelayed() not found on entity scheduler: " + e.getMessage());
                    }
                }
                try {
                    Method execute = entityScheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
                    execute.invoke(entityScheduler, plugin, task);
                    return;
                } catch (NoSuchMethodException e) {
                    Main.getThis().getServer().getLogger().warning("[SchedulerUtil] execute() not found on entity scheduler: " + e.getMessage());
                }
                try {
                    Method run = entityScheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                    Consumer<Object> consumer = scheduledTask -> task.run();
                    run.invoke(entityScheduler, plugin, consumer, null);
                    return;
                } catch (NoSuchMethodException e) {
                    Main.getThis().getServer().getLogger().warning("[SchedulerUtil] run() not found on entity scheduler: " + e.getMessage());
                }
            } catch (Throwable t) {
                Main.getThis().getServer().getLogger().warning("[SchedulerUtil] runEntity() failed: " + t.getMessage());
            }
            task.run();
            return;
        }
        if (delayTicks > 0) {
            Main.getThis().getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
        } else {
            Main.getThis().getServer().getScheduler().runTask(plugin, task);
        }
    }

    private static Object getServerMethod(String methodName) throws Exception {
        return Main.getThis().getServer().getClass()
                .getMethod(methodName)
                .invoke(Main.getThis().getServer());
    }
}