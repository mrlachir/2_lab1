package presentation;

import dao.IDao;
import metier.IMetier;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;

/** Mini-conteneur : configuration -> chargement -> instanciation -> injection. */
public final class Presentation2 {
    private Presentation2() {}

    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: Presentation2 [chemin/config.txt]");
        }
        Path config = Paths.get(args.length == 1 ? args[0] : "config.txt");
        // Propagate errors to the launcher: an invalid configuration must not look successful.
        double resultat = chargerMetier(config).calcul();
        System.out.println("Résultats = " + resultat);
    }

    public static IMetier chargerMetier(Path config) throws IOException, ReflectiveOperationException {
        String[] names = loadClassNames(config.toString());
        IDao dao = instantiateClass(names[0], IDao.class);
        System.out.println("[CONTAINER] DAO instancié : " + dao.getClass().getName());
        IMetier metier = instantiateClass(names[1], IMetier.class);
        System.out.println("[CONTAINER] Métier instancié : " + metier.getClass().getName());
        injectDependency(metier, "setDao", IDao.class, dao);
        System.out.println("[CONTAINER] Dépendance injectée via setDao(IDao).");
        return metier;
    }

    /** Two non-empty fully qualified class names, in DAO then business order. */
    public static String[] loadClassNames(String fileName) throws IOException {
        String[] names = new String[2];
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName), StandardCharsets.UTF_8)) {
            for (int i = 0; i < names.length; i++) {
                String line = reader.readLine();
                if (line == null || line.trim().isEmpty()) {
                    throw new IOException("Configuration invalide : nom de classe manquant a la ligne " + (i + 1));
                }
                names[i] = line.trim();
            }
            String extra;
            while ((extra = reader.readLine()) != null) {
                if (!extra.trim().isEmpty()) {
                    throw new IOException("Configuration invalide : exactement deux noms de classes sont attendus.");
                }
            }
        } catch (NoSuchFileException e) {
            throw new IOException("Configuration introuvable : " + Paths.get(fileName).toAbsolutePath()
                    + ". Definissez le repertoire de travail ou passez le chemin en argument.", e);
        }
        return names;
    }

    /** Public no-argument constructor required; validates the interface before instantiation. */
    public static <T> T instantiateClass(String className, Class<T> contract)
            throws ReflectiveOperationException {
        Class<?> clazz = Class.forName(className);
        if (!contract.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(className + " doit implementer " + contract.getName());
        }
        Constructor<?> constructor = clazz.getConstructor();
        return contract.cast(constructor.newInstance());
    }

    /** Explicit contract avoids relying on the ordering of getInterfaces(). */
    public static <T> void injectDependency(Object target, String setterName,
                                            Class<T> contract, T dependency)
            throws ReflectiveOperationException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(dependency, "dependency");
        Method setter = target.getClass().getMethod(setterName, contract);
        setter.invoke(target, contract.cast(dependency));
    }
}
