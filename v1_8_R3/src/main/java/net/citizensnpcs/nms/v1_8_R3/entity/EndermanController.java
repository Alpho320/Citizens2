package net.citizensnpcs.nms.v1_8_R3.entity;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEnderman;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Enderman;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.event.NPCEnderTeleportEvent;
import net.citizensnpcs.api.event.NPCKnockbackEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_8_R3.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityEnderman;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.World;

public class EndermanController extends MobEntityController {
    public EndermanController() {
        super(EntityEndermanNPC.class);
    }

    @Override
    public Enderman getBukkitEntity() {
        return (Enderman) super.getBukkitEntity();
    }

    public static class EndermanNPC extends CraftEnderman implements NPCHolder {
        private final CitizensNPC npc;

        public EndermanNPC(EntityEndermanNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
        }

        @Override
        public NPC getNPC() {
            return npc;
        }
    }

    public static class EntityEndermanNPC extends EntityEnderman implements NPCHolder {
        private final CitizensNPC npc;

        public EntityEndermanNPC(World world) {
            this(world, null);
        }

        public EntityEndermanNPC(World world, NPC npc) {
            super(world);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        protected void a(double d0, boolean flag, Block block, BlockPosition blockposition) {
            if (npc == null || !npc.isFlyable()) {
                super.a(d0, flag, block, blockposition);
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
        protected String bo() {
            return NMSImpl.getSoundEffect(npc, super.bo(), NPC.Metadata.HURT_SOUND);
        }

        @Override
        protected String bp() {
            return NMSImpl.getSoundEffect(npc, super.bp(), NPC.Metadata.DEATH_SOUND);
        }

        @Override
        public boolean cc() {
            if (npc == null)
                return super.cc();
            boolean protectedDefault = npc.isProtected();
            if (!protectedDefault || !npc.data().get(NPC.Metadata.LEASH_PROTECTED, protectedDefault))
                return super.cc();
            if (super.cc()) {
                unleash(true, false); // clearLeash with client update
            }
            return false; // shouldLeash
        }

        @Override
        public void collide(net.minecraft.server.v1_8_R3.Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.collide(entity);
            if (npc != null)
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
        }

        @Override
        public boolean d(NBTTagCompound save) {
            return npc == null ? super.d(save) : false;
        }

        @Override
        protected void D() {
            if (npc == null) {
                super.D();
            }
        }

        @Override
        public void e(float f, float f1) {
            if (npc == null || !npc.isFlyable()) {
                super.e(f, f1);
            }
        }

        @Override
        public void E() {
            super.E();
            if (npc != null) {
                npc.update();
            }
        }

        @Override
        public void enderTeleportTo(double d0, double d1, double d2) {
            if (npc == null)
                super.enderTeleportTo(d0, d1, d2);
            NPCEnderTeleportEvent event = new NPCEnderTeleportEvent(npc);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                super.enderTeleportTo(d0, d1, d2);
            }
        }

        @Override
        public void g(double x, double y, double z) {
            Vector vector = Util.callPushEvent(npc, x, y, z);
            if (vector != null) {
                super.g(vector.getX(), vector.getY(), vector.getZ());
            }
        }

        @Override
        public void g(float f, float f1) {
            if (npc == null || !npc.isFlyable()) {
                super.g(f, f1);
            } else {
                NMSImpl.flyingMoveLogic(this, f, f1);
            }
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(bukkitEntity instanceof NPCHolder))
                bukkitEntity = new EndermanNPC(this);
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public boolean k(double d1, double d2, double d3) {
            if (npc == null) {
                return super.k(d1, d2, d3);
            }
            return false;
        }

        @Override
        public boolean k_() {
            if (npc == null || !npc.isFlyable()) {
                return super.k_();
            } else {
                return false;
            }
        }

        @Override
        public void setSize(float f, float f1) {
            if (npc == null) {
                super.setSize(f, f1);
            } else {
                NMSImpl.setSize(this, f, f1, justCreated);
            }
        }

        @Override
        protected String z() {
            return NMSImpl.getSoundEffect(npc, super.z(), NPC.Metadata.AMBIENT_SOUND);
        }
    }
}