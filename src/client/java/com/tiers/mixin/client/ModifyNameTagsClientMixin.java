package com.tiers.mixin.client;

import com.tiers.TiersClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class ModifyNameTagsClientMixin {

	@Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
	private void modifyDisplayName(CallbackInfoReturnable<Text> cir) {
		if (TiersClient.toggleMod) {
			PlayerEntity self = (PlayerEntity) (Object) this;
			Text originalNameText = cir.getReturnValue();
			cir.setReturnValue(TiersClient.getFullName(self.getGameProfile().getName(), originalNameText));
		}
	}
}
