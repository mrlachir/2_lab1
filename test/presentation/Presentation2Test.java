package presentation;

import dao.*;
import metier.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.Assert.*;

public class Presentation2Test {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private Path config(String content) throws IOException {
        Path path = folder.newFile().toPath();
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path;
    }
    public static class AlternativeDao implements IDao {
        public double getValue() { return 21; }
    }
    public static class InheritedDao extends AlternativeDao {}
    public static class NoDefaultConstructor implements IDao {
        public NoDefaultConstructor(int value) {}
        public double getValue() { return 0; }
    }
    public static class NoSetter implements IMetier {
        public double calcul() { return 0; }
    }
    @Test public void loadsConfiguredComponentsAndCalculates() throws Exception {
        IMetier metier = Presentation2.chargerMetier(config("dao.DaoImpl\nmetier.MetierImpl\n"));
        assertEquals(200, metier.calcul(), 0);
    }
    @Test public void swapsImplementationThroughConfiguration() throws Exception {
        IMetier metier = Presentation2.chargerMetier(
                config(AlternativeDao.class.getName() + "\nmetier.MetierImpl\n"));
        assertEquals(42, metier.calcul(), 0);
    }
    @Test public void injectsInheritedInterfaceImplementation() throws Exception {
        IMetier metier = Presentation2.chargerMetier(
                config(InheritedDao.class.getName() + "\nmetier.MetierImpl\n"));
        assertEquals(42, metier.calcul(), 0);
    }
    @Test public void rejectsMissingConfiguration() {
        assertThrows(IOException.class, () -> Presentation2.loadClassNames(
                folder.getRoot().toPath().resolve("missing.txt").toString()));
    }
    @Test public void rejectsIncompleteBlankAndExtraEntries() throws Exception {
        for (String text : new String[]{"", "dao.DaoImpl\n", "\nmetier.MetierImpl",
                "dao.DaoImpl\n\n", "dao.DaoImpl\nmetier.MetierImpl\nextra"}) {
            Path path = config(text);
            assertThrows(IOException.class, () -> Presentation2.loadClassNames(path.toString()));
        }
    }
    @Test public void rejectsUnknownClass() {
        assertThrows(ClassNotFoundException.class,
                () -> Presentation2.instantiateClass("dao.UnknownDao", IDao.class));
    }
    @Test public void rejectsWrongContract() {
        assertThrows(IllegalArgumentException.class,
                () -> Presentation2.instantiateClass("java.lang.String", IDao.class));
    }
    @Test public void requiresPublicNoArgumentConstructor() {
        assertThrows(NoSuchMethodException.class,
                () -> Presentation2.instantiateClass(NoDefaultConstructor.class.getName(), IDao.class));
    }
    @Test public void rejectsMissingSetter() throws Exception {
        Path path = config("dao.DaoImpl\n" + NoSetter.class.getName());
        assertThrows(NoSuchMethodException.class, () -> Presentation2.chargerMetier(path));
    }
    @Test public void businessRequiresInjectionAndSupportsStub() {
        MetierImpl metier = new MetierImpl();
        assertThrows(IllegalStateException.class, metier::calcul);
        metier.setDao(() -> 12.5);
        assertEquals(25, metier.calcul(), 0);
    }
}
