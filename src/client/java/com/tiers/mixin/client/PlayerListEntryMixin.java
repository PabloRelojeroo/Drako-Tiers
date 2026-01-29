package com.tiers.mixin.client;

import com.tiers.TiersClient;
import com.tiers.profiles.PlayerProfile;
import com.tiers.profiles.Status;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void modifyTabListDisplayName(CallbackInfoReturnable<Text> cir) {
        if (TiersClient.toggleMod) {
            PlayerListEntry self = (PlayerListEntry) (Object) this;
            String playerName = self.getProfile().getName();
            
            PlayerProfile profile = TiersClient.addGetPlayer(playerName, false);
            
            if (profile.status == Status.READY) {
                Text originalText = cir.getReturnValue();
                if (originalText == null) {
                    originalText = Text.literal(playerName);
                }
                cir.setReturnValue(TiersClient.getFullName(playerName, originalText));
            }
        }
    }
}
