package net.citizensnpcs.nms.v1_15_R1.entity.nonliving;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftThrownPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_15_R1.entity.MobEntityController;
import net.citizensnpcs.nms.v1_15_R1.util.ForwardingNPCHolder;
import net.citizensnpcs.nms.v1_15_R1.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_15_R1.EntityPotion;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.FluidType;
import net.minecraft.server.v1_15_R1.Items;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.Tag;
import net.minecraft.server.v1_15_R1.Vec3D;
import net.minecraft.server.v1_15_R1.World;

public class ThrownPotionController extends MobEntityController {
    public ThrownPotionController() {
        super(EntityThrownPotionNPC.class);
    }

    @Override
    public ThrownPotion getBukkitEntity() {
        return (ThrownPotion) super.getBukkitEntity();
    }

    public static class EntityThrownPotionNPC extends EntityPotion implements NPCHolder {
        private final CitizensNPC npc;

        public EntityThrownPotionNPC(EntityTypes<? extends EntityPotion> types, World world) {
            this(types, world, null);
        }

        public EntityThrownPotionNPC(EntityTypes<? extends EntityPotion> types, World world, NPC npc) {
            super(types, world);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public boolean b(Tag<FluidType> tag) {
            Vec3D old = getMot().add(0, 0, 0);
            boolean res = super.b(tag);
            if (!npc.isPushableByFluids()) {
                this.setMot(old);
            }
            return res;
        }

        @Override
        public void collide(net.minecraft.server.v1_15_R1.Entity entity) {
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
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                if (getItem() != null && getItem().getItem().equals(Items.LINGERING_POTION)) {
                    NMSImpl.setBukkitEntity(this, new LingeringThrownPotionNPC(this));
                } else {
                    NMSImpl.setBukkitEntity(this, new SplashThrownPotionNPC(this));
                }
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public void h(double x, double y, double z) {
            Vector vector = Util.callPushEvent(npc, x, y, z);
            if (vector != null) {
                super.h(vector.getX(), vector.getY(), vector.getZ());
            }
        }

        @Override
        public void tick() {
            if (npc != null) {
                npc.update();
            } else {
                super.tick();
            }
        }
    }

    public static class LingeringThrownPotionNPC extends CraftThrownPotion implements ForwardingNPCHolder {
        public LingeringThrownPotionNPC(EntityThrownPotionNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }

    public static class SplashThrownPotionNPC extends CraftThrownPotion implements ForwardingNPCHolder {
        public SplashThrownPotionNPC(EntityThrownPotionNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }
}
