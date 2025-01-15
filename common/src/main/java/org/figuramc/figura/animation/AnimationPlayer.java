package org.figuramc.figura.animation;

import net.minecraft.util.Mth;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.model.FiguraModelPart;

import java.util.List;
import java.util.Map;

public class AnimationPlayer {

    public static int tick(Animation anim, int limit) {
        if (anim.playState == Animation.PlayState.STOPPED)
            return limit;

        FiguraMod.pushProfiler(anim.name);

        if (anim.playState != Animation.PlayState.PAUSED)
            anim.tick();

        for (Map.Entry<FiguraModelPart, List<Animation.AnimationChannel>> entry : anim.animationParts) {
            FiguraModelPart part = entry.getKey();

            if (part.lastAnimationPriority > anim.priority)
                continue;

            FiguraMod.pushProfiler(part.name);

            boolean merge = part.lastAnimationPriority == anim.priority;
            part.lastAnimationPriority = anim.priority;
            part.animated = true;

            for (Animation.AnimationChannel channel : entry.getValue()) {
                if (limit <= 0) {
                    FiguraMod.popProfiler(2);
                    return limit;
                }

                TransformType type = channel.type();
                FiguraMod.pushProfiler(type.name());

                Keyframe[] keyframes = channel.keyframes();
                if (keyframes.length == 0) {
                    FiguraMod.popProfiler(3);
                    return limit;
                }

                int currentIndex = Math.max(0, Mth.binarySearch(0, keyframes.length, index -> anim.frameTime <= keyframes[index].getTime()) - 1);
                int nextIndex = Math.min(keyframes.length - 1, currentIndex + 1);

                Keyframe current = keyframes[currentIndex];
                Keyframe next = keyframes[nextIndex];

                float delta;
                if (current == next) {
                    delta = 0;
                } else {
                    float timeDiff = anim.frameTime - current.getTime();
                    delta = Math.min(Math.max(timeDiff / (next.getTime() - current.getTime()), 0), 1);
                }

                Interpolation interpolation = next.getInterpolation() == Interpolation.BEZIER ? Interpolation.BEZIER : current.getInterpolation();
                FiguraVec3 transform = interpolation.generate(keyframes, currentIndex, nextIndex, anim.blend, delta, type, anim.loop == Animation.LoopMode.LOOP);
                type.apply(part, transform, merge);

                switch (type) {
                    case ROTATION, GLOBAL_ROT -> {
                        if (anim.getOverrideRot())
                            part.animationOverride |= 1;
                        else if (!merge) {
                            part.animationOverride = part.animationOverride & 6;
                        }
                    }
                    case POSITION -> {
                        if (anim.getOverridePos())
                            part.animationOverride |= 2;
                        else if (!merge) {
                            part.animationOverride = part.animationOverride & 5;
                        }
                    }
                    case SCALE -> {
                        if (anim.getOverrideScale())
                            part.animationOverride |= 4;
                        else if (!merge) {
                            part.animationOverride = part.animationOverride & 3;
                        }
                    }
                }

                limit--;
                FiguraMod.popProfiler();
            }

            FiguraMod.popProfiler();
        }

        FiguraMod.popProfiler();
        return limit;
    }

    public static void clear(Animation anim) {
        FiguraVec3 zero = FiguraVec3.of();
        for (Map.Entry<FiguraModelPart, List<Animation.AnimationChannel>> entry : anim.animationParts) {
            FiguraModelPart part = entry.getKey();
            if (!part.animated)
                continue;

            part.animPosition(zero, false);
            part.animRotation(zero, false);
            part.animScale(FiguraVec3.of(1, 1, 1), false);
            part.lastAnimationPriority = Integer.MIN_VALUE;
            part.animated = false;
            part.animationOverride = 0;
        }
    }
}
