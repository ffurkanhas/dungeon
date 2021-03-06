package org.mafagafogigante.dungeon.entity.items;

import org.mafagafogigante.dungeon.logging.DungeonLogger;

import java.io.Serializable;

public class DrinkableComponent implements Serializable {

  private static final long serialVersionUID = 6L;
  private final ItemUsageEffect effect;
  private int doses;
  private int integrityDecrementPerDose;
  private int effectTime;
  
  DrinkableComponent(ItemUsageEffect effect, int integrityDecrementPerDose, int doses , int effectTime) {
    this.effect = effect;
    this.integrityDecrementPerDose = integrityDecrementPerDose;
    this.doses = doses;
    this.effectTime = effectTime;
  }

  public ItemUsageEffect getEffect() {
    return effect;
  }

  public int getEffectTime() {
    return effectTime;
  }

  public boolean isDepleted() {
    return doses == 0;
  }

  public boolean notEffected() {
    return effectTime == 0;
  }

  /**
   * Decrements the amount of doses left in this component.
   */
  public void decrementDoses() {
    if (isDepleted()) {
      DungeonLogger.warning("Attempted to decrement doses after depletion!");
    } else {
      doses--;
    }
  }

  int getIntegrityDecrementPerDose() {
    return integrityDecrementPerDose;
  }

   /**
   * Added by OMU.
   */

  public void decrementEffect() {
    if (notEffected()) {
      DungeonLogger.warning("Attempted to decrement effect after that!");
    } else {
      effectTime --;
    }
  }
 

}
