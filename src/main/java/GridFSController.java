import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.bson.types.ObjectId;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoNamespace;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;

/**
 * Classe Controller
 * 
 * @author Alessio Lombardo
 *
 */
public class GridFSController {

	/**
	 * Identificativo del client in connessione con l'host o Replica Set
	 */
	private MongoClient client;

	/**
	 * Disco (Database) in uso
	 */
	private MongoDatabase disk;

	/**
	 * Stack che contiene la gerarchia di directory fino alla posizione corrente
	 */
	private Stack<GridFSBucket> path = new Stack<GridFSBucket>();

	/**
	 * Elenco di elementi da visualizzare in tabella
	 */
	private List<Record> elencoRecord = new ArrayList<Record>();

	/**
	 * Crea il controller e la connessione con MongoDB (singolo host)
	 * 
	 * @param ip
	 *            IP del server MongoDB
	 */
	public GridFSController(final String ip) {

		MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder();
		optionsBuilder.connectTimeout(1000);
		optionsBuilder.socketTimeout(5000);
		optionsBuilder.serverSelectionTimeout(1000);
		MongoClientOptions options = optionsBuilder.build();
		MongoClient testClient = new MongoClient(new ServerAddress(ip), options);
		testClient.getAddress();
		client = testClient;

	}

	/**
	 * Crea il controller e la connessione con MongoDB (Replica Set)
	 * 
	 * @param ipList
	 *            Elenco di IP del Replica Set MongoDB
	 */
	public GridFSController(List<String> ipList) {

		MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder();
		optionsBuilder.connectTimeout(2000);
		optionsBuilder.socketTimeout(6000);
		optionsBuilder.serverSelectionTimeout(2000);
		MongoClientOptions options = optionsBuilder.build();
		List<ServerAddress> serverList = new ArrayList<ServerAddress>();
		for (String s : ipList) {
			serverList.add(new ServerAddress(s));
		}
		MongoClient testClient = new MongoClient(serverList, options);
		testClient.getAddress();
		client = testClient;

	}

	/**
	 * Funzione corrispondente al "list"
	 * 
	 * @param type
	 *            Tipologia di elemento su cui effettuare il list
	 * @param name
	 *            Nome dell'elemento su cui effettuare il list
	 */
	public void list(String type, String name) {
		if (type == "DISK") { // Se e' stato selezionato un disco...
			GridFSGUI.btnCreaDirectory.setEnabled(true);
			GridFSGUI.btnDuplicaDisco.setEnabled(false);
			disk = client.getDatabase(name);
			listDisk();
		} else if (type == "DIR") { // Se e' stato selezionata una directory...
			GridFSGUI.btnCreaDirectory.setEnabled(true);
			GridFSGUI.btnDuplicaDisco.setEnabled(false);
			path.push(GridFSBuckets.create(disk, name));
			listDir();
		} else if (type == "PARENT_DIR" || type == "HOST") { // Se e' stato selezionata la parent directory...

			if (path.isEmpty()) { // Se e' stato raggiunto il primo livello...
				GridFSGUI.btnCreaDirectory.setEnabled(false);
				GridFSGUI.btnDuplicaDisco.setEnabled(true);
				listHost();
			} else { // Se non e' stato raggiunto il primo livello...
				GridFSGUI.btnCreaDirectory.setEnabled(true);
				GridFSGUI.btnDuplicaDisco.setEnabled(false);
				path.pop(); // Salgo al livello superiore

				if (path.isEmpty()) { // Se e' stato raggiunto il primo livello...
					listDisk();
				} else { // Altrimenti se non e' stato raggiunto il primo livello...
					listDir();
				}

			}

		}

	}

	/**
	 * List dell'host (saranno visualizzati i database, tra cui i dischi GridFS)
	 */
	public void listHost() {
		for (String diskName : client.listDatabaseNames()) {
			disk = client.getDatabase(diskName);

			if (isGridFS(disk)) { // Se il database e' GridFS

				Map<String, Object> info = client.getDB(disk.getName()).getStats().toMap();
				elencoRecord.add(new Record("DISK", diskName, "" + Math.round((Double) info.get("dataSize")),
						info.get("objects").toString()));

			} else if (!GridFSGUI.chckbxNoGridFS.isSelected()) { // Se il database non e' GridFS ma si vuole
																	// visualizzare ugualmente...
				elencoRecord.add(new Record("NO_GRIDFS", diskName, "", ""));
			}

		}

		disk = null; // Nessun disco viene considerato "selezionato"

		updateGUI();

	}

	/**
	 * List di un disco GridFS
	 */
	public void listDisk() {

		// Inserimento directory virtuale ".." (Parent Directory)
		elencoRecord.add(new Record("PARENT_DIR", "..", "", ""));

		for (String s : disk.listCollectionNames()) {

			String dirName;

			if (s.indexOf("/") != -1) {
				dirName = s.substring(0, s.indexOf("/"));
			} else {
				dirName = s.substring(0, s.indexOf("."));
			}

			if (!elencoRecord.contains(new Record("DIR", dirName, "", ""))) {

				Map<String, Number> infoChunks = client.getDB(disk.getName()).getCollection(dirName + ".chunks")
						.getStats().toMap();
				Map<String, Number> infoFiles = client.getDB(disk.getName()).getCollection(dirName + ".files")
						.getStats().toMap();

				elencoRecord.add(new Record("DIR", dirName,
						"" + (infoFiles.get("size").longValue() + infoChunks.get("size").longValue()),
						"" + infoChunks.get("count")));

			}

		}

		updateGUI();

	}

	/**
	 * List di una directory
	 */
	public void listDir() {

		// Inserimento directory virtuale ".." (Parent Directory)
		elencoRecord.add(new Record("PARENT_DIR", "..", "", ""));

		// List delle sotto-directory

		for (String s : disk.listCollectionNames()) {

			String dirName = s.substring(0, s.indexOf("."));
			String dirName2 = dirName;

			if (dirName.startsWith(pathToString() + "/")) {

				if (!dirName.substring(pathToString().length() + 1).contains("/")) {

					dirName = dirName.substring(dirName.lastIndexOf("/") + 1);

					if (!elencoRecord.contains(new Record("DIR", dirName, "", ""))) {

						Map<String, Number> infoChunks = client.getDB(disk.getName())
								.getCollection(dirName2 + ".chunks").getStats().toMap();
						Map<String, Number> infoFiles = client.getDB(disk.getName()).getCollection(dirName2 + ".files")
								.getStats().toMap();
						elencoRecord.add(new Record("DIR", dirName,
								"" + (infoFiles.get("size").longValue() + infoChunks.get("size").longValue()),
								"" + infoChunks.get("count")));

					}
				}

			}
		}

		// List dei file

		GridFSBuckets.create(disk, pathToString()).find().forEach(new Block<GridFSFile>() {
			public void apply(final GridFSFile f) {
				elencoRecord.add(new Record("FILE", f.getFilename(), Long.toString(f.getLength()),
						f.getObjectId().toHexString()));
			}
		});

		updateGUI();

	}

	/**
	 * Converte la path memorizzata nello Stack in stringa
	 * 
	 * @return Stringa corrispondente alla path nel formato "../.."
	 */
	public String pathToString() {

		String pathString = "";

		for (GridFSBucket dir : path) {
			pathString += dir.getBucketName() + "/";
		}

		return pathString.substring(0, pathString.length() - 1); // Rimuove l'ultimo "/"

	}

	/**
	 * Stabilisce se un database è un disco GridFS
	 * 
	 * @param disk
	 *            Database da verificare
	 * @return Vero se il database è un disco GridFS
	 */
	public boolean isGridFS(MongoDatabase disk) {

		// Si controllano i nomi di tutte le Collection per stabilire se ne esiste
		// qualcuna che non termina con ".files" o ".chunks"
		for (String s : disk.listCollectionNames()) {
			if (!s.endsWith(".files") && !s.endsWith(".chunks")) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Stabilisce se la posizione corrente è la radice cioè se viene visualizzato
	 * l'elenco dei dischi dell'host
	 * 
	 * @return Vero se la posizione corrente è la radice
	 */
	public boolean isRoot() {
		if (client != null && disk == null) {
			return true;
		}
		return false;
	}

	/**
	 * Stabilisce se la posizione corrente è una directory o una sotto-directory
	 * 
	 * @return Vero se la posizione corrente è una directory o sotto-directory
	 */
	public boolean isDir() {
		if (!path.isEmpty()) {
			return true;
		}
		return false;
	}

	/**
	 * Inserisce un file sulla directory corrente
	 * 
	 * @param inputFile
	 *            File da inserire
	 */
	public void uploadFile(File inputFile) {

		if (pathToString().length() - 1 < 0) {
			return;
		}

		GridFSBucket gridFSBucket = GridFSBuckets.create(disk, pathToString());

		try {
			InputStream streamToUploadFrom = new FileInputStream(inputFile);
			gridFSBucket.uploadFromStream(inputFile.getName(), streamToUploadFrom);
			streamToUploadFrom.close();
		} catch (FileNotFoundException ex) {
		} catch (IOException e) {
		}

		listDir();

	}

	/**
	 * Scarica un file
	 * 
	 * @param destination
	 *            Destinazione sul filesystem
	 * @param name
	 *            Nome del file da scaricare
	 * @param id
	 *            ID del file da scaricare
	 */
	public void downloadFile(String destination, String name, String id) {

		try {
			FileOutputStream streamToDownloadTo = new FileOutputStream(destination + "\\" + name);
			GridFSBuckets.create(disk, pathToString()).downloadToStream(name, streamToDownloadTo);
			streamToDownloadTo.close();
		} catch (IOException e) {
		}

	}

	/**
	 * Eliminazione di un disco, di una directory o di un file
	 * 
	 * @param type
	 *            Tipologia di elemento da eliminare
	 * @param name
	 *            Nome dell'elemento da eliminare (richiesto per dischi e directory)
	 * @param id
	 *            ID dell'elemento da eliminare (richiesto per i file)
	 */
	public void drop(String type, String name, String id) {
		if (type == "DISK" || type == "NO_GRIDFS") { // Rimozione dischi o database non GridFS...
			client.dropDatabase(name);
			listHost();
		} else if (type == "DIR") { // Rimozione directory...

			if (path.isEmpty()) { // Se e' stata selezionata una directory radice...
				// Rimozione directory
				GridFSBuckets.create(disk, name).drop();

				// Rimozione sotto-directory
				for (String s : disk.listCollectionNames()) {
					s = s.substring(0, s.indexOf("."));
					if (s.startsWith(name + "/")) {
						GridFSBuckets.create(disk, s).drop();
					}

				}

				listDisk();
			} else { // Altrimenti se e' stata selezionata una sotto-directory...
				// Rimozione directory
				GridFSBuckets.create(disk, pathToString() + "/" + name).drop();

				// Rimozione sotto-directory
				for (String s : disk.listCollectionNames()) {
					s = s.substring(0, s.indexOf("."));
					if (s.startsWith(pathToString() + "/" + name + "/")) {
						GridFSBuckets.create(disk, s).drop();
					}

				}

				listDir();
			}

		} else if (type == "FILE") { // Rimozione file...

			GridFSBuckets.create(disk, pathToString()).delete(new ObjectId(id));
			listDir();
		}
	}

	/**
	 * Crea una directory nella posizione corrente
	 * 
	 * @param dirName
	 *            Nome della directory
	 */
	public void createDirectory(String dirName) {

		if (dirName == null) {
			return;
		}

		if (path.isEmpty()) { // Se si e' alla radice della gerarchia...
			disk.createCollection(dirName + ".files");
			disk.createCollection(dirName + ".chunks");

			listDisk();

		} else { // Altrimenti se si e' in una directory
			disk.createCollection(pathToString() + "/" + dirName + ".files");
			disk.createCollection(pathToString() + "/" + dirName + ".chunks");

			listDir();
		}

	}

	/**
	 * Crea un nuovo disco GridFS
	 * 
	 * @param diskName
	 *            Nome del disco
	 */
	public void createDisk(String diskName) {

		if (diskName == null) {
			return;
		}

		client.getDatabase(diskName).createCollection("Nuova Cartella.files");
		client.getDatabase(diskName).createCollection("Nuova Cartella.chunks");

		path.clear(); // Reset della path. Necessario per ritornare alla visualizzazione dei dischi
						// dell'host
		listHost();
	}

	/**
	 * Rinomina un disco, una directory o un file
	 * 
	 * @param type
	 *            Tipologia di elemento da rinominare
	 * @param name
	 *            Nome dell'elemento da rinominare (richiesto per dischi e
	 *            directory)
	 * @param id
	 *            ID dell'elemento da rinominare (richiesto per i file)
	 * @param newName
	 *            Nuovo nome dell'elemento da rinominare
	 */
	public void rename(String type, String name, String id, String newName) {

		if (newName == null) {
			return;
		}

		if (type == "DISK") { // Se e' stato selezionato un disco...

			DBObject cmdObj = new BasicDBObject("copydb", 1).append("fromhost", client.getAddress().toString())
					.append("fromdb", name).append("todb", newName);
			client.getDB("admin").command(cmdObj);
			client.getDatabase(name).drop();
			listHost();

		} else if (type == "DIR") { // Se e' stata selezionata una directory

			if (path.isEmpty()) { // Se e' stata selezionata una directory radice

				// Ridenominazione directory
				disk.getCollection(name + ".files")
						.renameCollection(new MongoNamespace(disk.getName(), newName + ".files"));
				disk.getCollection(name + ".chunks")
						.renameCollection(new MongoNamespace(disk.getName(), newName + ".chunks"));

				// Ridenominazione sotto-directory
				for (String s : disk.listCollectionNames()) {
					if (s.startsWith(name + "/") && s.endsWith(".files")) {
						s = s.substring(0, s.indexOf("."));
						disk.getCollection(s + ".files").renameCollection(
								new MongoNamespace(disk.getName(), newName + s.substring(name.length()) + ".files"));
					} else if (s.startsWith(name + "/") && s.endsWith(".chunks")) {
						s = s.substring(0, s.indexOf("."));
						disk.getCollection(s + ".chunks").renameCollection(
								new MongoNamespace(disk.getName(), newName + s.substring(name.length()) + ".chunks"));
					}
				}

				listDisk();

			} else { // Altrimenti se e' stata selezionata una sotto-directory...

				// Ridenominazione directory
				disk.getCollection(pathToString() + "/" + name + ".files").renameCollection(
						new MongoNamespace(disk.getName(), pathToString() + "/" + newName + ".files"));
				disk.getCollection(pathToString() + "/" + name + ".chunks").renameCollection(
						new MongoNamespace(disk.getName(), pathToString() + "/" + newName + ".chunks"));

				// Ridenominazione sotto-directory
				for (String s : disk.listCollectionNames()) {

					if (s.startsWith(pathToString() + "/" + name + "/") && s.endsWith(".files")) {
						s = s.substring(0, s.indexOf("."));

						disk.getCollection(s + ".files")
								.renameCollection(new MongoNamespace(disk.getName(), pathToString() + "/" + newName
										+ s.substring((pathToString() + "/" + name).length()) + ".files"));

					} else if (s.startsWith(pathToString() + "/" + name + "/") && s.endsWith(".chunks")) {
						s = s.substring(0, s.indexOf("."));
						disk.getCollection(s + ".chunks")
								.renameCollection(new MongoNamespace(disk.getName(), pathToString() + "/" + newName
										+ s.substring((pathToString() + "/" + name).length()) + ".chunks"));
					}

				}

				listDir();
			}

		} else if (type == "FILE") { // Altrimenti se e' stato selezionato un file...
			GridFSBuckets.create(disk, pathToString()).rename(new ObjectId(id), newName);
			listDir();
		}

	}

	/**
	 * Duplica un disco
	 * 
	 * @param diskName
	 *            Disco origine
	 * @param diskName2
	 *            Disco clone
	 */
	public void duplicateDisk(String diskName, String diskName2) {

		if (diskName2 == null) {
			return;
		}

		DBObject cmdObj = new BasicDBObject("copydb", 1).append("fromhost", client.getAddress().toString())
				.append("fromdb", diskName).append("todb", diskName2);
		client.getDB("admin").command(cmdObj);
		path.clear();
		listHost();

	}

	/**
	 * Stampa un report su file contenente informazioni relative ai dischi dell'host
	 * 
	 * @param destination
	 *            Destinazione sul filesystem
	 */
	public void report(String destination) {

		try {
			final PrintWriter report = new PrintWriter(destination + "\\Report_GridFS.csv");

			report.println("TIPOLOGIA;NOME;DIMENSIONE;NUMERO ELEMENTI");

			for (String diskName : client.listDatabaseNames()) { // Per ogni disco...
				MongoDatabase disk = client.getDatabase(diskName);
				List<String> listDir = new ArrayList<String>();

				if (isGridFS(disk)) {

					Map<String, Object> info = client.getDB(disk.getName()).getStats().toMap();

					report.println("DISK;" + diskName + ";" + Math.round((Double) info.get("dataSize")) + ";"
							+ info.get("objects").toString());

					for (String s : disk.listCollectionNames()) { // Per ogni directory...

						String dirName;

						dirName = s.substring(0, s.indexOf("."));

						if (!listDir.contains(dirName)) {

							listDir.add(dirName);

							Map<String, Number> infoChunks = client.getDB(disk.getName())
									.getCollection(dirName + ".chunks").getStats().toMap();
							Map<String, Number> infoFiles = client.getDB(disk.getName())
									.getCollection(dirName + ".files").getStats().toMap();

							report.println("   DIR;" + dirName + ";"
									+ (infoFiles.get("size").longValue() + infoChunks.get("size").longValue()) + ";"
									+ infoChunks.get("count"));

							GridFSBuckets.create(disk, dirName).find().forEach(new Block<GridFSFile>() { // Per ogni
																											// file...
								public void apply(final GridFSFile f) {

									report.println(
											"      FILE;" + f.getFilename() + ";" + Long.toString(f.getLength()));

								}
							});

						}

					}

				}
			}

			report.close();
		} catch (IOException e) {
		}

	}

	/**
	 * Aggiorna la GUI in base all'operazione svolta dall'utente
	 */
	void updateGUI() {

		// Aggiornamento textbox "Percorso:"

		if (path != null && !path.isEmpty()) {
			GridFSGUI.txtPercorso.setText(client.getAddress() + ">/" + disk.getName() + "/" + pathToString() + "/");
		} else if (disk != null) {
			GridFSGUI.txtPercorso.setText(client.getAddress() + ">/" + disk.getName() + "/");
		} else {
			if (GridFSGUI.txtPercorso != null) {
				GridFSGUI.txtPercorso.setText(client.getAddress() + ">/");
			}

		}

		// Aggiornamento tabella

		GridFSGUI.updateTable(elencoRecord);

	}
}
