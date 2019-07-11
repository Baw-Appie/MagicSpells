package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class LeapSpell extends InstantSpell {

	private Set<Player> jumping;

	private float rotation;
	private float upwardVelocity;
	private float forwardVelocity;

	private boolean clientOnly;
	private boolean cancelDamage;
	private boolean addVelocityInstead;

	private Subspell landSpell;
	private String landSpellName;

	public LeapSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		jumping = new HashSet();

		rotation = getConfigFloat("rotation", 0F);
		upwardVelocity = getConfigFloat("upward-velocity", 15F) / 10F;
		forwardVelocity = getConfigFloat("forward-velocity", 40F) / 10F;

		clientOnly = getConfigBoolean("client-only", false);
		cancelDamage = getConfigBoolean("cancel-damage", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);

		landSpellName = getConfigString("land-spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		landSpell = new Subspell(landSpellName);
		if (!landSpell.process()) {
			if (!landSpellName.isEmpty()) MagicSpells.error("LeapSpell '" + internalName + "' has an invalid land-spell defined!");
			landSpell = null;
		}
	}

	public boolean isJumping(Player pl) {
		return jumping.contains(pl);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Vector v = player.getLocation().getDirection();
			v.setY(0).normalize().multiply(forwardVelocity * power).setY(upwardVelocity * power);
			if (rotation != 0) Util.rotateVector(v, rotation);
			if (clientOnly) MagicSpells.getVolatileCodeHandler().setClientVelocity(player, v);
			else {
				if (!addVelocityInstead) player.setVelocity(v);
				else player.setVelocity(player.getVelocity().add(v));
			}
			jumping.add(player);
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getCause() != EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player)) return;
		Player pl = (Player) e.getEntity();
		if (!jumping.remove(pl)) return;
		if (landSpell != null) landSpell.cast(pl, 1);
		playSpellEffects(EffectPosition.TARGET, pl.getLocation());
		if (cancelDamage) e.setCancelled(true);
	}

}
