package org.figuramc.figura.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.figuramc.figura.backend2.ForgeNetworking;

@Mod("figura")
public class FiguraModForge {
    // dummy empty mod class, we are client only
    public FiguraModForge() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> FiguraModClientForge::initClient);
        DistExecutor.safeRunWhenOn(Dist.DEDICATED_SERVER, () -> FiguraModServerForge::initServer);
        ForgeNetworking.init();
    }
}
