package net.citizensnpcs.nms.v1_12_R1.entity;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftVex;
import org.bukkit.entity.Vex;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.event.NPCEnderTeleportEvent;
import net.citizensnpcs.api.event.NPCKnockbackEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_12_R1.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_12_R1.DamageSource;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityVex;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.SoundEffect;
import net.minecraft.server.v1_12_R1.World;

public class VexController extends MobEntityController {
    public VexController() {
        super(EntityVexNPC.class);
    }

    @Override
    public Vex getBukkitEntity() {
        return (Vex) super.getBukkitEntity();
    }

    public static class EntityVexNPC extends EntityVex implements NPCHolder {
        private final CitizensNPC npc;

        public EntityVexNPC(World world) {
            this(world, null);
        }

        public EntityVexNPC(World world, NPC npc) {
            super(world);
            this.npc = (CitizensNPC) npc;
            if (npc != null) {
                setNoGravity(true);
            }
        }

        @Override
        public void a(Entity entity, float strength, double dx, double dz) {
            NPCKnockbackEvent event = new NPCKnockbackEvent(npc, strength, dx, dz);
            Bukkit.getPluginManager().callEvent(event);
            Vector kb = event.getKnockbackVector();
            if (!event.isCancelled()) {
                super.a(entity, (float) event.getStrength(), kb.getX(), kb.getZ());
            }
        }

        @Override
        protected SoundEffect cf() {
            return NMSImpl.getSoundEffect(npc, super.cf(), NPC.Metadata.DEATH_SOUND);
        }

        @Override
        public void collide(net.minecraft.server.v1_12_R1.Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.collide(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        protected SoundEffect d(DamageSource damagesource) {
            return NMSImpl.getSoundEffect(npc, super.d(damagesource), NPC.Metadata.HURT_SOUND);
        }

        @Override
        public boolean d(NBTTagCompound save) {
            return npc == null ? super.d(save) : false;
        }

        @Override
        public void enderTeleportTo(double d0, double d1, double d2) {
            if (npc == null) {
                super.enderTeleportTo(d0, d1, d2);
                return;
            }
            NPCEnderTeleportEvent event = new NPCEnderTeleportEvent(npc);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                super.enderTeleportTo(d0, d1, d2);
            }
        }

        @Override
        public void f(double x, double y, double z) {
            Vector vector = Util.callPushEvent(npc, x, y, z);
            if (vector != null) {
                super.f(vector.getX(), vector.getY(), vector.getZ());
            }
        }

        @Override
        protected SoundEffect F() {
            return NMSImpl.getSoundEffect(npc, super.F(), NPC.Metadata.AMBIENT_SOUND);
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(bukkitEntity instanceof NPCHolder))
                bukkitEntity = new VexNPC(this);
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public boolean isLeashed() {
            if (npc == null) {
                return super.isLeashed();
            }
            boolean protectedDefault = npc.isProtected();
            if (!protectedDefault || !npc.data().get(NPC.Metadata.LEASH_PROTECTED, protectedDefault))
                return super.isLeashed();
            if (super.isLeashed()) {
                unleash(true, false); // clearLeash with client update
            }
            return false; // shouldLeash
        }

        @Override
        protected void L() {
            if (npc == null) {
                super.L();
            }
        }

        @Override
        public void M() {
            super.M();
            if (npc != null) {
                npc.update();
            }
        }
    }

    public static class VexNPC extends CraftVex implements NPCHolder {
        private final CitizensNPC npc;

        public VexNPC(EntityVexNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
        }

        @Override
        public NPC getNPC() {
            return npc;
        }
    }
}