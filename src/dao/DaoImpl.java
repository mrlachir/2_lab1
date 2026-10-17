package dao;

/** Source de donnees simulee pour le laboratoire. */
public class DaoImpl implements IDao {
    @Override
    public double getValue() {
        System.out.println("[DAO] Récupération de la valeur depuis la source de données...");
        return 100.0;
    }
}
