package org.figuramc.figura.entries;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import org.figuramc.figura.model.ParentType;

import java.util.Collection;
import java.util.function.Function;

public interface FiguraVanillaPart {

    /**
     * @return a string of this mod's ID, case-insensitive
     * the ID will be used in the part's name, to avoid conflicts with other mods
     * a group of all the parts will be generated with the ID as name, and added into the "ALL" group
     */
    String getID();

    /**
     * @return returns a collection of a pair of the part name and a function to get its model part
     * the function consists about giving the current Entity Model and getting a Model Part for that Entity
     * the string is the parts name, case-insensitive, where the mod ID will be added into the final part name, as "ID_NAME"
     */
    Collection<Pair<String, Function<EntityModel<?>, ModelPart>>> getParts();

    /**
     * @return returns a collection of a pair of the part name and a function to get its model part
     * the function consists about giving the current Entity Model and getting a Model Part for that Entity
     * the string is the parts name, case-insensitive, where the mod ID will be added into the final part name, as "ID_NAME",
     * this version supports Parent Types, you can use either.
     */

    Collection<Pair<String, Pair<Function<EntityModel<?>, ModelPart>, ParentType>>> getPartsWithParent();
}
