// Decompiled with: CFR 0.152
// Class Version: 8
package net.citizensnpcs.trait;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCLookCloseChangeTargetEvent;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.trait.RotationTrait;
import net.citizensnpcs.trait.Toggleable;
import net.citizensnpcs.util.NMS;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffectType;

@TraitName(value="lookclosehead")
public class LookCloseHead
        extends Trait
        implements Toggleable {
    @Persist(value="headonly")
    private boolean headOnly = true;
    private Player lookingAt;
    private static final Location CACHE_LOCATION = new Location(null, 0.0, 0.0, 0.0);
    private static final Location NPC_LOCATION = new Location(null, 0.0, 0.0, 0.0);
    private static final Location PLAYER_LOCATION = new Location(null, 0.0, 0.0, 0.0);

    public LookCloseHead() {
        super("lookclosehead");
    }

    private boolean canSee(Player player) {
        if (player == null || !player.isValid()) {
            return false;
        }
        return this.npc.getEntity() instanceof LivingEntity ? ((LivingEntity)this.npc.getEntity()).hasLineOfSight((Entity)player) : true;
    }

    public boolean canSeeTarget() {
        return this.canSee(this.lookingAt);
    }

    public void findNewTarget() {
        if (this.lookingAt != null && !this.isValid(this.lookingAt)) {
            NPCLookCloseChangeTargetEvent event = new NPCLookCloseChangeTargetEvent(this.npc, this.lookingAt, null);
            Bukkit.getPluginManager().callEvent((Event)event);
            this.lookingAt = event.getNewTarget() != null && this.isValid(event.getNewTarget()) ? event.getNewTarget() : null;
        }
        Player old = this.lookingAt;
        if (this.lookingAt != null) {
            // empty if block
        }
        if (old != this.lookingAt) {
            NPCLookCloseChangeTargetEvent event = new NPCLookCloseChangeTargetEvent(this.npc, old, this.lookingAt);
            Bukkit.getPluginManager().callEvent((Event)event);
            if (this.lookingAt != event.getNewTarget() && event.getNewTarget() != null && !this.isValid(event.getNewTarget())) {
                return;
            }
            this.lookingAt = event.getNewTarget();
        }
    }

    private List<Player> getNearbyPlayers() {
        ArrayList<Player> options = Lists.newArrayList();
        Iterable<Player> nearby = CitizensAPI.getLocationLookup().getNearbyPlayers(NPC_LOCATION, 6.0);
        for (Player player : nearby) {
            if (player == this.lookingAt || CitizensAPI.getNPCRegistry().getNPC((Entity)player) != null || player.getLocation().getWorld() != NPC_LOCATION.getWorld() || this.isInvisible(player)) continue;
            options.add(player);
        }
        return options;
    }

    public Player getTarget() {
        return this.lookingAt;
    }

    public boolean isHeadOnly() {
        return this.headOnly;
    }

    private boolean isInvisible(Player player) {
        return player.getGameMode() == GameMode.SPECTATOR || player.hasPotionEffect(PotionEffectType.INVISIBILITY) || this.isPluginVanished(player) || !this.canSee(player);
    }

    private boolean isPluginVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (!meta.asBoolean()) continue;
            return true;
        }
        return false;
    }

    private boolean isValid(Player entity) {
        return entity.isOnline() && entity.isValid() && entity.getWorld() == this.npc.getEntity().getWorld() && entity.getLocation(PLAYER_LOCATION).distanceSquared(NPC_LOCATION) < 64.0 && !this.isInvisible(entity);
    }

    @Override
    public void load(DataKey key) {
    }

    public void lookClose(boolean lookClose) {
    }

    @Override
    public void onDespawn() {
        NPCLookCloseChangeTargetEvent event = new NPCLookCloseChangeTargetEvent(this.npc, this.lookingAt, null);
        Bukkit.getPluginManager().callEvent((Event)event);
        this.lookingAt = event.getNewTarget() != null && this.isValid(event.getNewTarget()) ? event.getNewTarget() : null;
    }

    private void randomLook() {
    }

    @Override
    public void run() {
        if (!this.npc.isSpawned()) {
            return;
        }
        this.npc.getEntity().getLocation(NPC_LOCATION);
        this.findNewTarget();
        if (this.npc.getNavigator().isNavigating()) {
            this.npc.getNavigator().setPaused(this.lookingAt != null);
        }
        if (this.lookingAt == null) {
            return;
        }
        RotationTrait rot = this.npc.getOrAddTrait(RotationTrait.class);
        rot.getGlobalParameters().headOnly(this.headOnly);
        rot.getPhysicalSession().rotateToFace((Entity)this.lookingAt);
        if (this.npc.getEntity().getType().name().equals("SHULKER")) {
            boolean wasSilent = this.npc.getEntity().isSilent();
            this.npc.getEntity().setSilent(true);
            NMS.setPeekShulker(this.npc.getEntity(), 100 - 4 * (int)Math.floor(this.npc.getStoredLocation().distanceSquared(this.lookingAt.getLocation(PLAYER_LOCATION))));
            this.npc.getEntity().setSilent(wasSilent);
        }
    }

    @Override
    public void save(DataKey key) {
    }

    public void setHeadOnly(boolean headOnly) {
        this.headOnly = headOnly;
    }

    @Override
    public boolean toggle() {
        return true;
    }

    public String toString() {
        return "LookClose{true}";
    }

    private static boolean isEqual(float[] array) {
        return (double)Math.abs(array[0] - array[1]) < 0.001;
    }
}
