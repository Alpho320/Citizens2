package net.citizensnpcs.nms.v1_8_R3.entity.nonliving;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftThrownPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_8_R3.entity.MobEntityController;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_8_R3.EntityPotion;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.World;

public class SplashPotionController extends MobEntityController {
    public SplashPotionController() {
        super(EntityPotionNPC.class);
    }

    @Override
    public ThrownPotion getBukkitEntity() {
        return (ThrownPotion) super.getBukkitEntity();
    }

    public static class EntityPotionNPC extends EntityPotion implements NPCHolder {
        private final CitizensNPC npc;

        public EntityPotionNPC(World world) {
            this(world, null);
        }

        public EntityPotionNPC(World world, NPC npc) {
            super(world);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public void collide(net.minecraft.server.v1_8_R3.Entity entity) {
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
        public void g(double x, double y, double z) {
            Vector vector = Util.callPushEvent(npc, x, y, z);
            if (vector != null) {
                super.g(vector.getX(), vector.getY(), vector.getZ());
            }
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(bukkitEntity instanceof NPCHolder)) {
                bukkitEntity = new PotionNPC(this);
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public void t_() {
            if (npc != null) {
                npc.update();
                if (!npc.isProtected()) {
                    super.t_();
                }
            } else {
                super.t_();
            }
        }
    }

    public static class PotionNPC extends CraftThrownPotion implements NPCHolder {
        private final CitizensNPC npc;

        public PotionNPC(EntityPotionNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
            this.npc = entity.npc;
        }

        @Override
        public NPC getNPC() {
            return npc;
        }
    }
}