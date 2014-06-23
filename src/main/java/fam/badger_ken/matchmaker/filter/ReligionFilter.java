// c(2012) i.am.badger.ken@gmail.com
// You may re-use, so long as I am credited, and you don't charge
// for your work that uses this.
package fam.badger_ken.matchmaker.filter;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import fam.badger_ken.matchmaker.Filterer;
import fam.badger_ken.matchmaker.Person;
import fam.badger_ken.matchmaker.ResultMaker;
import fam.badger_ken.matchmaker.SwingGui;
import fam.badger_ken.matchmaker.widget.AnyDropdownable;

/**
 * The 'ReligionFilter' filters for presence/absence of a given religion.
 */
public class ReligionFilter implements Filterer {
  private ResultMaker resultMaker;
  private boolean isPresenceChecker;

  private boolean onNow = false;
  private String value = null;  // the religion label

  public ReligionFilter(ResultMaker resultMaker, boolean isPresenceChecker) {
    this.resultMaker = resultMaker;
    this.isPresenceChecker = isPresenceChecker;
  }

  @Override
  public void notify(JComponent changee, JComponent backgrounder) {
    JComboBox box = (JComboBox) changee;
    AnyDropdownable dropdown = (AnyDropdownable) box.getSelectedItem();
    String newValue = (String) dropdown.infoForFilter();
    boolean toBeOn = (newValue != null);
    // a change is either changing null-ness, or changing value.
    boolean change = (toBeOn != onNow)
        || (onNow && !newValue.equals(value));
    onNow = toBeOn;
    value = newValue;
    if (change && backgrounder != null) {
      backgrounder.setBackground(onNow ? SwingGui.FILTER_ON_COLOR : SwingGui.FILTER_OFF_COLOR);
      resultMaker.makeResults();
    }
  }

  @Override
  public boolean passes(Person person) {
    if (!onNow) return true;
    return isPresenceChecker == (person.religionLabel.equals(value)); 
  }

  @Override
  public boolean isOn() {
    return onNow;
  }
}
