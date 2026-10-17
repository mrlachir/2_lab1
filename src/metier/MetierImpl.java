package metier;

import dao.IDao;

public class MetierImpl implements IMetier {
    private IDao dao;

    /** Setter invoque par reflexion par le conteneur. */
    public void setDao(IDao dao) {
        this.dao = java.util.Objects.requireNonNull(dao, "Le DAO ne peut pas etre null.");
    }

    @Override
    public double calcul() {
        if (dao == null) {
            throw new IllegalStateException("Le DAO n'a pas été injecté.");
        }
        double resultat = dao.getValue() * 2;
        System.out.println("[METIER] Calcul effectué. Résultat : " + resultat);
        return resultat;
    }
}
