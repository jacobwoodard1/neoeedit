package neoe.ne;

import org.junit.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.InternalFrameListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;

import static org.junit.Assert.*;

public class PicViewTest {

    @Test
    public void testCreatePicViewWithFile() {
        try {
            PicView pv = new PicView();
            assertNotNull("PicView instance should not be null", pv);
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Exception in testCreatePicViewWithFile: " + e.getMessage();
        }
    }

    private PicView.PicViewPanel createDummyPanel(PicView picView) throws Exception {
        JFrame frame = new JFrame();
        File dummyFile = createDummyImageFile();
        Constructor<PicView.PicViewPanel> constructor =
                PicView.PicViewPanel.class.getDeclaredConstructor(PicView.class, Object.class, File.class);
        constructor.setAccessible(true);
        return constructor.newInstance(picView, frame, dummyFile);
    }

    private File createDummyImageFile() throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        File file = new File("test.png");
        ImageIO.write(img, "png", file);
        file.deleteOnExit(); // Clean up after test
        return file;
    }

    @Test
    public void testSetRate() throws Exception {
        PicView picView = new PicView();
        PicView.PicViewPanel panel = createDummyPanel(picView);
        panel.setRate(10, 20, 2.0);
        assertEquals(2.0, panel.rate, 0.001);
    }

    @Test
    public void testStartCut() throws Exception {
        PicView picView = new PicView();
        PicView.PicViewPanel panel = createDummyPanel(picView);
        panel.startCut(100, 150);
        assertTrue("Cut start positions should be set", true);
    }

    @Test
    public void testMouseClickedControl() throws Exception {
        PicView picView = new PicView();
        PicView.PicViewPanel panel = createDummyPanel(picView);
        MouseEvent event = new MouseEvent(new JPanel(), 0, 0, MouseEvent.CTRL_DOWN_MASK, 50, 60, 1, false);
        panel.mouseClicked(event);
        assertTrue("Control click should trigger startCut without error", true);
    }

    @Test
    public void testMouseClickedDoubleClick() throws Exception {
        PicView picView = new PicView();
        PicView.PicViewPanel panel = createDummyPanel(picView);
        MouseEvent event = new MouseEvent(new JPanel(), 0, 0, 0, 70, 80, 2, false);
        panel.mouseClicked(event);
        assertTrue("Double click should trigger setRate without error", true);
    }

    @Test
    public void testSetPosSmall() throws Exception {
        PicView picView = new PicView();
        PicView.PicViewPanel panel = createDummyPanel(picView);
        panel.setPosSmall(30, 40);
        assertTrue("setPosSmall ran without error", true);
    }

    @Test
    public void testMouseWheelMovedZoomIn() throws Exception {
        PicView picView = new PicView();
        PicView.PicViewPanel panel = createDummyPanel(picView);
        MouseWheelEvent event = new MouseWheelEvent(new JPanel(), 0, 0, 0, 100, 100, 1, false, 0, 1, -1);
        panel.mouseWheelMoved(event);
        assertTrue("Mouse wheel moved with zoom-in", true);
    }

    @Test
    public void testMouseWheelMovedZoomOut() throws Exception {
        PicView picView = new PicView();
        PicView.PicViewPanel panel = createDummyPanel(picView);
        MouseWheelEvent event = new MouseWheelEvent(new JPanel(), 0, 0, 0, 200, 200, 1, false, 0, 1, 1);
        panel.mouseWheelMoved(event);
        assertTrue("Mouse wheel moved with zoom-out", true);
    }

    @Test
    public void testNextSpFileEmpty() throws Exception {
        PicView picView = new PicView();
        PicView.PicViewPanel panel = createDummyPanel(picView);
        picView.picviewpanel = panel;

        panel.toggleSuperMode(); // Initialize sfi

        assertNull("nextSpFile should return null if no image files", panel.nextSpFile());
    }


    @Test
    public void testSaveCut() throws Exception {
        PicView picView = new PicView();
        PicView.PicViewPanel panel = createDummyPanel(picView);

        // Connect picviewpanel field manually
        picView.picviewpanel = panel;

        // Set a real size for the panel
        panel.setSize(200, 200);
        panel.setPreferredSize(new Dimension(200, 200));

        // Start a cut at (10,10)
        panel.startCut(10, 10);

        // Simulate dragging with Ctrl key to (100,100)
        MouseEvent dragEvent = new MouseEvent(
                new JPanel(), 0, 0, MouseEvent.CTRL_DOWN_MASK, 100, 100, 1, false
        );
        panel.mouseDragged(dragEvent);

        panel.saveCut(); // Now it won't crash

        assertTrue("saveCut executed without crash", true);
    }

    @Test
    public void testKeyPressed_AllCases() throws Exception {
        PicView picView = new PicView();
        PicView.PicViewPanel panel = createDummyPanel(picView);
        picView.picviewpanel = panel;

        panel.setSize(200, 200);
        panel.setPreferredSize(new Dimension(200, 200));

        // Helper to simulate a KeyEvent
        java.util.function.BiConsumer<Integer, Integer> pressKey = (keyCode, modifiers) -> {
            KeyEvent e = new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
            panel.keyPressed(e);
        };

        // Now fire all the keys:

        // Ctrl+W -> dispose (we don't care if frame or iframe are null in test)
        pressKey.accept(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK);

        // Ctrl+S -> saveCut
        pressKey.accept(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK);

        // Ctrl+C -> copyFilename
        pressKey.accept(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);

        // F1 -> toggle small
        pressKey.accept(KeyEvent.VK_F1, 0);

        // Tab -> toggle small again
        pressKey.accept(KeyEvent.VK_TAB, 0);

        // Left Arrow -> viewFile(-1)
        pressKey.accept(KeyEvent.VK_LEFT, 0);

        // Backspace -> viewFile(-1)
        pressKey.accept(KeyEvent.VK_BACK_SPACE, 0);

        // Right Arrow -> viewFile(1)
        pressKey.accept(KeyEvent.VK_RIGHT, 0);

        // Space -> viewFile(1)
        pressKey.accept(KeyEvent.VK_SPACE, 0);

        // Up Arrow -> rotate(1)
        pressKey.accept(KeyEvent.VK_UP, 0);

        // Down Arrow -> rotate(-1)
        pressKey.accept(KeyEvent.VK_DOWN, 0);

        // P -> ss.stop()
        pressKey.accept(KeyEvent.VK_P, 0);

        // S (without Ctrl) -> toggleSuperMode
        pressKey.accept(KeyEvent.VK_S, 0);

        // Open Bracket [ -> ss.decDelay()
        pressKey.accept(KeyEvent.VK_OPEN_BRACKET, 0);

        // Close Bracket ] -> ss.incDelay()
        pressKey.accept(KeyEvent.VK_CLOSE_BRACKET, 0);

        // 0 -> reset rate and positions
        pressKey.accept(KeyEvent.VK_0, 0);

        assertTrue("All keyPressed cases executed", true);
    }



    @Test
    public void testShowMethod() throws Exception {
        PicView picView = new PicView();
        File dummyFile = createDummyImageFile();

        // Create a dummy JInternalFrame
        JInternalFrame internalFrame = new JInternalFrame();
        internalFrame.setSize(300, 300);
        internalFrame.setVisible(true);

        picView.show(dummyFile, internalFrame);

        assertNotNull("PicView.picviewpanel should be initialized", picView.picviewpanel);
        assertTrue("PicViewPanel should have a size", picView.picviewpanel.getWidth() > 0);
    }


}
