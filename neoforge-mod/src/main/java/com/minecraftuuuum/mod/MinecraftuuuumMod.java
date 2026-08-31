package com.minecraftuuuum.mod;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(MinecraftuuuumMod.MOD_ID)
public final class MinecraftuuuumMod {
    public static final String MOD_ID = "minecraftuuuum";
    private static final Logger LOG = LogUtils.getLogger();
    private static final Pattern P = Pattern.compile(
            "\\{P:([^}|]+)(?:\\|([^}]+))?\\}", Pattern.CASE_INSENSITIVE);

    public MinecraftuuuumMod(IEventBus modBus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(MinecraftuuuumMod::onRegisterCommands);
        LemmaPackLoader.loadFromDatapacks();
        LOG.info("Minecraftuuuum! lemmas ready");
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal(MOD_ID)
                        .then(Commands.literal("run")
                                .then(Commands.argument("script", MessageArgument.message())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String script = MessageArgument.getMessage(ctx, "script").getString();
                                            runScript(player, script);
                                            return 1;
                                        }))));
    }

    static void runScript(ServerPlayer player, String script) {
        Matcher m = P.matcher(script);
        while (m.find()) {
            String term = m.group(1).trim().toLowerCase(Locale.ROOT);
            String props = m.group(2);
            String registry = prop(props, "registry-id");
            switch (term) {
                case "say" -> player.sendSystemMessage(Component.literal(prop(props, "text")));
                case "place" -> place(player, registry == null ? "minecraft:oak_planks" : registry);
                case "give" -> give(player, registry == null ? "minecraft:apple" : registry);
                case "spawn" -> spawn(player, registry == null ? "minecraft:creeper" : registry);
                case "if" -> {
                    BlockState look = player.level().getBlockState(BlockPos.containing(player.getEyePosition()));
                    if (!look.isAir()) {
                        player.sendSystemMessage(Component.literal("if: block present"));
                    }
                }
                default -> player.sendSystemMessage(Component.literal("lemma " + term));
            }
        }
    }

    private static void place(ServerPlayer player, String id) {
        var loc = ResourceLocation.parse(id);
        var block = BuiltInRegistries.BLOCK.get(loc);
        BlockPos pos = player.blockPosition().relative(player.getDirection());
        player.level().setBlock(pos, block.defaultBlockState(), 3);
    }

    private static void give(ServerPlayer player, String id) {
        var loc = ResourceLocation.parse(id);
        var item = BuiltInRegistries.ITEM.get(loc);
        player.addItem(new ItemStack(item));
    }

    private static void spawn(ServerPlayer player, String id) {
        var loc = ResourceLocation.parse(id);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(loc);
        ServerLevel level = player.serverLevel();
        var e = type.create(level);
        if (e != null) {
            e.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
            level.addFreshEntity(e);
        }
    }

    private static String prop(String props, String key) {
        if (props == null) {
            return key.equals("text") ? "Minecraftuuuum!" : null;
        }
        for (String part : props.split("\\|")) {
            int eq = part.indexOf('=');
            if (eq > 0 && part.substring(0, eq).trim().equals(key)) {
                return part.substring(eq + 1).trim();
            }
        }
        return key.equals("text") ? "Minecraftuuuum!" : null;
    }
}
