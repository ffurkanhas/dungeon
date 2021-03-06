package org.mafagafogigante.dungeon.entity.creatures;

import static org.mafagafogigante.dungeon.date.DungeonTimeUnit.HOUR;
import static org.mafagafogigante.dungeon.date.DungeonTimeUnit.SECOND;

import org.mafagafogigante.dungeon.achievements.AchievementTracker;
import org.mafagafogigante.dungeon.date.Date;
import org.mafagafogigante.dungeon.date.Duration;
import org.mafagafogigante.dungeon.entity.Entity;
import org.mafagafogigante.dungeon.entity.items.BaseInventory;
import org.mafagafogigante.dungeon.entity.items.BookComponent;
import org.mafagafogigante.dungeon.entity.items.CreatureInventory.SimulationResult;
import org.mafagafogigante.dungeon.entity.items.DrinkableComponent;
import org.mafagafogigante.dungeon.entity.items.FoodComponent;
import org.mafagafogigante.dungeon.entity.items.Item;
import org.mafagafogigante.dungeon.game.DungeonString;
import org.mafagafogigante.dungeon.game.Engine;
import org.mafagafogigante.dungeon.game.Game;
import org.mafagafogigante.dungeon.game.GameState;
import org.mafagafogigante.dungeon.game.Location;
import org.mafagafogigante.dungeon.game.LocationPreset;
import org.mafagafogigante.dungeon.game.LocationPresetStore;
import org.mafagafogigante.dungeon.game.Name;
import org.mafagafogigante.dungeon.game.NameFactory;
import org.mafagafogigante.dungeon.game.PartOfDay;
import org.mafagafogigante.dungeon.game.Point;
import org.mafagafogigante.dungeon.game.QuantificationMode;
import org.mafagafogigante.dungeon.game.Random;
import org.mafagafogigante.dungeon.game.World;
import org.mafagafogigante.dungeon.io.Sleeper;
import org.mafagafogigante.dungeon.io.Writer;
import org.mafagafogigante.dungeon.spells.Spell;
import org.mafagafogigante.dungeon.spells.SpellData;
import org.mafagafogigante.dungeon.util.DungeonMath;
import org.mafagafogigante.dungeon.util.Matches;
import org.mafagafogigante.dungeon.util.Messenger;
import org.mafagafogigante.dungeon.util.Utils;
import org.mafagafogigante.dungeon.util.library.Libraries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Math;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;


/**
 * Hero class that defines the creature that the player controls.
 */
public class Hero extends Creature {

  private static final long serialVersionUID = 640405734043893761L;
  // The longest possible sleep starts at 19:00 and ends at 05:15 (takes 10 hours and 15 minutes).
  // It seems a good idea to let the Hero have one dream every 4 hours.
  private static final int DREAM_DURATION_IN_SECONDS = 4 * DungeonMath.safeCastLongToInteger(HOUR.as(SECOND));
  private static final int MILLISECONDS_TO_SLEEP_AN_HOUR = 500;
  private static final int SECONDS_TO_PICK_UP_AN_ITEM = 10;
  private static final int SECONDS_TO_HIT_AN_ITEM = 4;
  private static final int SECONDS_TO_EAT_AN_ITEM = 30;
  private static final int SECONDS_TO_DRINK_AN_ITEM = 10;
  private static final int SECONDS_TO_DROP_AN_ITEM = 2;
  private static final int SECONDS_TO_UNEQUIP = 4;
  private static final int SECONDS_TO_EQUIP = 6;
  private static final int SECONDS_TO_MILK_A_CREATURE = 45;
  private static final int SECONDS_TO_READ_EQUIPPED_CLOCK = 4;
  private static final int SECONDS_TO_READ_UNEQUIPPED_CLOCK = 10;
  private static final int TELEPORT_SUCCESS = 500;
  private static final int WOOD_COUNT = 2; // How many woods for building home
  private static final double MAXIMUM_HEALTH_THROUGH_REST = 0.6;
  private static final int SECONDS_TO_REGENERATE_FULL_HEALTH = 30000; // 500 minutes (or 8 hours and 20 minutes).
  private static final int MILK_NUTRITION = 12;
  private final Walker walker = new Walker();
  private final Observer observer = new Observer(this);
  private final Spellcaster spellcaster = new HeroSpellcaster(this);
  private final AchievementTracker achievementTracker;
  private final Date dateOfBirth;
  public boolean regenity = false;
  public int regenNumber = 0;
  public String effectType = "";
  public ArrayList<String> word = new ArrayList<String>();
  public String key;
  public final int random;
  public boolean control = false;
  public final Words keyList = new Words();

  Hero(CreaturePreset preset, AchievementTracker achievementTracker, Date dateOfBirth) {
    super(preset);
    this.achievementTracker = achievementTracker;
    this.dateOfBirth = dateOfBirth;
    this.word = keyList.wrd;
    this.random = (int)(Math.random() * word.size());
    this.key = word.get(random);
  }

  public Observer getObserver() {
    return observer;
  }

  public Spellcaster getSpellcaster() {
    return spellcaster;
  }

  public AchievementTracker getAchievementTracker() {
    return achievementTracker;
  }

  /**
   * Increments the Hero's health by a certain amount, without exceeding its maximum health. If at the end the Hero is
   * completely healed, a messaging about this is written.
   */
  private void addHealth(int amount) {
    getHealth().incrementBy(amount);
    if (getHealth().isFull()) {
      Writer.write("You are completely healed.");
    }
  }

  /**
   * It checks the keyword that is true or not.
   */
  public void key(String[] arguments) {
    if (key.equalsIgnoreCase(arguments[0])) {
      Writer.write("Congratulations ! You have the key !");
      Writer.write("Now you can equip Master !!!");
      this.control = true;
    } else {
      Writer.write("You should try again !");
    }
  }
  /**
   * Rests until the hero is considered to be rested.
   */
  public void rest() {
    int maximumHealthFromRest = (int) (MAXIMUM_HEALTH_THROUGH_REST * getHealth().getMaximum());
    if (getHealth().getCurrent() >= maximumHealthFromRest) {
      Writer.write("You are already rested.");
    } else {
      int healthRecovered = maximumHealthFromRest - getHealth().getCurrent(); // A positive integer.
      // The fraction SECONDS_TO_REGENERATE_FULL_HEALTH / getHealth().getMaximum() may be smaller than 1.
      // Therefore, the following expression may evaluate to 0 if we do not use Math.max to secure the call to
      // Engine.rollDateAndRefresh.
      int timeResting = Math.max(1, healthRecovered * SECONDS_TO_REGENERATE_FULL_HEALTH / getHealth().getMaximum());
      Engine.rollDateAndRefresh(timeResting);
      Writer.write("Resting...");
      getHealth().incrementBy(healthRecovered);
      Writer.write("You feel rested.");
    }
  }

  /**
   * Sleep until the sun rises.
   *
   * <p>Depending on how much the Hero will sleep, this method may print a few dreams.
   */
  public void sleep() {
    int seconds;
    World world = getLocation().getWorld();
    PartOfDay pod = world.getPartOfDay();
    if (pod == PartOfDay.EVENING || pod == PartOfDay.MIDNIGHT || pod == PartOfDay.NIGHT) {
      Writer.write("You fall asleep.");
      seconds = PartOfDay.getSecondsToNext(world.getWorldDate(), PartOfDay.DAWN);
      // In order to increase realism, add up to 15 minutes to the time it would take to wake up exactly at dawn.
      seconds += Random.nextInteger(15 * 60 + 1);
      while (seconds > 0) {
        final int cycleDuration = Math.min(DREAM_DURATION_IN_SECONDS, seconds);
        Engine.rollDateAndRefresh(cycleDuration);
        // Cast to long because it is considered best practice. We are going to end with a long anyway, so start doing
        // long arithmetic at the first multiplication. Reported by ICAST_INTEGER_MULTIPLY_CAST_TO_LONG in FindBugs.
        Sleeper.sleep((long) MILLISECONDS_TO_SLEEP_AN_HOUR * cycleDuration / HOUR.as(SECOND));
        if (cycleDuration == DREAM_DURATION_IN_SECONDS) {
          Writer.write(Libraries.getDreamLibrary().next());
        }
        seconds -= cycleDuration;
        if (!getHealth().isFull()) {
          int healing = getHealth().getMaximum() * cycleDuration / SECONDS_TO_REGENERATE_FULL_HEALTH;
          getHealth().incrementBy(healing);
        }
      }
      Writer.write("You wake up.");
    } else {
      Writer.write("You can only sleep at night.");
    }
  }

  /**
   * Returns whether any Item of the current Location is visible to the Hero.
   */
  private boolean canSeeAnItem() {
    for (Item item : getLocation().getItemList()) {
      if (canSee(item)) {
        return true;
      }
    }
    return false;
  }

  private <T extends Entity> Matches<T> filterByVisibility(Matches<T> matches) {
    return Matches.fromCollection(filterByVisibility(matches.toList()));
  }

  /**
   * Prints the name of the player's current location and lists all creatures and items the character sees.
   */
  public void look() {
    observer.look();
  }

  /**
   * Selects multiple items from the inventory.
   */
  private List<Item> selectInventoryItems(String[] arguments) {
    if (getInventory().getItemCount() == 0) {
      Writer.write("Your inventory is empty.");
      return Collections.emptyList();
    }
    return selectItems(arguments, getInventory(), false);
  }

  /**
   * Selects a single item from the inventory.
   */
  private Item selectInventoryItem(String[] arguments) {
    List<Item> selectedItems = selectInventoryItems(arguments);
    if (selectedItems.size() == 1) {
      return selectedItems.get(0);
    }
    if (selectedItems.size() > 1) {
      Writer.write("The query matched multiple items.");
    }
    return null;
  }

  /**
   * Select a list of items of the current location based on the arguments of a command.
   */
  private List<Item> selectLocationItems(String[] arguments) {
    if (filterByVisibility(getLocation().getItemList()).isEmpty()) {
      Writer.write("You don't see any items here.");
      return Collections.emptyList();
    } else {
      return selectItems(arguments, getLocation().getInventory(), true);
    }
  }

  /**
   * Selects items of the specified {@code BaseInventory} based on the arguments of a command.
   *
   * @param arguments an array of arguments that will determine the item search
   * @param inventory an object of a subclass of {@code BaseInventory}
   * @param checkForVisibility true if only visible items should be selectable
   * @return a List of items
   */
  private List<Item> selectItems(String[] arguments, BaseInventory inventory, boolean checkForVisibility) {
    List<Item> visibleItems;
    if (checkForVisibility) {
      visibleItems = filterByVisibility(inventory.getItems());
    } else {
      visibleItems = inventory.getItems();
    }
    if (arguments.length != 0 || HeroUtils.checkIfAllEntitiesHaveTheSameName(visibleItems)) {
      return HeroUtils.findItems(visibleItems, arguments);
    } else {
      Writer.write("You must specify an item.");
      return Collections.emptyList();
    }
  }

  /**
   * Issues this Hero to attack a target.
   */
  public void attackTarget(String[] arguments) {
    Creature target = selectTarget(arguments);
    if (target != null) {
      Engine.battle(this, target);
    }
  }

  /**
   * Attempts to select a target from the current location using the player input.
   *
   * @return a target Creature or {@code null}
   */
  private Creature selectTarget(String[] arguments) {
    List<Creature> visibleCreatures = filterByVisibility(getLocation().getCreatures());
    if (arguments.length != 0 || HeroUtils.checkIfAllEntitiesHaveTheSameName(visibleCreatures, this)) {
      return findCreature(arguments);
    } else {
      Writer.write("You must specify a target.");
      return null;
    }
  }

  /**
   * Attempts to find a creature in the current location comparing its name to an array of string tokens.
   *
   * <p>If there are no matches, {@code null} is returned.
   *
   * <p>If there is one match, it is returned.
   *
   * <p>If there are multiple matches but all have the same name, the first one is returned.
   *
   * <p>If there are multiple matches with only two different names and one of these names is the Hero's name, the first
   * creature match is returned.
   *
   * <p>Lastly, if there are multiple matches that do not fall in one of the two categories above, {@code null} is
   * returned.
   *
   * @param tokens an array of string tokens.
   * @return a Creature or null.
   */
  public Creature findCreature(String[] tokens) {
    Matches<Creature> result = Matches.findBestCompleteMatches(getLocation().getCreatures(), tokens);
    result = filterByVisibility(result);
    if (result.size() == 0) {
      Writer.write("Creature not found.");
    } else if (result.size() == 1 || result.getDifferentNames() == 1) {
      return result.getMatch(0);
    } else if (result.getDifferentNames() == 2 && result.hasMatchWithName(getName())) {
      return result.getMatch(0).getName().equals(getName()) ? result.getMatch(1) : result.getMatch(0);
    } else {
      Messenger.printAmbiguousSelectionMessage();
    }
    return null;
  }

  /**
   * Attempts to pick up items from the current location.
   */
  public void pickItems(String[] arguments) {
    if (canSeeAnItem()) {
      List<Item> selectedItems = selectLocationItems(arguments);
      for (Item item : selectedItems) {
        if (item.getName().toString().equalsIgnoreCase("wood")) {
          Writer.write("You can't pick the wood, you should cut");
        } else {
          final SimulationResult result = getInventory().simulateItemAddition(item);
          // We stop adding items as soon as we hit the first one which would exceed the amount or weight limit.
          if (result == SimulationResult.AMOUNT_LIMIT) {
            Writer.write("Your inventory is full.");
            break;
          } else if (result == SimulationResult.WEIGHT_LIMIT) {
            Writer.write("You can't carry more weight.");
            // This may not be ideal, as there may be a selection which has lighter items after this item.
            break;
          } else if (result == SimulationResult.SUCCESSFUL) {
            Engine.rollDateAndRefresh(SECONDS_TO_PICK_UP_AN_ITEM);
            if (getLocation().getInventory().hasItem(item)) {
              getLocation().removeItem(item);
              addItem(item);
            } else {
              HeroUtils.writeNoLongerInLocationMessage(item);
            }
          }
        }
      }
    } else {
      Writer.write("You do not see any item you could pick up.");
    }
  }

  /**
   * Attempts to cut wood from the current location.
   */
  public void cut(String[] arguments) {
    if (canSeeAnItem()) {
      List<Item> selectedItems = selectLocationItems(arguments);
      for (Item item : selectedItems) {
        if (!item.getName().toString().equalsIgnoreCase("wood")) {
          Writer.write("You can just cut wood");
        } else {
          final SimulationResult result = getInventory().simulateItemAddition(item);
          // We stop adding items as soon as we hit the first one which would exceed the amount or weight limit.
          if (result == SimulationResult.AMOUNT_LIMIT) {
            Writer.write("Your inventory is full.");
            break;
          } else if (result == SimulationResult.WEIGHT_LIMIT) {
            Writer.write("You can't carry more weight.");
            // This may not be ideal, as there may be a selection which has lighter items after this item.
            break;
          } else if (result == SimulationResult.SUCCESSFUL) {
            Engine.rollDateAndRefresh(SECONDS_TO_PICK_UP_AN_ITEM);
            if (getLocation().getInventory().hasItem(item)) {
              getLocation().removeItem(item);
              addItem(item);
            } else {
              HeroUtils.writeNoLongerInLocationMessage(item);
            }
          }
        }
      }
    } else {
      Writer.write("You do not see any item you could pick up.");
    }
  }
  /**
  * Create this method for getting hint on subitems.
   */
  public void hints(String[] arguments) {
    Item weapon = null;
    for (Item item : getInventory().getItems()) {
      if (item.getQualifiedName().equalsIgnoreCase(arguments[0])) {
        weapon = item;
      }
    }
    if (getInventory().hasItem(weapon)) {
      if (weapon.getName().toString().equalsIgnoreCase("gold")) {
        String wrd = "The hint is : " + key.charAt(0);
        Writer.write(wrd);
      } else if (weapon.getName().toString().equalsIgnoreCase("diamond")) {
        String wrd = "The hint is : " + key.charAt(1);
        Writer.write(wrd);
      } else if (weapon.getName().toString().equalsIgnoreCase("sand")) {
        String wrd = "The hint is : " + key.charAt(2);
        Writer.write(wrd);
      } else if (weapon.getName().toString().equalsIgnoreCase("fire")) {
        String wrd = "The hint is : " + key.charAt(3);
        Writer.write(wrd);
      } else if (weapon.getName().toString().equalsIgnoreCase("water")) {
        String wrd = "The hint is : " + key.charAt(4);
        Writer.write(wrd);
      } else {
        Writer.write("You can hint just for subitems");
      }
    } else {
      Writer.write("You should have the subitems");
    }
  }

  /**
   * Adds an Item object to the inventory. As a precondition, simulateItemAddition(Item) should return SUCCESSFUL.
   *
   * <p>Writes a message about this to the screen.
   *
   * @param item the Item to be added, not null
   */
  public void addItem(Item item) {
    if (getInventory().simulateItemAddition(item) == SimulationResult.SUCCESSFUL) {
      getInventory().addItem(item);
      Writer.write(String.format("Added %s to the inventory.", item.getQualifiedName()));
    } else {
      throw new IllegalStateException("simulateItemAddition did not return SUCCESSFUL.");
    }
  }

  /**
   * Tries to equip an item from the inventory.
   */
  public void parseEquip(String[] arguments) {
    Item selectedItem = selectInventoryItem(arguments);
    if (selectedItem != null) {
      if (selectedItem.hasTag(Item.Tag.WEAPON)) {
        equipWeapon(selectedItem);
      } else {
        Writer.write("You cannot equip that.");
      }
    }
  }

  /**
   * Attempts to drop items from the inventory.
   */
  public void dropItems(String[] arguments) {
    List<Item> selectedItems = selectInventoryItems(arguments);
    for (Item item : selectedItems) {
      if (item == getWeapon()) {
        unsetWeapon(); // Just unset the weapon, it does not need to be moved to the inventory before being dropped.
      }
      // Take the time to drop the item.
      Engine.rollDateAndRefresh(SECONDS_TO_DROP_AN_ITEM);
      if (getInventory().hasItem(item)) { // The item may have disappeared while dropping.
        dropItem(item); // Just drop it if has not disappeared.
      }
      // The character "dropped" the item even if it disappeared while doing it, so write about it.
      Writer.write(String.format("Dropped %s.", item.getQualifiedName()));
    }
  }

  /**
   * Writes the Hero's inventory to the screen.
   */
  public void writeInventory() {
    Name item = NameFactory.newInstance("item");
    String firstLine;
    if (getInventory().getItemCount() == 0) {
      firstLine = "Your inventory is empty.";
    } else {
      String itemCount = item.getQuantifiedName(getInventory().getItemCount(), QuantificationMode.NUMBER);
      firstLine = "You are carrying " + itemCount + ". Your inventory weights " + getInventory().getWeight() + ".";
    }
    Writer.write(firstLine);
    // Local variable to improve readability.
    String itemLimit = item.getQuantifiedName(getInventory().getItemLimit(), QuantificationMode.NUMBER);
    Writer.write("Your maximum carrying capacity is " + itemLimit + " and " + getInventory().getWeightLimit() + ".");
    if (getInventory().getItemCount() != 0) {
      printItems();
    }
  }

  /**
   * Prints all items in the Hero's inventory. This function should only be called if the inventory is not empty.
   */
  private void printItems() {
    if (getInventory().getItemCount() == 0) {
      throw new IllegalStateException("inventory item count is 0.");
    }
    Writer.write("You are carrying:");
    for (Item item : getInventory().getItems()) {
      String name = String.format("%s (%s)", item.getQualifiedName(), item.getWeight());
      if (hasWeapon() && getWeapon() == item) {
        Writer.write(" [Equipped] " + name);
      } else {
        Writer.write(" " + name);
      }
    }
  }

  /**
   * Attempts to eat an item.
   */
  public void eatItem(String[] arguments) {
    Item selectedItem = selectInventoryItem(arguments);
    if (selectedItem != null) {
      if (selectedItem.hasTag(Item.Tag.FOOD)) {
        Engine.rollDateAndRefresh(SECONDS_TO_EAT_AN_ITEM);
        if (getInventory().hasItem(selectedItem)) {
          FoodComponent food = selectedItem.getFoodComponent();
          double remainingBites = selectedItem.getIntegrity().getCurrent() / (double) food.getIntegrityDecrementOnEat();
          int healthChange;
          if (remainingBites >= 1.0) {
            healthChange = food.getNutrition();
          } else {
            // The absolute value of the healthChange will never be equal to nutrition, only smaller.
            healthChange = (int) (food.getNutrition() * remainingBites);
          }
          selectedItem.decrementIntegrityByEat();
          if (selectedItem.isBroken() && !selectedItem.hasTag(Item.Tag.REPAIRABLE)) {
            Writer.write("You ate " + selectedItem.getName() + ".");
          } else {
            Writer.write("You ate a bit of " + selectedItem.getName() + ".");
          }
          addHealth(healthChange);
        } else {
          HeroUtils.writeNoLongerInInventoryMessage(selectedItem);
        }
      } else {
        Writer.write("You can only eat food.");
      }
    }
  }

  /**
   * Attempts to drink an item.
   */
  public void drinkItem(String[] arguments) {
    Item selectedItem = selectInventoryItem(arguments);
    if (selectedItem != null) {
      if (selectedItem.hasTag(Item.Tag.DRINKABLE)) {
        Engine.rollDateAndRefresh(SECONDS_TO_DRINK_AN_ITEM);
        if (getInventory().hasItem(selectedItem)) {
          DrinkableComponent component = selectedItem.getDrinkableComponent();
          if (!component.isDepleted()) {
            component.decrementDoses();
            selectedItem.decrementIntegrityByDrinking();
            if (component.isDepleted()) {
              Writer.write("You drank the last dose of " + selectedItem.getName() + ".");
            } else {
              Writer.write("You drank a dose of " + selectedItem.getName() + ".");
            }
 
            if (regenity == true) {
              regenity = true;
              Writer.write("You already used potion");
            } else {
              if (selectedItem.getName().toString().equalsIgnoreCase("Regeneration Potion")) {
                //int oneDay=DungeonTimeUnit.DAY.milliseconds;
                //while (!(oneDay <0)) {
                regenity = true;
                regenNumber = component.getEffectTime();
                effectType = "Regeneration Potion";
                addHealth(11);
                Writer.write("its first effectTime " + component.getEffectTime());
                component.decrementEffect();
                Writer.write("its second effectTime " + component.getEffectTime());
              }
              if (selectedItem.getName().toString().equalsIgnoreCase("Health Potion")) {
                addHealth(component.getEffect().getHealing());
              }
              if (selectedItem.getName().toString().equalsIgnoreCase("Reduction Potion")) {
                regenity = true;
                regenNumber = component.getEffectTime();
                effectType = "Reduction Potion";
                setDamageReduction(50);
              }

              if (selectedItem.getName().toString().equalsIgnoreCase("Boost Potion")) {
                regenity = true;
                regenNumber = component.getEffectTime();
                effectType = "Boost Potion";
                setAttack(5);
              
              }
            }

          } else {
            Writer.write("This item is depleted.");
          }
        } else {
          HeroUtils.writeNoLongerInInventoryMessage(selectedItem);
        }
      } else {
        Writer.write("This item is not drinkable.");
      }
    }
  }

  /**
   * The method that enables a Hero to drink milk from a Creature.
   */
  public void parseMilk(String[] arguments) {
    if (arguments.length != 0) { // Specified which creature to milk from.
      Creature selectedCreature = selectTarget(arguments); // Finds the best match for the specified arguments.
      if (selectedCreature != null) {
        if (selectedCreature.hasTag(Creature.Tag.MILKABLE)) {
          milk(selectedCreature);
        } else {
          Writer.write("This creature is not milkable.");
        }
      }
    } else { // Filter milkable creatures.
      List<Creature> visibleCreatures = filterByVisibility(getLocation().getCreatures());
      List<Creature> milkableCreatures = HeroUtils.filterByTag(visibleCreatures, Tag.MILKABLE);
      if (milkableCreatures.isEmpty()) {
        Writer.write("You can't find a milkable creature.");
      } else {
        if (Matches.fromCollection(milkableCreatures).getDifferentNames() == 1) {
          milk(milkableCreatures.get(0));
        } else {
          Writer.write("You need to be more specific.");
        }
      }
    }
  }

  private void milk(Creature creature) {
    Engine.rollDateAndRefresh(SECONDS_TO_MILK_A_CREATURE);
    Writer.write("You drink milk directly from " + creature.getName().getSingular() + ".");
    addHealth(MILK_NUTRITION);
  }

  /**
   * Attempts to read an Item.
   */
  public void readItem(String[] arguments) {
    Item selectedItem = selectInventoryItem(arguments);
    if (selectedItem != null) {
      BookComponent book = selectedItem.getBookComponent();
      if (book != null) {
        Engine.rollDateAndRefresh(book.getTimeToRead());
        if (getInventory().hasItem(selectedItem)) { // Just in case if a readable item eventually decomposes.
          DungeonString string = new DungeonString(book.getText());
          string.append("\n\n");
          Writer.write(string);
          if (book.isDidactic()) {
            learnSpell(book);
          }
        } else {
          HeroUtils.writeNoLongerInInventoryMessage(selectedItem);
        }
      } else {
        Writer.write("You can only read books.");
      }
    }
  }

  /**
   * Attempts to learn a spell from a BookComponent object. As a precondition, book must be didactic (teach a spell).
   *
   * @param book a BookComponent that returns true to isDidactic, not null
   */
  private void learnSpell(@NotNull BookComponent book) {
    if (!book.isDidactic()) {
      throw new IllegalArgumentException("book should be didactic.");
    }
    Spell spell = SpellData.getSpellMap().get(book.getSpellId());
    if (getSpellcaster().knowsSpell(spell)) {
      Writer.write("You already knew " + spell.getName().getSingular() + ".");
    } else {
      getSpellcaster().learnSpell(spell);
      Writer.write("You learned " + spell.getName().getSingular() + ".");
    }
  }

  private void destroyItem(@NotNull Item target) {
    if (target.isBroken()) {
      Writer.write(target.getName() + " is already crashed.");
    } else {
      while (getLocation().getInventory().hasItem(target) && !target.isBroken()) {
        // Simulate item-on-item damage by decrementing an item's integrity by its own hit decrement.
        target.decrementIntegrityByHit();
        if (hasWeapon() && !getWeapon().isBroken()) {
          getWeapon().decrementIntegrityByHit();
        }
        Engine.rollDateAndRefresh(SECONDS_TO_HIT_AN_ITEM);
      }
      String verb = target.hasTag(Item.Tag.REPAIRABLE) ? "crashed" : "destroyed";
      Writer.write(getName() + " " + verb + " " + target.getName() + ".");
    }
  }

  /**
   * Tries to destroy an item from the current location.
   */
  public void destroyItems(String[] arguments) {
    final List<Item> selectedItems = selectLocationItems(arguments);
    for (Item target : selectedItems) {
      if (target != null) {
        destroyItem(target);
      }
    }
  }

  private void equipWeapon(Item weapon) {
    if (hasWeapon()) {
      if (getWeapon() == weapon) {
        Writer.write(getName() + " is already equipping " + weapon.getName() + ".");
        return;
      } else {
        unequipWeapon();
      }
    }
    if (weapon.getQualifiedName().equalsIgnoreCase("master")) {
      Item gold = null;
      Item sand = null;
      Item diamond = null;
      Item fire = null;
      Item water = null;
      for (Item item : getInventory().getItems()) {
        if (item.getQualifiedName().equalsIgnoreCase("gold")) {
          gold = item;
        }
        if (item.getQualifiedName().equalsIgnoreCase("sand")) {
          sand = item;
        }
        if (item.getQualifiedName().equalsIgnoreCase("diamond")) {
          diamond = item;
        }
        if (item.getQualifiedName().equalsIgnoreCase("water")) {
          water = item;
        }
        if (item.getQualifiedName().equalsIgnoreCase("fire")) {
          fire = item;
        }
      }
      if (gold != null && water != null && sand != null && diamond != null && fire != null && control) {
        setWeapon(weapon);
        DungeonString string = new DungeonString();
        string.append(getName() + "equipped " + weapon.getQualifiedName() + ".");
        string.append(" " + "Your total damage is now " + getTotalDamage() + ".");
        Writer.write(string);
        getInventory().removeItem(diamond);
        getInventory().removeItem(sand);
        getInventory().removeItem(water);
        getInventory().removeItem(fire);
        getInventory().removeItem(gold);
      } else {
        Writer.write("If you want to equip Master,you have to know key or you have to collect all subitems.");
        return;
      }
    }
    Engine.rollDateAndRefresh(SECONDS_TO_EQUIP);
    if (getInventory().hasItem(weapon)) {
      setWeapon(weapon);
      DungeonString string = new DungeonString();
      string.append(getName() + " equipped " + weapon.getQualifiedName() + ".");
      string.append(" " + "Your total damage is now " + getTotalDamage() + ".");
      Writer.write(string);
    } else {
      HeroUtils.writeNoLongerInInventoryMessage(weapon);
    }
  }

  /**
   * Unequips the currently equipped weapon.
   */
  public void unequipWeapon() {
    if (hasWeapon()) {
      Engine.rollDateAndRefresh(SECONDS_TO_UNEQUIP);
    }
    if (hasWeapon()) { // The weapon may have disappeared.
      Item equippedWeapon = getWeapon();
      unsetWeapon();
      Writer.write(getName() + " unequipped " + equippedWeapon.getName() + ".");
    } else {
      Writer.write("You are not equipping a weapon.");
    }
  }

  /**
   * Prints a message with the current status of the Hero.
   */
  public void printAllStatus() {
    DungeonString string = new DungeonString();
    string.append("Your name is ");
    string.append(getName().getSingular());
    string.append(".");
    string.append(" ");
    string.append("You are now ");
    string.append(getAgeString());
    string.append(" old");
    string.append(".\n");
    string.append("You are ");
    string.append("Your health is ");
    string.append(getHealth().toString());
    string.append(getHealth().getHealthState().toString().toLowerCase(Locale.ENGLISH));
    string.append(".\n");
    string.append("Your base attack is ");
    string.append(String.valueOf(getAttack()));
    string.append(".\n");
    string.append("Your damage reduction is ");
    string.append(String.valueOf(getDamageReduction()));
    string.append(".\n");
    if (hasWeapon()) {
      string.append("You are currently equipping ");
      string.append(getWeapon().getQualifiedName());
      string.append(", whose base damage is ");
      string.append(String.valueOf(getWeapon().getWeaponComponent().getDamage()));
      string.append(". This makes your total damage ");
      string.append(String.valueOf(getTotalDamage()));
      string.append(".\n");
    } else {
      string.append("You are fighting bare-handed.\n");
    }
    Writer.write(string);
  }

  private int getTotalDamage() {
    return getAttack() + getWeapon().getWeaponComponent().getDamage();
  }

  /**
   * Prints the Hero's age.
   */
  public void printAge() {
    Writer.write(new DungeonString("You are " + getAgeString() + " old.", Color.CYAN));
  }

  private String getAgeString() {
    return new Duration(dateOfBirth, Game.getGameState().getWorld().getWorldDate()).toString();
  }

  /**
   * Makes the Hero read the current date and time as well as he can.
   */
  public void readTime() {
    Item clock = getBestClock();
    if (clock != null) {
      Writer.write(clock.getClockComponent().getTimeString());
      // Assume that the hero takes the same time to read the clock and to put it back where it was.
      Engine.rollDateAndRefresh(getTimeToReadFromClock(clock));
    }
    World world = getLocation().getWorld();
    Date worldDate = getLocation().getWorld().getWorldDate();
    Writer.write("You think it is " + worldDate.toDateString() + ".");
    if (worldDate.getMonth() == dateOfBirth.getMonth() && worldDate.getDay() == dateOfBirth.getDay()) {
      Writer.write("Today is your birthday.");
    }
    if (canSeeTheSky()) {
      Writer.write("You can see that it is " + world.getPartOfDay().toString().toLowerCase(Locale.ENGLISH) + ".");
    } else {
      Writer.write("You can't see the sky.");
    }
  }

  /**
   * Attempts to walk according to the provided arguments.
   *
   * @param arguments an array of string arguments
   */
  public void walk(String[] arguments) {
    walker.parseHeroWalk(arguments);
    if (regenNumber > 0 && regenity == true) {
      if (effectType.equals("Regeneration Potion")) {
        addHealth(11);
        regenNumber --;
      }
      if (effectType.equals("Boost Potion")) {
        regenNumber --;
        if (regenNumber == 0 && regenity == true) {
          setAttack(-5);
        }
      }
      if (effectType.equals("Reduction Potion")) {
        regenNumber--;
        if (regenNumber == 0 && regenity == true) {
          setDamageReduction(0);
        }
      }
    }
    if (regenNumber <= 0) {
      regenity = false;
    }
  }

  /**
   * Gets the easiest-to-access unbroken clock of the Hero. If the Hero has no unbroken clock, the easiest-to-access
   * broken clock. Lastly, if the Hero does not have a clock at all, null is returned.
   *
   * @return an Item object of the clock Item (or null)
   */
  @Nullable
  private Item getBestClock() {
    Item clock = null;
    if (hasWeapon() && getWeapon().hasTag(Item.Tag.CLOCK)) {
      if (!getWeapon().isBroken()) {
        clock = getWeapon();
      } else { // The Hero is equipping a broken clock: check if he has a working one in his inventory.
        for (Item item : getInventory().getItems()) {
          if (item.hasTag(Item.Tag.CLOCK) && !item.isBroken()) {
            clock = item;
            break;
          }
        }
        if (clock == null) {
          clock = getWeapon(); // The Hero does not have a working clock in his inventory: use the equipped one.
        }
      }
    } else { // The Hero is not equipping a clock.
      Item brokenClock = null;
      for (Item item : getInventory().getItems()) {
        if (item.hasTag(Item.Tag.CLOCK)) {
          if (item.isBroken() && brokenClock == null) {
            brokenClock = item;
          } else {
            clock = item;
            break;
          }
        }
      }
      if (brokenClock != null) {
        clock = brokenClock;
      }
    }
    if (clock != null) {
      Engine.rollDateAndRefresh(getTimeToReadFromClock(clock));
    }
    return clock;
  }

  private int getTimeToReadFromClock(@NotNull Item clock) {
    return clock == getWeapon() ? SECONDS_TO_READ_EQUIPPED_CLOCK : SECONDS_TO_READ_UNEQUIPPED_CLOCK;
  }

  /**
   * Writes a list with all the Spells that the Hero knows.
   */
  public void writeSpellList() {
    DungeonString string = new DungeonString();
    if (getSpellcaster().getSpellList().isEmpty()) {
      string.append("You have not learned any spells yet.");
    } else {
      string.append("You know ");
      string.append(Utils.enumerate(getSpellcaster().getSpellList()));
      string.append(".");
    }
    Writer.write(string);
  }

  /**
   * Build home if you have enough woods.
   */
  public void buildHome() {
    if ( !getLocation().getName().toString().equalsIgnoreCase("forest") ) {
      Writer.write("You can just build home when you are at forest.");
    } else {
      GameState gameState = Game.getGameState();
      if (gameState.getHomePosition() != null) {
        Writer.write("You can have only one house.");
      } else {
        int woodCount = 0;
        for (Item item : getInventory().getItems()) {
          if (item.getQualifiedName().equals("Wood")) {
            woodCount++;
          }
        }
        if (woodCount < WOOD_COUNT) {
          Writer.write("You do not have enough wood. You need " + WOOD_COUNT + " woods. But you have " +
                  woodCount + " woods.");
        } else {
          List<Item> items = new ArrayList<Item>();
          for (Item item : getInventory().getItems()) {
            if (item.getQualifiedName().equals("Wood")) {
              items.add(item);
            }
          }
          for (int i = 0 ; i < WOOD_COUNT ; i++) {
            getInventory().removeItem(items.get(i));
          }
          Writer.write("You used " + WOOD_COUNT + " woods for make house.");
          World world = gameState.getWorld();
          Point point = gameState.getHero().getLocation().getPoint();
          gameState.setHomePosition(point);
          LocationPresetStore locationPresetStore = LocationPresetStore.getDefaultLocationPresetStore();
          world.addLocation(new Location(
                  Random.select(locationPresetStore.getLocationPresetsByType(LocationPreset.Type.HOME)),
                  world, point), point);
          Writer.write("Now you have a house.");
          Engine.refresh();
        }
      }
    }
  }

  /**
   *  Teleport to home.
   */
  public void goHome() {
    GameState gameState = Game.getGameState();
    Point home = gameState.getHomePosition();
    if (home == null) {
      Writer.write("You do not have any house");
    } else {
      World world = gameState.getWorld();
      Hero hero = gameState.getHero();
      if (hero.getLocation().getPoint() == home) {
        Writer.write("You are at home.");
      } else {
        Engine.rollDateAndRefresh(TELEPORT_SUCCESS); // Teleport spent time
        hero.getLocation().removeCreature(hero);
        world.getLocation(home).addCreature(hero);
        hero.setLocation(world.getLocation(home));
        gameState.setHeroPosition(home);
        Engine.refresh(); // Refresh game
        hero.look();
      }
    }
  }
}
