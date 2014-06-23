// c(2012) i.am.badger.ken@gmail.com
// You may re-use, so long as I am credited, and you don't charge
// for your work that uses this.
package fam.badger_ken.matchmaker;

import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.LocalDate;
import org.joda.time.Period;

/**
 * All the state found in a save-game file.
 */
public class SaveState {
  // parses a date into year, month, day-of-month.
  private static Pattern DATE_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
  public String rawDate;
  private LocalDate saveDate;
  public String playerRealm;
  // dynasties, indexed by key
  Map<Integer, Dynasty> dynasties = new HashMap<Integer, Dynasty>();
  // people, indexed by key
  Map<Integer, Person> people = new HashMap<Integer, Person>();
  // needed dynasties: only compute once
  BitSet neededDynasties = null;
  // dynasties, sorted alphabetically.
  private Vector<Dynasty> sortedDynasties = null;
  // all holdings, indexed by holder. Set is ordered, highest rank first.
  private Map<Integer, Set<Holding>> holdingsByHolder = new HashMap<Integer, Set<Holding>>();
  // all holdings, indexed by internal label
  private Map<String, Holding> holdingsByLabel = new HashMap<String, Holding>();
  
  /**
   * load the dynasties rooted at the given node.
   * NOTE: must do _after_ loading people, so only those that are needed.
   * NOTE: this can be called more than once, once for the save-game
   * dynasties, once for dynasties.txt
   */
  public void loadDynasties(Node dynastiesRoot) {
    if (dynastiesRoot == null) return;
    if (dynastiesRoot.children == null) {
      return;  // happens >= V1.06, old dynasties.txt is still there but bogus.
    }
    int numDynasties = 0;
    // only load those that are needed:
    if (neededDynasties == null) {
      neededDynasties = new BitSet(1000);
      for (Person person : people.values()) {
        if (person.dynastyKey == null) continue;
        neededDynasties.set(person.dynastyKey, true);
      }
    }
    // they have a numerical key (e.g. 2000016)
    // with a child of 'name' (e.g. d'Orl?ans)
    // and another of 'culture' (e.g. frankish)
    // ...and there are many, many of them (e.g. over 25K)
    int numSkipped = 0;
    for (Node child : dynastiesRoot.children) {
      Integer key = child.tagAsInt();
      if (key == null) continue;
      if (!neededDynasties.get(key)) {
        numSkipped++;
        continue;
      }
      String name = child.findAttribute("name");
      String culture = child.findAttribute("culture");
      Dynasty dynasty = new Dynasty(key, name, culture);
      dynasties.put(key, dynasty);
      numDynasties++;
    }
  }

  /**
   * load the characters rooted at the given node.
   * @param characterRoot
   */
  public void loadCharacters(Node characterRoot) {
    
    //int [] traitsCount = new int[1024];
    //int maxTrait = 0;
    // compute the save date, to get character ages.
    Matcher m = DATE_PATTERN.matcher(this.rawDate);
    if (m.matches()) {
      int year = Util.toInt(m.group(1), 0);
      int month = Util.toInt(m.group(2), 0);
      int dayOfMonth = Util.toInt(m.group(3), 0);
      this.saveDate = new LocalDate(year, month, dayOfMonth);
    }
    if (characterRoot == null) return;
    int numPersons = 0;
    int numMale = 0;
    int numFemale = 0;
    for (Node characterNode : characterRoot.children) {
      Integer key = characterNode.tagAsInt();
      if (key == null) continue;
      // don't see dead people.
      if (characterNode.findAttribute("death_date") != null) continue;
      numPersons++;
      Person person = new Person(key);
      person.key = key;
      person.birth_name = characterNode.findAttribute("birth_name");
      person.raw_birth_date = characterNode.findAttribute("birth_date");

      // compute their age, in years.
      m = DATE_PATTERN.matcher(person.raw_birth_date);
      if (m.matches()) {
        int year = Util.toInt(m.group(1), 0);
        int month = Util.toInt(m.group(2), 0);
        int dayOfMonth = Util.toInt(m.group(3), 0);
        LocalDate birthDate = new LocalDate(year, month, dayOfMonth);
        if (saveDate != null) {
          Period delta = new Period(birthDate, saveDate);
          person.ageInYears = delta.getYears();
        }
      }

      // some characters don't have parents.
      person.fatherKey = Util.toInt(characterNode.findAttribute("father"), null);
      person.motherKey = Util.toInt(characterNode.findAttribute("mother"), null);
      person.dynastyKey = Util.toInt(characterNode.findAttribute("dynasty"), null);
      person.hostKey = Util.toInt(characterNode.findAttribute("host"), null);
      person.cultureLabel = characterNode.findAttribute("culture");
      person.prestige = characterNode.getDouble("prestige");
      person.setPiety(characterNode.getDouble("piety"));
      person.religionLabel = characterNode.findAttribute("religion");
      person.setWealth(characterNode.getDouble("wealth"));
      person.isMale = !"yes".equals(characterNode.findAttribute("female"));
      if (person.isMale) {
        numMale++;
      } else {
        numFemale++;
      }
      Set<String> spouses = characterNode.findAttributes("spouse");
      if (spouses != null) {
        for (String spouse : spouses) {
          person.addSpouse(Util.toInt(spouse, null));
        }
      }
      Node traitsNode = characterNode.findDescendant("traits");
      if (traitsNode != null) {
        String [] traits = traitsNode.value.split("\\s+");
        if (traits != null) {
          for (String sTrait : traits) {
            person.addTrait(Util.toInt(sTrait, null));
            /*
            Integer traitNum = Util.toInt(sTrait, null);
            if (traitNum != null) {
              traitsCount[traitNum]++;
              if (traitNum > maxTrait) maxTrait = traitNum;
            }
            */
              
          }
        }
      }
      Node attributesNode = characterNode.findDescendant("attributes");
      for (String sAttribute : attributesNode.value.split("\\s+")) {
        person.addAttribute(Util.toInt(sAttribute, null));
      }
      // find the claims:
      int numClaims = 0;
      for (Node child : characterNode.children) {
        if (!"claim".equals(child.tag)) continue;
        numClaims++;
      }
      person.numClaims = numClaims;
      person.nickname = characterNode.findAttribute("nickname");
      // if they have a 'demesne', it always has a 'capital' (where
      // their shield goes?),
      // which is their primary demesne unless they have a higher-level
      // 'primary' which over-rides it.
      Node demesneNode = characterNode.findDescendant("demesne");
      if (demesneNode != null) {
        String primary = demesneNode.findAttribute("primary");
        if (primary == null || primary.isEmpty()) {
          primary = demesneNode.findAttribute("capital");
        }
        if (primary != null) {
          person.primaryDemesneLabel = primary;
        }
      }
      person.jobLabel = characterNode.findAttribute("job_title");
      person.titleLabel = characterNode.findAttribute("title");
      person.setHoldings(holdingsByHolder.get(key));

      people.put(key, person);
    }
    /*
    for (int i = 0; i < maxTrait; i++) {
      System.err.println(i + ":" + traitsCount[i]);
    }
    */
  }

  /**
   * Prune save-state info after first load.
   */
  public void pruneAndFinalize() {
    // compute everyones title, BEFORE tossing dead people,
    // as you often inherit your title from a dead parent.
    //computeTitles();
    // any spouses that aren't in the persons table are dead ones:
    int numKept = 0; int numTossed = 0;
    for (Person person : people.values()) {
      if (person.spouseKeys == null) continue;
      // can't remove from what you're iterating over....
      Set<Integer> goodKeys = new HashSet<Integer>();
      for (Integer spouseKey : person.spouseKeys) {
        if (!people.containsKey(spouseKey)) {
          numTossed++;
        } else {
          goodKeys.add(spouseKey);
          numKept++;
        }
      }
      person.spouseKeys = goodKeys;
    }
    // figure out how many kids everyone has
    for (Person person : people.values()) {
      // if I'm alive, and mom or dad is alive, then i'm a living child of theirs:
      Person parent = people.get(person.fatherKey);
      if (parent != null) {
        parent.addChild(person.isMale);
      }
      parent = people.get(person.motherKey);
      if (parent != null) {
        parent.addChild(person.isMale);
      }
    }
    // attributes less than 0 are upped to zero
    for (Person person : people.values()) {
      if (person.adjustedAttributes == null) continue;
      int numAttrs = person.adjustedAttributes.size();
      for (int i = 0; i < numAttrs; i++) {
        int attr = person.adjustedAttributes.get(i);
        if (attr < 0) {
          person.adjustedAttributes.set(i, 0);
        }
      }
    }
  }

  public Set<String> neededTitles() {
    Set<String> answer = new HashSet<String>();
    for (Person person : people.values()) {
      answer.add(person.titleLabel);
    }
    return answer;
  }
  
  public Set<String> neededDemesnes() {
    Set<String> answer = new HashSet<String>();
    for (Person person : people.values()) {
      if (person.primaryDemesneLabel != null) {
        answer.add(person.primaryDemesneLabel);
      }
      if (person.getHoldings() != null) {
        for (Holding holding : person.getHoldings()) {
          answer.add(holding.internalLabel);
        }
      }
    }
    return answer;
  }

  /**
   * @return the set of culture labels that we need to localise.
   */
  public Set<String> neededCultures() {
    Set<String> answer = new HashSet<String>();
    for (Person person : people.values()) {
      answer.add(person.cultureLabel);
    }
    return answer;
  }

  public Vector<Dynasty> getSortedDynasties() {
    if (sortedDynasties == null) {
      // do this lazily, as loadDynasties() can be called mlutiple times.
      sortedDynasties = new Vector<Dynasty>(dynasties.values());
      // and while we're at it, compute dynasty sizes:
      for (Person person : people.values()) {
        Dynasty dynasty = person.dynastyKey == null ? null : dynasties.get(person.dynastyKey);
        if (dynasty != null) {
          dynasty.addNoble();
        }
      }
      
      Collections.sort(sortedDynasties, new Comparator<Dynasty>() {
        @Override
        public int compare(Dynasty arg0, Dynasty arg1) {
          return arg0.toString().compareTo(arg1.toString());
        }});

    }
    return sortedDynasties;
  }
  
  private void addHolding(Integer holder, Holding holding) {
    Set<Holding> holdings = holdingsByHolder.get(holder);
    if (holdings == null) {
      holdings = new TreeSet<Holding>(new Comparator<Holding>() {
        @Override
        public int compare(Holding arg0, Holding arg1) {
          return arg0.holdingLevel.compareTo(arg1.holdingLevel);
        }});
    }
    holdings.add(holding);
    holdingsByHolder.put(holder, holdings);
  }

  public void loadHeldPlaces(Node root) {
    if (root == null) return;
    // places are top-level children of the root,
    // they have labels of the form [bcdek]_<something>
    // and we only care about those with a 'holder' child.
    Pattern PLACE_PATTERN = Pattern.compile("^\\s*([bcdek])_([^=]+)");
    for (Node kid : root.children) {
      Matcher m = PLACE_PATTERN.matcher(kid.tag);
      if (!m.find()) continue;
      Integer holder = Util.toInt(kid.findAttribute("holder"), null);
      Holding holding = new Holding(m.group(1), m.group(2));
      holdingsByLabel.put(holding.internalLabel, holding);
      if (holder != null) {
        addHolding(holder, holding);
      }
    }
  }
}
