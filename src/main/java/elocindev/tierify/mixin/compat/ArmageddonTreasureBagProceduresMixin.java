package elocindev.tierify.mixin.compat;

import elocindev.tierify.compat.ArmageddonTreasureBagHooks;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(
    targets = {
        // Keep this list in sync with what you discovered in Armageddon.
        "net.mcreator.armageddonmod.procedures.ArionTreasurebagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.ElderGuardianTreasurebagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.EldorathTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.ElvenitePaladinTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.EnderDragonTreasurebagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.GoblinLordTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.IronColossusTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.NyxarisTheVeilOfOblivionTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.SanghorLordOfBloodTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.TheBringerOfDoomTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.TheCalamitiesTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.VaedricTreasureBagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.ZoranthNewbornOfTheZenithTreasureBagRightclickedOnBlockProcedure",
        "net.mcreator.armageddonmod.procedures.ZoranthTreasureBagRightclickedProcedure"
    },
    remap = false
)
public abstract class ArmageddonTreasureBagProceduresMixin {

    /**
     * Armageddon procedures spawn loot with:
     *   new ItemEntity(_level, x, y, z, new ItemStack(...))
     *
     * On Connector/Forge the ctor is:
     *   net.minecraft.world.entity.item.ItemEntity(Level, double, double, double, ItemStack)
     *
     * We rewrite the ItemStack argument (index 4) to a possibly-reforged stack.
     */
    @ModifyArg(
        method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/ItemEntity;<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"
        ),
        index = 4,
        remap = false,
        require = 0,
        expect = 0
    )
    private static ItemStack echelon$maybeReforgeSpawnedStack_xyz(
        ItemStack stack,
        @Coerce Object levelAccessor,
        double x, double y, double z,
        @Coerce Object entityObj
    ) {
        return echelon$apply(stack, entityObj);
    }

    /**
     * Optional fallback for any MCreator procedure variants that use:
     *   execute(LevelAccessor, Entity)
     *
     * Kept require=0 so it will not crash if absent.
     */
    @ModifyArg(
        method = "execute(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/ItemEntity;<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"
        ),
        index = 4,
        remap = false,
        require = 0,
        expect = 0
    )
    private static ItemStack echelon$maybeReforgeSpawnedStack_entityOnly(
        ItemStack stack,
        @Coerce Object levelAccessor,
        @Coerce Object entityObj
    ) {
        return echelon$apply(stack, entityObj);
    }

    private static ItemStack echelon$apply(ItemStack stack, Object entityObj) {
        if (stack == null) return null;
        try {
            // Under Connector, this cast is remapped appropriately at runtime.
            Entity yarnEntity = (Entity) entityObj;
            return ArmageddonTreasureBagHooks.maybeReforgeFromHeldBag(stack, yarnEntity);
        } catch (Throwable t) {
            return stack;
        }
    }
}
