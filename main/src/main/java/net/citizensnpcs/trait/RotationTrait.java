package net.citizensnpcs.trait;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import net.citizensnpcs.Citizens;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.persistence.Persistable;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;

@TraitName("rotationtrait")
public class RotationTrait extends Trait {
    @Persist(reify = true)
    private final RotationParams globalParameters = new RotationParams();
    private final RotationSession globalSession = new RotationSession(globalParameters);
    private final List<PacketRotationSession> packetSessions = Lists.newArrayList();
    private final Map<UUID, PacketRotationSession> packetSessionsByUUID = Maps.newHashMap();

    public RotationTrait() {
        super("rotationtrait");
    }

    public void clearPacketSessions() {
        packetSessions.clear();
        packetSessionsByUUID.clear();
    }

    /**
     * @return The created session
     */
    public PacketRotationSession createPacketSession(RotationParams params) {
        if (params.filter == null && params.uuidFilter == null)
            throw new IllegalStateException();
        RotationSession session = new RotationSession(params);
        PacketRotationSession lrs = new PacketRotationSession(session);
        if (params.uuidFilter != null) {
            for (UUID uuid : params.uuidFilter) {
                packetSessionsByUUID.put(uuid, lrs);
            }
        } else {
            packetSessions.add(lrs);
        }
        return lrs;
    }

    private double getEyeY() {
        return NMS.getHeight(npc.getEntity());
    }

    /**
     * @return The global rotation parameters
     */
    public RotationParams getGlobalParameters() {
        return globalParameters;
    }

    public PacketRotationSession getPacketSession(Player player) {
        PacketRotationSession lrs = packetSessionsByUUID.get(player.getUniqueId());
        if (lrs != null && lrs.triple != null)
            return lrs;
        for (PacketRotationSession session : packetSessions) {
            if (session.accepts(player) && session.triple != null) {
                return session;
            }
        }
        return null;
    }

    public RotationSession getPhysicalSession() {
        return globalSession;
    }

    private double getX() {
        return npc.getStoredLocation().getX();
    }

    private double getY() {
        return npc.getStoredLocation().getY();
    }

    private double getZ() {
        return npc.getStoredLocation().getZ();
    }

    @Override
    public void run() {
        if (!npc.isSpawned())
            return;

        Set<PacketRotationSession> ran = Sets.newHashSet();
        for (Iterator<PacketRotationSession> itr = Iterables.concat(packetSessions, packetSessionsByUUID.values())
                .iterator(); itr.hasNext();) {
            PacketRotationSession session = itr.next();
            if (ran.contains(session))
                continue;
            ran.add(session);
            session.run(npc.getEntity());
            if (!session.isActive()) {
                itr.remove();
            }
        }

        if (npc.getNavigator().isNavigating()) {
            // npc.yHeadRot = rotateIfNecessary(npc.yHeadRot, npc.yBodyRot, 75);
            return;
        }

        globalSession.run(new EntityRotation(npc.getEntity()));
    }

    private static class EntityRotation extends RotationTriple {
        protected final Entity entity;

        public EntityRotation(Entity entity) {
            super(NMS.getYaw(entity), NMS.getHeadYaw(entity), entity.getLocation().getPitch());
            this.entity = entity;
        }

        @Override
        public void apply() {
            NMS.setBodyYaw(entity, bodyYaw);
            NMS.setHeadYaw(entity, headYaw);
            NMS.setPitch(entity, pitch);
        }
    }

    public static class PacketRotationSession {
        private boolean ended;
        private final RotationSession session;
        private PacketRotationTriple triple;

        public PacketRotationSession(RotationSession session) {
            this.session = session;
        }

        public boolean accepts(Player player) {
            return session.params.accepts(player);
        }

        public void end() {
            this.ended = true;
        }

        public float getBodyYaw() {
            return triple.bodyYaw;
        }

        public float getHeadYaw() {
            return triple.headYaw;
        }

        public float getPitch() {
            return triple.pitch;
        }

        // TODO: inheritance rather than composition?
        public RotationSession getSession() {
            return session;
        }

        public boolean isActive() {
            return !ended && session.isActive();
        }

        public void onPacketOverwritten() {
            if (triple == null)
                return;
            triple.record();
        }

        public void run(Entity entity) {
            if (triple == null) {
                triple = new PacketRotationTriple(entity);
            }
            session.run(triple);
            if (!session.isActive()) {
                Bukkit.broadcastMessage("!session");
                triple = null;
            }
        }
    }

    public static class PacketRotationTriple extends EntityRotation {
        private float lastBodyYaw;
        private float lastHeadYaw;
        private float lastPitch;

        public PacketRotationTriple(Entity entity) {
            super(entity);
        }

        @Override
        public void apply() {
            if (Math.abs(lastBodyYaw - bodyYaw) + Math.abs(lastHeadYaw - headYaw) + Math.abs(pitch - lastPitch) > 1) {
                NMS.sendRotationNearby(entity, bodyYaw, headYaw, pitch);
            }
        }

        public void record() {
            lastBodyYaw = bodyYaw;
            lastHeadYaw = headYaw;
            lastPitch = pitch;
        }
    }

    public static class RotationParams implements Persistable, Cloneable {
        private Function<Player, Boolean> filter;
        private boolean headOnly = true;
        private boolean immediate = false;
        private float maxPitchPerTick = 10;
        private float maxYawPerTick = 40;
        private boolean persist = false;
        private float[] pitchRange = { -180, 180 };
        private List<UUID> uuidFilter;
        private float[] yawRange = { -180, 180 };

        public boolean accepts(Player player) {
            return filter.apply(player);
        }

        @Override
        public RotationParams clone() {
            try {
                return (RotationParams) super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }

        public RotationParams filter(Function<Player, Boolean> filter) {
            this.filter = filter;
            return this;
        }

        public RotationParams headOnly(boolean headOnly) {
            this.headOnly = true;
            return this;
        }

        public RotationParams immediate(boolean immediate) {
            this.immediate = immediate;
            return this;
        }

        @Override
        public void load(DataKey key) {
            if (key.keyExists("headOnly")) {
                headOnly = true; //key.getBoolean("headOnly");
            }
            if (key.keyExists("immediate")) {
                immediate = key.getBoolean("immediate");
            }
            if (key.keyExists("maxPitchPerTick")) {
                maxPitchPerTick = (float) key.getDouble("maxPitchPerTick");
            }
            if (key.keyExists("maxYawPerTick")) {
                maxYawPerTick = (float) key.getDouble("maxYawPerTick");
            }
            if (key.keyExists("yawRange")) {
                String[] parts = key.getString("yawRange").split(",");
                yawRange = new float[] { Float.parseFloat(parts[0]), Float.parseFloat(parts[1]) };
            }
            if (key.keyExists("pitchRange")) {
                String[] parts = key.getString("pitchRange").split(",");
                pitchRange = new float[] { Float.parseFloat(parts[0]), Float.parseFloat(parts[1]) };
            }
        }

        public RotationParams maxPitchPerTick(float val) {
            this.maxPitchPerTick = val;
            return this;
        }

        public RotationParams maxYawPerTick(float val) {
            this.maxYawPerTick = val;
            return this;
        }

        public RotationParams persist(boolean persist) {
            this.persist = persist;
            return this;
        }

        public RotationParams pitchRange(float[] val) {
            this.pitchRange = val;
            return this;
        }

        public float rotateHeadYawTowards(int t, float yaw, float targetYaw) {
            float out = rotateTowards(yaw, targetYaw, maxYawPerTick);
            return Util.clamp(out, yawRange[0], yawRange[1], 360);
        }

        public float rotatePitchTowards(int t, float pitch, float targetPitch) {
            float out = rotateTowards(pitch, targetPitch, maxPitchPerTick);
            return Util.clamp(out, pitchRange[0], pitchRange[1], 360);
        }

        private float rotateTowards(float target, float current, float maxRotPerTick) {
            float diff = Util.clamp(current - target);
            return target + clamp(diff, -maxRotPerTick, maxRotPerTick);
        }

        /*
         *  public Vector3 SuperSmoothVector3Lerp( Vector3 pastPosition, Vector3 pastTargetPosition, Vector3 targetPosition, float time, float speed ){
         Vector3 f = pastPosition - pastTargetPosition + (targetPosition - pastTargetPosition) / (speed * time);
         return targetPosition - (targetPosition - pastTargetPosition) / (speed*time) + f * Mathf.Exp(-speed*time);
         }
         */

        @Override
        public void save(DataKey key) {
            if (headOnly) {
                key.setBoolean("headOnly", headOnly);
            }

            if (immediate) {
                key.setBoolean("immediate", immediate);
            }

            if (maxPitchPerTick != 10) {
                key.setDouble("maxPitchPerTick", maxPitchPerTick);
            } else {
                key.removeKey("maxPitchPerTick");
            }

            if (maxYawPerTick != 40) {
                key.setDouble("maxYawPerTick", maxYawPerTick);
            } else {
                key.removeKey("maxYawPerTick");
            }

            if (pitchRange[0] != -180 || pitchRange[1] != 180) {
                key.setString("pitchRange", pitchRange[0] + "," + pitchRange[1]);
            } else {
                key.removeKey("pitchRange");
            }

            if (yawRange[0] != -180 || yawRange[1] != 180) {
                key.setString("yawRange", yawRange[0] + "," + yawRange[1]);
            } else {
                key.removeKey("yawRange");
            }
        }

        public RotationParams uuidFilter(List<UUID> uuids) {
            this.uuidFilter = uuids;
            return this;
        }

        public RotationParams uuidFilter(UUID... uuids) {
            return uuidFilter(Arrays.asList(uuids));
        }

        public RotationParams yawRange(float[] val) {
            this.yawRange = val;
            return this;
        }
    }

    public class RotationSession {
        private final RotationParams params;
        private int t = -1;
        private Supplier<Float> targetPitch = () -> 0F;
        private Supplier<Float> targetYaw = targetPitch;

        public RotationSession(RotationParams params) {
            this.params = params;
        }

        public float getTargetPitch() {
            return targetPitch.get();
        }

        public float getTargetYaw() {
            switch (npc.getEntity().getType()) {
                case PHANTOM:
                    return Util.clamp(targetYaw.get() + 45);
                case ENDER_DRAGON:
                    return Util.clamp(targetYaw.get() - 180);
                default:
                    return targetYaw.get();
            }
        }

        public boolean isActive() {
            return params.persist || t >= 0;
        }

        /**
         * Rotates to face target entity
         *
         * @param target
         *            The target entity to face
         */
        public void rotateToFace(Entity target) {
            Location loc = target.getLocation();
            loc.setY(loc.getY() + NMS.getHeight(target));
            rotateToFace(loc);
        }

        /**
         * Rotates to face target location
         *
         * @param target
         *            The target location to face
         */
        public void rotateToFace(Location target) {
            t = 0;
            targetPitch = () -> {
                double dx = target.getX() - getX();
                double dy = target.getY() - (getY() + getEyeY());
                double dz = target.getZ() - getZ();
                double diag = Math.sqrt((float) (dx * dx + dz * dz));
                return (float) -Math.toDegrees(Math.atan2(dy, diag));
            };
            targetYaw = () -> {
                return (float) Math.toDegrees(Math.atan2(target.getZ() - getZ(), target.getX() - getX())) - 90.0F;
            };
        }

        /**
         * Rotate to have the given yaw and pitch
         *
         * @param yaw
         * @param pitch
         */
        public void rotateToHave(float yaw, float pitch) {
            t = 0;
            targetYaw = () -> yaw;
            targetPitch = () -> pitch;
        }

        private void run(RotationTriple rot) {
            if (!isActive()) {
                //
                if (Citizens.CITIZENS.contains(npc.getId())) {
                    //Bukkit.broadcastMessage("!isactive");
                    if (rot.bodyYaw > 0 && rot.bodyYaw <= 90) {
                        //Bukkit.broadcastMessage("rotate 0 0");
                        rotateToHave(0, 0);
                    } else {
                        //Bukkit.broadcastMessage("!rotate -180 0");
                        //trotateToHave(-180, 0);
                    }
                }
                return;
            }

            rot.headYaw = params.immediate ? getTargetYaw()
                    : Util.clamp(params.rotateHeadYawTowards(t, rot.headYaw, getTargetYaw()));

            if (!params.headOnly && !Citizens.CITIZENS.contains(npc.getId())) {
                float d = Util.clamp(rot.headYaw - 20);
                if (d > rot.bodyYaw) {
                    rot.bodyYaw = d;
                }
                if (d != rot.bodyYaw) {
                    d = Util.clamp(rot.headYaw + 20);
                    if (d < rot.bodyYaw) {
                        rot.bodyYaw = d;
                    }
                }
            }

            rot.pitch = params.immediate ? getTargetPitch() : params.rotatePitchTowards(t, rot.pitch, getTargetPitch());
            t++;

            //Bukkit.broadcastMessage("RunBodyYaw-HeadYaw: " +  rot.bodyYaw + " - " + rot.headYaw);
            if (Citizens.CITIZENS.contains(npc.getId())) {
                //Bukkit.broadcastMessage(npc.getId() + "-RunBodyYaw-HeadYaw: " +  rot.bodyYaw + " - " + rot.headYaw);
                if (rot.bodyYaw > 0 && rot.bodyYaw <= 90) { // must -180 - 0 88.58223 - -234.15111
                    if (rot.headYaw > 45) {
                        rot.headYaw = 45;
                    } else if (rot.headYaw < 0) {
                        float clone = rot.headYaw * -1;
                        if (clone > 45) {
                            rot.headYaw = -45;
                        }
                    }
                } else {
                    int intBodyYaw = Math.round(rot.bodyYaw);
                    if (intBodyYaw == -95 || intBodyYaw == -96) {
                        float clone = rot.headYaw * -1;
                        if (clone < 40) {
                            rot.headYaw = -40;
                        } else if (clone > 130) {
                            rot.headYaw = -140;
                        }
                    } else if (intBodyYaw == 90 || intBodyYaw == 91) {
                        if (rot.headYaw > 130) {
                            rot.headYaw = 130;
                        } else if (rot.headYaw < 50) {
                            rot.headYaw = 50;
                        }
                    } else {
                        if (rot.headYaw > 0) {
                            if (rot.headYaw < 135) {
                                rot.headYaw = 135;
                            }
                        } else {
                            float clone = rot.headYaw * -1;
                            if (clone < 135) {
                                rot.headYaw = -135;
                            }
                        }
                    }
                }
            }

            /*if (Math.abs(rot.pitch - getTargetPitch()) + Math.abs(rot.headYaw - getTargetYaw()) < 0.1) {
                t = -1;
                rot.bodyYaw = rot.headYaw;
            }*/

            rot.apply();
        }
    }

    private static abstract class RotationTriple implements Cloneable {
        protected float bodyYaw, headYaw, pitch;

        public RotationTriple(float bodyYaw, float headYaw, float pitch) {
            this.bodyYaw = bodyYaw;
            this.headYaw = headYaw;
            this.pitch = pitch;
        }

        public abstract void apply();

        @Override
        public RotationTriple clone() {
            try {
                return (RotationTriple) super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private static float clamp(float orig, float min, float max) {
        return Math.max(min, Math.min(max, orig));
    }
}
