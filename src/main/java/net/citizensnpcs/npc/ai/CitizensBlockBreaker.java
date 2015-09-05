package net.citizensnpcs.npc.ai;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import net.citizensnpcs.api.ai.tree.BehaviorStatus;
import net.citizensnpcs.api.npc.BlockBreaker;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.util.PlayerAnimation;
import net.citizensnpcs.util.Util;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.Enchantment;
import net.minecraft.server.v1_8_R3.EnchantmentManager;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.Material;
import net.minecraft.server.v1_8_R3.MobEffectList;

public class CitizensBlockBreaker extends BlockBreaker {
    private final BlockBreakerConfiguration configuration;
    private int currentDamage;
    private int currentTick;
    private final Entity entity;
    private boolean isDigging = true;
    private final Location location;
    private int startDigTick;
    private final int x, y, z;

    public CitizensBlockBreaker(org.bukkit.entity.Entity entity, org.bukkit.block.Block target,
            BlockBreakerConfiguration config) {
        this.entity = ((CraftEntity) entity).getHandle();
        this.x = target.getX();
        this.y = target.getY();
        this.z = target.getZ();
        this.location = target.getLocation();
        this.startDigTick = (int) (System.currentTimeMillis() / 50);
        this.configuration = config;
    }

    private double distanceSquared() {
        return Math.pow(entity.locX - x, 2) + Math.pow(entity.locY - y, 2) + Math.pow(entity.locZ - z, 2);
    }

    private net.minecraft.server.v1_8_R3.ItemStack getCurrentItem() {
        return configuration.item() != null ? CraftItemStack.asNMSCopy(configuration.item())
                : entity instanceof EntityLiving ? ((EntityLiving) entity).getEquipment(0) : null;
    }

    private float getStrength(Block block) {
        float base = block.g(null, new BlockPosition(0, 0, 0));
        return base < 0.0F ? 0.0F : (!isDestroyable(block) ? 1.0F / base / 100.0F : strengthMod(block) / base / 30.0F);
    }

    private boolean isDestroyable(Block block) {
        if (block.getMaterial().isAlwaysDestroyable()) {
            return true;
        } else {
            ItemStack current = getCurrentItem();
            return current != null ? current.b(block) : false;
        }
    }

    @Override
    public void reset() {
        if (configuration.callback() != null) {
            configuration.callback().run();
        }
        isDigging = false;
        setBlockDamage(currentDamage = -1);
    }

    @Override
    public BehaviorStatus run() {
        if (entity.dead) {
            return BehaviorStatus.FAILURE;
        }
        if (!isDigging) {
            return BehaviorStatus.SUCCESS;
        }
        currentTick = (int) (System.currentTimeMillis() / 50); // CraftBukkit
        if (configuration.radiusSquared() > 0 && distanceSquared() >= configuration.radiusSquared()) {
            startDigTick = currentTick;
            if (entity instanceof NPCHolder) {
                NPC npc = ((NPCHolder) entity).getNPC();
                if (npc != null && !npc.getNavigator().isNavigating()) {
                    npc.getNavigator()
                            .setTarget(entity.world.getWorld().getBlockAt(x, y, z).getLocation().add(0, 1, 0));
                }
            }
            return BehaviorStatus.RUNNING;
        }
        Util.faceLocation(entity.getBukkitEntity(), location);
        if (entity instanceof EntityPlayer) {
            PlayerAnimation.ARM_SWING.play((Player) entity.getBukkitEntity());
        }
        Block block = entity.world.getType(new BlockPosition(x, y, z)).getBlock();
        if (block == null || block == Blocks.AIR) {
            return BehaviorStatus.SUCCESS;
        } else {
            int tickDifference = currentTick - startDigTick;
            float damage = getStrength(block) * (tickDifference + 1) * configuration.blockStrengthModifier();
            if (damage >= 1F) {
                entity.world.getWorld().getBlockAt(x, y, z)
                        .breakNaturally(CraftItemStack.asCraftMirror(getCurrentItem()));
                return BehaviorStatus.SUCCESS;
            }
            int modifiedDamage = (int) (damage * 10.0F);
            if (modifiedDamage != currentDamage) {
                setBlockDamage(modifiedDamage);
                currentDamage = modifiedDamage;
            }
        }
        return BehaviorStatus.RUNNING;
    }

    private void setBlockDamage(int modifiedDamage) {
        entity.world.c(entity.getId(), new BlockPosition(x, y, z), modifiedDamage);
    }

    @Override
    public boolean shouldExecute() {
        return entity.world.getType(new BlockPosition(x, y, z)).getBlock() != Blocks.AIR;
    }

    private float strengthMod(Block block) {
        ItemStack itemstack = getCurrentItem();
        float strength = itemstack != null ? itemstack.a(block) : 1;
        int ench = EnchantmentManager.getEnchantmentLevel(Enchantment.DURABILITY.id, getCurrentItem());

        if (ench > 0 && itemstack != null) {
            float levelSquared = ench * ench + 1;

            if (!itemstack.b(block) && strength <= 1.0F) {
                strength += levelSquared * 0.08F;
            } else {
                strength += levelSquared;
            }
        }
        if (entity instanceof EntityLiving) {
            EntityLiving living = (EntityLiving) entity;
            if (living.hasEffect(MobEffectList.FASTER_DIG)) {
                strength *= 1.0F + (living.getEffect(MobEffectList.FASTER_DIG).getAmplifier() + 1) * 0.2F;
            }
            if (living.hasEffect(MobEffectList.SLOWER_DIG)) {
                strength *= 1.0F - (living.getEffect(MobEffectList.SLOWER_DIG).getAmplifier() + 1) * 0.2F;
            }
            if (entity.a(Material.WATER) && !EnchantmentManager.j(living)) {
                strength /= 5.0F;
            }
        }
        if (!entity.onGround) {
            strength /= 5.0F;
        }
        return strength;
    }
}