package org.figuramc.figura.lua.api.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.ducks.ParticleEngineAccessor;
import org.figuramc.figura.lua.LuaNotNil;
import org.figuramc.figura.lua.LuaWhitelist;
import org.figuramc.figura.lua.api.world.WorldAPI;
import org.figuramc.figura.lua.docs.LuaMethodDoc;
import org.figuramc.figura.lua.docs.LuaMethodOverload;
import org.figuramc.figura.lua.docs.LuaTypeDoc;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.LuaUtils;
import org.luaj.vm2.LuaError;

import java.util.Objects;

@LuaWhitelist
@LuaTypeDoc(
        name = "ParticleAPI",
        value = "particles"
)
public class ParticleAPI {

    private final Avatar owner;

    public ParticleAPI(Avatar owner) {
        this.owner = owner;
    }

    public static ParticleEngineAccessor getParticleEngine() {
        return (ParticleEngineAccessor) Minecraft.getInstance().particleEngine;
    }

    private LuaParticle generate(String id, double x, double y, double z, double w, double t, double h) {
        try {
            id = convertOldToNewParticleFormat(id);
            ParticleOptions options = ParticleArgument.readParticle(new StringReader(id), WorldAPI.getCurrentWorld().registryAccess());
            Particle p = getParticleEngine().figura$makeParticle(options, x, y, z, w, t, h);
            if (p == null) throw new LuaError("Could not parse particle \"" + id + "\"");
            return new LuaParticle(id, p, owner);
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }

    private String convertOldToNewParticleFormat(String id) {
        if (id.contains("block ")) {
            id = id.replaceFirst("minecraft:", "");
            String blockPart = id.split("block ")[1];
            String ret = "block{block_state:{";
            String blockName;

            if (blockPart.contains("[")) {
                blockName = blockPart.split("\\[")[0];
                String properties = id.substring(id.indexOf("[")+1, id.indexOf("]")+1);
                properties = properties.replaceAll("=", ":\"").replaceAll(",", "\",").replace("]", "\"");
                ret += ("Name:\"" + blockName + "\",Properties:{" + properties + "}");
            } else {
                blockName = blockPart;
                ret += ("Name:\"" + blockName + "\"");
            }

            ret += "}}";
            id = ret;
        } else if (id.contains("block_marker ")) {
            id = id.replaceFirst("minecraft:", "");
            String blockPart = id.split("block_marker ")[1];
            String ret = "block_marker{block_state:{";
            String blockName;

            if (blockPart.contains("[")) {
                blockName = blockPart.split("\\[")[0];
                String properties = id.substring(id.indexOf("[")+1, id.indexOf("]")+1);
                properties = properties.replaceAll("=", ":\"").replaceAll(",", "\",").replace("]", "\"");
                ret += ("Name:\"" + blockName + "\",Properties:{" + properties + "}");
            } else {
                blockName = blockPart;
                ret += ("Name:\"" + blockName + "\"");
            }

            ret += "}}";
            id = ret;
        } else if (id.contains("falling_dust ")) {
            id = id.replaceFirst("minecraft:", "");
            String blockPart = id.split("falling_dust ")[1];
            String ret = "falling_dust{block_state:{";
            String blockName;

            if (blockPart.contains("[")) {
                blockName = blockPart.split("\\[")[0];
                String properties = id.substring(id.indexOf("[")+1, id.indexOf("]")+1);
                properties = properties.replaceAll("=", ":\"").replaceAll(",", "\",").replace("]", "\"");
                ret += ("Name:\"" + blockName + "\",Properties:{" + properties + "}");
            } else {
                blockName = blockPart;
                ret += ("Name:\"" + blockName + "\"");
            }

            ret += "}}";
            id = ret;
        } else if (id.contains("dust ")) {
            id = id.replaceFirst("minecraft:", "");
            String[] parts = id.split(" ");
            String ret = "dust{";
            if (Objects.equals(parts[0], "dust")) {
                String num1 = parts[1];
                String num2 = parts[2];
                String num3 = parts[3];
                ret += ("color:[" + num1 + "," + num2 + "," + num3+ "],scale:"+parts[4]+"}");
            }

            ret += "}";
            id = ret;
        } else if (id.contains("dust_color_transition ")) {
            id = id.replaceFirst("minecraft:", "");
            String[] parts = id.split(" ");
            String ret = "dust_color_transition{";
            if (Objects.equals(parts[0], "dust_color_transition")) {
                String rFrom = parts[1];
                String gFrom = parts[2];
                String bFrom = parts[3];
                String scale = parts[4];
                String rTo = parts[5];
                String gTo = parts[6];
                String bTo = parts[7];
                ret += ("from_color:[" + rFrom + "," + gFrom + "," + bFrom + "],scale:"+scale+",to_color:["+rTo + "," + gTo + "," + bTo + "]");
            }
            ret += "}";
            id = ret;
        } else if (id.contains("item ")) {
            id = id.replaceFirst("minecraft:", "");
            String[] parts = id.split(" ");
            String ret = "item{";
            if (Objects.equals(parts[0], "item")) {
                String itemId = parts[1];
                ret += ("item:{id:"+itemId+"}");
            }
            ret += "}";
            id = ret;
        } else if (id.contains("sculk_charge ")) {
            id = id.replaceFirst("minecraft:", "");
            String[] parts = id.split(" ");
            String ret = "sculk_charge{";
            if (Objects.equals(parts[0], "sculk_charge")) {
                String roll = parts[1];
                ret += ("roll:"+roll);
            }
            ret += "}";
            id = ret;
        } else if (id.contains("shriek ")) {
            id = id.replaceFirst("minecraft:", "");
            String[] parts = id.split(" ");
            String ret = "shriek{";
            if (Objects.equals(parts[0], "shriek")) {
                String delay = parts[1];
                ret += ("delay:"+delay);
            }
            ret += "}";
            id = ret;
        } else if (id.contains("vibration ")) {
            id = id.replaceFirst("minecraft:", "");
            String[] parts = id.split(" ");
            String ret = "vibration{";
            if (Objects.equals(parts[0], "vibration")) {
                String dsX = parts[1];
                String dsY = parts[2];
                String dsZ = parts[3];
                String arr = parts[4];
                ret += ("arrival_in_ticks:"+arr+",destination:{type:block,pos:["+dsX+","+dsY+","+dsZ+"]}");
            }
            ret += "}";
            id = ret;
        } else if (id.contains("entity_effect")) {
            if (!id.contains("{"))
                id += "{color:[0.0,0.0,0.0,1.0]}";
        }
        return id;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = {
                    @LuaMethodOverload(
                            argumentTypes = {String.class, FiguraVec3.class},
                            argumentNames = {"name", "pos"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, FiguraVec3.class, FiguraVec3.class},
                            argumentNames = {"name", "pos", "vel"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Double.class, Double.class, Double.class},
                            argumentNames = {"name", "posX", "posY", "posZ"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, FiguraVec3.class, Double.class, Double.class, Double.class},
                            argumentNames = {"name", "pos", "velX", "velY", "velZ"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Double.class, Double.class, Double.class, FiguraVec3.class},
                            argumentNames = {"name", "posX", "posY", "posZ", "vel"}
                    ),
                    @LuaMethodOverload(
                            argumentTypes = {String.class, Double.class, Double.class, Double.class, Double.class, Double.class, Double.class},
                            argumentNames = {"name", "posX", "posY", "posZ", "velX", "velY", "velZ"}
                    )
            },
            value = "particles.new_particle"
    )
    public LuaParticle newParticle(@LuaNotNil String id, Object x, Object y, Double z, Object w, Double t, Double h) {
        FiguraVec3 pos, vel;

        // Parse pos and vel
        Pair<FiguraVec3, FiguraVec3> pair = LuaUtils.parse2Vec3("newParticle", x, y, z, w, t, h, 2);
        pos = pair.getFirst();
        vel = pair.getSecond();

        LuaParticle particle = generate(id, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
        particle.spawn();
        return particle;
    }

    @LuaWhitelist
    @LuaMethodDoc("particles.remove_particles")
    public ParticleAPI removeParticles() {
        getParticleEngine().figura$clearParticles(owner.owner);
        return this;
    }

    @LuaWhitelist
    @LuaMethodDoc(
            overloads = @LuaMethodOverload(
                    argumentTypes = String.class,
                    argumentNames = "id"
            ),
            value = "particles.is_present"
    )
    public boolean isPresent(String id) {
        try {
            ParticleOptions options = ParticleArgument.readParticle(new StringReader(id), (HolderLookup.Provider) BuiltInRegistries.PARTICLE_TYPE.asLookup());
            return getParticleEngine().figura$makeParticle(options, 0, 0, 0, 0, 0, 0) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    @LuaWhitelist
    public LuaParticle __index(String id) {
        return generate(id, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public String toString() {
        return "ParticleAPI";
    }
}
