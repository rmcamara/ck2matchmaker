// c(2012) i.am.badger.ken@gmail.com
// You may re-use, so long as I am credited, and you don't charge
// for your work that uses this.
package fam.badger_ken.matchmaker.cell;

import java.awt.Component;
import java.util.Comparator;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import fam.badger_ken.matchmaker.Matchmaker;
import fam.badger_ken.matchmaker.Person;

public class TraitsHandler implements TableCellRenderer, Comparator<Person> {
  Matchmaker matchmaker;
  public TraitsHandler(Matchmaker matchmaker) {
    this.matchmaker = matchmaker;
  }

  @Override
  public int compare(Person arg0, Person arg1) {
    // it seems to work a little better to compare on length.
    return arg1.getNumTraits() - arg0.getNumTraits();
    //String name0 = arg0.getDisplayTraits(matchmaker.gameConfig, matchmaker.saveState);
    //String name1 = arg1.getDisplayTraits(matchmaker.gameConfig, matchmaker.saveState);
    //return name0.compareTo(name1);
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {
    Person person = (Person) value;
    if (person == null) return null;
    String val = person.getDisplayTraits(matchmaker.gameConfig, matchmaker.saveState);
    return new JLabel(val);
  }

}
