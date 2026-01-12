package elocindev.tierify.forge.mixin.compat;

import elocindev.tierify.forge.compat.ArmageddonTreasureBagHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "net.mcreator.armageddonmod.procedures.EnderDragonTreasurebagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.ElderGuardianTreasurebagRightclickedOnBlockProcedure"
}, remap = false)
public class ArmageddonTreasureBagEntityProceduresMixin {

    private static final ThreadLocal<Object> ECHELON_BAG_USER = new ThreadLocal<>();

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private static void echelon$captureBagUser_entityOnly(LevelAccessor levelAccessor, Entity entityObj, CallbackInfo ci) {
        ECHELON_BAG_USER.set(entityObj);
    }

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private static void echelon$clearBagUser_entityOnly(LevelAccessor levelAccessor, Entity entityObj, CallbackInfo ci) {
        ECHELON_BAG_USER.remove();
    }

    @ModifyArg(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/item/ItemEntity;<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"
            ),
            index = 4,
            remap = false,
            require = 0
    )
    private static ItemStack echelon$maybeReforgeSpawnedStack(ItemStack stack) {
        Object userObj = ECHELON_BAG_USER.get();
        if (!(userObj instanceof Entity user)) return stack;

        try {
            return ArmageddonTreasureBagHooks.maybeReforgeFromHeldBag(stack, user);
        } catch (Throwable ignored) {
            return stack;
        }
    }
}
