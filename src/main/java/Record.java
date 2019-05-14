/**
 * Classe che modella un record da visualizzare in tabella
 * 
 * @author Alessio Lombardo
 *
 */
public class Record {

	public static int numColonne = 4;

	public String type;
	public String name;
	public String dimension;
	public String id;

	/**
	 * Crea un record che rappresenta un elemento da visualizzare
	 * 
	 * @param type
	 *            Tipologia elemento (NO_GRIDFS, DISK, DIR, PARENT_DIR, FILE)
	 * @param name
	 *            Nome elemento
	 * @param dimension
	 *            Dimensione elemento
	 * @param id
	 *            ID file o numero elementi nel caso di DISK o DIR
	 */
	public Record(String type, String name, String dimension, String id) {
		super();
		this.type = type;
		this.name = name;
		this.dimension = dimension;
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {

		if (!type.equals(((Record) obj).type)) {
			return false;
		}

		if (!name.equals(((Record) obj).name)) {
			return false;
		}

		return true;

	}

}
