package util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;

public class ThemeManager {
    private static boolean isDarkMode = false;

    // INPUT: None -> ACTION: Applies initial light theme settings -> OUTPUT: None
    public static void initTheme() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf theme. Reverting to system default.");
        }
    }

    // INPUT: Component rootComponent -> ACTION: Toggles between Light and Dark FlatLaf looks and updates UI -> OUTPUT: None
    public static void toggleTheme(Component rootComponent) {
        try {
            if (isDarkMode) {
                UIManager.setLookAndFeel(new FlatLightLaf());
                isDarkMode = false;
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                isDarkMode = true;
            }
            if (rootComponent != null) {
                SwingUtilities.updateComponentTreeUI(SwingUtilities.getRoot(rootComponent));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // INPUT: None -> ACTION: Reports current theme mode -> OUTPUT: boolean isDarkMode
    public static boolean isDarkMode() {
        return isDarkMode;
    }
}
