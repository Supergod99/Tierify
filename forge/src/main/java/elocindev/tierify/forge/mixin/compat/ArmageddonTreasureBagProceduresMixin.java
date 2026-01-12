package elocindev.tierify.forge.mixin.compat;

import elocindev.tierify.forge.compat.ArmageddonTreasureBagHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "net.mcreator.armageddonmod.procedures.IronColossusTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.ArionTreasurebagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.EldorathTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.ElvenitePaladinTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.GoblinLordTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.VaedricTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.ZoranthTreasureBagRightclickedProcedure",

        "net.mcreator.armageddonmod.procedures.SanghorLordOfBloodTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.NyxarisTheVeilOfOblivionTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.TheBringerOfDoomTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.TheCalamitiesTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.ZoranthNewbornOfTheZenithTreasureBagRightclickedOnBlockProcedure"
}, remap = false)
public class ArmageddonTreasureBagProceduresMixin {

    private static final ThreadLocal<Object> ECHELON_BAG_USER = new ThreadLocal<>();

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private static void echelon$captureBagUser_xyz(LevelAccessor levelAccessor, double x, double y, double z,
                                                   Entity entityObj, CallbackInfo ci) {
        ECHELON_BAG_USER.set(entityObj);
    }

    @Inject(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private static void echelon$clearBagUser_xyz(LevelAccessor levelAccessor, double x, double y, double z,
                                                 Entity entityObj, CallbackInfo ci) {
        ECHELON_BAG_USER.remove();
    }

    @ModifyArg(
            method = {
                    "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V"
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
