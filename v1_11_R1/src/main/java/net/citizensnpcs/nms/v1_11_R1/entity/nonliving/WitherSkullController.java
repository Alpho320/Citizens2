package net.citizensnpcs.nms.v1_11_R1.entity.nonliving;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_11_R1.CraftServer;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftWitherSkull;
import org.bukkit.entity.WitherSkull;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_11_R1.entity.MobEntityController;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_11_R1.EntityWitherSkull;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.World;

public class WitherSkullController extends MobEntityController {
    public WitherSkullController() {
        super(EntityWitherSkullNPC.class);
    }

    @Override
    public WitherSkull getBukkitEntity() {
        return (WitherSkull) super.getBukkitEntity();
    }

    public static class EntityWitherSkullNPC extends EntityWitherSkull implements NPCHolder {
        private final CitizensNPC npc;

        public EntityWitherSkullNPC(World world) {
            this(world, null);
        }

        public EntityWitherSkullNPC(World world, NPC npc) {
            super(world);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public void A_() {
            if (npc != null) {
                npc.update();
            } else {
                super.A_();
            }
        }

        @Override
        public void collide(net.minecraft.server.v1_11_R1.Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.collide(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public boolean d(NBTTagCompound save) {
            return npc == null ? super.d(save) : false;
        }

        @Override
        public void f(double x, double y, double z) {
            Vector vector = Util.callPushEvent(npc, x, y, z);
            if (vector != null) {
                super.f(vector.getX(), vector.getY(), vector.getZ());
            }
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(bukkitEntity instanceof NPCHolder)) {
                bukkitEntity = new WitherSkullNPC(this);
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }
    }

    public static class WitherSkullNPC extends CraftWitherSkull implements NPCHolder {
        private final CitizensNPC npc;

        public WitherSkullNPC(EntityWitherSkullNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
        }

        @Override
        public NPC getNPC() {
            return npc;
        }
    }
}