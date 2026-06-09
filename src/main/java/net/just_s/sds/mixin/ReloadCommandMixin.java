package net.just_s.sds.mixin;

import net.just_s.sds.config.Config;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(ReloadCommand.class)
public class ReloadCommandMixin {

    @Inject(method = "reloadPacks", at = @At("RETURN"))
    private static void sds$after_reload(Collection<String> selectedPacks, CommandSourceStack source, CallbackInfo ci) {
        Config.load();
    }
}
