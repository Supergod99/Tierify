package elocindev.tierify.mixin.compat;

import elocindev.tierify.compat.ArmageddonTreasureBagHooks;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(
        targets = {
                "net.mcreator.armageddonmod.procedures.ArionTreasurebagRightclickedProcedure",
                "net.mcreator.armageddonmod.procedures.IronColossusTreasureBagRightclickedProcedure"
        },
        remap = false
)
public abstract class ArmageddonTreasureBagProceduresMixin {

    /**
     * Armageddon procedures spawn loot with: new ItemEntity(_level, x, y, z, new ItemStack(...))
     * We rewrite the ItemStack argument (index 4) to a possibly-reforged stack.
     *
     * NOTE: remap=true here is important under Connector so the ItemEntity ctor target can be matched.
     */
    @ModifyArg(
            method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ItemEntity;<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V"
            ),
            index = 4,
            remap = true
    )
    private static ItemStack echelon$maybeReforgeSpawnedStack(
            ItemStack stack,
            Object world,
            double x,
            double y,
            double z,
            Object entity
    ) {
        if (entity instanceof net.minecraft.entity.Entity e) {
            return ArmageddonTreasureBagHooks.maybeReforgeFromHeldBag(stack, e);
        }
        return stack;
    }
}
