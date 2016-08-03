package me.corriekay.pokegoutil.windows;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.text.WordUtils;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.pokemon.EvolutionResult;
import com.pokegoapi.api.player.PlayerProfile.Currency;
import com.pokegoapi.api.pokemon.Pokemon;
import me.corriekay.pokegoutil.utils.GhostText;
import me.corriekay.pokegoutil.utils.JTableColumnPacker;
import me.corriekay.pokegoutil.utils.LDocumentListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.text.WordUtils;

import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass;
import POGOProtos.Networking.Responses.UpgradePokemonResponseOuterClass;
import me.corriekay.pokegoutil.utils.*;

@SuppressWarnings("serial")
public class PokemonTab extends JPanel {
	
	private final PokemonGo go;
	private final PokemonTable pt = new PokemonTable();
	private final JTextField searchBar = new JTextField("");
	
	public PokemonTab(PokemonGo go) {
		setLayout(new BorderLayout());
		this.go = go;
		JPanel topPanel = new JPanel(new GridBagLayout());
		JButton transferSelected, evolveSelected, powerUpSelected;
		transferSelected = new JButton("Transfer Selected");
		evolveSelected = new JButton("Evolve Selected");
		powerUpSelected = new JButton("Power Up Selected");

		GridBagConstraints gbc = new GridBagConstraints();
		topPanel.add(transferSelected, gbc);
		transferSelected.addActionListener(l-> new SwingWorker<Void, Void>(){
			protected Void doInBackground() throws Exception { transferSelected(); return null; }
		}.execute());
		topPanel.add(evolveSelected, gbc);
		evolveSelected.addActionListener(l -> new SwingWorker<Void, Void>() {
			protected Void doInBackground() throws Exception { evolveSelected(); return null; }
		}.execute());
		topPanel.add(powerUpSelected, gbc);
		powerUpSelected.addActionListener(l -> new SwingWorker<Void, Void>() {
			protected Void doInBackground() throws Exception { powerUpSelected(); return null; }
		}.execute());
		
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.gridwidth = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		topPanel.add(searchBar, gbc);
		
		LDocumentListener.addChangeListener(searchBar, e -> refreshList());
		new GhostText(searchBar, "Search Pokemon...");
		
		add(topPanel, BorderLayout.NORTH);
		JScrollPane sp = new JScrollPane(pt);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(sp, BorderLayout.CENTER);
	}
	
	private void transferSelected() {
		ArrayList<Pokemon> selection = getSelectedPokemon();
		if(selection.size() == 0) return;
		if(confirmOperation("Transfer", selection)) {
			MutableInt err = new MutableInt(), success = new MutableInt(), total = new MutableInt(1);
			selection.forEach(poke -> {
				System.out.println("Doing Operation " + total.getValue() + " of " + selection.size());
				total.increment();
				if (poke.isFavorite()){
					System.out.println("Pokemon is favorite, not transferring.");
					err.increment();
					return;
				}
				try {
					int candies = poke.getCandy();
					ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result result = poke.transferPokemon();
					go.getInventories().updateInventories(true);
					if(result == ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result.SUCCESS) {
						int newCandies = poke.getCandy();
						System.out.println("Transferring " + StringUtils.capitalize(poke.getPokemonId().toString().toLowerCase()) + ", Result: Success!");
						System.out.println("Stat changes: (Candies : " + newCandies + "[+" + (newCandies - candies) + "])");
						success.increment();						
					} else {
						System.out.println("Error transferring " + StringUtils.capitalize(poke.getPokemonId().toString().toLowerCase()) + ", result: " + result);
						err.increment();
					}
				} catch (Exception e) {
					err.increment();
					System.out.println("Error transferring pokemon! " + e.getMessage());
				}
			});
			try {
				go.getInventories().updateInventories(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			SwingUtilities.invokeLater(this::refreshList);
			JOptionPane.showMessageDialog(null, "Pokemon batch transfer complete!\nPokemon total: " + selection.size() + "\nSuccessful Transfers: " +success.getValue() + (err.getValue() > 0 ? "\nErrors: " + err.getValue() :""));
		}
	}
	
	private void evolveSelected() {
		ArrayList<Pokemon> selection = getSelectedPokemon();
		if(selection.size() > 0) {
			if(confirmOperation("Evolve", selection)) {
				MutableInt err = new MutableInt(), success = new MutableInt(), total = new MutableInt(1);
				selection.forEach(poke -> {
					System.out.println("Doing Operation " + total.getValue() + " of " + selection.size());
					total.increment();
					try {
						int candies = poke.getCandy();
						int candiesToEvolve = poke.getCandiesToEvolve();
						int cp = poke.getCp();
						int hp = poke.getMaxStamina();
						EvolutionResult er = poke.evolve();
						if(er.isSuccessful()) {
							go.getInventories().updateInventories(true);
							Pokemon newpoke = er.getEvolvedPokemon();
							int newcandies = newpoke.getCandy();
							int newcp = newpoke.getCp();
							int newhp = newpoke.getStamina();
							System.out.println("Evolving " + StringUtils.capitalize(poke.getPokemonId().toString().toLowerCase()) + ". Evolve result: Success!");
							System.out.println("Stat changes: (Candies: " + newcandies + "[" + candies + "-" + candiesToEvolve + "], CP: " + newcp + "[+" + (newcp - cp) + "], HP: " + newhp + "[+" + (newhp - hp) +"])");
							success.increment();
						} else {
							err.increment();
							System.out.println("Error evolving " + StringUtils.capitalize(poke.evolve().toString().toLowerCase())+ ", result: " + er);
						}
					} catch (Exception e) {
						err.increment();
						System.out.println("Error evolving pokemon! " + e.getMessage());
					}
				});
				try {
					go.getInventories().updateInventories(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
				SwingUtilities.invokeLater(this::refreshList);
				JOptionPane.showMessageDialog(null, "Pokemon batch evolve complete!\nPokemon total: " + selection.size() + "\nSuccessful evolves: " +success.getValue() + (err.getValue() > 0 ? "\nErrors: " + err.getValue() :""));
			}
		}
	}
	
	private void powerUpSelected() {
		ArrayList<Pokemon> selection = getSelectedPokemon();
		if(selection.size() > 0) {
			if(confirmOperation("PowerUp", selection)) {
				MutableInt err = new MutableInt(), success = new MutableInt(), total = new MutableInt(1);
				selection.forEach(poke -> {
					try {
						System.out.println("Doing Operation " + total.getValue() + " of " + selection.size());
						total.increment();
						int candies = poke.getCandy();
						int cp = poke.getCp();
						int hp = poke.getMaxStamina();
						int stardustUsed = poke.getStardustCostsForPowerup();
						UpgradePokemonResponseOuterClass.UpgradePokemonResponse.Result result = poke.powerUp();
						go.getPlayerProfile().updateProfile();
						if(result == UpgradePokemonResponseOuterClass.UpgradePokemonResponse.Result.SUCCESS) {
							int newCandies = poke.getCandy();
							int newCp = poke.getCp();
							int newHp = poke.getMaxStamina();
							System.out.println("Powering Up " + StringUtils.capitalize(poke.getPokemonId().toString().toLowerCase()) + ", Result: Success!");
							System.out.println("Stat changes: (Candies : " + newCandies + "[-" + (newCandies - candies) + "], CP: " + newCp + "[+" + (newCp - cp) + "], HP: " + newHp + "[+" + (newHp - hp) + "]) Stardust used " + stardustUsed + "[remaining: " + go.getPlayerProfile().getCurrency(Currency.STARDUST) + "]");
							success.increment();
						} else {
							err.increment();
							System.out.println("Error powering up " + StringUtils.capitalize(poke.getPokemonId().toString().toLowerCase()) + ", result: " + result);
						}
					} catch (Exception e) {
						err.increment();
						System.out.println("Error powering up pokemon! " + e.getMessage());
					}
				});
				try {
					go.getInventories().updateInventories(true);
					PokemonGoMainWindow.window.refreshTitle();
				} catch (Exception e) {
					e.printStackTrace();
				}
				SwingUtilities.invokeLater(this::refreshList);
				JOptionPane.showMessageDialog(null, "Pokemon batch powerup complete!\nPokemon total: " + selection.size() + "\nSuccessful powerups: " +success.getValue() + (err.getValue() > 0 ? "\nErrors: " + err.getValue() :""));
			}
		}
	}
	
	private boolean confirmOperation(String operation, ArrayList<Pokemon> pokes) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		
		JPanel innerPanel = new JPanel();
		innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
		innerPanel.setAlignmentX(CENTER_ALIGNMENT);
		
		JScrollPane scroll = new JScrollPane(innerPanel);
		scroll.setAlignmentX(CENTER_ALIGNMENT);
		scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
		
		pokes.forEach(p -> {
			String str = StringUtils.capitalize(p.getPokemonId() + "") + " - CP: " + p.getCp() + ", IV: " + (Math.round(p.getIvRatio() * 10000)/100) + "%";
			innerPanel.add(new JLabel(str));
		});
		panel.add(scroll);
		int response = JOptionPane.showConfirmDialog(null, panel, "Please confirm " + operation + " of " + pokes.size() + " pokemon", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		return response == JOptionPane.OK_OPTION;
	}
	
	private ArrayList<Pokemon> getSelectedPokemon() {
		ArrayList<Pokemon> pokes = new ArrayList<>();
		PokemonTableModel model = (PokemonTableModel)pt.getModel();
		for(int i : pt.getSelectedRows()) {
			Pokemon poke = model.getPokemonByIndex(i);
			if(poke != null) {
				pokes.add(poke);
			}
		}
		return pokes;
	}

	private void refreshList() {
		List<Pokemon> pokes = new ArrayList<>();
		String search = searchBar.getText().replaceAll(" ", "").replaceAll("_", "").replaceAll("snek", "ekans").toLowerCase();
		go.getInventories().getPokebank().getPokemons().forEach(poke -> {
			String searchme = poke.getPokemonId() + "" + poke.getPokemonFamily() + poke.getNickname() + poke.getMeta().getType1() + poke.getMeta().getType2() + poke.getMove1() + poke.getMove2() + poke.getPokeball() + poke.getLevel();
			searchme = searchme.replaceAll("_FAST", "").replaceAll("FAMILY_", "").replaceAll("NONE", "").replaceAll("ITEM_", "").replaceAll("_", "").replaceAll(" ", "").toLowerCase();
			if(searchme.contains(search)) {
				pokes.add(poke);
			}
		});
		pt.constructNewTableModel(go, (search.equals("") || search.equals("searchpokemon...") ? go.getInventories().getPokebank().getPokemons() : pokes));
		for(int i = 0; i < pt.getModel().getColumnCount(); i++) {
			JTableColumnPacker.packColumn(pt, i, 4);
		}
	}
	
	private static class PokemonTable extends JTable {
		
		/**
		 * data types:
		 * 0 String - Nickname
		 * 1 Integer - Pokemon Number
		 * 2 String - Type / Pokemon
		 * 3 Double - IV %
		 * 4 String - Type 1
		 * 5 String - Type 2
		 * 6 String - Move 1
		 * 7 String - Move 2
		 * 8 Integer - CP
		 * 9 Integer - HP
		 * 10 Integer - Candies of type
		 * 11 Integer - Candies to Evolve
		 * 12 Integer - Star Dust to level
		 * 13 String - Pokeball Type
         * 14 Integer - Level
		 */
		int sortColIndex = 0;
		SortOrder so = SortOrder.ASCENDING;
		private PokemonTable() {
			setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			setAutoResizeMode(AUTO_RESIZE_OFF);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private void constructNewTableModel(PokemonGo go, List<Pokemon> pokes) {
			PokemonTableModel ptm = new PokemonTableModel(pokes, this);
			setModel(ptm);
			TableRowSorter trs = new TableRowSorter(getModel());
			Comparator<Integer> c = (i1, i2) -> Math.round(i1 - i2);
            trs.setComparator(0, c);
            trs.setComparator(3, c);
			trs.setComparator(4, (d1, d2) -> (int)((double)d1 - (double)d2));
            trs.setComparator(5, c);
			trs.setComparator(10, c);
            //TODO: needs to be fixed/debugged
			trs.setComparator(11, (e1, e2) -> {
			    if(e1.equals("-"))
			        e1 = "0";
                if(e2.equals("-"))
                    e2 = "0";
                return Math.round(Float.parseFloat((String) e1) - Float.parseFloat((String) e2));
			});
			//trs.setComparator(12, c);
            trs.setComparator(13, c);
			setRowSorter(trs);
			trs.toggleSortOrder(sortColIndex);
			List<SortKey> sortKeys = new ArrayList<>();
			sortKeys.add(new SortKey(sortColIndex, so));
			trs.setSortKeys(sortKeys);
		}
	}
	private static class PokemonTableModel extends AbstractTableModel {
		
		PokemonTable pt;

		private final ArrayList<Pokemon> pokeCol = new ArrayList<>();
		private final ArrayList<String>  nickCol = new ArrayList<>();//0
		private final ArrayList<Integer> numIdCol = new ArrayList<>();//1
		private final ArrayList<String>  speciesCol = new ArrayList<>();//2
		private final ArrayList<Double>  ivCol = new ArrayList<>();//3
		private final ArrayList<String>  type1Col = new ArrayList<>(),//4
										 type2Col = new ArrayList<>(),//5
										 move1Col = new ArrayList<>(),//6
										 move2Col = new ArrayList<>();//7
		private final ArrayList<Integer> cpCol = new ArrayList<>(),//8
										 hpCol = new ArrayList<>();//9
		private final ArrayList<Integer> candiesCol = new ArrayList<>();//10
        private final ArrayList<String> candies2EvlvCol = new ArrayList<>();//11
        private final ArrayList<Integer> dustToLevelCol = new ArrayList<>();//12
		private final ArrayList<String>  pokeballCol = new ArrayList<>();//13
        private final ArrayList<Integer>  levelCol = new ArrayList<>();
		
		private PokemonTableModel(List<Pokemon> pokes, PokemonTable pt) {
			this.pt = pt;
			MutableInt i = new MutableInt();
			pokes.forEach(p -> {
				pokeCol.add(i.getValue(), p);
                numIdCol.add(i.getValue(), p.getMeta().getNumber());
				nickCol.add(i.getValue(), p.getNickname());
				speciesCol.add(i.getValue(), StringUtils.capitalize(p.getPokemonId().toString().toLowerCase().replaceAll("_male", "♂").replaceAll("_female", "♀")));
                levelCol.add(i.getValue(), Math.round(p.getLevel()));
                ivCol.add(i.getValue(), Math.round(p.getIvRatio() * 10000) / 100.00);
                cpCol.add(i.getValue(), p.getCp());
				type1Col.add(i.getValue(), StringUtils.capitalize(p.getMeta().getType1().toString().toLowerCase()));
				type2Col.add(i.getValue(), StringUtils.capitalize(p.getMeta().getType2().toString().toLowerCase().replaceAll("none", "")));
				move1Col.add(i.getValue(), WordUtils.capitalize(p.getMove1().toString().toLowerCase().replaceAll("_fast", "").replaceAll("_", " ")));
				move2Col.add(i.getValue(), WordUtils.capitalize(p.getMove2().toString().toLowerCase().replaceAll("_", " ")));
				hpCol.add(i.getValue(), p.getStamina());
				candiesCol.add(i.getValue(), p.getCandy());
                if(p.getCandiesToEvolve() != 0)
				    candies2EvlvCol.add(i.getValue(), String.valueOf(p.getCandiesToEvolve()));
                else
                    candies2EvlvCol.add(i.getValue(), "-");
				dustToLevelCol.add(i.getValue(), p.getStardustCostsForPowerup());
				pokeballCol.add(i.getValue(), WordUtils.capitalize(p.getPokeball().toString().toLowerCase().replaceAll("item_", "").replaceAll("_", " ")));
                i.increment();
			});
		}
		
		private Pokemon getPokemonByIndex(int i) {
			try {
				return pokeCol.get(pt.convertRowIndexToModel(i));
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch(columnIndex) {
                case 0: return "Id";
				case 1: return "Nickname";
				case 2: return "Species";
                case 3: return "Lvl";
				case 4: return "IV %";
                case 5: return "CP";
				case 6: return "Type 1";
				case 7: return "Type 2";
				case 8: return "Move 1";
				case 9: return "Move 2";
				case 10: return "HP";
				case 11: return "Candies";
				case 12: return "To Evolve";
				case 13: return "Stardust";
				case 14: return "Caught With";
				default: return "UNKNOWN?";
			}
		}

		@Override
		public int getColumnCount() {
			return 15;
		}

		@Override
		public int getRowCount() {
			return pokeCol.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch(columnIndex) {
                case 0: return numIdCol.get(rowIndex);
				case 1: return nickCol.get(rowIndex);
				case 2: return speciesCol.get(rowIndex);
                case 3: return levelCol.get(rowIndex);
				case 4: return ivCol.get(rowIndex);
                case 5: return cpCol.get(rowIndex);
				case 6: return type1Col.get(rowIndex);
				case 7: return type2Col.get(rowIndex);
				case 8: return move1Col.get(rowIndex);
				case 9: return move2Col.get(rowIndex);
				case 10: return hpCol.get(rowIndex);
				case 11: return candiesCol.get(rowIndex);
				case 12: return candies2EvlvCol.get(rowIndex);
				case 13: return dustToLevelCol.get(rowIndex);
				case 14: return pokeballCol.get(rowIndex);
				default: return null;
			}
		}
	}
}
