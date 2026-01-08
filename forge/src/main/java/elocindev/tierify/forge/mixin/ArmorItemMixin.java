package elocindev.tierify.forge.mixin;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ArmorItem.class)
public class ArmorItemMixin {

    @Shadow @Final @Mutable
    private Multimap<Attribute, AttributeModifier> defaultModifiers;

    private static final UUID[] TIERIFY_ARMOR_MODIFIER_UUIDS = new UUID[] {
        UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"),
        UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"),
        UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"),
        UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"),
        UUID.fromString("4a88bc27-9563-4eeb-96d5-fe50917cc24f"),
        UUID.fromString("fee48d8c-1b51-4c46-9f4b-c58162623a7a")
    };

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void tierify$addKnockbackResistance(ArmorMaterial material, ArmorItem.Type type, Item.Properties properties,
                                                CallbackInfo ci) {
        if (material != ArmorMaterials.NETHERITE && material.getKnockbackResistance() > 0.0001f) {
            if (defaultModifiers.containsKey(Attributes.KNOCKBACK_RESISTANCE)) return;
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(defaultModifiers);
            UUID uuid = TIERIFY_ARMOR_MODIFIER_UUIDS[type.getSlot().getIndex()];
            builder.put(
                Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(
                    uuid,
                    "Armor knockback resistance",
                    (double) material.getKnockbackResistance(),
                    AttributeModifier.Operation.ADDITION
                )
            );
            defaultModifiers = builder.build();
        }
    }
}
