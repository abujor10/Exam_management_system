import ui.LoginFrame;
import util.ThemeManager;
import javax.swing.SwingUtilities;

public class Main {
    // INPUT: String[] args -> ACTION: Initializes look & feel and launches the main LoginFrame -> OUTPUT: Application Entry Point
    public static void main(String[] args) {
        ThemeManager.initTheme();
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}