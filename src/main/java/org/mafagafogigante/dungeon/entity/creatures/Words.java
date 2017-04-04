package org.mafagafogigante.dungeon.entity.creatures;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Words class is for key words.
 */
public class Words implements Serializable {
  public ArrayList<String> wrd = new ArrayList<String>();

  /**
  * Words put key words in the arraylist.
  */
  public Words() {
    wrd.add("penta");
    wrd.add("gamer");
    wrd.add("skill");
    wrd.add("skins");
    wrd.add("words");
    wrd.add("sword");
    wrd.add("balls");
    wrd.add("proje");
  }
}
