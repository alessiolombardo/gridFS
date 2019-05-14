import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.ListSelectionModel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.ImageIcon;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;

/**
 * Classe che gestisce la GUI
 * 
 * @author Alessio Lombardo
 *
 */
public class GridFSGUI extends JFrame {

	private static final long serialVersionUID = 1L;
	private GridFSController c;

	private static JTable table = new JTable();
	private JTextField txtHost = new JTextField();
	public static JTextField txtPercorso;
	public static JCheckBox chckbxNoGridFS;
	public static JButton btnCreaDirectory = new JButton("Crea Directory");
	public static JButton btnElimina = new JButton("Elimina");
	public static JButton btnCreaDisco = new JButton("Crea Disco");
	public static JButton btnRinomina = new JButton("Rinomina");
	public static JButton btnDuplicaDisco = new JButton("Duplica Disco");
	public static JButton btnReport = new JButton("Report");

	/**
	 * Lancio dell'applicazione
	 * 
	 * @throws UnknownHostException
	 */
	public static void main(String[] args) throws UnknownHostException {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GridFSGUI frame = new GridFSGUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	/**
	 * Disegno della GUI
	 */
	public GridFSGUI() {

		/* --------------------------------------------- */
		/* TABELLA */

		table.setShowVerticalLines(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent mouseEvent) {
				JTable table = (JTable) mouseEvent.getSource();
				if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {

					String type = table.getValueAt(table.getSelectedRow(), 0).toString();
					String name = table.getValueAt(table.getSelectedRow(), 1).toString();
					String id = table.getValueAt(table.getSelectedRow(), 3).toString();

					if (type == "FILE") {

						/* --------------------------------------------- */
						/* DOWNLOAD */

						JFileChooser fc = new JFileChooser();
						fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						fc.showSaveDialog(null);
						if (fc.getSelectedFile() != null) {
							c.downloadFile(fc.getSelectedFile().getAbsolutePath(), name, id);
						}

					} else {

						/* --------------------------------------------- */
						/* LIST */

						c.list(type, name);
					}

				}
			}
		});

		/* --------------------------------------------- */
		/* LABEL */

		JLabel lblHost = new JLabel("Host:");
		lblHost.setBounds(10, 33, 70, 14);

		JLabel lblReplicaSet = new JLabel("Replica Set:");
		lblReplicaSet.setBounds(10, 58, 70, 14);

		JLabel lblPercorso = new JLabel("Percorso:");
		lblPercorso.setBounds(10, 103, 59, 14);

		/* --------------------------------------------- */
		/* TEXT BOX */

		txtHost.setText("localhost");
		txtHost.setBounds(90, 30, 164, 20);
		txtHost.setColumns(10);

		txtPercorso = new JTextField();
		txtPercorso.setEditable(false);
		txtPercorso.setBounds(79, 97, 671, 20);
		txtPercorso.setColumns(10);

		/* --------------------------------------------- */
		/* CHECK BOX */

		chckbxNoGridFS = new JCheckBox("Nascondi Database non GridFS");
		chckbxNoGridFS.setSelected(true);
		chckbxNoGridFS.setBounds(540, 29, 210, 23);
		chckbxNoGridFS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (c != null && c.isRoot()) {
					c.list("HOST", "");
				}
			}
		});

		/* --------------------------------------------- */
		/* COMBO BOX */

		final JComboBox<String> comboBoxRS = new JComboBox<String>();
		comboBoxRS.setModel(new DefaultComboBoxModel<String>(new String[] { "localhost" }));
		comboBoxRS.setEditable(true);
		comboBoxRS.setBounds(90, 55, 164, 20);
		comboBoxRS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				for (int i = 0; i < comboBoxRS.getModel().getSize(); i++) {
					if (comboBoxRS.getSelectedItem().toString()
							.equals(comboBoxRS.getModel().getElementAt(i).toString())) {

						return;

					}
				}

				comboBoxRS.addItem(comboBoxRS.getSelectedItem().toString());

			}
		});

		/* --------------------------------------------- */
		/* BOTTONE CONNESSIONE HOST */

		JButton btnConnetti = new JButton("Connetti (Host)");
		btnConnetti.setIcon(
				new ImageIcon(GridFSGUI.class.getResource("/com/sun/java/swing/plaf/windows/icons/Computer.gif")));
		btnConnetti.setBounds(264, 29, 196, 23);
		btnConnetti.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					c = new GridFSController(txtHost.getText());
					btnElimina.setEnabled(true);
					btnRinomina.setEnabled(true);
					btnCreaDisco.setEnabled(true);
					btnDuplicaDisco.setEnabled(true);
					btnReport.setEnabled(true);
					c.list("HOST", "");
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(null, "Impossibile connettersi con l'host", "Errore",
							JOptionPane.ERROR_MESSAGE);
				}

			}
		});

		/* --------------------------------------------- */
		/* BOTTONE CONNESSIONE REPLICA SET */

		JButton btnConnettiRS = new JButton("Connetti (Replica Set)");
		btnConnettiRS.setIcon(
				new ImageIcon(GridFSGUI.class.getResource("/javax/swing/plaf/metal/icons/ocean/computer.gif")));
		btnConnettiRS.setBounds(264, 54, 196, 23);
		btnConnettiRS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {

					List<String> ipList = new ArrayList<String>();
					for (int i = 0; i < comboBoxRS.getModel().getSize(); i++) {
						ipList.add(comboBoxRS.getModel().getElementAt(i).toString());
					}

					c = new GridFSController(ipList);

					btnElimina.setEnabled(true);
					btnRinomina.setEnabled(true);
					btnCreaDisco.setEnabled(true);
					btnDuplicaDisco.setEnabled(true);
					btnReport.setEnabled(true);
					c.list("HOST", "");
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(null, "Impossibile connettersi con il Replica Set", "Errore",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		/* --------------------------------------------- */
		/* BOTTONE ELIMINA */

		btnElimina.setEnabled(false);
		btnElimina
				.setIcon(new ImageIcon(GridFSGUI.class.getResource("/com/sun/java/swing/plaf/motif/icons/Error.gif")));
		btnElimina.setBounds(10, 438, 140, 23);
		btnElimina.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (table.getSelectedRow() != -1) {

					String type = table.getValueAt(table.getSelectedRow(), 0).toString();
					String name = table.getValueAt(table.getSelectedRow(), 1).toString();
					String id = table.getValueAt(table.getSelectedRow(), 3).toString();

					c.drop(type, name, id);

				}

			}
		});

		/* --------------------------------------------- */
		/* BOTTONE CREA DISCO */

		btnCreaDisco.setEnabled(false);
		btnCreaDisco.setIcon(
				new ImageIcon(GridFSGUI.class.getResource("/com/sun/java/swing/plaf/windows/icons/HardDrive.gif")));
		btnCreaDisco.setBounds(310, 438, 140, 23);
		btnCreaDisco.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				c.createDisk(JOptionPane.showInputDialog(null, "Inserisci nome del disco", "Nuovo_Disco"));

			}
		});

		/* --------------------------------------------- */
		/* BOTTONE CREA DIRECTORY */

		btnCreaDirectory.setEnabled(false);
		btnCreaDirectory.setIcon(
				new ImageIcon(GridFSGUI.class.getResource("/com/sun/java/swing/plaf/windows/icons/Directory.gif")));
		btnCreaDirectory.setBounds(610, 438, 140, 23);
		btnCreaDirectory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				c.createDirectory(
						JOptionPane.showInputDialog(null, "Inserisci nome della directory", "Nuova Cartella"));
			}
		});

		/* --------------------------------------------- */
		/* BOTTONE RINOMINA */

		btnRinomina.setEnabled(false);
		btnRinomina.setIcon(new ImageIcon(
				GridFSGUI.class.getResource("/com/sun/javafx/scene/web/skin/FontBackgroundColor_16x16_JFX.png")));
		btnRinomina.setBounds(160, 438, 140, 23);
		btnRinomina.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (table.getSelectedRow() != -1) {

					table.editCellAt(table.getSelectedRow(), 1);

					String type = table.getValueAt(table.getSelectedRow(), 0).toString();
					String name = table.getValueAt(table.getSelectedRow(), 1).toString();
					String id = table.getValueAt(table.getSelectedRow(), 3).toString();
					if (type != "PARENT_DIR") {
						c.rename(type, name, id, JOptionPane.showInputDialog(null, "Inserisci nuovo nome", name));
					}

				}

			}
		});

		/* --------------------------------------------- */
		/* BOTTONE DUPLICA DISCO */

		btnDuplicaDisco.setEnabled(false);
		btnDuplicaDisco.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (table.getSelectedRow() != -1) {

					table.editCellAt(table.getSelectedRow(), 1);

					String type = table.getValueAt(table.getSelectedRow(), 0).toString();
					String name = table.getValueAt(table.getSelectedRow(), 1).toString();

					if (type == "DISK") {
						c.duplicateDisk(name,
								JOptionPane.showInputDialog(null, "Inserisci nome del disco", name + "_Copia"));
					}

				}

			}

		});
		btnDuplicaDisco.setIcon(new ImageIcon(
				GridFSGUI.class.getResource("/com/sun/javafx/scene/control/skin/modena/HTMLEditor-Copy-Black.png")));
		btnDuplicaDisco.setBounds(460, 438, 140, 23);

		/* --------------------------------------------- */
		/* BOTTONE REPORT */

		btnReport.setEnabled(false);
		btnReport.setIcon(new ImageIcon(
				GridFSGUI.class.getResource("/com/sun/deploy/uitoolkit/impl/fx/ui/resources/image/graybox_error.png")));
		btnReport.setBounds(630, 54, 120, 23);
		btnReport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fc.showSaveDialog(null);
				if (fc.getSelectedFile() != null) {
					c.report(fc.getSelectedFile().getAbsolutePath());
				}

			}
		});

		/* --------------------------------------------- */
		/* PANNELLI */

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(10, 128, 740, 299);
		scrollPane.setViewportView(table);

		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(null);
		contentPane.add(lblHost);
		contentPane.add(lblReplicaSet);
		contentPane.add(comboBoxRS);
		contentPane.add(btnConnettiRS);
		contentPane.add(txtHost);
		contentPane.add(scrollPane);
		contentPane.add(btnConnetti);
		contentPane.add(chckbxNoGridFS);
		contentPane.add(lblPercorso);
		contentPane.add(txtPercorso);
		contentPane.add(btnElimina);
		contentPane.add(btnCreaDisco);
		contentPane.add(btnCreaDirectory);
		contentPane.add(btnRinomina);
		contentPane.add(btnDuplicaDisco);
		contentPane.add(btnReport);

		/* --------------------------------------------- */
		/* UPLOAD */

		new FileDrop(scrollPane, new FileDrop.Listener() {
			public void filesDropped(java.io.File[] files) {
				if (c != null && c.isDir()) {
					for (File f : files) {
						c.uploadFile(f);
					}
				}
			}
		});

		/* --------------------------------------------- */
		/* FINESTRA */

		setResizable(false);
		setTitle("MongoDB - GridFS [Alessio Lombardo]");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 767, 500);
		setContentPane(contentPane);

	}

	/**
	 * Aggiorna la tabella che visualizza gli elementi della posizione corrente
	 * 
	 * @param elencoRecord
	 *            Elenco dei file e/o delle cartelle da visualizzare
	 */
	public static void updateTable(List<Record> elencoRecord) {

		// Conversione dei record in matrice di stringhe

		String[][] elencoFileArray = new String[elencoRecord.size()][Record.numColonne];

		int i = 0;
		for (Record r : elencoRecord) {
			elencoFileArray[i][0] = r.type;
			elencoFileArray[i][1] = r.name;
			elencoFileArray[i][2] = r.dimension;
			elencoFileArray[i][3] = r.id;
			i++;
		}
		elencoRecord.clear();

		// Aggiornamento effettivo della tabella

		table.setModel(new DefaultTableModel(elencoFileArray,
				new String[] { "Tipo", "Nome", "Dimensione", "ID - Numero Elementi" }) {
			private static final long serialVersionUID = 1L;
			boolean[] columnEditables = new boolean[] { false, false, false, false };

			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
	}

}
