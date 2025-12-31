package elocindev.tierify.mixin.compat;

import elocindev.tierify.compat.ArmageddonTreasureBagHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(
    targets = {
        "net.mcreator.armageddonmod.procedures.ArionTreasurebagRightclickedProcedure",
        "net.mcreator.armageddonmod.procedures.IronColossusTreasureBagRightclickedProcedure"
    },
    remap = false
)
public class ArmageddonTreasureBagProceduresMixin {
    @ModifyArg(
        method = "execute",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ItemEntity;<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V"
        ),
        index = 4,
        require = 0
    )
    private static ItemStack echelon$maybeReforgeSpawnedStack(ItemStack stack,
                                                             @Coerce Object levelAccessor,
                                                             double x, double y, double z,
                                                             @Coerce Object entityObj) {
        try {
            Entity yarnEntity = (Entity) entityObj;
            return ArmageddonTreasureBagHooks.maybeReforgeFromHeldBag(stack, yarnEntity);
        } catch (Throwable t) {
            return stack;
        }
    }
}
