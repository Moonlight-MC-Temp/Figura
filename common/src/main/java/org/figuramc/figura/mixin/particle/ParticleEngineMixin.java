package org.figuramc.figura.mixin.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.ParticleEngineAccessor;
import org.figuramc.figura.lua.api.event.EventsAPI;
import org.figuramc.figura.lua.api.particle.LuaParticle;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin implements ParticleEngineAccessor {

    @Final @Shadow private Map<ResourceLocation, SpriteSet> spriteSets;

    @Shadow @Nullable protected abstract <T extends ParticleOptions> Particle makeParticle(T parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ);

    @Shadow public abstract void add(Particle particle);

    @Unique private final HashMap<Particle, UUID> particleMap = new HashMap<>();

    @Inject(at = @At("HEAD"), method = "add", cancellable = true)
    private void figura$checkParticle(Particle particle, CallbackInfo ci) {
        LuaParticle luaParticle = new LuaParticle("event", particle, null);
        AvatarManager.executeAll("checkParticle", avatar -> {
            var results = avatar.run("PARTICLE_CREATED", avatar.worldRender, luaParticle);
            if (results == null) return;
            for (int i = 0; i < results.narg(); i++) {
                var value = results.arg(1);
                if (value.isboolean() && value.toboolean()) ci.cancel();
            }
        });
    }

    // This fixes a conflict with Optifine having slightly different args + it should be more stable in general, capturing Locals is bad practice
    @ModifyVariable(method = "tickParticleList", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V", ordinal = 0))
    private Particle tickParticleList(Particle particle) {
        particleMap.remove(particle);
        return particle;
    }

    @Override @Intrinsic
    public <T extends ParticleOptions> Particle figura$makeParticle(T parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        return this.makeParticle(parameters, x, y, z, velocityX, velocityY, velocityZ);
    }

    @Override @Intrinsic
    public void figura$spawnParticle(Particle particle, UUID owner) {
        this.add(particle);
        particleMap.put(particle, owner);
    }

    @Override @Intrinsic
    public void figura$clearParticles(UUID owner) {
        Iterator<Map.Entry<Particle, UUID>> iterator = particleMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Particle, UUID> entry = iterator.next();
            if ((owner == null || entry.getValue().equals(owner))) {
                if (entry.getKey() != null)
                    entry.getKey().remove();
                iterator.remove();
            }
        }
    }

    @Override @Intrinsic
    public SpriteSet figura$getParticleSprite(ResourceLocation particleID) {
        return spriteSets.get(particleID);
    }
}
