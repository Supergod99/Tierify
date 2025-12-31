package elocindev.tierify.mixin.compat;

import elocindev.tierify.compat.ArmageddonTreasureBagHooks;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = {
        // Right-click in hand
        "net.mcreator.armageddonmod.procedures.IronColossusTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.SanghorLordOfBloodTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.NyxarisTheVeilOfOblivionTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.ArionTreasurebagRightclickedProcedure",

        // Right-click on block
        "net.mcreator.armageddonmod.procedures.IronColossusTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.SanghorLordOfBloodTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.NyxarisTheVeilOfOblivionTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.ArionTreasurebagRightclickedOnBlockProcedure",

        // Some bags use a different execute signature
        "net.mcreator.armageddonmod.procedures.ElderGuardianTreasurebagRightclickedOnBlockProcedure"
})
public class ArmageddonTreasureBagProceduresMixin {

    private static final ThreadLocal<Object> ECHELON_BAG_USER = new ThreadLocal<>();

    // Capture entity (user)

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private static void echelon$captureBagUser_xyz(@Coerce Object levelAccessor, double x, double y, double z, @Coerce Object entityObj, CallbackInfo ci) {
        ECHELON_BAG_USER.set(entityObj);
    }

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private static void echelon$captureBagUser_entityOnly(@Coerce Object levelAccessor, @Coerce Object entityObj, CallbackInfo ci) {
        ECHELON_BAG_USER.set(entityObj);
    }

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private static void echelon$clearBagUser_xyz(@Coerce Object levelAccessor, double x, double y, double z, @Coerce Object entityObj, CallbackInfo ci) {
        ECHELON_BAG_USER.remove();
    }

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private static void echelon$clearBagUser_entityOnly(@Coerce Object levelAccessor, @Coerce Object entityObj, CallbackInfo ci) {
        ECHELON_BAG_USER.remove();
    }

    // Reforge spawned drops

    @ModifyArg(
            method = {
                    "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
                    "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V"
            },
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
